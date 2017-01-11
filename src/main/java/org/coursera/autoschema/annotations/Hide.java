package org.coursera.autoschema.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Hides the annotated field in the generated schema
 * @example
 * {{{
 *      case class MyType(@Term.Hide mySecretField: Int)
 * }}}
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME) // sad
public @interface Hide {
}
