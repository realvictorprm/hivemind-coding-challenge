import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties, RequestSuccess}
import zio.*
import zio.http.*
import zio.*
import zio.schema.{DeriveSchema, Schema}
import zio.stream.{ZSink, ZStream}
import zio.json.*
import zio.json.ast.Json

import java.time.{LocalDate, LocalTime, ZoneOffset}
import scala.io.Source
import scala.language.postfixOps
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.fields.KeywordField
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.searches.aggs.DateRangeAggregation
import com.sksamuel.elastic4s.zio.instances.*
import com.sksamuel.elastic4s.requests.searches.aggs.*
import com.sksamuel.elastic4s.requests.searches.aggs.responses.bucket.Terms
import com.sksamuel.elastic4s.requests.searches.queries.*

// {
//  "start": "01.01.2010",
//  "end": "31.12.2020",
//  "limit": 2,
//  "min_number_reviews": 2
// }
case class InputPayload(
    start: LocalDate,
    end: LocalDate,
    limit: Int,
    min_number_reviews: Int
)

object InputPayload {
  implicit val encoder: JsonEncoder[InputPayload] =
    DeriveJsonEncoder.gen[InputPayload]
  implicit val decoder: JsonDecoder[InputPayload] =
    DeriveJsonDecoder.gen[InputPayload]
}

case class OutputPayload(asin: String, average_rating: Double)

object OutputPayload {
  implicit val encoder: JsonEncoder[OutputPayload] =
    DeriveJsonEncoder.gen[OutputPayload]
  implicit val decoder: JsonDecoder[OutputPayload] =
    DeriveJsonDecoder.gen[OutputPayload]
}

case class Review(
    asin: String,
    helpful: List[Int],
    overall: Double,
    reviewText: String,
    reviewerName: String,
    summary: String,
    unixReviewTime: Long
)

object Review {
  implicit val encoder: JsonEncoder[Review] = DeriveJsonEncoder.gen[Review]
  implicit val decoder: JsonDecoder[Review] = DeriveJsonDecoder.gen[Review]
}

object HelloWorld extends ZIOAppDefault {

  import concurrent.ExecutionContext.Implicits.global
  import OutputPayload._

  val indexName = "index"

  def responseError(msg: String) =
    Response.json(msg).withStatus(Status.InternalServerError)

  def app(client: ElasticClient): App[Any] =
    Http
      .collectZIO[Request] {
        case req @ Method.POST -> Root / "amazon" / "best-rated" =>
          for {
            // We need to extract the information from the body first
            body <- req.body.asString.mapError(it =>
              responseError(it.getMessage)
            )
            payload <- ZIO
              .fromEither(body.fromJson[InputPayload])
              .mapError(it => responseError(it))
            // Use the payload to construct the search-aggregation request
            req = search(indexName)
              // Select only those values which are within the requested timeframe.
              // Yes the way time is handled here could possibly be nicer.
              .query(
                RangeQuery("unixReviewTime")
                  .lte(
                    payload.end.toEpochSecond(LocalTime.NOON, ZoneOffset.MIN)
                  )
                  .gte(
                    payload.start.toEpochSecond(LocalTime.NOON, ZoneOffset.MIN)
                  )
              )
              .aggs(
                // We want to group by asin and avg the overall field while ordering descending
                TermsAggregation("avgOverall")
                  .field("asin")
                  .size(payload.limit)
                  .minDocCount(payload.min_number_reviews)
                  .subaggs(AvgAggregation("overall").field("overall"))
                  .order(TermsOrder("overall", false))
              )
            // Excecute the search request and extract the resulting avg-overall data to construct the response
            res <- client
              .execute(req)
              .map {
                case RequestSuccess(_, _, _, res) =>
                  Response.json(
                    res.aggs
                      .result[Terms]("avgOverall")
                      .buckets
                      .map { bucket =>
                        OutputPayload(bucket.key, bucket.avg("overall").value)
                      }
                      .toJson
                  )
                case rest => responseError(rest.toString)
              }
              .mapError { it => responseError(it.getMessage) }
          } yield res
      }
      .mapError { error =>
        println(error.toString)
        error
      }

  override val run =
    for {
      // Begin with reading in the command line argument that provides the path to the sample data
      args <- this.getArgs
      ingestFileUrl = args.headOption.getOrElse("./amazon-reviews.json")
      _ = println(ingestFileUrl)
      // Init the elastic client
      client <- ZIO.fromAutoCloseable(
        ZIO.from(
          ElasticClient(JavaClient(ElasticProperties("http://localhost:9200")))
        )
      )
      // We need a fresh index with asin being a keyword field for group-by operations
      _ <- client.execute(deleteIndex(indexName))
      _ <- client.execute(
        createIndex(indexName).mapping(properties(keywordField("asin")))
      )
      // Read in the sample data and ingest it into the elasticsearch index as a bulk
      source <- ZIO.fromAutoCloseable(ZIO.from(Source.fromFile(ingestFileUrl)))
      // Side Note: The chunk size is something that needs to be tested invidually for the machine.
      // It definitely is necessary as the ingestion process is otherwise either way too slow or without any chunking
      // we will crash at some point as we run out of memory.
      // My number can be quite optimistic considering how long a line can be ;)
      _ <- ZStream
        .fromIterator(source.getLines(), maxChunkSize = 50_000)
        .runForeachChunkScoped { lines =>
          println("ingesting " + lines.size)
          lines
            .map { line =>
              ZIO
                .fromEither(line.fromJson[Review])
                .map(payload => indexInto(indexName).source(payload.toJson))
            }
            .collectZIO(identity(_))
            .flatMap { requests =>
              val it = requests.toArray
              client.execute(bulk(it: _*))
            }
        }

      _ = println("starting webserver")
      // Get the webserver running
      _ <- Server.serve(app(client)).provide(Server.defaultWithPort(8080))
    } yield ()
}
