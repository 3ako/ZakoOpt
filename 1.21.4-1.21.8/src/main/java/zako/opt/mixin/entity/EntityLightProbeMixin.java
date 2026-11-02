package zako.opt.mixin.entity;

import zako.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.entity.LightProbe;

// every entity's light made an eye position and a block position per frame; the eye block goes into one mutable position,
// and the probe returns a marker that tells the next call to hand that position out instead of making one
@Mixin(EntityRenderer.class)
public class EntityLightProbeMixin {
	@Unique
	private static final Vec3 ZAKOOPT$PROBED = new Vec3(0, 0, 0);
	@Unique
	private static final BlockPos.MutableBlockPos ZAKOOPT$PROBE = new BlockPos.MutableBlockPos();

	@WrapOperation(method = "getPackedLightCoords", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getLightProbePosition(F)Lnet/minecraft/world/phys/Vec3;"))
	private Vec3 zakoopt$probe(Entity entity, float partialTick, Operation<Vec3> original) {
		if (!Features.FEWER_ALLOCATIONS.on || !RenderSystem.isOnRenderThread() || !LightProbe.plain(entity)) {
			return original.call(entity, partialTick);
		}
		ZAKOOPT$PROBE.set(Mth.floor(Mth.lerp(partialTick, entity.xo, entity.getX())),
				Mth.floor(Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getEyeHeight()),
				Mth.floor(Mth.lerp(partialTick, entity.zo, entity.getZ())));
		return ZAKOOPT$PROBED;
	}

	@WrapOperation(method = "getPackedLightCoords", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;containing(Lnet/minecraft/core/Position;)Lnet/minecraft/core/BlockPos;"))
	private BlockPos zakoopt$probePos(Position pos, Operation<BlockPos> original) {
		return pos == ZAKOOPT$PROBED ? ZAKOOPT$PROBE : original.call(pos);
	}
}
