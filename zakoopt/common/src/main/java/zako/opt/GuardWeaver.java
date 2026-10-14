package zako.opt;

import lombok.experimental.UtilityClass;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
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
	private final String OPERATION_OWNER = "com/llamalad7/mixinextras/injector/wrapoperation/Operation";
	private final String OPERATION = "L" + OPERATION_OWNER + ";";
	// postApply runs once per mixin, and several of ours may share a target class and a handler name
	private final Set<String> WRAPPED = ConcurrentHashMap.newKeySet();

	public void weave(ClassNode target, String mixinClass, IMixinInfo mixin) {
		// entity and particle handlers run thousands of times a frame: guarded they cost 5% FPS with mobs, measured
		if (mixinClass.contains(".mixin.entity.") || mixinClass.contains(".mixin.particle.")) {
			return;
		}
		Set<String> own = mixin.getClassNode(0).methods.stream().map(m -> m.name).filter(n -> n.startsWith("zakoopt$")).collect(Collectors.toSet());
		for (MethodNode m : List.copyOf(target.methods)) {
			int at = m.name.indexOf("zakoopt$");
			String merged = mergedFrom(m);
			if (at < 0 || !own.contains(m.name.substring(at)) || merged != null && !merged.equals(mixinClass)
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
		String called = markOriginalCalls(c, m, itf);
		Type ret = Type.getReturnType(m.desc);
		Type[] args = Type.getArgumentTypes(m.desc);
		int[] slots = new int[args.length];
		int slot = isStatic ? 0 : 1, operation = -1;
		for (int i = 0; i < args.length; i++) {
			slots[i] = slot;
			slot += args[i].getSize();
			if (operation < 0 && args[i].getDescriptor().equals(OPERATION)) {
				operation = i;
			}
		}
		int access = ACC_PRIVATE | ACC_SYNTHETIC | (isStatic ? ACC_STATIC : 0);
		boolean fallsBack = operation >= 0 || ret.getSort() == Type.VOID || args.length > 0 && args[0].equals(ret);
		if (fallsBack) {
			MethodNode fallback = new MethodNode(access, m.name + "$fallback", m.desc, null, null);
			fallback.instructions = fallback(args, slots, ret, operation);
			fallback.maxStack = 8 + operation * 2;
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
		// the game or another mod threw inside our original call: rethrown untouched, so it is neither blamed on us nor run twice
		LabelNode ours = new LabelNode();
		w.add(new InsnNode(DUP));
		w.add(new MethodInsnNode(INVOKESTATIC, "zako/opt/Guard", "isForeign", "(Ljava/lang/Throwable;)Z", false));
		w.add(new JumpInsnNode(IFEQ, ours));
		w.add(new InsnNode(ATHROW));
		w.add(ours);
		w.add(new InsnNode(DUP));
		w.add(new LdcInsnNode(mixinClass));
		w.add(new LdcInsnNode(name));
		w.add(new MethodInsnNode(INVOKESTATIC, "zako/opt/Guard", "fail", "(Ljava/lang/Throwable;Ljava/lang/String;Ljava/lang/String;)V", false));
		if (fallsBack) {
			if (called != null) {
				// ours threw after the original already ran: running it again would do the game's work twice, so a void handler
				// is done and one that returns a value cannot go on without it
				LabelNode fresh = new LabelNode();
				w.add(new FieldInsnNode(GETSTATIC, c.name, called, "Z"));
				w.add(new JumpInsnNode(IFEQ, fresh));
				if (ret.getSort() == Type.VOID) {
					w.add(new InsnNode(POP));
					w.add(new InsnNode(RETURN));
				} else {
					w.add(new InsnNode(ATHROW));
				}
				w.add(fresh);
			}
			w.add(new InsnNode(POP));
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

	// whatever escapes an Operation.call is the game's or another mod's: marked on the way out, so the catch around the handler lets it
	// through instead of turning our feature off and running the original a second time; an Operation handed to another method is not
	// seen. A flag set after each call tells the catch the original already ran; returns its field, null when there is none
	private String markOriginalCalls(ClassNode c, MethodNode m, boolean itf) {
		List<MethodInsnNode> calls = new ArrayList<>();
		for (AbstractInsnNode insn : m.instructions) {
			if (insn instanceof MethodInsnNode call && call.getOpcode() == INVOKEINTERFACE && call.name.equals("call") && call.owner.equals(OPERATION_OWNER)) {
				calls.add(call);
			}
		}
		if (calls.isEmpty()) {
			return null;
		}
		// an interface can hold no such field
		String called = itf ? null : m.name + "$called";
		if (called != null) {
			c.fields.add(new FieldNode(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, called, "Z", null, null));
			InsnList reset = new InsnList();
			reset.add(new InsnNode(ICONST_0));
			reset.add(new FieldInsnNode(PUTSTATIC, c.name, called, "Z"));
			m.instructions.insert(reset);
		}
		List<TryCatchBlockNode> marks = new ArrayList<>();
		InsnList handlers = new InsnList();
		for (MethodInsnNode call : calls) {
			LabelNode start = new LabelNode(), end = new LabelNode(), handler = new LabelNode();
			m.instructions.insertBefore(call, start);
			InsnList after = new InsnList();
			after.add(end);
			if (called != null) {
				after.add(new InsnNode(ICONST_1));
				after.add(new FieldInsnNode(PUTSTATIC, c.name, called, "Z"));
			}
			m.instructions.insert(call, after);
			handlers.add(handler);
			handlers.add(new MethodInsnNode(INVOKESTATIC, "zako/opt/Guard", "foreign", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;", false));
			handlers.add(new InsnNode(ATHROW));
			marks.add(new TryCatchBlockNode(start, end, handler, null));
		}
		m.instructions.add(handlers);
		// ahead of the handler's own try blocks, so a try/finally around the call still gets the marked exception
		m.tryCatchBlocks.addAll(0, marks);
		return called;
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
	private InsnList fallback(Type[] args, int[] slots, Type ret, int operation) {
		InsnList h = new InsnList();
		if (operation >= 0) {
			h.add(new VarInsnNode(ALOAD, slots[operation]));
			h.add(new IntInsnNode(SIPUSH, operation));
			h.add(new TypeInsnNode(ANEWARRAY, "java/lang/Object"));
			for (int i = 0; i < operation; i++) {
				h.add(new InsnNode(DUP));
				h.add(new IntInsnNode(SIPUSH, i));
				h.add(new VarInsnNode(args[i].getOpcode(ILOAD), slots[i]));
				box(h, args[i]);
				h.add(new InsnNode(AASTORE));
			}
			h.add(new MethodInsnNode(INVOKEINTERFACE, "com/llamalad7/mixinextras/injector/wrapoperation/Operation", "call", "([Ljava/lang/Object;)Ljava/lang/Object;", true));
			unbox(h, ret);
		} else if (ret.getSort() != Type.VOID) {
			h.add(new VarInsnNode(args[0].getOpcode(ILOAD), slots[0]));
		}
		h.add(new InsnNode(ret.getOpcode(IRETURN)));
		return h;
	}

	private void box(InsnList h, Type t) {
		String owner = boxed(t);
		if (owner != null) {
			h.add(new MethodInsnNode(INVOKESTATIC, owner, "valueOf", "(" + t.getDescriptor() + ")L" + owner + ";", false));
		}
	}

	private void unbox(InsnList h, Type t) {
		String owner = boxed(t);
		if (t.getSort() == Type.VOID) {
			h.add(new InsnNode(POP));
		} else if (owner != null) {
			h.add(new TypeInsnNode(CHECKCAST, owner));
			h.add(new MethodInsnNode(INVOKEVIRTUAL, owner, t.getClassName() + "Value", "()" + t.getDescriptor(), false));
		} else {
			h.add(new TypeInsnNode(CHECKCAST, t.getInternalName()));
		}
	}

	private String boxed(Type t) {
		return switch (t.getSort()) {
			case Type.BOOLEAN -> "java/lang/Boolean";
			case Type.BYTE -> "java/lang/Byte";
			case Type.CHAR -> "java/lang/Character";
			case Type.SHORT -> "java/lang/Short";
			case Type.INT -> "java/lang/Integer";
			case Type.FLOAT -> "java/lang/Float";
			case Type.LONG -> "java/lang/Long";
			case Type.DOUBLE -> "java/lang/Double";
			default -> null;
		};
	}
}
