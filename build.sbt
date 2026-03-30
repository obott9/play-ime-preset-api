name := "play-ime-preset-api"
organization := "com.github.obott9"
version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayJava, PlayEbean)

scalaVersion := "2.13.16"

// Play Framework 3.0.10 (Pekko-based)
libraryDependencies ++= Seq(
  guice,
  jdbc,
  "org.postgresql" % "postgresql" % "42.7.5",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.18.3",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.18.3"
)

// Java 21
javacOptions ++= Seq("-source", "21", "-target", "21")

// Play filters
libraryDependencies += filters
