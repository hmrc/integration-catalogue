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

import play.api.mvc.Results.Unauthorized
import play.api.mvc._
import play.api.test.Helpers.AUTHORIZATION
import uk.gov.hmrc.integrationcatalogue.controllers.actionBuilders.IdentifierAction

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FakeIdentifierAction @Inject()
  (bodyParsers: PlayBodyParsers)
  (implicit override val executionContext: ExecutionContext) extends IdentifierAction {

  import FakeIdentifierAction._

  override def parser: BodyParser[AnyContent] = bodyParsers.default

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    request.headers.get(AUTHORIZATION) match {
      case Some(authorization) if authorization.equals(fakeAuthToken) => block.apply(request)
      case _ => Future.successful(Unauthorized)
    }
  }

}

object FakeIdentifierAction {

  val fakeAuthToken: String = "test-auth-token"
  val fakeAuthorizationHeader: (String, String) = (AUTHORIZATION, fakeAuthToken)

}
