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

import uk.gov.hmrc.integrationcatalogue.models.ApiStatus.*
import uk.gov.hmrc.integrationcatalogue.models.*
import uk.gov.hmrc.integrationcatalogue.models.common.{ApiGeneration, ApiType, IntegrationId, Maintainer, PlatformType, SpecificationType}

import java.time.Instant
import java.util.UUID

trait OasParsedItTestData {

  val filename            = "API10000_Get_Data_1.1.0.yaml"
  val fileContents        = "{}"
  val uuid: UUID          = UUID.fromString("28c0bd67-4176-42c7-be13-53be98a4db58")
  val dateValue: Instant = Instant.parse("2929-11-04T20:27:05Z")
  val reviewedDate: Instant = Instant.parse("2020-12-25T20:27:05Z")

  val apiPlatformMaintainer: Maintainer = Maintainer("API Platform Team", "#team-api-platform-sup")
  val coreIfMaintainer: Maintainer      = Maintainer("IF Team", "N/A", List.empty)

  val jsonMediaType = "application/json"

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

  val exampleRequest1name      = "example request 1"
  val exampleRequest1Body      = "{\"someValue\": \"abcdefg\"}"
  val exampleRequest1: Example = Example(exampleRequest1name, exampleRequest1Body)

  val exampleResponse1: Example = Example("example response name", "example response body")

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

  val putEndpoint1: EndpointMethod = EndpointMethod("PUT", Some("some summary"), Some("some description"), List("write scope"))
  val getEndpoint1: EndpointMethod = EndpointMethod("GET", Some("some summary"), Some("some description"), List("read scope"))

  val endpoint1: Endpoint          = Endpoint("/some/url", List(putEndpoint1, getEndpoint1))
  val getEndpoint2: EndpointMethod = EndpointMethod("GET", Some("some BOOP summary"), Some("some  DEEPSEARCH description"), List.empty)
  val putEndpoint2: EndpointMethod = EndpointMethod("PUT", Some("some DEEPSEARCH summary"), Some("some DEEPSEARCH description"), List.empty)
  val endpoint2: Endpoint          = Endpoint("/someOther/Newurl", List(putEndpoint2, getEndpoint2))

  val endpoints  = List(endpoint1)
  val endpoints2 = List(endpoint2)

  val endpointsNoDeepSearch = List(Endpoint("/some/url", List(getEndpoint1, putEndpoint1)))

  val endpointsWithScopes = List(
    Endpoint("/path/1", List(EndpointMethod("GET", None, None, List("read:scope1")))),
    Endpoint("/path/2", List(EndpointMethod("GET", None, None, List("read:scope2"))))
  )

  val apiDetail1: ApiDetail = ApiDetail(
    IntegrationId(UUID.fromString("dda7249a-9b88-11eb-a8b3-0242ac130003")),
    publisherReference = "API1001",
    title = "getKnownFactsName 1 ETMP",
    description = "getKnownFactsDesc",
    lastUpdated = dateValue,
    platform = PlatformType.CORE_IF,
    maintainer = coreIfMaintainer,
    version = "1.1.0",
    specificationType = SpecificationType.OAS_V3,
    hods = List("POOP"),
    endpoints = endpoints,
    shortDescription = Some("exampleApiDetail's short description"),
    openApiSpecification = "OAS file contents 1",
    apiStatus = LIVE,
    reviewedDate = reviewedDate,
    scopes = Set(Scope("scopename", Some("Scope definition"))),
    domain = Some("test-domain-1"),
    subDomain = Some("test-sub-domain-1"),
    teamId = Some("team1"),
    apiNumber = Some("API#1001"),
    apiGeneration = Some(ApiGeneration.V2),
  )

  val apiDetail3: ApiDetail = ApiDetail(
    IntegrationId(UUID.fromString("fab1868e-9b88-11eb-a8b3-0242ac130003")),
    publisherReference = "API1003",
    title = "getKnownFactsName 3",
    description = "api detail 3",
    lastUpdated = dateValue,
    platform = PlatformType.CORE_IF,
    maintainer = coreIfMaintainer,
    version = "1.1.0",
    specificationType = SpecificationType.OAS_V3,
    hods = List("CUSTOMS"),
    endpoints = endpoints2,
    shortDescription = None,
    openApiSpecification = "OAS file contents 2",
    apiStatus = LIVE,
    reviewedDate = reviewedDate,
    scopes = Set.empty,
    apiGeneration = Some(ApiGeneration.V2),
  )

  val apiDetail4: ApiDetail = ApiDetail(
    IntegrationId(UUID.fromString("e2e4ce48-29b0-11eb-adc1-0242ac120001")),
    publisherReference = "API1004",
    title = "getKnownFactsName 4",
    description = "getKnownFactsDesc ETMP",
    lastUpdated = dateValue,
    platform = PlatformType.DES,
    maintainer = coreIfMaintainer,
    version = "1.1.0",
    specificationType = SpecificationType.OAS_V3,
    hods = List("CUSTOMS"),
    endpoints = endpointsNoDeepSearch,
    shortDescription = None,
    openApiSpecification = "OAS file contents 3 BOOP",
    apiStatus = LIVE,
    reviewedDate = reviewedDate,
    scopes = Set.empty,
    apiGeneration = Some(ApiGeneration.V2),
  )

  val apiDetail5: ApiDetail = ApiDetail(
    IntegrationId(UUID.fromString("28c0bd67-4176-42c7-be13-53be98a4db58")),
    publisherReference = "API1005",
    title = "getOtherFactsName 5 BOOP",
    description = "api detail 5",
    lastUpdated = dateValue,
    platform = PlatformType.CORE_IF,
    maintainer = coreIfMaintainer,
    version = "1.2.0",
    specificationType = SpecificationType.OAS_V3,
    hods = List("ETMP"),
    endpoints = endpointsNoDeepSearch,
    shortDescription = Some("A short description"),
    openApiSpecification = "OAS file contents 4",
    apiStatus = LIVE,
    reviewedDate = reviewedDate,
    scopes = Set.empty,
    domain = Some("test-domain-5"),
    subDomain = Some("test-sub-domain-5"),
    apiType = Some(ApiType.SIMPLE),
    apiNumber = Some("API#1005"),
    apiGeneration = Some(ApiGeneration.V2),
  )

  val apiDetail6: ApiDetail = ApiDetail(
    IntegrationId(UUID.fromString("6d5a98fc-a33a-11eb-bcbc-0242ac130002")),
    publisherReference = "API1006",
    title = "CDS Classic API",
    description = "CDS Classic API",
    lastUpdated = dateValue,
    platform = PlatformType.CDS_CLASSIC,
    maintainer = coreIfMaintainer,
    version = "1.2.0",
    specificationType = SpecificationType.OAS_V3,
    hods = List("ETMP"),
    endpoints = endpointsNoDeepSearch,
    shortDescription = None,
    openApiSpecification = "OAS file contents 5",
    apiStatus = LIVE,
    reviewedDate = reviewedDate,
    scopes = Set.empty,
    apiGeneration = Some(ApiGeneration.V2),
  )

  val apiDetail9: ApiDetail = ApiDetail(
    IntegrationId(UUID.fromString("53b96a37-d9bb-4d55-8528-be107e3003bf")),
    publisherReference = "API1009",
    title = "API9 with Scopes",
    description = "Description of API9 with Scopes",
    lastUpdated = dateValue,
    platform = PlatformType.HIP,
    maintainer = coreIfMaintainer,
    version = "9.2.0",
    specificationType = SpecificationType.OAS_V3,
    hods = List("EMS"),
    endpoints = endpointsWithScopes,
    shortDescription = None,
    openApiSpecification = "OAS file contents 9",
    apiStatus = LIVE,
    reviewedDate = reviewedDate,
    scopes = Set.empty,
    apiGeneration = Some(ApiGeneration.V2)
  )

  val fileTransfer2: FileTransferDetail =
    FileTransferDetail(
      IntegrationId(UUID.fromString("e2e4ce48-29b0-11eb-adc1-0242ac120002")),
      fileTransferSpecificationVersion = "0.1",
      publisherReference = "API1002",
      title = "filetransfer 1",
      description = "file transfer 1 desc",
      lastUpdated = dateValue,
      reviewedDate = reviewedDate,
      platform = PlatformType.CORE_IF,
      maintainer = coreIfMaintainer,
      sourceSystem = List("source"),
      targetSystem = List("target"),
      transports = List("UTM"),
      fileTransferPattern = "pattern1",
    )

  val fileTransfer7: FileTransferDetail =
    FileTransferDetail(
      IntegrationId(UUID.fromString("8f8190dc-d992-11eb-b8bc-0242ac130003")),
      fileTransferSpecificationVersion = "0.1",
      publisherReference = "API1007",
      title = "filetransfer 2",
      description = "file transfer 2 desc",
      lastUpdated = dateValue,
      reviewedDate = reviewedDate,
      platform = PlatformType.API_PLATFORM,
      maintainer = apiPlatformMaintainer,
      sourceSystem = List("someSource"),
      targetSystem = List("target"),
      transports = List("AB"),
      fileTransferPattern = "pattern3"
    )

  val fileTransfer8: FileTransferDetail =
    FileTransferDetail(
      IntegrationId(UUID.fromString("8f8190dc-d992-11eb-b8bc-0242ac130003")),
      fileTransferSpecificationVersion = "0.1",
      publisherReference = "API1008",
      title = "filetransfer 3",
      description = "file transfer 3 desc",
      lastUpdated = dateValue,
      reviewedDate = reviewedDate,
      platform = PlatformType.API_PLATFORM,
      maintainer = apiPlatformMaintainer,
      sourceSystem = List("someSource"),
      targetSystem = List("target"),
      transports = List("WTM", "AB", "S3"),
      fileTransferPattern = "pattern3"
    )

}
