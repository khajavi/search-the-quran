package quran

import mhtml.{Rx, Var}
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.ext.Ajax

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.timers.SetTimeoutHandle
import scala.util.{Failure, Success, Try}
import scala.xml.{Elem, Node}


@js.native
trait AyahJs extends js.Object {
  val number: Int    = js.native
  val text  : String = js.native
}

object Quran {
  val address = Var((0, 0))

  def updateAddressByHashUrl = { e: Event =>
    val hash = dom.window.location.hash
    val str = hash.drop(1)
    println(s"onload: $hash, $str")
    address.update(
      _ => {
        val surah = str.split(":").head.toInt
        val ayah = str.split(":").last.toInt
        (surah, ayah)
      }
    )
  }

  dom.window.onload = updateAddressByHashUrl
  dom.window.onhashchange = updateAddressByHashUrl

  val onkeyup: (Event) => Unit =
    Utils.inputEvent(
      input => address.update(
        _ => {
          if (input.value.split(":").length != 2) onkeyup
          val surah = input.value.split(":").head.toInt
          val ayah = input.value.split(":").last.toInt
          dom.window.location.hash = s"$surah:$ayah"
          (surah, ayah)
        }
      )
    )

  def app: Node = {
    def ayah = address.map(surah => ayahdiv(surah._1, surah._2))
    <div class="well row">
      <div class="col-lg-2 col-md-2 col-sm-1"></div>
      <div class="col-lg-8 col-md-8 col-sm-10">
        <div class="row">
          <div class="col-md-2"></div>
          <div class="col-md-8">
            <input type="text"
                   class="form-control"
                   value={address.map(a => a._1 + ":" + a._2)}
                   oninput={debounce(300)(onkeyup)}
                   onfocus={debounce(300)(onkeyup)}/>
          </div>
          <div class="col-md-2"></div>
        </div>
        <div id="content">
          {ayah}
        </div>
      </div>
      <div class="col-lg-2 col-md-2 col-sm-1"></div>
    </div>
  }

  object debounce {
    var timeoutHandler: js.UndefOr[SetTimeoutHandle] = js.undefined

    def apply[A, B](timeout: Double)(f: A => B): A => Unit = { a =>
      timeoutHandler foreach js.timers.clearTimeout
      timeoutHandler = js.timers.setTimeout(timeout) {
        f(a)
        ()
      }
    }
  }

  def ayahdiv(surah: Int, ayah: Int): Node = {
    if (surah < 0) <div>Please enter surah and ayah</div>
    else {
      <div>
        {printAyah(surah, ayah)}
      </div>
    }
  }

  def doRequest[T](suffix: String)(f: js.Dynamic => T): Rx[Option[Try[T]]] =
    Utils
      .fromFuture(Ajax.get(s"/$suffix"))
      .map(_.map(_.withFilter(_.status == 200).map { x =>
        val json = JSON.parse(x.responseText)
        println("JSON: " + JSON.stringify(json))
        f(json)
      }))


  def getAyah(surah: Int, ayah: Int): Rx[Option[Try[AyahJs]]] =
    doRequest(s"$surah/$ayah")(_.asInstanceOf[AyahJs])

  def printAyah(surah: Int, ayah: Int): Rx[Elem] = getAyah(surah, ayah).map {
    case None => <div>Loading surah and ayah for
      {surah}
      and
      {ayah}
    </div>
    case Some(Success(json)) =>
      <div>
        <h2>
          {json.text.toString}
        </h2>
      </div>
    case Some(Failure(error)) =>
      <div style="background: red">
        {error.getMessage}
      </div>
  }
}