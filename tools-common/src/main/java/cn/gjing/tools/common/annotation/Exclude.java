package cn.gjing.tools.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Gjing
 * Parameters that use this annotation do not need to be validated，Use with @NotNull
 **/
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Exclude {
}
