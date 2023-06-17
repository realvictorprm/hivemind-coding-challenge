package challenge.model

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class Review(
    asin: String,
    helpful: List[Int],
    overall: Double,
    reviewText: String,
    reviewerID: String,
    reviewerName: String,
    summary: String,
    unixReviewTime: Long
)

object Review {
  implicit val encoder: JsonEncoder[Review] = DeriveJsonEncoder.gen[Review]
  implicit val decoder: JsonDecoder[Review] = DeriveJsonDecoder.gen[Review]
}
