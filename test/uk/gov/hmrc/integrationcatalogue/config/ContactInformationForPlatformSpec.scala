/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.integrationcatalogue.config

import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType
import uk.gov.hmrc.integrationcatalogue.models.common.ContactInformation

class ContactInformationForPlatformSpec extends WordSpec with Matchers {

  "getContactInformationForPlatform" should {
    "return Some ContactInformation when both name and email are passed in" in {
      val result = ContactInformationForPlatform.getContactInformationForPlatform(PlatformType.API_PLATFORM, Some("name"), Some("support@example.com"))
      result shouldBe Some(ContactInformation("name", "support@example.com"))
    }

    "return None when contact name not passed in" in {
      val result = ContactInformationForPlatform.getContactInformationForPlatform(PlatformType.API_PLATFORM, None, Some("support@example.com"))
      result shouldBe None
    }

    "return None when contact email is not passed in" in {
      val result2 = ContactInformationForPlatform.getContactInformationForPlatform(PlatformType.API_PLATFORM, Some("name"), None)
      result2 shouldBe None

    }

    "return None when both name and email are not passed in" in {
      val result3 = ContactInformationForPlatform.getContactInformationForPlatform(PlatformType.API_PLATFORM, None, None)
      result3 shouldBe None

    }
  }
}
