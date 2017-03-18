
lazy val root = project

lazy val client = project.in(file("client"))
  .settings(
    crossScalaVersions := Seq("2.12.1", "2.11.8"),
    scalaVersion := "2.12.1",
    emitSourceMaps := true,
    artifactPath in (Compile, fastOptJS) :=
      ((crossTarget in (Compile, fastOptJS)).value /
        ((moduleName in fastOptJS).value + "-opt.js")),
    libraryDependencies ++= Seq(
      "in.nvilla" % "monadic-html_sjs0.6_2.12" % "0.2.3"
    )
  ).enablePlugins(ScalaJSPlugin)

lazy val server = project.in(file("server"))
  .settings(
    scalaVersion := "2.11.8",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.0.4",
      "com.typesafe.akka" %% "akka-http" % "10.0.4",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.4",
      "ch.megard" %% "akka-http-cors" % "0.1.11",
      "com.typesafe" % "config" % "1.3.1"
    )
    ,
    (resources in Compile) := {
      (resources in Compile).value ++ (jsResources in LocalRootProject).value
    }
  )
  .enablePlugins(JavaAppPackaging)

lazy val jsResources = taskKey[Seq[File]](
  "All scalajs generated JS files, including source maps"
)

jsResources := {
  val fastOpt = (fastOptJS in (client, Compile)).value.data
  val dir = (crossTarget in (client, Compile)).value
  dir.listFiles.filter(f => f.getName.endsWith(".js") || f.getName.endsWith(".js.map"))
}