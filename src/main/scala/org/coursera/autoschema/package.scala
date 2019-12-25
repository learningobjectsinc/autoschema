package org.coursera

import scala.reflect.api.{Mirror, Universe}

import scala.compat.java8.StreamConverters._

package object autoschema {
  import reflect.runtime.{universe => ru}

  // @CallerSensitive
  def mkTag(tpe: ru.Type): ru.TypeTag[Any] = {
    val cls = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
      .walk(_.toScala[Stream].map(_.getDeclaringClass).filterNot(_.getName startsWith "org.coursera.autoschema.").head): Class[_]
    SynTypeTag(tpe, ru.runtimeMirror(cls.getClassLoader))
  }

  def rec(tag: ru.TypeTag[Any], tpe: ru.Type): ru.TypeTag[Any] =
    SynTypeTag(tpe, tag.mirror)

  private[this] case class SynTypeTag(tpe: ru.Type, mirror: ru.Mirror) extends ru.TypeTag[Any] {
    def in[U <: Universe with Singleton](other: Mirror[U]): U#TypeTag[Any] =
      if (other.universe ne mirror.universe)
        throw ScalaReflectionException("Unexpected multiple runtime universes!")
      else SynTypeTag(tpe, other.asInstanceOf[ru.Mirror]).asInstanceOf[U#TypeTag[Any]]
  }

}
