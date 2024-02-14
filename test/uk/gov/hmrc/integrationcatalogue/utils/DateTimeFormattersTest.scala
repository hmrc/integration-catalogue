/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.integrationcatalogue.utils

import org.scalatest.matchers.must.Matchers.{be, convertToAnyMustWrapper}
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters.instantReads

import java.time.{Instant, OffsetDateTime, ZoneOffset}

class DateTimeFormattersTest extends AnyWordSpec with DateTimeFormatters {

  "dateAndOptionalTimeFormatter" should {
    "parse ISO 8601 Date Time With Z offset and nanos" in {

      val candidate = "2024-02-09T10:13:21.123456789Z"

      val expected = OffsetDateTime.of(2024, 2, 9, 10, 13, 21, 123456789, ZoneOffset.UTC).toInstant

      expected.toString must be(candidate)

      val actual = Instant.from(dateAndOptionalTimeFormatter.parse(candidate))

      actual must be(expected)
    }

    "parse ISO 8601 Date Time With Z offset and micros" in {

      val candidate = "2024-02-09T10:13:21.123456Z"

      val expected = OffsetDateTime.of(2024, 2, 9, 10, 13, 21, 123456000, ZoneOffset.UTC).toInstant

      expected.toString must be(candidate)

      val actual = Instant.from(dateAndOptionalTimeFormatter.parse(candidate))

      actual must be(expected)
    }

    "parse ISO 8601 Date Time With Z offset and millis" in {

      val candidate = "2024-02-09T10:13:21.123Z"

      val expected = OffsetDateTime.of(2024, 2, 9, 10, 13, 21, 123000000, ZoneOffset.UTC).toInstant

      expected.toString must be(candidate)

      val actual = Instant.from(dateAndOptionalTimeFormatter.parse(candidate))

      actual must be(expected)
    }

    "parse ISO 8601 Date Time With Z offset and no fractions" in {

      val candidate = "2024-02-09T10:13:21Z"

      val expected = OffsetDateTime.of(2024, 2, 9, 10, 13, 21, 0, ZoneOffset.UTC).toInstant

      expected.toString must be(candidate)

      val actual = Instant.from(dateAndOptionalTimeFormatter.parse(candidate))

      actual must be(expected)
    }

    "parse ISO 8601 Date Time With numeric offset and nanos" in {

      val candidate = "2024-02-09T10:13:21.123456789+0000"

      val expected = OffsetDateTime.of(2024, 2, 9, 10, 13, 21, 123456789, ZoneOffset.UTC).toInstant

      val actual = Instant.from(dateAndOptionalTimeFormatter.parse(candidate))

      actual must be(expected)
    }

    "parse ISO 8601 Date Time With numeric offset and micros" in {

      val candidate = "2024-02-09T10:13:21.123456+0000"

      val expected = OffsetDateTime.of(2024, 2, 9, 10, 13, 21, 123456000, ZoneOffset.UTC).toInstant

      val actual = Instant.from(dateAndOptionalTimeFormatter.parse(candidate))

      actual must be(expected)
    }

    "parse ISO 8601 Date Time With numeric offset and millis" in {

      val candidate = "2024-02-09T10:13:21.123+0000"

      val expected = OffsetDateTime.of(2024, 2, 9, 10, 13, 21, 123000000, ZoneOffset.UTC).toInstant

      val actual = Instant.from(dateAndOptionalTimeFormatter.parse(candidate))

      actual must be(expected)
    }

    "parse ISO 8601 Date Time With numeric offset and no fractions" in {

      val candidate = "2024-02-09T10:13:21+0000"

      val expected = OffsetDateTime.of(2024, 2, 9, 10, 13, 21, 0, ZoneOffset.UTC).toInstant

      val actual = Instant.from(dateAndOptionalTimeFormatter.parse(candidate))

      actual must be(expected)
    }

    "parse ISO 8601 Date Time With no offset and nanos" in {

      val candidate = "2024-02-09T10:13:21.123456789"
      val candidateAsOffsetDateTime = candidate + "Z"
      val expected = OffsetDateTime.of(2024, 2, 9, 10, 13, 21, 123456789, ZoneOffset.UTC).toInstant

      expected.toString must be(candidateAsOffsetDateTime)

      val actual = Instant.from(dateAndOptionalTimeFormatter.parse(candidate))

      actual must be(expected)
    }

    "parse ISO 8601 Date Time With no offset and micros" in {

      val candidate = "2024-02-09T10:13:21.123456"
      val candidateAsOffsetDateTime = candidate + "Z"

      val expected = OffsetDateTime.of(2024, 2, 9, 10, 13, 21, 123456000, ZoneOffset.UTC).toInstant

      expected.toString must be(candidateAsOffsetDateTime)

      val actual = Instant.from(dateAndOptionalTimeFormatter.parse(candidate))

      actual must be(expected)
    }

    "parse ISO 8601 Date Time With no offset and millis" in {

      val candidate = "2024-02-09T10:13:21.123"
      val candidateAsOffsetDateTime = candidate + "Z"
      val expected = OffsetDateTime.of(2024, 2, 9, 10, 13, 21, 123000000, ZoneOffset.UTC).toInstant

      expected.toString must be(candidateAsOffsetDateTime)

      val actual = Instant.from(dateAndOptionalTimeFormatter.parse(candidate))

      actual must be(expected)
    }

    "parse ISO 8601 Date Time With no offset and no fractions" in {

      val candidate = "2024-02-09T10:13:21"
      val candidateAsOffsetDateTime = candidate + "Z"
      val expected = OffsetDateTime.of(2024, 2, 9, 10, 13, 21, 0, ZoneOffset.UTC).toInstant

      expected.toString must be(candidateAsOffsetDateTime)

      val actual = Instant.from(dateAndOptionalTimeFormatter.parse(candidate))

      actual must be(expected)
    }

    "parse ISO 8601 Date" in {

      val candidate = "2024-02-09"
      val candidateAsOffsetDateTime = "2024-02-09T00:00:00Z"

      val expected = OffsetDateTime.of(2024, 2, 9, 0, 0, 0, 0, ZoneOffset.UTC).toInstant

      expected.toString must be(candidateAsOffsetDateTime)

      val actual = Instant.from(dateAndOptionalTimeFormatter.parse(candidate))

      actual must be(expected)
    }
  }

  "reads" should {
    "parse ISO 8601 Date into Instant" in {
      val expected = Instant.parse("2024-02-12T00:00:00Z")
      val actual = instantReads.reads(Json.parse("\"2024-02-12\""))
      actual must be(JsSuccess(expected))
    }

    "not parse non Iso 8601 strings" in {
      val actual = instantReads.reads(Json.parse("\"Cheese\""))
      actual must be(JsError("Could not interpret date[/time] as one of the supported ISO formats: \"Cheese\""))
    }

    "not parse other json types" in {
      val actual = instantReads.reads(Json.parse("12345"))
      actual must be(JsError("Could not interpret date[/time] as one of the supported ISO formats: 12345"))
    }
  }
}