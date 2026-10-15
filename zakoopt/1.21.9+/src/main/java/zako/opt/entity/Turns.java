package zako.opt.entity;

import zako.opt.feature.Features;

import com.mojang.math.Axis;
import lombok.experimental.UtilityClass;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

// Axis.rotationDegrees makes a quaternion that mulPose multiplies in at once and drops: for call sites that do exactly that,
// one reused quaternion. Render thread only, and only between the call and its mulPose
@UtilityClass
public class Turns {
	private final Quaternionf TURN = new Quaternionf();

	// null: not one of the six plain axes, or the feature is off; the caller makes its own
	public Quaternionf degrees(Axis axis, float degrees) {
		if (!Features.FEWER_ALLOCATIONS.on) {
			return null;
		}
		float radians = degrees * Mth.DEG_TO_RAD;
		if (axis == Axis.YP) {
			return TURN.rotationY(radians);
		}
		if (axis == Axis.XP) {
			return TURN.rotationX(radians);
		}
		if (axis == Axis.ZP) {
			return TURN.rotationZ(radians);
		}
		if (axis == Axis.YN) {
			return TURN.rotationY(-radians);
		}
		if (axis == Axis.XN) {
			return TURN.rotationX(-radians);
		}
		if (axis == Axis.ZN) {
			return TURN.rotationZ(-radians);
		}
		return null;
	}
}
