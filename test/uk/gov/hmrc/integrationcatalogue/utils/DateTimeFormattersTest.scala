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

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters.instantReads

import java.time.{Instant, OffsetDateTime, ZoneOffset}

class DateTimeFormattersTest extends AnyWordSpec with DateTimeFormatters with Matchers with TableDrivenPropertyChecks {

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

    "parse ISO 8601 Date Time with fractions of a second into Instant" in {
      forAll(Table(
        "dateWithFractionsOfASecond",
        "2025-01-23T01:12:23.34567889Z",
        "2025-01-23T01:12:23.3456788Z",
        "2025-01-23T01:12:23.345678Z",
        "2025-01-23T01:12:23.34567Z",
        "2025-01-23T01:12:23.3456Z",
        "2025-01-23T01:12:23.345Z",
        "2025-01-23T01:12:23.34Z",
        "2025-01-23T01:12:23.3Z"
      )) { dateWithFractionsOfASecond =>
        val expected = Instant.parse(dateWithFractionsOfASecond)
        val actual = instantReads.reads(Json.parse(s"\"${dateWithFractionsOfASecond}\""))
        actual must be(JsSuccess(expected))
      }
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