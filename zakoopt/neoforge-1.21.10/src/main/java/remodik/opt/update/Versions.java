package remodik.opt.update;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Versions {
	// numeric parts before any "+build" or "-pre" suffix: "v1.2.10" > "1.2.9"; a missing part counts as 0
	public boolean newer(String candidate, String current) {
		int[] a = parts(candidate), b = parts(current);
		for (int i = 0; i < Math.max(a.length, b.length); i++) {
			int x = i < a.length ? a[i] : 0, y = i < b.length ? b[i] : 0;
			if (x != y) {
				return x > y;
			}
		}
		return false;
	}

	// patch releases (1.1.1 -> 1.1.2) go out quietly: only a new major.minor is offered
	public boolean offered(String candidate, String current) {
		return newer(release(candidate), release(current));
	}

	private String release(String version) {
		int[] p = parts(version);
		return p[0] + "." + (p.length > 1 ? p[1] : 0);
	}

	private int[] parts(String version) {
		String core = version.replaceFirst("^[vV]", "").split("[+\\-]", 2)[0];
		String[] split = core.split("\\.");
		int[] out = new int[split.length];
		for (int i = 0; i < split.length; i++) {
			String digits = split[i].replaceAll("\\D.*", "");
			out[i] = digits.isEmpty() ? 0 : Integer.parseInt(digits);
		}
		return out;
	}

	public static void main(String[] args) {
		check(newer("v1.0.1", "1.0.0"));
		check(newer("1.2.10", "1.2.9"));
		check(!newer("1.0.0", "1.0.0"));
		check(!newer("1.0.0+1.21.11", "1.0.0"));
		check(newer("1.1", "1.0.9"));
		check(!newer("0.9.9", "1.0.0"));
		check(!newer("garbage", "1.0.0"));
		check(!offered("1.1.2", "1.1.1"));
		check(!offered("v1.1.0", "1.1.5"));
		check(offered("1.2.0", "1.1.9"));
		check(offered("v2.0", "1.9.9"));
		check(offered("1.1.2", "1.0.3"));
		System.out.println("ok");
	}

	private static void check(boolean condition) {
		if (!condition) {
			throw new AssertionError();
		}
	}
}
