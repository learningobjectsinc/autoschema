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

package org.coursera.AutoSchema

import org.coursera.autoschema.AutoSchema.createSchema
import org.coursera.autoschema.annotations._
import org.junit.Test
import org.scalatest.junit.AssertionsForJUnit
import org.coursera.autoschema.jackson._
import java.util.UUID

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

case class TypeOne(param1: Int)
case class TypeTwo(param1: Int, param2: Long)

case class TypeThreeParamOne(param1: String)
case class TypeThree(param1: TypeThreeParamOne, param2: Double)

case class TypeFour(param1: Int, @Term.Hide param2: Int)

@FormatAs("string")
case class TypeFiveParamOne()
case class TypeFiveParamTwo()
case class TypeFive(param1: TypeFiveParamOne, @Term.FormatAs("string") param2: TypeFiveParamTwo)

@ExposeAs(classOf[Int])
case class TypeSixParamOne()
case class TypeSixParamTwo()
case class TypeSix(firstBit: TypeSixParamOne, @Term.ExposeAs(classOf[Int]) secondBit: TypeSixParamTwo)

case class TypeSeven(param1: UUID)

case class RecursiveType(param1: RecursiveType)

case class MutuallyRecursiveTypeOne(param1: MutuallyRecursiveTypeTwo)
case class MutuallyRecursiveTypeTwo(param1: MutuallyRecursiveTypeOne)

@Description("Type description")
case class TypeWithDescription(@Term.Description("Parameter description") param1: String)

case class TypeWithMap(map: Map[String, TypeOne])

class AutoSchemaTest extends AssertionsForJUnit {
  implicit val om: ObjectMapper = new ObjectMapper().registerModule(new DefaultScalaModule)

  @Test
  def justAnInt: Unit = {
    assert(createSchema[Int] ===
      JsObject(
        "type" -> "number",
        "format" -> "number"))
  }

  @Test
  def typeOne: Unit = {
    assert(createSchema[TypeOne] ===
      JsObject(
        "title" -> "Type One",
        "type" -> "object",
        "properties" -> JsObject(
          "param1" -> JsObject(
            "title" -> "Param1",
            "type" -> "number",
            "format" -> "number"))))
  }

  @Test
  def typeTwo: Unit = {
    assert(createSchema[TypeTwo] ===
      JsObject(
        "title" -> "Type Two",
        "type" -> "object",
        "properties" -> JsObject(
          "param1" -> JsObject(
            "title" -> "Param1",
            "type" -> "number",
            "format" -> "number"),
          "param2" -> JsObject(
            "title" -> "Param2",
            "type" -> "number",
            "format" -> "number"))))
  }

  @Test
  def typeThree: Unit = {
    assert(createSchema[TypeThree] ===
      JsObject(
        "title" -> "Type Three",
        "type" -> "object",
        "properties" -> JsObject(
          "param1" -> JsObject(
            "title" -> "Type Three Param One",
            "type" -> "object",
            "properties" -> JsObject(
              "param1" -> JsObject(
                "title" -> "Param1",
                "type" -> "string"))),
          "param2" -> JsObject(
            "title" -> "Param2",
            "type" -> "number",
            "format" -> "number"))))
  }

  @Test
  def typeFour: Unit = {
    assert(createSchema[TypeFour] ===
      JsObject(
        "title" -> "Type Four",
        "type" -> "object",
        "properties" -> JsObject(
          "param1" -> JsObject(
            "title" -> "Param1",
            "type" -> "number",
            "format" -> "number"))))
  }

  @Test
  def typeFive: Unit = {
    assert(createSchema[TypeFive] ===
      JsObject(
        "title" -> "Type Five",
        "type" -> "object",
        "properties" -> JsObject(
          "param1" -> JsObject(
            "title" -> "Param1",
            "type" -> "string"),
          "param2" -> JsObject(
            "title" -> "Param2",
            "type" -> "string"))))
  }

  @Test
  def typeSix: Unit = {
    assert(createSchema[TypeSix] ===
      JsObject(
        "title" -> "Type Six",
        "type" -> "object",
        "properties" -> JsObject(
          "firstBit" -> JsObject(
            "title" -> "First Bit",
            "type" -> "number",
            "format" -> "number"),
          "secondBit" -> JsObject(
            "title" -> "Second Bit",
            "type" -> "number",
            "format" -> "number"))))
  }

  @Test
  def typeSeven: Unit = {
    assert(createSchema[TypeSeven] ===
      JsObject(
        "title" -> "Type Seven",
        "type" -> "object",
        "properties" -> JsObject(
          "param1" -> JsObject(
            "title" -> "Param1",
            "type" -> "string",
            "pattern" -> "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$"))))
  }

  @Test
  def collections1: Unit = {
    assert(createSchema[Array[Int]] ===
      JsObject(
        "type" -> "array",
        "items" -> JsObject(
          "type" -> "number",
          "format" -> "number")))
  }

  @Test
  def collections2: Unit = {
    assert(createSchema[List[Int]] ===
      JsObject(
        "type" -> "array",
        "items" -> JsObject(
          "type" -> "number",
          "format" -> "number")))
  }

  @Test
  def collections3: Unit = {
    assert(createSchema[Seq[Int]] ===
      JsObject(
        "type" -> "array",
        "items" -> JsObject(
          "type" -> "number",
          "format" -> "number")))
  }

  @Test
  def recursiveType: Unit = {
    intercept[IllegalArgumentException] {
      createSchema[RecursiveType]
    }
  }

  @Test
  def mutuallyRecursiveType: Unit = {
    intercept[IllegalArgumentException] {
      createSchema[MutuallyRecursiveTypeOne]
    }
  }

  @Test
  def typeWithDescription: Unit = {
    assert(createSchema[TypeWithDescription] ===
      JsObject(
        "title" -> "Type With Description",
        "type" -> "object",
        "properties" -> JsObject(
          "param1" -> JsObject(
            "title" -> "Param1",
            "type" -> "string",
            "description" -> "Parameter description")),
        "description" -> "Type description"))
  }

  @Test
  def mapType: Unit = {
    assert(createSchema[TypeWithMap] ===
      JsObject(
        "title" -> "Type With Map",
        "type" -> "object",
        "properties" -> JsObject(
          "map" -> JsObject(
            "title" -> "Map",
            "type" -> "object",
            "properties" -> JsObject(),
            "additionalProperties" -> JsObject(
              "title" -> "Type One",
              "type" -> "object",
              "properties" -> JsObject(
                "param1" -> JsObject(
                  "title" -> "Param1",
                  "type" -> "number",
                  "format" -> "number"
                )
              )
            )
          )
        )
      )
    )
  }

  @Test
  def javaType: Unit = {
    assert(createSchema[JavaType] ===
        JsObject(
          "title" -> "Java Type",
          "type" -> "object",
          "properties" -> JsObject(
            "int" -> JsObject(
              "title" -> "Int",
              "type" -> "number",
              "format" -> "number"),
            "jlist" -> JsObject(
              "title" -> "Jlist",
              "type" -> "array",
              "items" -> JsObject(
                "type" -> "number",
                "format" -> "number"
              )
            ),
            "jopt" -> JsObject(
              "title" -> "Jopt",
              "required" -> false,
              "type" -> "string"
            ),
            "str2Int" -> JsObject(
              "type" -> "object",
              "title" -> "Str2Int",
              "properties" -> JsObject(),
              "additionalProperties" -> JsObject(
                "type" -> "number",
                "format" -> "number")))))
  }


  @Test
  def javaTypeWithDescription: Unit = {
    assert(createSchema[JavaTypeWithDescription] ===
        JsObject(
          "title" -> "Java Type With Description",
          "type" -> "object",
          "properties" -> JsObject(
            "param" -> JsObject(
              "title" -> "Param",
              "type" -> "string",
              "description" -> "Parameter description")),
          "description" -> "Type description"))
  }

}
