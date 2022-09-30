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

package uk.gov.hmrc.integrationcatalogue.parser.oas

import cats.data.Validated._
import cats.data._
import cats.implicits._
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.mockito.scalatest.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.integrationcatalogue.models.ApiDetail
import uk.gov.hmrc.integrationcatalogue.models.common.{ContactInformation, IntegrationId, PlatformType, SpecificationType}
import uk.gov.hmrc.integrationcatalogue.parser.oas.adapters.OASV3Adapter
import uk.gov.hmrc.integrationcatalogue.service.UuidService
import uk.gov.hmrc.integrationcatalogue.testdata.{ApiTestData, OasTestData}

import java.util.UUID
import uk.gov.hmrc.integrationcatalogue.config.AppConfig
import org.joda.time.format.ISODateTimeFormat
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class OASV3AdapterSpec extends AnyWordSpec with Matchers with MockitoSugar with ApiTestData with OasTestData with BeforeAndAfterEach {

  trait Setup {
    val publisherRef                        = "publisherRef"
    val platform: PlatformType.CORE_IF.type = PlatformType.CORE_IF
    val specType: SpecificationType         = SpecificationType.OAS_V3
    val generatedUuid: UUID                 = UUID.fromString("f26babbb-c9b1-4b79-b99a-9f99cf741f78")
    val mockUuidService: UuidService        = mock[UuidService]
    val mockAppConfig                       = mock[AppConfig]

    val objInTest = new OASV3Adapter(mockUuidService, mockAppConfig)

    val parseSuccess: ValidatedNel[List[String], ApiDetail] = valid(apiDetail0.copy(id = IntegrationId(generatedUuid)))

    def createInvalidMessage(messages: List[String]): Validated[NonEmptyList[List[String]], Nothing] = {
      invalid(NonEmptyList[List[String]](messages, List()))
    }

    val parseFailure: ValidatedNel[List[String], ApiDetail]                                          =
      invalid(NonEmptyList[List[String]](List("Invalid OAS, info item missing from OAS specification"), List()))

    val reviewedDate = DateTime.parse("25/12/20", DateTimeFormat.forPattern("dd/MM/yy"));
  }

  "extractOpenApi" should {
    "do happy path with extensions" in new Setup {
      when(mockUuidService.newUuid()).thenReturn(generatedUuid)
      val hods                                          = List("ITMP", "NPS")
      val expectedReviewedDate                          = DateTime.parse("2021-07-24T00:00:00", ISODateTimeFormat.dateOptionalTimeParser());
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

        case errors: Invalid[NonEmptyList[List[String]]] => {
          errors.e.head.foreach(println)
          fail()
        }
      }

    }

    "do happy path with reviewedDate but without backends and publisherRef extensions" in new Setup {
      when(mockUuidService.newUuid()).thenReturn(generatedUuid)
      val expectedReviewedDate                          = DateTime.parse("2021-07-24T00:00:00", ISODateTimeFormat.dateOptionalTimeParser());
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
      result match {
        case Valid(parsedObject)                    => fail()
        case _: Invalid[NonEmptyList[List[String]]] => succeed
      }

    }

    "parse extensions returns error(s)" in new Setup {
      val result: ValidatedNel[List[String], ApiDetail] = objInTest.extractOpenApi(
        Some(apiDetail0.publisherReference),
        apiDetail0.platform,
        apiDetail0.specificationType,
        getOpenAPIObject(true, null),
        openApiSpecificationContent = apiDetail0.openApiSpecification
      )
      result match {
        case _: Invalid[NonEmptyList[List[String]]] => succeed
        case Valid(parsedObject)                    => fail

      }

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
      parseResult shouldBe createInvalidMessage(List("Invalid OAS, title missing from OAS specification", "Invalid OAS, version missing from OAS specification"))
    }

  }

}
