import java.util.Date

enablePlugins(JavaAppPackaging)

name := "quran-api"

version := "0.0.1"

scalaVersion := "2.11.8"


libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.4"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.4"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.4"
libraryDependencies += "ch.megard" %% "akka-http-cors" % "0.1.11"
libraryDependencies += "com.typesafe" % "config" % "1.3.1"

val copyIndex = TaskKey[Unit]("copyIndex", "copy index file in target")

copyIndex := {
  val source = baseDirectory.value / "index.html"
  val target = (crossTarget in Compile).value / "index.html"
  val log = streams.value.log
  IO.writeLines(target,
    IO.readLines(source)
  )

}