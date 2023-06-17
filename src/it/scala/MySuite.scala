import challenge.model.{InputPayload, InputPayloadDateTime, OutputPayload}
import zio.http.{Body, Client, Method}
import zio.json.EncoderOps
import zio.test._
import zio.json._
import zio._

import java.time.LocalDate

object HelloWorldSpec extends ZIOSpecDefault {
  import Encoders._
  import Decoders._
  def spec =
    suite("HTTP Tests") {
      test("Test coding challenge query") {
        for {
          _ <- Client.request("http://www.google.de", method = Method.POST)
          res <- Client.request(
            "http://localhost:8080/amazon/best-rated",
            method = Method.POST,
            content = Body.fromString(
              InputPayload(
                start = InputPayloadDateTime(LocalDate.of(2010, 1, 1)),
                end = InputPayloadDateTime(LocalDate.of(2020, 12, 31)),
                limit = 2,
                min_number_reviews = 2
              ).toJson.toString
            )
          )
          bodyStr <- res.body.asString
          parsedResult <- ZIO.fromEither(bodyStr.fromJson[Array[OutputPayload]])
        } yield assertTrue(
          parsedResult == Array(
            OutputPayload(
              asin = "B000JQ0JNS",
              average_rating = 4.5
            ),
            OutputPayload(
              asin = "B000NI7RW8",
              average_rating = 3.666666666666666666666666666666667
            )
          )
        )
      }
    }.provideLayer(Client.default)
}
