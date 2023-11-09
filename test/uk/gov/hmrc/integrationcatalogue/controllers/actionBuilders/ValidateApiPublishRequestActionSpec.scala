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

package uk.gov.hmrc.integrationcatalogue.controllers.actionBuilders

import cats.syntax.validated._
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.integrationcatalogue.models.common.{PlatformType, SpecificationType}
import uk.gov.hmrc.integrationcatalogue.models.{ErrorResponseMessage, ExtractedHeaders, ValidatedApiPublishRequest}

import scala.concurrent.ExecutionContext.Implicits.global

class ValidateApiPublishRequestActionSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {

  trait Setup {
    val mockPublishHeaderValidator = mock[PublishHeaderValidator]
    val requestAction              = new ValidateApiPublishRequestAction(mockPublishHeaderValidator)
  }

  "ValidateApiPublishRequestAction" should {

    "return a validated publish request when header validation succeeds" in new Setup {
      val request            = FakeRequest()
      val publisherReference = Some("publisherReference")
      val platformType       = PlatformType.HIP
      val specificationType  = SpecificationType.OAS_V3
      val extractedHeaders   = ExtractedHeaders(publisherReference, platformType, specificationType)
      val expectedResult     = Right(ValidatedApiPublishRequest(publisherReference, platformType, specificationType, request))

      when(mockPublishHeaderValidator.validateHeaders(request)).thenReturn(extractedHeaders.validNel)

      val result = requestAction.refine(request).futureValue
      result shouldBe expectedResult
    }

    "return bad request when header validation fails" in new Setup {
      val request = FakeRequest()

      when(mockPublishHeaderValidator.validateHeaders(request)).thenReturn(ErrorResponseMessage("error").invalidNel[ExtractedHeaders])

      val result = requestAction.refine(request).futureValue

      result match {
        case Left(result: Result) =>
          result.header.status shouldBe BAD_REQUEST
        case _                    => fail(s"Unexpected result: $result")
      }
    }
  }
}
