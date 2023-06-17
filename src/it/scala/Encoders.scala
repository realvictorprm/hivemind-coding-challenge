import challenge.model.{InputPayload, InputPayloadDateTime}
import challenge.model.InputPayloadDateTime.InputPayloadDateTime
import zio.json.{DeriveJsonEncoder, JsonEncoder}

object Decoders {

  import java.time.format.DateTimeFormatter

  // The magic of DateTimeFormatter, yay!
  implicit val specialDateTimeEncoder: JsonEncoder[InputPayloadDateTime] = {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    JsonEncoder.string.contramap((date: InputPayloadDateTime) =>
      InputPayloadDateTime.unwrap(date).format(formatter)
    )
  }
  implicit val inputPayloadEncoder: JsonEncoder[InputPayload] =
    DeriveJsonEncoder.gen[InputPayload]
}
