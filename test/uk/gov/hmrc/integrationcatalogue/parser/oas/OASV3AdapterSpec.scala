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

package uk.gov.hmrc.integrationcatalogue.parser.oas

import cats.data.Validated._
import cats.data._
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, ISODateTimeFormat}
import org.mockito.scalatest.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.integrationcatalogue.config.AppConfig
import uk.gov.hmrc.integrationcatalogue.models.common.{ContactInformation, IntegrationId, PlatformType, SpecificationType}
import uk.gov.hmrc.integrationcatalogue.models.{ApiDetail, Scope}
import uk.gov.hmrc.integrationcatalogue.parser.oas.adapters.OASV3Adapter
import uk.gov.hmrc.integrationcatalogue.service.UuidService
import uk.gov.hmrc.integrationcatalogue.testdata.{ApiTestData, OasTestData}

import java.util.UUID

class OASV3AdapterSpec extends AnyWordSpec with Matchers with MockitoSugar with ApiTestData with OasTestData with BeforeAndAfterEach {

  trait Setup {
    val publisherRef                        = "publisherRef"
    val platform: PlatformType.CORE_IF.type = PlatformType.CORE_IF
    val specType: SpecificationType         = SpecificationType.OAS_V3
    val generatedUuid: UUID                 = UUID.fromString("f26babbb-c9b1-4b79-b99a-9f99cf741f78")
    val mockUuidService: UuidService        = mock[UuidService]
    val mockAppConfig: AppConfig            = mock[AppConfig]

    val objInTest = new OASV3Adapter(mockUuidService, mockAppConfig)

    val parseSuccess: ValidatedNel[List[String], ApiDetail] = valid(apiDetail0.copy(id = IntegrationId(generatedUuid)))

    def createInvalidMessage(messages: List[String]): Validated[NonEmptyList[List[String]], Nothing] = {
      invalid(NonEmptyList[List[String]](messages, List()))
    }

    val parseFailure: ValidatedNel[List[String], ApiDetail] =
      invalid(NonEmptyList[List[String]](List("Invalid OAS, info item missing from OAS specification"), List()))

    val reviewedDate: DateTime = DateTime.parse("25/12/20", DateTimeFormat.forPattern("dd/MM/yy"))
  }

  "extractOpenApi" should {
    "do happy path with extensions" in new Setup {
      when(mockUuidService.newUuid()).thenReturn(generatedUuid)
      val hods                                          = List("ITMP", "NPS")
      val expectedReviewedDate: DateTime                = DateTime.parse("2021-07-24T00:00:00", ISODateTimeFormat.dateOptionalTimeParser())
      val result: ValidatedNel[List[String], ApiDetail] =
        objInTest.extractOpenApi(
          Some(apiDetail0.publisherReference),
          apiDetail0.platform,
          apiDetail0.specificationType,
          getOpenAPIObject(withExtensions = true, hods, reviewedDateExtension = Some("2021-07-24")),
          openApiSpecificationContent = apiDetail0.openApiSpecification
        )
      result match {
        case Valid(parsedObject) =>
          parsedObject.id shouldBe IntegrationId(generatedUuid)
          parsedObject.title shouldBe oasApiName
          parsedObject.description shouldBe oasApiDescription
          parsedObject.version shouldBe oasVersion
          parsedObject.platform shouldBe apiDetail0.platform
          parsedObject.specificationType shouldBe apiDetail0.specificationType
          parsedObject.openApiSpecification shouldBe apiDetail0.openApiSpecification
          parsedObject.reviewedDate shouldBe expectedReviewedDate

          val contact: ContactInformation = parsedObject.maintainer.contactInfo.head
          contact.name.getOrElse("") shouldBe oasContactName
          contact.emailAddress.getOrElse("") shouldBe oasContactEMail
          parsedObject.hods shouldBe hods

        case _: Invalid[NonEmptyList[List[String]]] =>
          fail()
      }

    }

    "do happy path with extensions but empty content in response and requests" in new Setup {
      when(mockUuidService.newUuid()).thenReturn(generatedUuid)
      val hods                                          = List("ITMP", "NPS")
      val expectedReviewedDate: DateTime                = DateTime.parse("2021-07-24T00:00:00", ISODateTimeFormat.dateOptionalTimeParser())
      val result: ValidatedNel[List[String], ApiDetail] = objInTest.extractOpenApi(
        Some(apiDetail0.publisherReference),
        apiDetail0.platform,
        apiDetail0.specificationType,
        getOpenAPIObject(withExtensions = true, backendsExtension = hods, reviewedDateExtension = Some("2021-07-24"), hasEmptyReqRespContent = true),
        openApiSpecificationContent = apiDetail0.openApiSpecification
      )

      result match {
        case Valid(parsedObject) =>
          parsedObject.id shouldBe IntegrationId(generatedUuid)
          parsedObject.publisherReference shouldBe apiDetail0.publisherReference
          parsedObject.title shouldBe oasApiName
          parsedObject.description shouldBe oasApiDescription
          parsedObject.version shouldBe oasVersion
          parsedObject.platform shouldBe apiDetail0.platform
          parsedObject.specificationType shouldBe apiDetail0.specificationType
          parsedObject.openApiSpecification shouldBe apiDetail0.openApiSpecification
          parsedObject.reviewedDate shouldBe expectedReviewedDate

          val contact: ContactInformation = parsedObject.maintainer.contactInfo.head
          contact.name.getOrElse("") shouldBe oasContactName
          contact.emailAddress.getOrElse("") shouldBe oasContactEMail
          parsedObject.hods shouldBe hods

        case _: Invalid[NonEmptyList[List[String]]] => fail()
      }

    }

    "do happy path with reviewedDate but without backends and publisherRef extensions" in new Setup {
      when(mockUuidService.newUuid()).thenReturn(generatedUuid)
      val expectedReviewedDate: DateTime                = DateTime.parse("2021-07-24T00:00:00", ISODateTimeFormat.dateOptionalTimeParser())
      val result: ValidatedNel[List[String], ApiDetail] = objInTest.extractOpenApi(
        Some(apiDetail0.publisherReference),
        apiDetail0.platform,
        apiDetail0.specificationType,
        getOpenAPIObject(withExtensions = false, reviewedDateExtension = Some("2021-07-24")),
        openApiSpecificationContent = apiDetail0.openApiSpecification
      )
      result match {
        case Valid(parsedObject) =>
          parsedObject.id shouldBe IntegrationId(generatedUuid)
          parsedObject.publisherReference shouldBe apiDetail0.publisherReference
          parsedObject.title shouldBe oasApiName
          parsedObject.description shouldBe oasApiDescription
          parsedObject.version shouldBe oasVersion
          parsedObject.platform shouldBe apiDetail0.platform
          parsedObject.specificationType shouldBe apiDetail0.specificationType
          parsedObject.openApiSpecification shouldBe apiDetail0.openApiSpecification
          parsedObject.reviewedDate shouldBe expectedReviewedDate

          val contact: ContactInformation = parsedObject.maintainer.contactInfo.head
          contact.name.getOrElse("") shouldBe oasContactName
          contact.emailAddress.getOrElse("") shouldBe oasContactEMail
          parsedObject.hods shouldBe Nil

        case _: Invalid[NonEmptyList[List[String]]] => fail()
      }

    }

    "fail with no extensions" in new Setup {
      val result: ValidatedNel[List[String], ApiDetail] = objInTest.extractOpenApi(
        Some(apiDetail0.publisherReference),
        apiDetail0.platform,
        apiDetail0.specificationType,
        getOpenAPIObject(withExtensions = false, reviewedDateExtension = None),
        openApiSpecificationContent = apiDetail0.openApiSpecification
      )

      result shouldBe an[Invalid[_]]
    }

    "parse extensions returns error(s)" in new Setup {
      //noinspection ScalaStyle
      val result: ValidatedNel[List[String], ApiDetail] = objInTest.extractOpenApi(
        Some(apiDetail0.publisherReference),
        apiDetail0.platform,
        apiDetail0.specificationType,
        getOpenAPIObject(withExtensions = true, backendsExtension = null),
        openApiSpecificationContent = apiDetail0.openApiSpecification
      )

      result shouldBe an[Invalid[_]]
    }

    "return failure with correct message when empty openApi object is passed in" in new Setup {
      val parseResult: ValidatedNel[List[String], ApiDetail] =
        objInTest.extractOpenApi(
          Some(apiDetail0.publisherReference),
          apiDetail0.platform,
          apiDetail0.specificationType,
          new OpenAPI(),
          openApiSpecificationContent = apiDetail0.openApiSpecification
        )
      parseResult shouldBe createInvalidMessage(List("Invalid OAS, info item missing from OAS specification"))
    }

    "return failure with all three error messages when openApi object missing title, version " in new Setup {
      val openApi                                            = new OpenAPI()
      openApi.setInfo(new Info())
      val parseResult: ValidatedNel[List[String], ApiDetail] =
        objInTest.extractOpenApi(
          Some(apiDetail0.publisherReference),
          apiDetail0.platform,
          apiDetail0.specificationType,
          openApi,
          openApiSpecificationContent = apiDetail0.openApiSpecification
        )
      parseResult shouldBe createInvalidMessage(
        List("Invalid OAS, title missing from OAS specification", "Invalid OAS, version missing from OAS specification")
      )
    }

    "extract endpoint-level scopes when there are no global scopes" in new Setup {
      when(mockUuidService.newUuid()).thenReturn(generatedUuid)

      val endpointScopes: Map[String, List[String]] = Map(
        oasPath1Uri -> List("read:scope-common", s"read:scope-path1"),
        oasPath2Uri -> List.empty,
        oasPath3Uri -> List("read:scope-common", s"read:scope-path3")
      )

      val openApi: OpenAPI = getOpenAPIObject(
        withExtensions = true,
        reviewedDateExtension = Some("2021-07-24"),
        oAuth2SecuritySchemeName = Some("oAuth2Test"),
        endpointScopes = endpointScopes
      )

      val result: ValidatedNel[List[String], ApiDetail] =
        objInTest.extractOpenApi(
          Some(apiDetail0.publisherReference),
          apiDetail0.platform,
          apiDetail0.specificationType,
          openApi,
          openApiSpecificationContent = apiDetail0.openApiSpecification
        )

      result match {
        case Valid(apiDetail) =>
          checkEndpointScopes(apiDetail, endpointScopes)
        case invalid =>
          fail(s"Result was not a valid ApiDetail: $invalid")
      }
    }

    "extract endpoint-level scopes defaulting to global scopes" in  new Setup {
      when(mockUuidService.newUuid()).thenReturn(generatedUuid)
      val globalScopes: List[String] = List("read:global", "write:global")

      val endpointScopes: Map[String, List[String]] = Map(
        oasPath1Uri -> List("read:scope-common", s"read:scope-path1"),
        oasPath2Uri -> List.empty,
        oasPath3Uri -> List("read:scope-common", s"read:scope-path3")
      )

      val expectedScopes: Map[String, List[String]] = Map(
        oasPath1Uri -> List("read:scope-common", s"read:scope-path1"),
        oasPath2Uri -> List("read:global", "write:global"),
        oasPath3Uri -> List("read:scope-common", s"read:scope-path3")
      )

      val openApi: OpenAPI = getOpenAPIObject(
        withExtensions = true,
        reviewedDateExtension = Some("2021-07-24"),
        oAuth2SecuritySchemeName = Some("oAuth2Test"),
        globalScopes = globalScopes,
        endpointScopes = endpointScopes
      )

      val result: ValidatedNel[List[String], ApiDetail] =
        objInTest.extractOpenApi(
          Some(apiDetail0.publisherReference),
          apiDetail0.platform,
          apiDetail0.specificationType,
          openApi,
          openApiSpecificationContent = apiDetail0.openApiSpecification
        )

      result match {
        case Valid(apiDetail) =>
          checkEndpointScopes(apiDetail, expectedScopes)
        case invalid =>
          fail(s"Result was not a valid ApiDetail: $invalid")
      }
    }

    "extract empty scopes when there is an OAuth2 security scheme but no endpoint or global scopes" in new Setup {
      when(mockUuidService.newUuid()).thenReturn(generatedUuid)

      val expectedScopes: Map[String, List[String]] = Map(
        oasPath1Uri -> List.empty,
        oasPath2Uri -> List.empty,
        oasPath3Uri -> List.empty
      )

      val openApi: OpenAPI = getOpenAPIObject(
        withExtensions = true,
        reviewedDateExtension = Some("2021-07-24"),
        oAuth2SecuritySchemeName = Some("oAuth2Test")
      )

      val result: ValidatedNel[List[String], ApiDetail] =
        objInTest.extractOpenApi(
          Some(apiDetail0.publisherReference),
          apiDetail0.platform,
          apiDetail0.specificationType,
          openApi,
          openApiSpecificationContent = apiDetail0.openApiSpecification
        )

      result match {
        case Valid(apiDetail) =>
          checkEndpointScopes(apiDetail, expectedScopes)
        case invalid =>
          fail(s"Result was not a valid ApiDetail: $invalid")
      }
    }
  }

  "extract oauth scopes and definitions for all scopes being used" in new Setup {
    when(mockUuidService.newUuid()).thenReturn(generatedUuid)
    val globalScopes: List[String] = List("read:global", "write:global")

    val endpointScopes: Map[String, List[String]] = Map(
      oasPath1Uri -> List("read:scope-common", s"read:scope-path1"),
      oasPath2Uri -> List.empty,
      oasPath3Uri -> List("read:scope-common", s"read:scope-path3")
    )

    private val oauthScopes: List[(String, String)] = List[(String, String)](
      ("read:global", "Global READ scope"),
      ("read:scope-common", "READ scope COMMON"),
      ("read:scope-cheese", "CHEESE scope"))

    private val expectedScopes: Set[Scope] = Set(
      Scope("read:global", Option("Global READ scope")),
      Scope("write:global", Option.empty),
      Scope("read:scope-common", Option("READ scope COMMON")),
      Scope("read:scope-path1",Option.empty),
      Scope("read:scope-path3",Option.empty))

    val openApi: OpenAPI = getOpenAPIObject(
      withExtensions = true,
      reviewedDateExtension = Some("2021-07-24"),
      oAuth2SecuritySchemeName = Some("oAuth2Test"),
      globalScopes = globalScopes,
      endpointScopes = endpointScopes,
      oauthFlowScopes = Map("clientCredentials" -> oauthScopes))

    val result: ValidatedNel[List[String], ApiDetail] =
      objInTest.extractOpenApi(
        Some(apiDetail0.publisherReference),
        apiDetail0.platform,
        apiDetail0.specificationType,
        openApi,
        openApiSpecificationContent = apiDetail0.openApiSpecification
      )

    result match {
      case Valid(apiDetail) =>
        checkScopeDefinitions(apiDetail, expectedScopes)
      case invalid =>
        fail(s"Result was not a valid ApiDetail: $invalid")
    }
  }

  "not provide any definitions listed in oauth flows that are not also mentioned elsewhere" in new Setup {
    when(mockUuidService.newUuid()).thenReturn(generatedUuid)
    val globalScopes: List[String] = List("read:global", "write:global")

    val endpointScopes: Map[String, List[String]] = Map(
      oasPath1Uri -> List("read:scope-common", s"read:scope-path1"),
      oasPath2Uri -> List.empty,
      oasPath3Uri -> List("read:scope-common", s"read:scope-path3")
    )

    private val definedScopes: List[(String, String)] = List[(String, String)](
      ("read:cheese", "CHEESE scope"),
      ("read:crackers", "CRACKERS scope"))

    private val expectedScopes: Set[Scope] = Set(
      Scope("read:global", Option.empty),
      Scope("write:global", Option.empty),
      Scope("read:scope-common", Option.empty),
      Scope("read:scope-path1", Option.empty),
      Scope("read:scope-path3", Option.empty))

    val openApi: OpenAPI = getOpenAPIObject(
      withExtensions = true,
      reviewedDateExtension = Some("2021-07-24"),
      oAuth2SecuritySchemeName = Some("oAuth2Test"),
      globalScopes = globalScopes,
      endpointScopes = endpointScopes,
      oauthFlowScopes = Map("clientCredentials" -> definedScopes))

    val result: ValidatedNel[List[String], ApiDetail] =
      objInTest.extractOpenApi(
        Some(apiDetail0.publisherReference),
        apiDetail0.platform,
        apiDetail0.specificationType,
        openApi,
        openApiSpecificationContent = apiDetail0.openApiSpecification
      )

    result match {
      case Valid(apiDetail) =>
        checkScopeDefinitions(apiDetail, expectedScopes)
      case invalid =>
        fail(s"Result was not a valid ApiDetail: $invalid")
    }
  }
  private def checkEndpointScopes(apiDetail: ApiDetail, expectedScopes: Map[String, List[String]]): Unit = {
    apiDetail.endpoints.foreach(
      endpoint =>
        endpoint.methods.foreach(
          method =>
            method.scopes shouldBe expectedScopes(endpoint.path)
        )
    )
  }

  private def checkScopeDefinitions(apiDetail: ApiDetail, expectedScopes: Set[Scope]): Unit = {
    apiDetail.scopes should be(expectedScopes)
  }

}
