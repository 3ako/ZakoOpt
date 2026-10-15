package zako.opt;

import lombok.experimental.UtilityClass;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// each of our handlers merged into a game class gets a catch that turns its feature off and does what the game would have done
@UtilityClass
public class GuardWeaver implements Opcodes {
	private final String OPERATION = "Lcom/llamalad7/mixinextras/injector/wrapoperation/Operation;";
	// postApply runs once per mixin, and several of ours may share a target class and a handler name
	private final Set<String> WRAPPED = ConcurrentHashMap.newKeySet();
	// tiny hooks run per block entity or per buffer every frame: guarded, the handler is too big to inline and its CallbackInfo
	// is allocated on every call (earlyCull alone was 3% of the render thread's garbage on a farm); unguarded the JIT drops it
	private final Set<String> HOT = Set.of("zakoopt$earlyCull", "zakoopt$renderType", "zakoopt$release");

	public void weave(ClassNode target, String mixinClass, IMixinInfo mixin) {
		// entity and particle handlers run thousands of times a frame: guarded they cost 5% FPS with mobs, measured
		if (mixinClass.contains(".mixin.entity.") || mixinClass.contains(".mixin.particle.")) {
			return;
		}
		Set<String> own = mixin.getClassNode(0).methods.stream().map(m -> m.name).filter(n -> n.startsWith("zakoopt$")).collect(Collectors.toSet());
		for (MethodNode m : List.copyOf(target.methods)) {
			int at = m.name.indexOf("zakoopt$");
			String merged = mergedFrom(m);
			// a handler around an original call (@WrapOperation) is left as is: the catch would push it past the JIT's inlining limits,
			// and running the original again after ours threw could do the game's work twice, so an error there crashes plainly
			if (at < 0 || !own.contains(m.name.substring(at)) || HOT.contains(m.name.substring(at)) || merged != null && !merged.equals(mixinClass) || m.desc.contains(OPERATION)
					|| (m.access & (ACC_ABSTRACT | ACC_NATIVE)) != 0 || m.instructions.size() == 0 || !WRAPPED.add(target.name + "." + m.name + m.desc)) {
				continue;
			}
			wrap(target, m, mixinClass, m.name.substring(at), sugared(m));
		}
	}

	private String mergedFrom(MethodNode m) {
		if (m.invisibleAnnotations != null) {
			for (AnnotationNode a : m.invisibleAnnotations) {
				if (a.desc.equals("Lorg/spongepowered/asm/mixin/transformer/meta/MixinMerged;")) {
					for (int i = 0; i + 1 < a.values.size(); i += 2) {
						if ("mixin".equals(a.values.get(i))) {
							return (String) a.values.get(i + 1);
						}
					}
				}
			}
		}
		return null;
	}

	// MixinExtras strips @Local and the like from the descriptor before we run and puts them back after: a slot past the
	// parameters read before anything is stored in it is such a hidden parameter, and those handlers keep their body
	private boolean sugared(MethodNode m) {
		int params = (m.access & ACC_STATIC) != 0 ? 0 : 1;
		for (Type t : Type.getArgumentTypes(m.desc)) {
			params += t.getSize();
		}
		Set<Integer> stored = new HashSet<>();
		for (AbstractInsnNode insn : m.instructions) {
			if (insn instanceof VarInsnNode v && v.var >= params) {
				boolean load = v.getOpcode() >= ILOAD && v.getOpcode() <= ALOAD;
				if (load && !stored.contains(v.var)) {
					return true;
				}
				stored.add(v.var);
			} else if (insn instanceof IincInsnNode i && i.var >= params && !stored.contains(i.var)) {
				return true;
			}
		}
		return false;
	}

	// a plain handler becomes a tiny wrapper around its unchanged body and a sugared one keeps its body with a short catch; the
	// fallback is a cold method either way, since catch code in the handler would pass the JIT's inlining size limits
	private void wrap(ClassNode c, MethodNode m, String mixinClass, String name, boolean inPlace) {
		boolean isStatic = (m.access & ACC_STATIC) != 0, itf = (c.access & ACC_INTERFACE) != 0;
		Type ret = Type.getReturnType(m.desc);
		Type[] args = Type.getArgumentTypes(m.desc);
		int[] slots = new int[args.length];
		int slot = isStatic ? 0 : 1;
		for (int i = 0; i < args.length; i++) {
			slots[i] = slot;
			slot += args[i].getSize();
		}
		int access = ACC_PRIVATE | ACC_SYNTHETIC | (isStatic ? ACC_STATIC : 0);
		boolean fallsBack = ret.getSort() == Type.VOID || args.length > 0 && args[0].equals(ret);
		if (fallsBack) {
			MethodNode fallback = new MethodNode(access, m.name + "$fallback", m.desc, null, null);
			fallback.instructions = fallback(args, slots, ret);
			fallback.maxStack = 2;
			fallback.maxLocals = slot;
			c.methods.add(fallback);
		}
		LabelNode start = new LabelNode(), end = new LabelNode(), handler = new LabelNode();
		InsnList w = new InsnList();
		if (inPlace) {
			m.instructions.insert(start);
			m.instructions.add(end);
		} else {
			MethodNode body = new MethodNode(access, m.name + "$body", m.desc, null, m.exceptions.toArray(new String[0]));
			body.instructions = m.instructions;
			body.tryCatchBlocks = m.tryCatchBlocks;
			body.localVariables = m.localVariables;
			body.visibleLocalVariableAnnotations = m.visibleLocalVariableAnnotations;
			body.invisibleLocalVariableAnnotations = m.invisibleLocalVariableAnnotations;
			body.maxStack = m.maxStack;
			body.maxLocals = m.maxLocals;
			c.methods.add(body);
			m.instructions = new InsnList();
			m.tryCatchBlocks = new ArrayList<>();
			m.localVariables = null;
			m.visibleLocalVariableAnnotations = null;
			m.invisibleLocalVariableAnnotations = null;
			w.add(start);
			call(w, c, body.name, m.desc, isStatic, itf, args, slots);
			w.add(new InsnNode(ret.getOpcode(IRETURN)));
			w.add(end);
		}
		w.add(handler);
		if (!fallsBack) {
			w.add(new InsnNode(DUP));
		}
		w.add(new LdcInsnNode(mixinClass));
		w.add(new LdcInsnNode(name));
		w.add(new MethodInsnNode(INVOKESTATIC, "zako/opt/Guard", "fail", "(Ljava/lang/Throwable;Ljava/lang/String;Ljava/lang/String;)V", false));
		if (fallsBack) {
			call(w, c, m.name + "$fallback", m.desc, isStatic, itf, args, slots);
			w.add(new InsnNode(ret.getOpcode(IRETURN)));
		} else {
			w.add(new InsnNode(ATHROW));
		}
		m.instructions.add(w);
		m.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, "java/lang/RuntimeException"));
		m.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, "java/lang/LinkageError"));
		m.maxStack = Math.max(m.maxStack, slot + 4);
		m.maxLocals = Math.max(m.maxLocals, slot);
	}

	private void call(InsnList w, ClassNode c, String method, String desc, boolean isStatic, boolean itf, Type[] args, int[] slots) {
		if (!isStatic) {
			w.add(new VarInsnNode(ALOAD, 0));
		}
		for (int i = 0; i < args.length; i++) {
			w.add(new VarInsnNode(args[i].getOpcode(ILOAD), slots[i]));
		}
		w.add(new MethodInsnNode(isStatic ? INVOKESTATIC : INVOKESPECIAL, c.name, method, desc, itf));
	}

	// what the game would have done without the handler
	private InsnList fallback(Type[] args, int[] slots, Type ret) {
		InsnList h = new InsnList();
		if (ret.getSort() != Type.VOID) {
			h.add(new VarInsnNode(args[0].getOpcode(ILOAD), slots[0]));
		}
		h.add(new InsnNode(ret.getOpcode(IRETURN)));
		return h;
	}
}
