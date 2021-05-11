import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  lazy val scalaCheckVersion = "1.14.0"
  lazy val enumeratumVersion = "1.6.2"
  lazy val reactivemongoVersion = "7.31.0-play-27"
  lazy val hmrcmongoVersion = "0.49.0"
  lazy val bootstrapVersion = "3.4.0"

  val compile = Seq(
    "uk.gov.hmrc"                       %% "bootstrap-backend-play-27"      % bootstrapVersion,
    "com.typesafe.play"                 %% "play-json"                      % "2.9.2",
    "com.typesafe.play"                 %% "play-json-joda"                 % "2.9.2",
    "com.beachape"                      %% "enumeratum-play-json"           % enumeratumVersion,
     "uk.gov.hmrc.mongo"                %% "hmrc-mongo-play-27"             % hmrcmongoVersion,

    "com.fasterxml.jackson.core"        % "jackson-annotations"             % "2.12.2",
    "com.fasterxml.jackson.core"        % "jackson-databind"                % "2.12.2",
    "com.fasterxml.jackson.dataformat"  % "jackson-dataformat-yaml"         % "2.12.2",
    "io.swagger.parser.v3"              % "swagger-parser-v3"               % "2.0.24",
    "org.typelevel"                     %% "cats-core"                      % "2.4.2",
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-27"   % bootstrapVersion    % Test,
    "org.pegdown"             %  "pegdown"                  % "1.6.0"             % "test, it",
    "com.typesafe.play"       %% "play-test"                % current             % Test,
    "org.mockito"             %% "mockito-scala-scalatest"  % "1.7.1"             % "test, it",
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.36.8"            % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "4.0.3"             % "test, it",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-27"  % hmrcmongoVersion    % "test, it",
    "com.github.tomakehurst"  % "wiremock-jre8-standalone"  % "2.27.1"            % "test, it",
    "org.scalacheck"          %% "scalacheck"               % scalaCheckVersion   % "test, it"
  )
}
