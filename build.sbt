import sbt.Keys.{publish, publishMavenStyle}
import xerial.sbt.Sonatype.GitHubHosting
import Path.relativeTo

ThisBuild / scalaVersion := "2.13.2"
ThisBuild / organization := "de.martinpallmann.gchat"
ThisBuild / organizationName := "Martin Pallmann"
ThisBuild / startYear := Some(2020)
ThisBuild / licenses := Seq(License.apache2)
Global / onChangedBuildSource := ReloadOnSourceChanges
publish / skip := true

def circeVersion = "0.14.9"
def http4sVersion = "0.21.4"

lazy val commonSettings = Seq(
  publishTo := sonatypePublishToBundle.value,
  publishMavenStyle := true,
  sonatypeProjectHosting := Some(
    GitHubHosting("martinpallmann", "gchat", "sayhello@martinpallmann.de")
  )
)

lazy val core = project
  .settings(
    moduleName := s"gchat-core",
    commonSettings,
    Compile / sourceGenerators += Def.task {
      val input = (Compile / sourceDirectory).value / "json" / "rest.json"
      val target = (Compile / sourceManaged).value
      SourceGen.generate(input, target)
    },
    mappings in (Compile, packageSrc) ++= {
      val allGeneratedFiles = ((sourceManaged in Compile).value ** "*") filter {
        _.isFile
      }
      allGeneratedFiles.get.pair(relativeTo((sourceManaged in Compile).value))
    }
  )

lazy val tck = project
  .dependsOn(core)
  .settings(
    moduleName := s"gchat-tck",
    commonSettings,
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "org.scalatest" %% "scalatest" % "3.1.2"
    )
  )

lazy val circe = project
  .dependsOn(core, tck % "test->compile")
  .settings(
    moduleName := s"gchat-circe",
    commonSettings,
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion
    )
  )

lazy val bot = project
  .dependsOn(core, circe)
  .settings(
    moduleName := s"gchat-bot",
    commonSettings,
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "com.auth0" % "java-jwt" % "3.10.3",
      "io.monix" %% "minitest" % "2.8.2" % Test,
      "org.slf4j" % "slf4j-nop" % "1.7.30" % Test
    ),
    testFrameworks += new TestFramework("minitest.runner.Framework")
  )

lazy val docs = project
  .in(file("gchat-docs"))
  .dependsOn(bot)
  .enablePlugins(MdocPlugin)
  .settings(
    mdocVariables := Map("VERSION" -> MavenCentral.version),
    publish / skip := true
  )
