import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  lazy val scalaCheckVersion = "1.14.0"
  lazy val enumeratumVersion = "1.6.3"
  lazy val hmrcmongoVersion = "0.49.0"
  lazy val bootstrapVersion = "5.14.0"
  lazy val jacksonVersion = "2.11.1"

  val compile = Seq(
    "uk.gov.hmrc"                       %% "bootstrap-backend-play-28"      % bootstrapVersion,
    "com.typesafe.play"                 %% "play-json"                      % "2.9.2",
    "com.typesafe.play"                 %% "play-json-joda"                 % "2.9.2",
    "com.beachape"                      %% "enumeratum-play-json"           % enumeratumVersion,
     "uk.gov.hmrc.mongo"                %% "hmrc-mongo-play-28"             % hmrcmongoVersion,

    "com.fasterxml.jackson.module"      %% "jackson-module-scala"           % jacksonVersion,
    "com.fasterxml.jackson.core"        % "jackson-annotations"             % jacksonVersion,
    "com.fasterxml.jackson.core"        % "jackson-databind"                % jacksonVersion,
    "com.fasterxml.jackson.core"        % "jackson-core"                    % jacksonVersion,
    "com.fasterxml.jackson.dataformat"  % "jackson-dataformat-yaml"         % jacksonVersion,

    "io.swagger.parser.v3"              % "swagger-parser-v3"               % "2.0.24",
    "org.typelevel"                     %% "cats-core"                      % "2.4.2"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"   % bootstrapVersion    % "test, it",
    "org.pegdown"             %  "pegdown"                  % "1.6.0"             % "test, it",
    "org.mockito"             %% "mockito-scala-scalatest"  % "1.7.1"           % "test, it",
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.36.8"            % "test, it",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"  % hmrcmongoVersion    % "test, it",
    "com.github.tomakehurst"  % "wiremock-jre8-standalone"  % "2.27.1"            % "test, it",
    "org.scalacheck"          %% "scalacheck"               % scalaCheckVersion   % "test, it"
  )
}
