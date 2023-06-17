package challenge

import zio._
import zio.http._
import zio.json._
import zio.stream.ZStream
import challenge.model.{
  InputPayload,
  InputPayloadDateTime,
  OutputPayload,
  Review,
  WebserverConf
}
import com.sksamuel.elastic4s.{
  ElasticClient,
  ElasticProperties,
  RequestFailure,
  RequestSuccess
}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.http.{JavaClient, JavaClientExceptionWrapper}
import com.sksamuel.elastic4s.requests.searches.aggs._
import com.sksamuel.elastic4s.requests.searches.aggs.responses.bucket.Terms
import com.sksamuel.elastic4s.requests.searches.queries._
import com.sksamuel.elastic4s.zio.instances._
import scala.io.Source

object HelloWorld extends ZIOAppDefault {

  import org.slf4j.LoggerFactory

  val logger = LoggerFactory.getLogger(getClass)

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
            body <- {
              println("Incoming request")
              req.body.asString.mapError(it =>
                respondeWithInternalServerError(
                  "Failure extracting body: " + it.getMessage
                )
              )
            }
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
    import pureconfig._
    import pureconfig.generic.auto._
    val prog =
      for {
        // Begin with reading in the command line argument that provides the path to the sample data
        config <- ZIO
          .from(ConfigSource.default.load[WebserverConf])
          .mapError(it => new Exception(it.toString()))
        ingestFileUrl = config.ingestFileUrl
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
                  .mapError(it => new Exception(s"Failed parsing payload: $it"))
              }
              .collectZIO(identity(_))
              .flatMap { requests =>
                val res =
                  client.execute(
                    bulk(requests.toArray: _*)
                  )
                res
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
