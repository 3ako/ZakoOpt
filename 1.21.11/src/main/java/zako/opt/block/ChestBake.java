package zako.opt.block;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import zako.opt.feature.Features;

// how the last chunk mesh drew this chest; written by the mesher threads, read on ticks
public interface ChestBake {
	byte NONE = 0;
	byte BAKED = 1;
	byte LIVE = 2;

	byte zakoopt$mesh();

	void zakoopt$mesh(byte mesh);

	static boolean idle(LidBlockEntity lid) {
		return lid.getOpenNess(0.0F) == 0.0F && lid.getOpenNess(1.0F) == 0.0F;
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
			Minecraft.getInstance().levelRenderer.setSectionDirty(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getY()), SectionPos.blockToSectionCoord(pos.getZ()));
		}
	}
}
