name := "3_asteroids"

version := "0.1"

scalaVersion := "2.13.8"

idePackagePrefix := Some("nl.rug.aoop")

val AkkaVersion = "2.6.17"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % AkkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-remote" % AkkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % AkkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.6" % Runtime

libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.36.0.3"


val circeVersion = "0.14.1"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies  ++= Seq(
  // Last stable release
  "org.scalanlp" %% "breeze" % "1.3",

  // The visualization library is distributed separately as well.
  // It depends on LGPL code
  "org.scalanlp" %% "breeze-viz" % "1.3"
)

libraryDependencies ++= Seq("org.specs2" %% "specs2-core" % "4.12.12" % "test")