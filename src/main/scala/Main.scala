import zio.*
import zio.http.*
import sttp.client3.httpclient.zio.HttpClientZioBackend
import zio.elasticsearch.*
import zio.*
import zio.elasticsearch.aggregation.ElasticAggregation
import zio.elasticsearch.request.CreationOutcome
import zio.elasticsearch.result.SearchAndAggregateResult
import zio.schema.{DeriveSchema, Schema}
import zio.stream.{ZSink, ZStream}
import zio.json.*
import zio.json.ast.Json

import java.time.{LocalDate, ZoneOffset}
import scala.io.Source
import scala.language.postfixOps

// {
//  "start": "01.01.2010",
//  "end": "31.12.2020",
//  "limit": 2,
//  "min_number_reviews": 2
// }
case class Payload(start: LocalDate, end: LocalDate, limit: Int, min_number_reviews: Int)

object Payload {
  implicit val decoder: JsonDecoder[Payload] = DeriveJsonDecoder.gen[Payload]
}

// {"asin":"","helpful":[],"overall":null,"reviewText":"","reviewerID":"","reviewerName":"","summary":"","unixReviewTime":-1}
case class Review(asin: String, helpful: List[Int], overall: Float, reviewText: String, reviewerName: String, summary: String, unixReviewTime: Long)

object Review {
  implicit val schema: Schema.CaseClass7[_, _, _, _, _, _, _, Review] = DeriveSchema.gen[Review]
  implicit val decoder: JsonDecoder[Review] = DeriveJsonDecoder.gen[Review]
  val (asin, helpful, overall, reviewText, reviewerName, summary, unixReviewTime) = schema.makeAccessors(FieldAccessorBuilder)
}

object HelloWorld extends ZIOAppDefault {
  val indexName = IndexName("index")

  def responseError(msg: String) = Response.text(msg).withStatus(Status.InternalServerError)

  def getEpoch(localDate: LocalDate) = localDate.toEpochDay

  def app(): App[Any] =
    Http.collectZIO[Request] {
      case req@Method.POST -> Root / "amazon" / "best-rated" =>
        for {
          body <- req.body.asString.mapError(it => responseError(it.getMessage))
          _ = println("extracted body")
          payload <- ZIO.fromEither(body.fromJson[Payload]).mapError(it => responseError(it))
          _ = println(payload)
          agg = ElasticAggregation.termsAggregation("agg", "overall").withSubAgg(ElasticAggregation.("avg", "overall"))
          _ = println(agg.paramsToJson.toString())
          req = ElasticRequest
            .search(indexName, ElasticQuery.range(Review.unixReviewTime).gte(payload.start.toEpochDay).lt(payload.end.toEpochDay))
            .aggregate(agg)
          res <- Elasticsearch
            .execute(req).provide(
            ElasticExecutor.local,
            Elasticsearch.layer,
            HttpClientZioBackend.layer()
          ).aggregation("asin").map { it => Response.json(it.map(_.toString()).toString()) }
            .mapError { it => responseError(it.getMessage) }
        } yield res
    }

  override val run =
    for {
      args <- this.getArgs
      ingestFileUrl = args.headOption.getOrElse("./sampleData.json")
      _ = println(ingestFileUrl)
      _ <- Elasticsearch.execute(ElasticRequest.createIndex(indexName)).provide(
        ElasticExecutor.local,
        Elasticsearch.layer,
        HttpClientZioBackend.layer()
      )
      source <- ZIO.fromAutoCloseable(ZIO.from(Source.fromFile(ingestFileUrl)))
      lines <- ZStream.fromIterator(source.getLines()).run(ZSink.collectAll)
      res <- lines.map { line =>
        ZIO.fromEither(line.fromJson[Review]).map(payload => ElasticRequest.create[Review](indexName, payload))
      }.collectZIO(identity(_)).flatMap { requests =>
        val it = requests.toArray[BulkableRequest[_]]
        Elasticsearch.execute(ElasticRequest.bulk(it: _*)).provide(
          ElasticExecutor.local,
          Elasticsearch.layer,
          HttpClientZioBackend.layer()
        )
      }

      _ = println(res)
      _ = println("starting webserver")
      _ <- Server.serve(app()).provide(Server.defaultWithPort(8080))
    } yield ()
}
