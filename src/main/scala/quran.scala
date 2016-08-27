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
	private val index = (source \ "@index").text
	override val ayas = index match {
		case "1" => (source \ "aya").map(aya => new Aya(aya))		
		case "9" => (source \ "aya").map(aya => new Aya(aya))
		case _ => new Bismillah((source \ "aya")(0)) +: (source \ "aya").map(aya => new Aya(aya))
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
		println(quran.suras(2).ayas(2))
	}
}
