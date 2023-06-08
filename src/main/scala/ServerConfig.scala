import pureconfig._
import pureconfig.generic.auto._
import pureconfig.generic.hlist.hListReader

case class ServerConfig(dataToIngestUrl: String, elasticsearchUrl: String, port: Int = 2500)

//object ServerConfig {
//
//  import pureconfig._
//  import pureconfig.generic.auto._
//  def apply(str: String) = {
//    import pureconfig.generic.hlist.hListReader
//    ConfigSource.string(str).load[ServerConfig]
//  }
//}