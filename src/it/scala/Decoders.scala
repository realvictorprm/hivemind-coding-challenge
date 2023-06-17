import challenge.model.OutputPayload
import zio.json.{DeriveJsonDecoder, JsonDecoder}

object Encoders {
  implicit val decoder: JsonDecoder[OutputPayload] =
    DeriveJsonDecoder.gen[OutputPayload]
}
