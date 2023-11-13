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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.integrationcatalogue.config.AppConfig
import uk.gov.hmrc.integrationcatalogue.models.HeaderKeys
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType

import scala.concurrent.ExecutionContext

class ValidateAuthorizationHeaderActionSpec extends AnyWordSpec with Matchers {

  trait Setup {
    def globalEc = scala.concurrent.ExecutionContext.Implicits.global

    def sourceAction(anyContentParser: BodyParser[AnyContent], validateAction: ValidateAuthorizationHeaderAction)
                    (implicit ec: ExecutionContext) =
      new ActionBuilderImpl[AnyContent](anyContentParser)(ec) andThen validateAction

    val applicationBuilder = new GuiceApplicationBuilder()

    def anyContentParser = Helpers.stubControllerComponents().parsers.anyContent
  }

  "ValidateAuthorizationHeaderAction" should {

    "return Ok for a valid master authorisation header" in new Setup {
      val application = applicationBuilder.configure(
        "authorizationKey" -> "test-auth-key"
      ).build()

      val request = FakeRequest().withHeaders(
        HeaderNames.AUTHORIZATION -> "dGVzdC1hdXRoLWtleQ=="
      )

      val validateAction: ValidateAuthorizationHeaderAction =
        new ValidateAuthorizationHeaderAction(application.injector.instanceOf[AppConfig])(globalEc)

      running(application) {
        val action = sourceAction(anyContentParser, validateAction)(globalEc) { _ => Results.Ok }
        status(action.apply(request)) shouldBe OK
      }
    }

    "return Ok for a valid platform authorisation header" in new Setup {
      val application: Application = applicationBuilder.configure(
        "auth.authKey.HIP" -> "someKey11"
      ).build()

      val request = FakeRequest().withHeaders(
        HeaderKeys.platformKey    -> PlatformType.HIP.toString,
        HeaderNames.AUTHORIZATION -> "c29tZUtleTEx"
      )

      val validateAction: ValidateAuthorizationHeaderAction =
        new ValidateAuthorizationHeaderAction(application.injector.instanceOf[AppConfig])(globalEc)

      running(application) {
        val action = sourceAction(anyContentParser, validateAction)(globalEc) { _ => Results.Ok }
        status(action.apply(request)) shouldBe OK
      }
    }

    "return bad request for an invalid master authorisation header" in new Setup {
      val application = applicationBuilder.configure(
        "authorizationKey" -> "test-auth-key"
      ).build()

      val request = FakeRequest().withHeaders(
        HeaderNames.AUTHORIZATION -> "invalidKey"
      )

      val validateAction: ValidateAuthorizationHeaderAction =
        new ValidateAuthorizationHeaderAction(application.injector.instanceOf[AppConfig])(globalEc)

      running(application) {
        val action = sourceAction(anyContentParser, validateAction)(globalEc) { _ => Results.Ok }
        status(action.apply(request)) shouldBe BAD_REQUEST
      }
    }

    "return unauthorised for an invalid platform authorisation header" in new Setup {
      val application: Application = applicationBuilder.configure(
        "auth.authKey.HIP" -> "someKey11"
      ).build()

      val request = FakeRequest().withHeaders(
        HeaderKeys.platformKey    -> PlatformType.HIP.toString,
        HeaderNames.AUTHORIZATION -> "invalidKey"
      )

      val validateAction: ValidateAuthorizationHeaderAction =
        new ValidateAuthorizationHeaderAction(application.injector.instanceOf[AppConfig])(globalEc)

      running(application) {
        val action = sourceAction(anyContentParser, validateAction)(globalEc) { _ => Results.Ok }
        status(action.apply(request)) shouldBe UNAUTHORIZED
      }
    }

    "return bad request for a missing platform authorisation header" in new Setup {
      val application: Application = applicationBuilder.configure(
        "auth.authKey.HIP" -> "someKey11"
      ).build()

      val request = FakeRequest()

      val validateAction: ValidateAuthorizationHeaderAction =
        new ValidateAuthorizationHeaderAction(application.injector.instanceOf[AppConfig])(globalEc)

      running(application) {
        val action = sourceAction(anyContentParser, validateAction)(globalEc) { _ => Results.Ok }
        status(action.apply(request)) shouldBe BAD_REQUEST
      }
    }
  }
}
