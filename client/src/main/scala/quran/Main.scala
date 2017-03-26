package quran

import mhtml._
import org.scalajs.dom

import scala.scalajs.js.JSApp
import scala.xml.Node


object Main extends JSApp {
  val quran: Rx[Node] = Var(Quran).map(_.app)
  val app = quran

  def main(): Unit =
    mount(dom.document.body, app)
}

