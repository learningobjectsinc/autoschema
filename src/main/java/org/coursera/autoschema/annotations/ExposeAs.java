package org.coursera.autoschema.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Lets you set the schema for one type to be the schema of another type.
 * Ignored if FormatAs is present.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExposeAs {
    /** The type whose schema shall be used */
    Class<?> value();
}
