/*
 * Copyright 2023 HM Revenue & Customs
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

import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.parameters.{Parameter => OasParameter}
import scala.collection.JavaConverters._
import uk.gov.hmrc.integrationcatalogue.models.Parameter
import io.swagger.v3.oas.models.OpenAPI

trait OASV3ParameterAdapter extends OASV3SchemaAdapter {

  def extractEndpointMethodParameters(operation: Operation): List[Parameter] = {
    val maybeparameters = Option(operation.getParameters())
    maybeparameters.map(_.asScala.toList)
      .getOrElse(List.empty).map(adaptParameter)
  }

  private def adaptParameter(parameter: OasParameter): Parameter = {

    Option(parameter.get$ref()).map(value => Parameter(Option(parameter.getName()), Option(value)))
      .getOrElse({
        val parameterSchema = Option(parameter.getSchema())
        val schema          = parameterSchema.map(schema => extractOasSchema(Option(parameter.getName), schema))
        Parameter(
          name = Option(parameter.getName()),
          description = Option(parameter.getDescription()),
          in = Option(parameter.getIn()),
          ref = Option(parameter.get$ref()),
          required = Option(Boolean.unbox(parameter.getRequired)),
          deprecated = Option(Boolean.unbox(parameter.getDeprecated())),
          schema = schema,
          allowEmptyValue = Option(Boolean.unbox(parameter.getAllowEmptyValue()))
        )
      })
  }

  def extractComponentParameters(openApi: OpenAPI): List[Parameter] = {
    Option(openApi.getComponents())
      .flatMap(components => Option(components.getParameters()))
      .map(_.asScala.toList).getOrElse(List.empty)
      .map {
        case (_: String, parameter: OasParameter) => {
          adaptParameter(parameter)
        }
      }
  }

}
