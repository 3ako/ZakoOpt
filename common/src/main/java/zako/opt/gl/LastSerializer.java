package zako.opt.gl;

import net.caffeinemc.mods.sodium.api.vertex.serializer.VertexSerializer;

// outside the mixin package: a nested class there crashes Fabric Loader 0.18.x the moment it is touched
public record LastSerializer(long key, VertexSerializer serializer) {
}
