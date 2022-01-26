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

package uk.gov.hmrc.integrationcatalogue.parser.oas.adapters

trait ExtensionKeys {
  val EXTENSIONS_KEY = "x-integration-catalogue"
  val BACKEND_EXTENSION_KEY = "backends"
  val PUBLISHER_REF_EXTENSION_KEY = "publisher-reference"
  val SHORT_DESC_EXTENSION_KEY = "short-description"
  val STATUS_EXTENSION_KEY = "status"
  val REVIEWED_DATE_EXTENSION_KEY = "reviewed-date"
}
