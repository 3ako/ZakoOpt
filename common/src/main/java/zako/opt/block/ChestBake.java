package zako.opt.block;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import org.slf4j.LoggerFactory;
import zako.opt.feature.Features;

import java.util.concurrent.atomic.AtomicBoolean;

// how the last chunk mesh drew this chest; written by the mesher threads, read on ticks
public interface ChestBake {
	byte NONE = 0;
	byte BAKED = 1;
	byte LIVE = 2;
	// a failure inside baking (seen only with some modpacks, #18) turns it off for the session instead of crashing the mesher
	AtomicBoolean FAILED = new AtomicBoolean();

	byte zakoopt$mesh();

	void zakoopt$mesh(byte mesh);

	boolean zakoopt$breaking();

	void zakoopt$breaking(boolean breaking);

	static boolean idle(LidBlockEntity lid) {
		// the crack overlay is drawn by the chest renderer: a chest being broken stays out of the mesh
		return !((ChestBake) lid).zakoopt$breaking() && lid.getOpenNess(0.0F) == 0.0F && lid.getOpenNess(1.0F) == 0.0F;
	}

	static boolean bake(BlockEntity blockEntity, Object renderer, ChunkBuildBuffers buffers, LevelSlice slice) {
		Class<?> type = blockEntity.getClass();
		if (type != ChestBlockEntity.class && type != TrappedChestBlockEntity.class && type != EnderChestBlockEntity.class) {
			return false;
		}
		ChestBake bake = (ChestBake) blockEntity;
		if (!Features.BLOCK_ENTITY_CACHE.chestMesh || FAILED.get() || renderer.getClass() != ChestRenderer.class) {
			bake.zakoopt$mesh(NONE);
			return false;
		}
		if (!idle((LidBlockEntity) blockEntity)) {
			bake.zakoopt$mesh(LIVE);
			return false;
		}
		boolean baked;
		try {
			baked = ChestMesh.bake(blockEntity, renderer, buffers, slice);
		} catch (RuntimeException e) {
			if (!FAILED.getAndSet(true)) {
				LoggerFactory.getLogger("zakoopt").error("Baking chests into the chunk mesh failed, drawing them the vanilla way from now on", e);
			}
			baked = false;
		}
		bake.zakoopt$mesh(baked ? BAKED : NONE);
		return baked;
	}

	static void breaking(BlockPos pos, ChestBake chest, boolean breaking) {
		if (chest.zakoopt$breaking() != breaking) {
			chest.zakoopt$breaking(breaking);
			tick(pos, (LidBlockEntity) chest);
		}
	}

	static void tick(BlockPos pos, LidBlockEntity lid) {
		if (!Features.BLOCK_ENTITY_CACHE.chestMesh) {
			return;
		}
		ChestBake bake = (ChestBake) lid;
		byte mesh = bake.zakoopt$mesh();
		boolean idle = idle(lid);
		if (mesh == BAKED && !idle || mesh == LIVE && idle) {
			bake.zakoopt$mesh(NONE);
			ChestMesh.dirty(pos);
		}
	}
}
