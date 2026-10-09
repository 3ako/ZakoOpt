package zako.opt.block;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// far spawners spin per tick, so everything their display entity submits is identical for the whole tick:
// record the submit calls on the first frame, replay them (shifted by the camera movement) on the rest
@UtilityClass
public class SpawnerReplay {
	public final double DISTANCE = 6.0;
	private final Map<Object, Recording> CACHE = new IdentityHashMap<>();
	private long cachedTick = Long.MIN_VALUE;
	// every vanilla collector copies the pose on submit, so one stack can carry all replayed poses
	private final PoseStack SCRATCH = new PoseStack();

	private record Call(Method order, Object[] orderArgs, Method method, Object[] args, PoseStack.Pose pose) {
	}

	private final class Recording {
		final List<Call> calls = new ArrayList<>();
		final double x, y, z;
		boolean replayable = true;

		Recording(Vec3 camera) {
			x = camera.x;
			y = camera.y;
			z = camera.z;
		}
	}

	public void submit(Object key, long tick, Vec3 camera, SubmitNodeCollector collector, Consumer<SubmitNodeCollector> body) {
		if (tick != cachedTick) {
			CACHE.clear();
			cachedTick = tick;
		}
		Recording recording = CACHE.get(key);
		if (recording == null) {
			recording = new Recording(camera);
			CACHE.put(key, recording);
			body.accept((SubmitNodeCollector) recorder(SubmitNodeCollector.class, collector, recording, null, -1));
		} else if (!recording.replayable) {
			body.accept(collector);
		} else {
			replay(recording, camera, collector);
		}
	}

	private Object recorder(Class<?> type, Object target, Recording recording, Method order, int orderIndex) {
		return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
			if (method.getDeclaringClass() == Object.class) {
				return method.getName().equals("equals") ? proxy == args[0] : method.invoke(target, args);
			}
			Object result = invoke(method, target, args);
			if (method.getReturnType() == OrderedSubmitNodeCollector.class && order == null) {
				return recorder(OrderedSubmitNodeCollector.class, result, recording, method, (Integer) args[0]);
			}
			if (method.getReturnType() == void.class) {
				record(recording, order, orderIndex, method, args);
			} else {
				recording.replayable = false;
			}
			return result;
		});
	}

	private void record(Recording recording, Method order, int orderIndex, Method method, Object[] args) {
		Object[] stored = args == null ? null : args.clone();
		PoseStack.Pose pose = null;
		for (int i = 0; stored != null && i < stored.length; i++) {
			if (stored[i] instanceof PoseStack stack) {
				if (pose != null) {
					recording.replayable = false;
				}
				pose = stack.last().copy();
				stored[i] = SCRATCH;
			}
		}
		recording.calls.add(new Call(order, order == null ? null : new Object[]{orderIndex}, method, stored, pose));
	}

	private void replay(Recording recording, Vec3 camera, SubmitNodeCollector collector) {
		float dx = (float) (recording.x - camera.x), dy = (float) (recording.y - camera.y), dz = (float) (recording.z - camera.z);
		for (Call call : recording.calls) {
			if (call.pose != null) {
				SCRATCH.last().set(call.pose);
				SCRATCH.last().pose().translateLocal(dx, dy, dz);
			}
			Object target = call.order == null ? collector : invoke(call.order, collector, call.orderArgs);
			invoke(call.method, target, call.args);
		}
	}

	private Object invoke(Method method, Object target, Object[] args) {
		try {
			return method.invoke(target, args);
		} catch (InvocationTargetException e) {
			throw e.getCause() instanceof RuntimeException r ? r : new RuntimeException(e.getCause());
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
