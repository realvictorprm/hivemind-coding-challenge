package challenge.model

import challenge.model.InputPayloadDateTime.InputPayloadDateTime
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}
import zio.prelude.Newtype

import java.time.{LocalDate, ZoneOffset}
import scala.language.postfixOps
import scala.util.Try

// To conform with the input payload unusual date time format,
// we use a newtype wrapping the localdate type
// and create a custom decoder for it.
object InputPayloadDateTime extends Newtype[LocalDate] {
  import java.time.format.{DateTimeFormatter, DateTimeParseException}

  // To complete the newtype declaration
  type InputPayloadDateTime = InputPayloadDateTime.Type

  // The magic of DateTimeFormatter, yay!
  implicit val decoder: JsonDecoder[InputPayloadDateTime] = {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    JsonDecoder.string.mapOrFail((it: String) =>
      Try(LocalDate.parse(it, formatter)).fold(
        err => {
          val errMsg = err match {
            case _: DateTimeParseException =>
              s"Failed parsing time field, did you make sure to use the format dd.MM.yyyy?"
            case _ =>
              s"Unknown failure while parsing date time field. ${err.toString}"
          }
          Left(errMsg)
        },
        date => Right(InputPayloadDateTime.wrap(date))
      )
    )
  }

  // The data in ElasticSearch is using UnixTimestamps thus
  // we need a function to convert the payload date time into a timestamp
  def toUnixTimestamp(date: InputPayloadDateTime): Long =
    InputPayloadDateTime
      .unwrap(date)
      .atStartOfDay(ZoneOffset.UTC)
      .toEpochSecond
}

// The min_number_reviews field name does not conform with standard styling.
// However adding an extra type for converting between payload type and inner used type
// seemed a bit excessive to me.
case class InputPayload(
    start: InputPayloadDateTime,
    end: InputPayloadDateTime,
    limit: Int,
    min_number_reviews: Int
)

object InputPayload {
  implicit val decoder: JsonDecoder[InputPayload] =
    DeriveJsonDecoder.gen[InputPayload]
}

case class OutputPayload(asin: String, average_rating: Double)

object OutputPayload {
  implicit val encoder: JsonEncoder[OutputPayload] =
    DeriveJsonEncoder.gen[OutputPayload]
}
