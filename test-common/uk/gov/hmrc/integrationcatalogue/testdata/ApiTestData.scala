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

package uk.gov.hmrc.integrationcatalogue.testdata

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import uk.gov.hmrc.integrationcatalogue.models.ApiStatus._
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.common.{IntegrationId, Maintainer, PlatformType, SpecificationType}

import java.util.UUID

trait ApiTestData {

  val dateValue: DateTime = DateTime.parse("04/11/2020 20:27:05", DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss"))
  val reviewedDate: DateTime = DateTime.parse("25/12/2020 20:27:05", DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss"))

  val coreIfPlatform: PlatformType = PlatformType.CORE_IF
  val apiPlatform: PlatformType = PlatformType.API_PLATFORM

  val jsonMediaType       = "application/json"
  val exampleRequest1name = "example request 1"
  val exampleRequest1Body = "{\"someValue\": \"abcdefg\"}"
  val exampleRequest1: Example = Example(exampleRequest1name, exampleRequest1Body)
  val exampleResponse1: Example = Example("example response name", "example response body")

  val schema1: DefaultSchema = DefaultSchema(
    name = Some("agentReferenceNumber"),
    not = None,
    `type` = Some("string"),
    pattern = Some("^[A-Z](ARN)[0-9]{7}$"),
    description = None,
    ref = None,
    properties = List.empty,
    `enum` = List.empty,
    required = List.empty,
    stringAttributes = None,
    numberAttributes = None,
    minProperties = None,
    maxProperties = None,
    format = None,
    default = None,
    example = None
  )

  val schema2: DefaultSchema = DefaultSchema(
    name = Some("agentReferenceNumber"),
    not = None,
    `type` = Some("object"),
    pattern = None,
    description = None,
    ref = None,
    properties = List(schema1),
    `enum` = List.empty,
    required = List.empty,
    stringAttributes = None,
    numberAttributes = None,
    minProperties = None,
    maxProperties = None,
    format = None,
    default = None,
    example = None
  )

  val request: Request = Request(
    description = Some("request"),
    schema = Some(schema1),
    mediaType = Some(jsonMediaType),
    examples = List(exampleRequest1)
  )

  val response: Response = Response(
    statusCode = "200",
    description = Some("response"),
    schema = Some(schema2),
    mediaType = Some("application/json"),
    examples = List(exampleResponse1)
  )

  val apiPlatformMaintainer: Maintainer = Maintainer("Api Platform Team", "#team-api-platform-sup")
  val coreIfMaintainer: Maintainer = Maintainer("Core IF Team", "**core-if-slack-channel**")

  val selfassessmentApiId: IntegrationId = IntegrationId(UUID.fromString("b7c649e6-e10b-4815-8a2c-706317ec484d"))

  val endpointGetMethod: EndpointMethod = EndpointMethod("GET", Some("summary"), Some("some description"), List.empty)
  val endpointPutMethod: EndpointMethod = EndpointMethod("PUT", Some("summary"), Some("some description"), List.empty)
  val endpoint1: Endpoint = Endpoint("/some/url", List(endpointGetMethod, endpointPutMethod))

  val endpoints = List(endpoint1, Endpoint("/some/url", List.empty))

  val apiDetail0: ApiDetail = ApiDetail(
    selfassessmentApiId,
    publisherReference = "self-assessment-api",
    title = "Self Assessment (MTD)",
    description = "Making Tax Digital introduces digital record keeping for most businesses, self-employed people and landlords.",
    lastUpdated = dateValue,
    platform = apiPlatform,
    maintainer = apiPlatformMaintainer,
    version = "2.0",
    specificationType = SpecificationType.OAS_V3,
    endpoints = endpoints,
    shortDescription = None,
    openApiSpecification = "OAS content for Self Assessment",
    apiStatus = LIVE,
    reviewedDate = reviewedDate,
    scopes = Set.empty
  )

  val apiDetail1: ApiDetail = ApiDetail(
    IntegrationId(UUID.fromString("2f0c9fc4-7773-433b-b4cf-15d4cb932e36")),
    publisherReference = "marriage-allowance",
    title = "Marriage Allowance",
    description = "This API provides resources related to [Marriage Allowance](https://www.gov.uk/marriage-allowance).",
    lastUpdated = dateValue,
    platform = apiPlatform,
    maintainer = apiPlatformMaintainer,
    version = "2.0",
    specificationType = SpecificationType.OAS_V3,
    endpoints = endpoints,
    shortDescription = None,
    openApiSpecification = "OAS content for Marriage Allowance",
    apiStatus = LIVE,
    reviewedDate = reviewedDate,
    scopes = Set.empty
  )

  val apiDetail2: ApiDetail = ApiDetail(
    IntegrationId(UUID.fromString("bd05e606-b400-49f2-a436-89d1ed1513bc")),
    publisherReference = "API1001",
    title = "API#1001 Get Data 1",
    description = "This is will be an automated information sharing arrangement with Local Authorities (LAs) via DWP, with a view to LAs recovering (or remitting) as much Housing Benefit (HB) debt as possible, prior to it being subsumption into UC.",
    lastUpdated = dateValue,
    platform = coreIfPlatform,
    maintainer = coreIfMaintainer,
    version = "0.1.0",
    specificationType = SpecificationType.OAS_V3,
    hods = List.empty,
    endpoints = endpoints,
    shortDescription = None,
    openApiSpecification = "OAS content for API#1001",
    apiStatus = LIVE,
    reviewedDate = reviewedDate,
    scopes = Set.empty
  )

  val apiDetail3: ApiDetail = ApiDetail(
    IntegrationId(UUID.fromString("136791a6-2b1c-11eb-adc1-0242ac120002")),
    publisherReference = "API1002",
    title = "API#1002 Get Data 2",
    description = "This API provides the capability to retrieve the customer known data.",
    lastUpdated = dateValue,
    platform = coreIfPlatform,
    maintainer = coreIfMaintainer,
    version = "1.1.0",
    specificationType = SpecificationType.OAS_V3,
    hods = List("ETMP"),
    endpoints = endpoints,
    shortDescription = None,
    openApiSpecification = "OAS content for API#1002",
    apiStatus = LIVE,
    reviewedDate = reviewedDate,
    scopes = Set.empty
  )

  val apiList = List(apiDetail0, apiDetail1, apiDetail2, apiDetail3)

}
