package zako.opt.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix3f;
import zako.opt.feature.Features;
import zako.opt.mixin.block.CreateFrustumAccessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// a factory gauge's arrow path is dozens of 4-vertex sprites, each with its own model lookup, three rotations and vertex loop;
// it is drawn once in the gauge's own space and replayed through the gauge's pose while everything renderPath reads stays the same
@UtilityClass
public class FactoryPaths {
	// by where the path runs, not by the connection object: every block entity packet from the server decodes new connections
	private final Map<Key, Entry> CACHE = new HashMap<>();
	// Create's classes no longer match ours: the original draws from then on
	private boolean off;
	public boolean recording;

	private record Key(FactoryPanelPosition to, FactoryPanelPosition from) {
	}

	private static final class Entry {
		Level level;
		BlockState state;
		Object slot;
		Direction[] path = new Direction[0];
		int flags, color, light, overlay;
		float glow;
		List<SpawnerMeshCache.Segment> mesh;
		// the mesh's box in the gauge's own space, for the view test
		float minX, minY, minZ, maxX, maxY, maxZ;

		void bounds() {
			minX = minY = minZ = Float.MAX_VALUE;
			maxX = maxY = maxZ = -Float.MAX_VALUE;
			for (SpawnerMeshCache.Segment segment : mesh) {
				float[] p = segment.positions();
				for (int i = 0; i < p.length; i += 3) {
					minX = Math.min(minX, p[i]);
					minY = Math.min(minY, p[i + 1]);
					minZ = Math.min(minZ, p[i + 2]);
					maxX = Math.max(maxX, p[i]);
					maxY = Math.max(maxY, p[i + 1]);
					maxZ = Math.max(maxZ, p[i + 2]);
				}
			}
		}

		boolean same(Level level, BlockState state, Object slot, List<Direction> path, int flags, int color, float glow, int light, int overlay) {
			if (mesh == null || level != this.level || state != this.state || slot != this.slot || flags != this.flags || color != this.color
					|| Float.floatToIntBits(glow) != Float.floatToIntBits(this.glow) || light != this.light || overlay != this.overlay || path.size() != this.path.length) {
				return false;
			}
			for (int i = 0; i < this.path.length; i++) {
				if (path.get(i) != this.path[i]) {
					return false;
				}
			}
			return true;
		}
	}

	// false: draw it the original way
	public boolean render(FactoryPanelBehaviour behaviour, FactoryPanelConnection connection, float partialTicks, PoseStack ms, MultiBufferSource buffer,
						  int light, int overlay, Consumer<MultiBufferSource> draw) {
		// the sprites' diffuse shade is baked from their own normals: only a pose that does not rotate them may replay it
		Matrix3f n = ms.last().normal();
		if (off || n.m00() != 1.0F || n.m11() != 1.0F || n.m22() != 1.0F) {
			return false;
		}
		try {
			Level level = behaviour.getWorld();
			BlockState state = behaviour.blockEntity.getBlockState();
			List<Direction> path = connection.getPath(level, state, behaviour.getPanelPosition());
			FactoryPanelSupportBehaviour link = FactoryPanelBehaviour.linkAt(level, connection);
			int flags = link == null ? 0 : link.blockEntity instanceof DisplayLinkBlockEntity ? 1 : link.blockEntity instanceof RedstoneLinkBlockEntity ? 2 : 3;
			flags |= (link != null && link.isOutput() ? 4 : 0) | (behaviour.count == 0 ? 8 : 0) | (behaviour.satisfied ? 16 : 0) | (behaviour.promisedSatisfied ? 32 : 0)
					| (behaviour.redstonePowered ? 64 : 0) | (behaviour.waitingForNetwork ? 128 : 0) | (behaviour.isMissingAddress() ? 256 : 0)
					| (connection.success ? 512 : 0) | (Features.shaders() ? 1024 : 0);
			int color = behaviour.getIngredientStatusColor();
			float glow = behaviour.bulb.getValue(partialTicks);
			// ponytail: wholesale clear instead of LRU, the panels in view refill it within a frame
			if (CACHE.size() >= 8192) {
				CACHE.values().stream().filter(v -> v.mesh != null).forEach(v -> SpawnerMeshCache.free(v.mesh));
				CACHE.clear();
			}
			Entry e = CACHE.computeIfAbsent(new Key(behaviour.getPanelPosition(), connection.from), k -> new Entry());
			if (!e.same(level, state, behaviour.slot, path, flags, color, glow, light, overlay)) {
				e.level = level;
				e.state = state;
				e.slot = behaviour.slot;
				e.path = path.toArray(new Direction[0]);
				e.flags = flags;
				e.color = color;
				e.glow = glow;
				e.light = light;
				e.overlay = overlay;
				recording = true;
				try {
					if (e.mesh != null) {
						SpawnerMeshCache.free(e.mesh);
						e.mesh = null;
					}
					e.mesh = SpawnerMeshCache.record(draw, true);
					e.bounds();
				} finally {
					recording = false;
				}
			}
			// Create draws a gauge whatever its box, as its paths reach far: a path out of view is skipped here, all of it
			Frustum frustum = ((CreateFrustumAccessor) Minecraft.getInstance().levelRenderer).zakoopt$frustum();
			BlockPos at = behaviour.blockEntity.getBlockPos();
			if (frustum != null && !frustum.isVisible(new AABB(at.getX() + e.minX, at.getY() + e.minY, at.getZ() + e.minZ, at.getX() + e.maxX, at.getY() + e.maxY, at.getZ() + e.maxZ))) {
				return true;
			}
			return SpawnerMeshCache.emit(e.mesh, ms.last(), buffer, light);
		} catch (RuntimeException | LinkageError t) {
			off = true;
			throw t;
		}
	}
}
