scalaVersion := "2.11.8"

name := "standup-dashboard-api"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
	"org.specs2" %% "specs2-core" % "3.8.5.1" % Test,
  "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.45",
  "com.typesafe.akka" %% "akka-actor" % "2.4.11",
  "com.typesafe.akka" %% "akka-http-core" % "2.4.11",
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.11",
  "com.typesafe.akka" %% "akka-http-jackson-experimental" % "2.4.11",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.11"
)
resolvers += Resolver.jcenterRepo

val circeVersion = "0.5.4"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

fork in Test := true

parallelExecution in Test := false

scalacOptions in Test ++= Seq("-Yrangepos")

enablePlugins(UniversalPlugin)

enablePlugins(JavaAppPackaging)
