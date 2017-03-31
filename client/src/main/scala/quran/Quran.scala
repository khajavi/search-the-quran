package quran

import java.util.Date

import mhtml.{Rx, Var}
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw.{Element, TouchEvent}
import org.scalajs.dom.{Event, KeyboardEvent, Touch}

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
  val address = Var((1, 1))

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

  def swipedetect(touchsurface: dom.Node, callback: (String) => Unit) = {
    var swipedir = ""
    var startX: Double = 0.0
    var startY: Double = 0.0
    var distX, distY = 0.0
    val threshold = 150
    val restraint = 100
    val allowedTime = 300
    var elapsedTime = 0L
    var startTime = 0L

    touchsurface.addEventListener("touchstart", (e: TouchEvent) => {
      val touchobj: Touch = e.changedTouches(0)
      var dist = 0
      startX = touchobj.pageX
      startY = touchobj.pageY
      startTime = new Date().getTime
      e.defaultPrevented
    }, useCapture = false)

    touchsurface.addEventListener("touchmove", (e: TouchEvent) => {
      e.defaultPrevented // prevent scrolling when inside DIV
    }, useCapture = false)


    touchsurface.addEventListener("touchend", (e: TouchEvent) => {
      val touchobj = e.changedTouches(0)
      distX = touchobj.pageX - startX // get horizontal dist traveled by finger while in contact with surface
      distY = touchobj.pageY - startY // get vertical dist traveled by finger while in contact with surface
      elapsedTime = new Date().getTime - startTime // get time elapsed
      if (elapsedTime <= allowedTime) {
        // first condition for awipe met
        if (Math.abs(distX) >= threshold && Math.abs(distY) <= restraint) {
          // 2nd condition for horizontal swipe met
          swipedir = if (distX < 0) "left" else "right" // if dist traveled is negative, it indicates left swipe
          println(swipedir)
        }
        else if (Math.abs(distY) >= threshold && Math.abs(distX) <= restraint) {
          // 2nd condition for vertical swipe met
          swipedir = if (distY < 0) "up" else "down" // if dist traveled is negative, it indicates up swipe
          println(swipedir)
        }
      }
      callback(swipedir)
      e.preventDefault()
    }, useCapture = false)

  }

  dom.window.onload =(e: Event) => {
    val el: Element = dom.document.getElementById("content")
    swipedetect(el, {
      case "right" =>
        address.update(_ => (address.value._1, address.value._2 - 1))
        dom.window.location.hash = "#" + address.map(a => a._1 + ":" + a._2).value
      case "left" =>
        address.update(_ => (address.value._1, address.value._2 + 1))
        dom.window.location.hash = "#" + address.map(a => a._1 + ":" + a._2).value
      case "up" =>
        address.update(_ => (address.value._1 - 1, 1))
        dom.window.location.hash = "#" + address.map(a => a._1 + ":" + a._2).value
      case "down" =>
        address.update(_ => (address.value._1 + 1, 1))
        dom.window.location.hash = "#" + address.map(a => a._1 + ":" + a._2).value
      case _ => ()
    })

    dom.window.document.addEventListener("keydown", (e: KeyboardEvent) => {
      val key = e.keyCode
      println("key", key)
      key match {
        case 37 =>
          address.update(_ => (address.value._1, address.value._2 + 1))
          dom.window.location.hash = "#" + address.map(a => a._1 + ":" + a._2).value
        case 38 => //up
          address.update(_ => (address.value._1 - 1, 1))
          dom.window.location.hash = "#" + address.map(a => a._1 + ":" + a._2).value
        case 39 => //right
          address.update(_ => (address.value._1, address.value._2 - 1))
          dom.window.location.hash = "#" + address.map(a => a._1 + ":" + a._2).value
        case 40 => //down
          address.update(_ => (address.value._1 + 1, 1))
          dom.window.location.hash = "#" + address.map(a => a._1 + ":" + a._2).value
      }
    }, useCapture = false)


  }
}