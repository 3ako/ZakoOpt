package zako.opt.feature.blocks;

import zako.opt.MixinPlugin;
import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Features;

@FeatureData(
	key = "block_entity_cache",
	group = FeatureGroup.BLOCKS,
	mixins = {"block.ChestRendererMixin", "block.SodiumChestMeshMixin"},
	bench = BenchView.BLOCKS
)
public class BlockEntityCacheFeature extends Feature {
	private static final boolean MESH = MixinPlugin.applies("block.SodiumChestMeshMixin");

	// shut chests drawn in Sodium's chunk mesh; a change needs the chunks meshed again
	public boolean chestMesh;

	@Override
	public void refresh() {
		super.refresh();
		boolean was = chestMesh;
		chestMesh = on && MESH && flag("block_entity_cache.chest_mesh", value) && !Features.shaders();
		if (was != chestMesh) {
			Features.terrainChanged = true;
		}
	}
}
