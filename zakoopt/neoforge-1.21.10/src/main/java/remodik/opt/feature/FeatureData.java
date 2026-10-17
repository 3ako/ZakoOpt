package remodik.opt.feature;

import remodik.opt.bench.BenchView;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// everything about a switch that is not its code: where it shows, how it is stored and overridden, what the benchmark does with it
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FeatureData {
	// option and translation key (zakoopt.option.<key>); -Dzakoopt.<key> overrides the setting
	String key();

	FeatureGroup group();

	// changes what the player sees: the settings screen marks it, zakoopt.visual.<key> says what changes
	boolean visual() default false;

	boolean enabled() default true;

	// field name in zakoopt.json; empty = the key in camelCase
	String config() default "";

	// shown only if one of these mixins is built for this version and applied; none = always
	String[] mixins() default {};

	// the benchmark view it is measured on
	BenchView bench() default BenchView.NONE;
}
