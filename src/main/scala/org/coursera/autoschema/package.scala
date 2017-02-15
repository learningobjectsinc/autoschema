package org.coursera

import sun.reflect._

import scala.reflect.api.{Mirror, Universe}

package object autoschema {
  import reflect.runtime.{universe => ru}

  @CallerSensitive
  def mkTag(tpe: ru.Type): ru.TypeTag[Any] = {
    val depth = new Throwable().getStackTrace.toStream
      .indexWhere(elt => !(elt.getClassName startsWith "org.coursera.autoschema."))
    val cl = Reflection.getCallerClass(depth).getClassLoader
    SynTypeTag(tpe, ru.runtimeMirror(cl))
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
