/*
 * Copyright 2021 HM Revenue & Customs
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
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.headers.{Header => OasHeader}

import java.util
import scala.collection.JavaConverters._

trait OASV3HeaderAdapter extends OASV3SchemaAdapter {


  def extractComponentHeaders(openApi: OpenAPI): List[uk.gov.hmrc.integrationcatalogue.models.Header] = {
    nullSafeGetHeaders(openApi).map(headers => adaptHeaders(headers.asScala.toMap)).getOrElse(List.empty)
  }

  private def nullSafeGetHeaders(openApi: OpenAPI): Option[util.Map[String, OasHeader]] = {
    Option(openApi.getComponents).flatMap(components => Option(components.getHeaders))
  }


  def extractResponseHeaders(response: ApiResponse): List[uk.gov.hmrc.integrationcatalogue.models.Header] = {
    val headerMap = Option(response.getHeaders).getOrElse(new util.HashMap()).asScala.toMap
    adaptHeaders(headerMap)

  }

  private def adaptHeaders(headerMap: Map[String, OasHeader])={

    headerMap.map {
      case (name: String, header: OasHeader) =>
        Option(header.get$ref())
          .map(value => uk.gov.hmrc.integrationcatalogue.models.Header(name, Some(value)))
          .getOrElse({
            val description = Option(header.getDescription)
            val isRequired = Option(Boolean.unbox(header.getRequired))
            val deprecated = Option(Boolean.unbox(header.getDeprecated()))
            val headerSchema = Option(header.getSchema)
            val responseHeaderSchema = headerSchema.map(schema => extractOasSchema(Some(name), schema))

            uk.gov.hmrc.integrationcatalogue.models.Header(name, description = description, required = isRequired, deprecated = deprecated, schema = responseHeaderSchema)
          })
    }.toList
  }

}
