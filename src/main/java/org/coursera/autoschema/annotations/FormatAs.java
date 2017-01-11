package org.coursera.autoschema.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Lets you manually set the type and format to be used when generating schema for the annotated type
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface FormatAs {
 /** The type to be used when generating the schema */
    String value();

 /** Optional: How the type should be viewed */
    String format() default "";
}
