package remodik.opt.feature;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// a feature's own extra setting, shown right under the feature; how it shows follows the field's type: a boolean is a switch,
// an int a slider over its @Range
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Setting {
	String key();

	// field name in zakoopt.json; empty = the key in camelCase
	String config() default "";
}
