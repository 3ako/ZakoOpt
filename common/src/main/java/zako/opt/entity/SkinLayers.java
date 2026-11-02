package zako.opt.entity;

import lombok.experimental.UtilityClass;
import net.minecraft.client.model.geom.ModelPart;
import zako.opt.Platform;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

// 3D Skin Layers poses its voxel mesh into the shared model's parts in setupAnim (the outer skin layer, a player head's hat)
// and draws it from ModelPart.render instead of the part's cubes. The parallel builder copies cubes and never calls
// ModelPart.render, so a model carrying such a mesh goes the vanilla way
@UtilityClass
public class SkinLayers {
	// ModelPartInjector.getInjectedMesh, typed (ModelPart) -> Object; null without the mod, so the check folds away
	private final MethodHandle MESH = find();
	public final boolean ACTIVE = MESH != null;

	// render thread, right after setupAnim
	public boolean meshed(ModelPart part) {
		try {
			return (Object) MESH.invokeExact(part) != null;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private MethodHandle find() {
		if (!Platform.isModLoaded("skinlayers3d")) {
			return null;
		}
		try {
			Class<?> injector = Class.forName("dev.tr7zw.skinlayers.accessor.ModelPartInjector");
			MethodHandle mesh = MethodHandles.publicLookup().findVirtual(injector, "getInjectedMesh",
					MethodType.methodType(Class.forName("dev.tr7zw.skinlayers.api.Mesh")));
			return mesh.asType(MethodType.methodType(Object.class, ModelPart.class));
		} catch (ReflectiveOperationException e) {
			// renamed in some other version of the mod: the meshes cannot be seen, so no model takes the parallel path
			return MethodHandles.dropArguments(MethodHandles.constant(Object.class, Boolean.TRUE), 0, ModelPart.class);
		}
	}
}
