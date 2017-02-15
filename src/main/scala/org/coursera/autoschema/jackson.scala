package org.coursera.autoschema

import com.fasterxml.jackson.databind.node.{ArrayNode, BooleanNode, ObjectNode, TextNode}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}

import scala.collection.JavaConverters._

object jackson {
  /* --- Types --- */
  type JsArray   = ArrayNode
  type JsBoolean = BooleanNode
  type JsObject  = ObjectNode
  type JsString  = TextNode

  /* --- Methods --- */
  implicit final class ObjectNodeOps(val self: ObjectNode) extends AnyVal {
    def +(mapping: (String, JsonNode)): ObjectNode =
      self.deepCopy().set(mapping._1, mapping._2).asInstanceOf[ObjectNode]

    def ++(mappings: ObjectNode): ObjectNode = {
      val o = self.deepCopy()
      mappings.fields().asScala foreach {
        kv => o.set(kv.getKey, kv.getValue)
      }
      o
    }
  }

  /* --- Constructors --- */
  def JsString(str: String): JsString = new TextNode(str)
  def JsArray(values: List[JsonNode])(implicit om: ObjectMapper): JsArray = {
    val a = om.createArrayNode()
    values foreach a.add
    a
  }
  def JsObject(bindings: (String, JsonNode)*)(implicit om: ObjectMapper): ObjectNode = {
    val o = om.createObjectNode()
    bindings foreach {
      case (key, value) => o.put(key, value)
    }
    o
  }

  /* --- Stuff --- */
  def prettyPrint(n: JsonNode): String = n.toString

  /* --- Conversions --- */
  import language.implicitConversions
  implicit def stringToNode(str: String): JsString = JsString(str)
  implicit def booleanToNode(bool: Boolean): JsBoolean = BooleanNode.valueOf(bool)

}
