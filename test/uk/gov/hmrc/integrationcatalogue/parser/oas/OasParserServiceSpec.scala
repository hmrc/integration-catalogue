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

import scala.jdk.CollectionConverters._

import cats.data.Validated._
import cats.data._
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.core.models.SwaggerParseResult
import org.mockito.scalatest.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import uk.gov.hmrc.integrationcatalogue.models.ApiDetail
import uk.gov.hmrc.integrationcatalogue.models.common.{PlatformType, SpecificationType}
import uk.gov.hmrc.integrationcatalogue.parser.oas.adapters.OASV3Adapter
import uk.gov.hmrc.integrationcatalogue.testdata.{ApiTestData, OasTestData}

class OasParserServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with ApiTestData with OasTestData with BeforeAndAfterEach {

  val mockAdapterService: OASV3Adapter           = mock[OASV3Adapter]
  val mockFileLoader: OASFileLoader              = mock[OASFileLoader]
  val mockSwaggerParseResult: SwaggerParseResult = mock[SwaggerParseResult]

  trait Setup {

    val errors                         = List("error1", "error2")
    val OASSpecType: SpecificationType = SpecificationType.OAS_V3
    val objInTest                      = new OASParserService(mockFileLoader, mockAdapterService)
    when(mockFileLoader.parseOasSpec(*)).thenReturn(mockSwaggerParseResult)

    def primeSuccessNoWarnings() : Unit = {
      when(mockSwaggerParseResult.getOpenAPI).thenReturn(getOpenAPIObject(withExtensions = false))
      when(mockSwaggerParseResult.getMessages).thenReturn(null)
    }

    def primeFailureWithErrors() : Unit = {
      when(mockSwaggerParseResult.getOpenAPI).thenReturn(null)

      when(mockSwaggerParseResult.getMessages).thenReturn(errors.asJava)
    }
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockFileLoader, mockAdapterService, mockSwaggerParseResult)
  }

  "OASParserService" should {
    "parse OpenApi object correctly" in new Setup {
      val publisherReference = "SOMEFILEREFERENCE"

      primeSuccessNoWarnings()

      when(mockAdapterService.extractOpenApi(*, *, *, *, *)).thenReturn(Valid(apiDetail0))

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some(publisherReference), PlatformType.CORE_IF, OASSpecType, rawOASData(oasContactName))

      result match {
        case Valid(parsedObject) =>
          parsedObject shouldBe apiDetail0
          verify(mockAdapterService).extractOpenApi(eqTo(Some(publisherReference)), eqTo(PlatformType.CORE_IF), eqTo(OASSpecType), *, *)

        case _: Invalid[NonEmptyList[List[String]]] => fail()
      }
    }

    "handle when OAS version is not 3" in new Setup {
      val publisherReference = "SOMEFILEREFERENCE"

      val openApi: OpenAPI = getOpenAPIObject(withExtensions = false)
      openApi.setOpenapi("2.0")
      when(mockSwaggerParseResult.getOpenAPI).thenReturn(openApi)
      when(mockSwaggerParseResult.getMessages).thenReturn(new java.util.ArrayList())

      when(mockAdapterService.extractOpenApi(*, *, *, *, *)).thenReturn(Valid(apiDetail0))

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some(publisherReference), PlatformType.CORE_IF, OASSpecType, rawOASData(oasContactName))

      result match {
        case Valid(parsedObject)                               => fail("parsed object shouold not be returned")
        case errorMessage: Invalid[NonEmptyList[List[String]]] => {
          errorMessage.e.head shouldBe List(s"Unhandled OAS specification version for platform ${PlatformType.CORE_IF} OAS")
        }
      }
    }

    "handle when spec file is not present" in new Setup {

      primeFailureWithErrors()
      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some("someref"), PlatformType.CORE_IF, OASSpecType, "{Unparseable}")
      result match {
        case _: Valid[ApiDetail]                               => fail("should not return a parsed object")
        case errorMessage: Invalid[NonEmptyList[List[String]]] => {
          errorMessage.e.head shouldBe errors
        }
      }

    }

    "handle when OasParser returns object with null info and no errors" in new Setup {

      when(mockSwaggerParseResult.getOpenAPI).thenReturn(null)

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some("someref"), PlatformType.CORE_IF, OASSpecType, "{Unparseable}")
      result match {
        case _: Valid[ApiDetail]                               => fail("should not return a parsed object")
        case errorMessage: Invalid[NonEmptyList[List[String]]] => {
          errorMessage.e.head shouldBe List("Error loading OAS specification for platform CORE_IF OAS")
        }
      }
    }

    "handle when OasParser returns null" in new Setup {

      when(mockFileLoader.parseOasSpec(*)).thenReturn(null)

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some("someref"), PlatformType.CORE_IF, OASSpecType, "{Unparseable}")
      result match {
        case _: Valid[ApiDetail]                               => fail("should not return a parsed object")
        case errorMessage: Invalid[NonEmptyList[List[String]]] => {
          errorMessage.e.head shouldBe List("Oas Parser returned null")
        }
      }

    }
  }
}
