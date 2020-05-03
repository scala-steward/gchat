import sbt.Keys.publishMavenStyle
import xerial.sbt.Sonatype.GitHubHosting

ThisBuild / scalaVersion := "2.13.2"
ThisBuild / organization := "de.martinpallmann.gchat"
Global / onChangedBuildSource := ReloadOnSourceChanges
sonatypeProfileName := "de.martinpallmann.gchat"
publish / skip := true

lazy val commonSettings = Seq(
  publishTo := sonatypePublishToBundle.value,
  headerLicense := Some(HeaderLicense.ALv2("2020", "Martin Pallmann")),
  publishMavenStyle := true,
  licenses := Seq(
    "APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")
  ),
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
      "org.scalatest" %% "scalatest" % "3.1.1"
    ),
    publish / skip := true
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
      "org.http4s" %% "http4s-circe" % http4sVersion
    )
  )

lazy val docs = project
  .in(file("gchat-docs"))
  .dependsOn(bot)
  .enablePlugins(MdocPlugin)
  .settings(mdocVariables := Map("VERSION" -> version.value))

def circeVersion = "0.13.0"
def http4sVersion = "0.21.2"
