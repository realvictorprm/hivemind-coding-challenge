import com.sksamuel.elastic4s.http.{JavaClient, JavaClientExceptionWrapper}
import com.sksamuel.elastic4s.{
  ElasticClient,
  ElasticProperties,
  RequestFailure,
  RequestSuccess,
  Response
}
import zio.*
import zio.http.*
import zio.stream.ZStream
import zio.json.*

import java.time.{LocalDate, ZoneOffset}
import scala.io.Source
import scala.language.postfixOps
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.searches.aggs.*
import com.sksamuel.elastic4s.zio.instances.*
import com.sksamuel.elastic4s.requests.searches.aggs.responses.bucket.Terms
import com.sksamuel.elastic4s.requests.searches.queries.*

import scala.util.Try
import zio.prelude.Newtype

// To conform with the input payload unusual date time format,
// we use a newtype wrapping the localdate type
// and create a custom decoder for it.
// As there is no need for encoding I did not write an encoder.
object InputPayloadDateTime extends Newtype[LocalDate] {
  import java.time.format.DateTimeFormatter
  import java.time.format.DateTimeParseException

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

// To complete the newtype declaration
type InputPayloadDateTime = InputPayloadDateTime.Type

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

object HelloWorld extends ZIOAppDefault {

  import org.slf4j.LoggerFactory

  val logger = LoggerFactory.getLogger(getClass)

  import concurrent.ExecutionContext.Implicits.global

  val indexName = "index"

  // This could be improved but that requires much more time to do right
  def respondeWithInternalServerError(msg: String): http.Response =
    Response.text(msg).withStatus(Status.InternalServerError)

  def respondeWithServiceUnavailableError(msg: String): http.Response =
    Response.text(msg).withStatus(Status.ServiceUnavailable)

  // This just serves as a helper function to figure out whether we are failing to connect to ElasticSearch
  def catchElasticSearchConnectionError(error: Throwable): Option[String] =
    error match {
      case JavaClientExceptionWrapper(ex) =>
        ex match {
          case _: java.net.ConnectException =>
            Some("Failed to connect to elastic search")
          case _ => None
        }
      case _ => None
    }

  def app(client: ElasticClient) =
    Http
      .collectZIO[Request] {
        case req @ Method.POST -> Root / "amazon" / "best-rated" =>
          for {
            // We need to extract the information from the body first
            body <- req.body.asString.mapError(it =>
              respondeWithInternalServerError(
                "Failure extracting body: " + it.getMessage
              )
            )
            payload <- ZIO
              .fromEither(body.fromJson[InputPayload])
              .mapError(error =>
                respondeWithInternalServerError(
                  "Failure parsing input body: " + error
                )
              )
            // Use the payload to construct the search-aggregation request
            req = search(indexName)
              // Select only those values which are within the requested timeframe.
              // Yes the way time is handled here could possibly be nicer.
              .query(
                RangeQuery("unixReviewTime")
                  .gte(InputPayloadDateTime.toUnixTimestamp(payload.start))
                  .lte(InputPayloadDateTime.toUnixTimestamp(payload.end))
              )
              .aggs(
                // We want to group by asin and avg the overall field while ordering descending
                TermsAggregation("avgOverall")
                  .field("asin")
                  .size(payload.limit)
                  .minDocCount(payload.min_number_reviews)
                  .subaggs(AvgAggregation("overall").field("overall"))
                  .order(TermsOrder("overall", asc = false))
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
                case RequestFailure(status, body, headers, error) =>
                  respondeWithInternalServerError(
                    s"Query to elasticsearch failed, status: $status, error: $error"
                  )
              }
              .mapError { error =>
                catchElasticSearchConnectionError(error)
                  .fold[zio.http.Response](
                    respondeWithInternalServerError(error.toString)
                  )(msg => respondeWithServiceUnavailableError(msg))

              }
          } yield res
      }
      .mapError { it =>
        // We want to do some form of error logging for the web service
        // and this place seems to be the right one for doing that.
        // To be fair though, converting the request body back is not that efficient but it works for now
        // (and in the error case we most likely care less about performance for now).
        logger.error(
          s"Error while processing request, status: ${it.status}, reason: ${it.body.toString}"
        )
        it
      }

  override val run = {
    val prog =
      for {
        // Begin with reading in the command line argument that provides the path to the sample data
        args <- this.getArgs
        ingestFileUrl = args.headOption.getOrElse("./sample-data.json")
        _ = logger.info(
          s"Input file: $ingestFileUrl"
        )
        // Init the elastic client
        client <- ZIO.fromAutoCloseable(
          ZIO.from(
            ElasticClient(
              JavaClient(ElasticProperties("http://elasticsearch:9200"))
            )
          )
        )
        // We need a fresh index with asin being a keyword field for group-by operations
        _ <- client.execute(deleteIndex(indexName))
        _ <- client.execute(
          createIndex(indexName).mapping(properties(keywordField("asin")))
        )
        // Read in the sample data and ingest it into the elasticsearch index as a bulk
        source <- ZIO.fromAutoCloseable(
          ZIO.from(Source.fromFile(ingestFileUrl))
        )
        // Side Note: The chunk size is something that needs to be tested individually for the machine.
        // It definitely is necessary as the ingestion process is otherwise either way too slow or without any chunking
        // we will crash at some point as we run out of memory.
        // My number can be quite optimistic considering how long a line can be ;)
        _ <- ZStream
          .fromIterator(source.getLines(), maxChunkSize = 50_000)
          .runForeachChunkScoped { lines =>
            logger.info("ingesting " + lines.size)
            lines
              .map { line =>
                ZIO
                  .fromEither(line.fromJson[Review])
                  .map(payload =>
                    indexInto(indexName)
                      .source(payload.toJson)
                  )
                  .mapError(it =>
                    new Exception(s"Failed parsing payload: $it")
                  )
              }
              .collectZIO(identity(_))
              .flatMap { requests =>
                val it = requests.toArray
                client.execute(bulk(it: _*))
              }
          }

        _ = logger.info("starting webserver")
        // Get the webserver running
        _ <- Server.serve(app(client)).provide(Server.defaultWithPort(8080))
      } yield ()
    prog.mapError(it =>
      catchElasticSearchConnectionError(it) match {
        case Some(newMsg) => new Exception(newMsg)
        case None         => it
      }
    )
  }
}
