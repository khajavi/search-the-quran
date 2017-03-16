import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.{CorsDirectives, CorsSettings}
import com.typesafe.config.ConfigFactory
import spray.json.DefaultJsonProtocol._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.StdIn
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
    println(quran.suras(1))
    println(quran.suras(2).ayas(3).text)

    // domain model
    final case class Item(name: String, id: Long)
    final case class Order(items: List[Item])
    case class Aya(number: Int, text: String)


    // (fake) async database query api
    def fetchItem(itemId: Long): Future[Option[Item]] = Future {
      Some(Item("a", 1L))
    }
    def saveOrder(order: Order): Future[Done] = Future {
      Done
    }


    // needed to run the route
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    // formats for unmarshalling and marshalling
    implicit val itemFormat = jsonFormat2(Item)
    implicit val orderFormat = jsonFormat1(Order)
    implicit val ayaFormat = jsonFormat2(Aya)


    val settings = CorsSettings.defaultSettings.copy(allowCredentials = false)


    val route: Route =
      handleRejections(CorsDirectives.corsRejectionHandler) {

        CorsDirectives.cors(settings) {
          handleRejections(RejectionHandler.default) {
            get {
              pathPrefix("item" / LongNumber) { id =>
                // there might be no item for a given id
                val maybeItem: Future[Option[Item]] = fetchItem(id)

                onSuccess(maybeItem) {
                  case Some(item) => complete(item)
                  case None => complete(StatusCodes.NotFound)
                }
              } ~ path(IntNumber / IntNumber) {
                case (a: Int, b) => complete(
                  Future {
                    Aya(a, quran.suras(a).ayas(b).text)
                  }
                )
              }
            } ~
              post {
                path("create-order") {
                  entity(as[Order]) { order =>
                    val saved: Future[Done] = saveOrder(order)
                    onComplete(saved) { done =>
                      complete("order created")
                    }
                  }
                }
              }
          }
        }
      }

    val conf = ConfigFactory.load()
    val port = conf.getInt("http.port")
    val host = conf.getString("http.host")

    val bindingFuture = Http().bindAndHandle(RouteResult.route2HandlerFlow(route), host, port)
    println(s"Server online at http://$host:$port/\nPress RETURN to stop...")
  }
}

