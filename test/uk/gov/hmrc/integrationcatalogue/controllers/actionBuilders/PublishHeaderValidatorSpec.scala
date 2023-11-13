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
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.FakeRequest
import uk.gov.hmrc.integrationcatalogue.models.common.{PlatformType, SpecificationType}
import uk.gov.hmrc.integrationcatalogue.models.{ErrorResponseMessage, ExtractedHeaders, HeaderKeys}

class PublishHeaderValidatorSpec extends AnyWordSpec with Matchers {

  trait Setup {
    val headerValidator   = new PublishHeaderValidator()
    val referenceKey      = "some-value"
    val platformType      = PlatformType.HIP
    val specificationType = SpecificationType.OAS_V3
  }

  "PublishHeaderValidator" should {

    "return extracted headers when the request contains valid publish headers" in new Setup {
      val request  = FakeRequest().withHeaders(
        HeaderKeys.publisherRefKey      -> referenceKey,
        HeaderKeys.platformKey          -> platformType.toString,
        HeaderKeys.specificationTypeKey -> specificationType.toString
      )
      val expected = ExtractedHeaders(Some(referenceKey), platformType, specificationType).validNel

      headerValidator.validateHeaders(request) shouldBe expected
    }

    "return extracted headers when publisher reference header is missing" in new Setup {
      val request  = FakeRequest().withHeaders(
        HeaderKeys.platformKey          -> platformType.toString,
        HeaderKeys.specificationTypeKey -> specificationType.toString
      )
      val expected = ExtractedHeaders(None, platformType, specificationType).validNel

      headerValidator.validateHeaders(request) shouldBe expected
    }

    "return error when platform type header is missing" in new Setup {
      val request = FakeRequest().withHeaders(
        HeaderKeys.publisherRefKey      -> referenceKey,
        HeaderKeys.specificationTypeKey -> specificationType.toString
      )

      val expected = ErrorResponseMessage("platform type header is missing or invalid").invalidNel

      headerValidator.validateHeaders(request) shouldBe expected
    }

    "return error when an invalid platform type header is provided" in new Setup {
      val request = FakeRequest().withHeaders(
        HeaderKeys.publisherRefKey      -> referenceKey,
        HeaderKeys.platformKey          -> "INVALID",
        HeaderKeys.specificationTypeKey -> specificationType.toString
      )

      val expected = ErrorResponseMessage("platform type header is missing or invalid").invalidNel

      headerValidator.validateHeaders(request) shouldBe expected
    }

    "return error when the platform type header is missing" in new Setup {
      val request = FakeRequest().withHeaders(
        HeaderKeys.publisherRefKey      -> referenceKey,
        HeaderKeys.specificationTypeKey -> specificationType.toString
      )

      val expected = ErrorResponseMessage("platform type header is missing or invalid").invalidNel

      headerValidator.validateHeaders(request) shouldBe expected

    }

    "return error when an invalid specification type header is provided" in new Setup {
      val request  = FakeRequest().withHeaders(
        HeaderKeys.publisherRefKey      -> referenceKey,
        HeaderKeys.platformKey          -> platformType.toString,
        HeaderKeys.specificationTypeKey -> "INVALID"
      )

      val expected = ErrorResponseMessage("specification type header is missing or invalid").invalidNel

      headerValidator.validateHeaders(request) shouldBe expected
    }

    "return error when the specification type header is missing" in new Setup {
      val request  = FakeRequest().withHeaders(
        HeaderKeys.publisherRefKey      -> referenceKey,
        HeaderKeys.platformKey          -> platformType.toString
      )

      val expected = ErrorResponseMessage("specification type header is missing or invalid").invalidNel

      headerValidator.validateHeaders(request) shouldBe expected
    }
  }
}
