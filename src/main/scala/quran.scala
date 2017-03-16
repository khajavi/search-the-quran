import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.{CorsDirectives, CorsSettings}
import com.typesafe.config.ConfigFactory
import spray.json.DefaultJsonProtocol._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml._

class Aya(source: NodeSeq) {
  val text = (source \ "@text").text
}

class Bismillah(source: NodeSeq) extends Aya(source: NodeSeq) {
  override val text = (source \ "@bismillah").text
}

class Sura(source: NodeSeq) {
  val name = (source \ "@name").text
  val ayas = (source \ "aya").map(aya => new Aya(aya))
}

class SuraWithBismillah(source: NodeSeq) extends Sura(source: NodeSeq) {
  private  val index = (source \ "@index").text
  override val ayas  = index match {
    case "1" => (source \ "aya").map(aya => new Aya(aya))
    case "9" => (source \ "aya").map(aya => new Aya(aya))
    case _ => new Bismillah((source \ "aya").head) +: (source \ "aya").map(aya => new Aya(aya))
  }
}

class Quran(source: Elem) {
  val suras = (source \ "sura").map(sura => new SuraWithBismillah(sura))
}

object Quran {
  def fromFile(name: String): Quran = new Quran(XML.loadFile(name))

  def main(args: Array[String]) {
    val quran = Quran.fromFile("quran-simple-enhanced.xml")

    case class Aya(surah: Int, ayah: Int, text: String)

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    implicit val ayaFormat = jsonFormat3(Aya)

    val settings = CorsSettings.defaultSettings.copy(allowCredentials = false)

    val route: Route =
      handleRejections(CorsDirectives.corsRejectionHandler) {
        CorsDirectives.cors(settings) {
          handleRejections(RejectionHandler.default) {
            path(IntNumber / IntNumber) {
              case (surah: Int, ayah: Int) => complete(
                Future {
                  Aya(surah, ayah, quran.suras(surah).ayas(ayah).text)
                }
              )
            }
          }
        }
      }

    val conf = ConfigFactory.load()
    val port = conf.getInt("http.port")
    val host = conf.getString("http.host")

    val bindingFuture = Http().bindAndHandle(RouteResult.route2HandlerFlow(route), host, port)
    println(s"Server online at http://$host:$port")
  }
}

