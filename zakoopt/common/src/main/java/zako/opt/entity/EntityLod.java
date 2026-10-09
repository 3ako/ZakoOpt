package zako.opt.entity;

import lombok.experimental.UtilityClass;
import zako.opt.ZakoOptConfig;

@UtilityClass
public class EntityLod {

	public boolean far(double distanceSq) {
		if (!ZakoOptConfig.entityLod()) {
			return false;
		}
		double d = ZakoOptConfig.entityLodDistance();
		return distanceSq > d * d;
	}

	public boolean farPlayer(double distanceSq) {
		return ZakoOptConfig.playerLod() && far(distanceSq);
	}
}
