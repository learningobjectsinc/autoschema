/**
 *  Copyright 2014 Coursera Inc. Modifications by Learning Objects, LLC.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.coursera.autoschema

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import org.coursera.autoschema.jackson._

import scala.collection.MapLike
import scala.reflect.runtime.{universe => ru}

/**
 * AutoSchema lets you take any Scala type and create JSON Schema out of it
 * @example
 * {{{
 *      // Pass the type as a type parameter
 *      case class MyType(...)
 *
 *      AutoSchema.createSchema[MyType]
 *
 *
 *      // Or pass the reflection type
 *      case class MyOtherType(...)
 *
 *      AutoSchema.createSchema(ru.typeOf[MyOtherType])
 * }}}
 */
object AutoSchema {
  private type Tag = ru.TypeTag[Any]

  // Hand written schemas for common types
  private[this] def schemaTypeForScala(implicit om: ObjectMapper): PartialFunction[String, JsObject] = {
    case "org.joda.time.DateTime" => JsObject("type" -> "string", "format" -> "date")
    case "java.util.Date" => JsObject("type" -> "string", "format" -> "date")
    case "java.lang.String" => JsObject("type" -> "string")
    case "scala.Boolean" | "java.lang.Boolean" => JsObject("type" -> "boolean")
    case "scala.Int" | "java.lang.Integer" => JsObject("type" -> "number", "format" -> "number")
    case "scala.Long" | "java.lang.Long" => JsObject("type" -> "number", "format" -> "number")
    case "scala.Double" | "java.lang.Double" => JsObject("type" -> "number", "format" -> "number")
    case "java.util.UUID" => JsObject(
      "type" -> "string",
      "pattern" -> "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$"
    )
  }

  private[this] val classSchemaCache = collection.concurrent.TrieMap[String, JsObject]()

  /*_*/
  private[this] def stripTypeAnnots: ru.Type => ru.Type = {
    case ru.AnnotatedType(annots, tpe) => tpe.dealias
    case tpe                           => tpe.dealias
  }
  /*_*/

  private[this] val isHideAnnotation = (annotation: ru.Annotation) =>
    stripTypeAnnots(annotation.tree.tpe.dealias).typeSymbol.fullName == "org.coursera.autoschema.annotations.Hide"

  private[this] val isFormatAnnotation = (annotation: ru.Annotation) =>
    stripTypeAnnots(annotation.tree.tpe.dealias).typeSymbol.fullName == "org.coursera.autoschema.annotations.FormatAs"

  private[this] val isExposeAnnotation = (annotation: ru.Annotation) =>
    stripTypeAnnots(annotation.tree.tpe.dealias).typeSymbol.fullName == "org.coursera.autoschema.annotations.ExposeAs"

  private[this] val isDescriptionAnnotation = (annotaion: ru.Annotation) =>
    stripTypeAnnots(annotaion.tree.tpe.dealias).typeSymbol.fullName == "org.coursera.autoschema.annotations.Description"

  // Generates JSON schema based on a FormatAs annotation
  private[this] def formatAnnotationJson(annotation: ru.Annotation)(implicit mapper: ObjectMapper): ObjectNode = {
    annotation.tree.children.tail match {
      case ru.AssignOrNamedArg(_, typ) :: Nil =>
        JsObject("type" -> typ.toString.tail.init)
      case ru.AssignOrNamedArg(ru.Ident("type"), ru.Literal(ru.Constant(typ)))
              :: ru.AssignOrNamedArg(ru.Ident("format"), ru.Literal(ru.Constant(format)))
              :: Nil =>
        JsObject("type" -> typ.toString.tail.init, "format" -> format.toString.tail.init)
      case ru.AssignOrNamedArg(ru.Ident("format"), ru.Literal(ru.Constant(format)))
          :: ru.AssignOrNamedArg(ru.Ident("type"), ru.Literal(ru.Constant(typ)))
          :: Nil =>
        JsObject("type" -> typ.toString.tail.init, "format" -> format.toString.tail.init)
      case _ =>
        JsObject()
    }
  }

  private[this] def descriptionAnnotationJson(annotation: ru.Annotation): Option[(String, JsString)] = {
    annotation.tree.children.tail match {
      case ru.AssignOrNamedArg(_, ru.Literal(ru.Constant(description))) :: Nil =>
        Some("description" -> JsString(description.toString))
      case _ => None
    }
  }

  private[this] def exposeAnnotationType(annotation: ru.Annotation): ru.Type = {
    annotation.tree.children match {
      case _ :: ru.AssignOrNamedArg(_, ru.Literal(ru.Constant(tpe: ru.Type))) :: Nil => tpe
      case _ => throw new IllegalArgumentException("@ExposeAs annotation did not have a type?")
    }
  }

  private[this] def isProbablyAGetter(m: ru.MethodSymbol): Boolean =
    m.name.decodedName.toString.startsWith("get") && m.paramLists == (Nil :: Nil)

  private[this] def isHidden(term: ru.TermSymbol): Boolean =
    term.annotations exists isHideAnnotation

  private[this] def deGetterfy(m: ru.TermSymbol): String =
    m.name.decodedName.toString.trim.toList.drop(3) match {
      case init :: rest => new String((init.toLower :: rest).toArray)
      case Nil          => ""
    }

  private[this] val camel = "([a-z](?=[A-Z])|[A-Z](?=[A-Z][a-z])|[A-Z](?=[A-Z]))".r
  private[this] def nicify(name: String): String =
    camel.replaceAllIn(name.capitalize, m => s"${m.matched} ")

  private[this] def createClassJson(tag: Tag, previousTypes: Set[String])(implicit om: ObjectMapper): JsObject = {
    val tpe = tag.tpe
    // Check if schema for this class has already been generated
    classSchemaCache.getOrElseUpdate(tpe.typeSymbol.fullName, {
      val title = nicify(tpe.typeSymbol.name.decodedName.toString)
      val propertiesList: Seq[(String, JsonNode)] = tpe.members.flatMap { member =>
        if (member.isTerm && !isHidden(member.asTerm) && !(member.owner == ru.symbolOf[Object])) {
          val term = member.asTerm
          val propName: Option[String] =
            if (term.isJava && term.isMethod && isProbablyAGetter(term.asMethod)) {
              Some(deGetterfy(term))
            } else if (!term.isJava && (term.isVal || term.isVar)) {
              Some(term.name.decodedName.toString.trim)
            } else None
          propName.map { name =>
            val termFormat = term.annotations.find(isFormatAnnotation)
              .map(formatAnnotationJson)
              .getOrElse {
                term.annotations.find(isExposeAnnotation)
                  .map(annotation => createSchema(rec(tag, exposeAnnotationType(annotation)), previousTypes))
                  .getOrElse(createSchema(rec(tag, term.typeSignature.resultType), previousTypes + tpe.typeSymbol.fullName))
              }

            val description = term.annotations.find(isDescriptionAnnotation).flatMap(descriptionAnnotationJson)
            val termFormatWithDescription = description.fold(termFormat)(termFormat + _)

            val termFormatDescriptionName =
              if (termFormatWithDescription.get("title") eq null)
                termFormatWithDescription + ("title" -> nicify(name))
              else termFormatWithDescription

            name -> termFormatDescriptionName
            }
        } else {
          None
        }
      }.toList.sortBy(_._1)

      val properties = JsObject(propertiesList: _*)

      // Return the value and add it to the cache (since we're using getOrElseUpdate)
      JsObject("title" -> title, "type" -> "object", "properties" -> properties)
    })
  }

  private[this] def extendsValue(tpe: ru.Type): Boolean = {
    tpe.baseClasses.exists(_.fullName == "scala.Enumeration.Value")
  }

  private[this] def addDescription[T](tpe: ru.Type, obj: JsObject): JsObject = {
    val description = tpe.typeSymbol.annotations.find(isDescriptionAnnotation).flatMap(descriptionAnnotationJson)
    description match {
      case Some(descr) => obj + descr
      case None => obj
    }
  }

  private[this] def createSchema(tag: Tag, previousTypes: Set[String])(implicit om: ObjectMapper): JsObject = {
    val tpe = tag.tpe
    val typeName = tpe.typeSymbol.fullName

    if (extendsValue(tpe)) {
      val ru.TypeRef(pre, _, _) = tpe
      val module = pre.typeSymbol.asClass.module.asModule
      val enum = tag.mirror.reflectModule(module).instance.asInstanceOf[Enumeration]
      val options = enum.values.map { v =>
        JsString(v.toString)
      }.toList

      val optionsArr = JsArray(options)
      val enumJson = JsObject(
        "type" -> "string",
        "enum" -> optionsArr
      )
      addDescription(tpe, enumJson)

    } else if (tpe.typeSymbol.isJavaEnum) {
      val enumConstants = tpe.companion.decls.filter(d => d.isTerm && d.isJavaEnum).map(_.asTerm.name.decodedName.toString)
      val enumJson = JsObject(
        "type" -> "string",
        "enum" -> JsArray(enumConstants.map(stringToNode).toList)
      )
      addDescription(tpe, enumJson)
    } else if (tpe.baseClasses.exists(_.fullName == "enumeratum.EnumEntry")) {
      val module = tpe.typeSymbol.companion.asModule
      val enum = tag.mirror.reflectModule(module).instance.asInstanceOf[enumeratum.Enum[_ <: enumeratum.EnumEntry]]

      val enumJson = JsObject(
        "type" -> "string",
        "enum" -> JsArray(enum.values.map(_.entryName: JsString).toList)
      )
      addDescription(tpe, enumJson)

    } else if (typeName == "scala.Option" || typeName == "java.util.Optional") {
      // Option[T] becomes the schema of T with required set to false
      val jsonOption = JsObject("required" -> false) ++ createSchema(rec(tag, tpe.asInstanceOf[ru.TypeRefApi].args.head), previousTypes)
      addDescription(tpe, jsonOption)
    } else if (tpe.baseClasses.exists(s => s == ru.symbolOf[MapLike[_, _, _]]
                                        || s == ru.symbolOf[java.util.Map[_, _]])) {
      if (tpe.typeArgs.head.dealias != ru.typeOf[String].dealias) {
        println("Maps with non-String keys not supported by JSONSchema... be warned...")
      }
      val jsonMap = JsObject(
        "type" -> "object",
        "properties" -> JsObject(),
        "additionalProperties" -> createSchema(tpe.typeArgs(1))
      )
      addDescription(tpe, jsonMap)
    } else if (tpe.baseClasses.exists(s => s.fullName == "scala.collection.Traversable" ||
                                           s.fullName == "scala.Array" ||
                                           s.fullName == "scala.Seq" ||
                                           s.fullName == "scala.List" ||
                                           s.fullName == "java.util.Collection" ||
                                           s.fullName == "scala.Vector")) {
      // (Traversable)[T] becomes a schema with items set to the schema of T
      val jsonSeq = JsObject("type" -> "array", "items" -> createSchema(rec(tag, tpe.asInstanceOf[ru.TypeRefApi].args.head), previousTypes))
      addDescription(tpe, jsonSeq)
    } else {
      val jsonObj = tpe.typeSymbol.annotations.find(isFormatAnnotation)
        .map(formatAnnotationJson)
        .getOrElse {
        tpe.typeSymbol.annotations.find(isExposeAnnotation)
          .map(annotation => createSchema(rec(tag, exposeAnnotationType(annotation)), previousTypes))
          .getOrElse {
          schemaTypeForScala.applyOrElse(typeName, { (_: String) =>
            if (tpe.typeSymbol.isClass) {
              // Check if this schema is recursive
              if (previousTypes.contains(tpe.typeSymbol.fullName)) {
                throw new IllegalArgumentException(s"Recursive types detected: $typeName")
              }

              createClassJson(rec(tag, tpe), previousTypes)
            } else {
              JsObject()
            }
          })
        }
      }
      addDescription(tpe, jsonObj)
    }
  }

  /**
   * Create schema based on reflection type
   * @param tpe
   * The reflection type to be converted into JSON Schema
   * @return
   * The JSON Schema for the type as a JsObject
   */
  def createSchema(tpe: ru.Type)(implicit om: ObjectMapper): JsObject =
    createSchema(mkTag(tpe), Set.empty[String])(om)

  /**
   *
   * @tparam T
   * The type to be converted into JSON Schema
   * @return
   * The JSON Schema for the type as a JsObject
   */
  def createSchema[T: ru.TypeTag](implicit om: ObjectMapper): JsObject = createSchema(ru.typeOf[T])

  /**
   * Create a schema and format it according to the style
   * @param tpe The reflection type to be converted into JSON Schema
   * @param indent The left margin indent in pixels
   * @return The JSON Schema for the type as a formatted string
   */
  def createPrettySchema(tpe: ru.Type, indent: Int)(implicit om: ObjectMapper): String =
    styleSchema(prettyPrint(createSchema(tpe)), indent)

  /**
   * Create a schema and format it according to the style
   * @param indent The left margin indent in pixels
   * @return The JSON Schema for the type as a formatted string
   */
  def createPrettySchema[T: ru.TypeTag](indent: Int)(implicit om: ObjectMapper): String =
    styleSchema(prettyPrint(createSchema(ru.typeOf[T])), indent)

  private[this] def styleSchema(schema: String, indent: Int)(implicit om: ObjectMapper): String =
    s"""<div style="margin-left: ${indent}px; background-color: #E8E8E8; border-width: 1px;"><i>${schema}</i></div>"""
}
