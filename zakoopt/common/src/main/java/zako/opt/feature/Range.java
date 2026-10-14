package zako.opt.feature;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Range {
	int min();

	int max();

	// translation key of the value as shown, %s the number
	String unit() default "zakoopt.blocks";
}
