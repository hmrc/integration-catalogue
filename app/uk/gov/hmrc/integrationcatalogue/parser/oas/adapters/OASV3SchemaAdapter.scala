/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.integrationcatalogue.parser.oas.adapters

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.{ArraySchema => OasArraySchema, ComposedSchema => OasComposedSchema, Schema => OasSchema}
import uk.gov.hmrc.integrationcatalogue.models._

import java.util
import scala.collection.JavaConverters._

trait OASV3SchemaAdapter {

  def extractComponentSchemas(openApi: OpenAPI): List[uk.gov.hmrc.integrationcatalogue.models.Schema] = {
    nullSafeGetSchemas(openApi).map(schemas =>
      schemas.asScala.map(oasSchema => {
        extractOasSchema(Option(oasSchema._1), oasSchema._2)
      }).toList
    ).getOrElse(List.empty)
  }

  private def nullSafeGetSchemas(openApi: OpenAPI): Option[util.Map[String, OasSchema[_]]] = {
    Option(openApi.getComponents).flatMap(components => Option(components.getSchemas))
  }

  def extractOasSchema(mayBeSchemaName: Option[String], oasSchemaVal: OasSchema[_]): Schema = {
    oasSchemaVal match {
      case _: OasComposedSchema => handleComposedSchema(mayBeSchemaName, oasSchemaVal.asInstanceOf[OasComposedSchema])
      case _: OasArraySchema    => handleArraySchema(mayBeSchemaName, oasSchemaVal.asInstanceOf[OasArraySchema])
      case _                    => handleSchema(mayBeSchemaName, oasSchemaVal)
    }
  }

  def handleArraySchema(mayBeSchemaName: Option[String], oasSchemaVal: OasArraySchema): ArraySchema = {
    val items: Option[Schema]        = Option(oasSchemaVal.getItems).map(item => extractOasSchema(None, item))
    val defaultSchema                = createDefaultSchema(mayBeSchemaName, oasSchemaVal)
    val minItems: Option[Int]        = Option(oasSchemaVal.getMinItems).map(x => x.toInt)
    val maxItems: Option[Int]        = Option(oasSchemaVal.getMaxItems).map(x => x.toInt)
    val uniqueItems: Option[Boolean] = Option(Boolean.unbox(oasSchemaVal.getUniqueItems))

    ArraySchema(
      defaultSchema.name,
      defaultSchema.not,
      defaultSchema.`type`,
      defaultSchema.pattern,
      defaultSchema.description,
      ref = defaultSchema.ref,
      defaultSchema.properties,
      defaultSchema.`enum`,
      defaultSchema.required,
      defaultSchema.minProperties,
      defaultSchema.maxProperties,
      minItems,
      maxItems,
      uniqueItems,
      items = items
    )
  }

  def handleComposedSchema(mayBeSchemaName: Option[String], oasSchemaVal: OasComposedSchema): ComposedSchema = {
    val allOf: List[Schema] = Option(oasSchemaVal.getAllOf).map(allOf => allOf.asScala.map(x => extractOasSchema(None, x)).toList).getOrElse(List.empty)
    val anyOf: List[Schema] = Option(oasSchemaVal.getAnyOf).map(anyOff => anyOff.asScala.map(x => extractOasSchema(None, x)).toList).getOrElse(List.empty)
    val oneOf: List[Schema] = Option(oasSchemaVal.getOneOf).map(oneOf => oneOf.asScala.map(x => extractOasSchema(None, x)).toList).getOrElse(List.empty)
    val defaultSchema       = createDefaultSchema(mayBeSchemaName, oasSchemaVal)

    ComposedSchema(
      defaultSchema.name,
      defaultSchema.not,
      defaultSchema.`type`,
      defaultSchema.pattern,
      defaultSchema.description,
      ref = defaultSchema.ref,
      defaultSchema.properties,
      defaultSchema.`enum`,
      defaultSchema.required,
      defaultSchema.minProperties,
      defaultSchema.maxProperties,
      allOf = allOf,
      anyOf = anyOf,
      oneOf = oneOf
    )
  }

  def handleSchema(mayBeSchemaName: Option[String], oasSchemaVal: OasSchema[_]): DefaultSchema = {
    createDefaultSchema(mayBeSchemaName, oasSchemaVal)
  }

  private def createDefaultSchema(mayBeSchemaName: Option[String], oasSchemaVal: OasSchema[_]) = {

    def parseEnums(oasEnums: Option[java.util.List[_]]) = {
      oasEnums
        .map(`enum` =>
          `enum`.asScala.map(Option(_))
            // to handle an enum value that is 'null'. See 'Nullable enums' here: https://swagger.io/docs/specification/data-models/enums/
            .map(_.getOrElse("null").toString)
            .toList
        )
        .getOrElse(List.empty)
    }

    val maybeSchemaType                                            = Option(oasSchemaVal.getType)
    val maybePattern                                               = Option(oasSchemaVal.getPattern)
    val maybeDescription                                           = Option(oasSchemaVal.getDescription)
    val maybeRef                                                   = Option(oasSchemaVal.get$ref)
    val mayBeOasProperties: Option[util.Map[String, OasSchema[_]]] = Option(oasSchemaVal.getProperties)
    val maybeNot: Option[Schema]                                   = Option(oasSchemaVal.getNot).map(x => extractOasSchema(None, x))
    val enums: List[String]                                        = parseEnums(Option(oasSchemaVal.getEnum))
    val required: List[String]                                     = Option(oasSchemaVal.getRequired).map(x => x.asScala.toList).getOrElse(List.empty)
    val maxLength: Option[Int]                                     = Option(oasSchemaVal.getMaxLength).map(x => x.toInt)
    val minLength: Option[Int]                                     = Option(oasSchemaVal.getMinLength).map(x => x.toInt)
    val minimum: Option[BigDecimal]                                = Option(oasSchemaVal.getMinimum).map(scala.math.BigDecimal(_))
    val maximum: Option[BigDecimal]                                = Option(oasSchemaVal.getMaximum).map(scala.math.BigDecimal(_))
    val multipleOf: Option[BigDecimal]                             = Option(oasSchemaVal.getMultipleOf).map(scala.math.BigDecimal(_))
    val exclusiveMinimum: Option[Boolean]                          = Option(Boolean.unbox(oasSchemaVal.getExclusiveMinimum))
    val exclusiveMaximum: Option[Boolean]                          = Option(Boolean.unbox(oasSchemaVal.getExclusiveMaximum))
    val minProperties: Option[Int]                                 = Option(oasSchemaVal.getMinProperties).map(x => x.toInt)
    val maxProperties: Option[Int]                                 = Option(oasSchemaVal.getMaxProperties).map(x => x.toInt)
    val default: Option[String]                                    = Option(oasSchemaVal.getDefault).map(_.toString)
    val example: Option[String]                                    = Option(oasSchemaVal.getExample).map(_.toString)
    val format: Option[String]                                     = Option(oasSchemaVal.getFormat)

    val mayBeProperties: List[Schema] = mayBeOasProperties
      .map(properties => properties.asScala.map(x => extractOasSchema(Option(x._1), x._2)).toList).getOrElse(List.empty)

    DefaultSchema(
      mayBeSchemaName,
      maybeNot,
      maybeSchemaType,
      maybePattern,
      maybeDescription,
      ref = maybeRef,
      mayBeProperties,
      enums,
      required,
      buildStringAttributes(minLength, maxLength),
      buildNumberAttributes(minimum, maximum, multipleOf, exclusiveMinimum, exclusiveMaximum),
      minProperties,
      maxProperties,
      format,
      default,
      example
    )
  }

  def buildStringAttributes(minLength: Option[Int], maxLength: Option[Int]): Option[StringAttributes] = {
    if (minLength.isDefined || maxLength.isDefined) {
      Some(StringAttributes(minLength, maxLength))
    } else None

  }

  def buildNumberAttributes(
      minimum: Option[BigDecimal],
      maximum: Option[BigDecimal],
      multipleOf: Option[BigDecimal],
      exclusiveMinimum: Option[Boolean],
      exclusiveMaximum: Option[Boolean]
    ): Option[NumberAttributes] = {
    if (minimum.isDefined || maximum.isDefined || multipleOf.isDefined || exclusiveMinimum.isDefined || exclusiveMaximum.isDefined) {
      Some(NumberAttributes(minimum, maximum, multipleOf, exclusiveMinimum, exclusiveMaximum))
    } else None
  }

  def extractEndpointSchema(contentMap: Map[String, MediaType]): Option[Schema] = {
    Option(contentMap)
      .flatMap {
        case m: Map[String, MediaType] if m.isEmpty => None
        case m: Map[String, MediaType]              => m.map(mediaTypeKeyValue => {
            extractSchemaValue(Option(mediaTypeKeyValue._2))
          }).toList.head
      }
  }

  private def extractSchemaValue(mayBeMediaType: Option[MediaType]): Option[Schema] = {
    mayBeMediaType.flatMap(mediaType =>
      Option(mediaType.getSchema)
        .map(schema => extractOasSchema(None, schema))
    )
  }

}
