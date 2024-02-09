package uk.gov.hmrc.integrationcatalogue.utils

import java.time.ZoneOffset
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.ChronoField

trait DateTimeFormatters {

  val dateAndOptionalTimeFormatter: DateTimeFormatter = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd['T'HH:mm:ss[.SSSSSSSSS][.SSSSSS][.SSS][XXX]]")
    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
    .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
    .parseDefaulting(ChronoField.OFFSET_SECONDS, ZoneOffset.UTC.getTotalSeconds)
    .toFormatter()
}
