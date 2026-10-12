package zako.opt.entity;

import zako.opt.feature.Features;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EntityLod {

	public boolean far(double distanceSq) {
		if (!Features.ENTITY_LOD.on) {
			return false;
		}
		double d = Features.ENTITY_LOD.distance;
		return distanceSq > d * d;
	}

	public boolean farPlayer(double distanceSq) {
		double d = Features.PLAYER_LOD.distance;
		return Features.ENTITY_LOD.on && Features.PLAYER_LOD.on && distanceSq > d * d;
	}
}
