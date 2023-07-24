import sbt._

object AppDependencies {

  lazy val scalaCheckVersion = "1.14.0"
  lazy val enumeratumVersion = "1.6.3"
  lazy val hmrcMongoVersion = "1.3.0"
  lazy val bootstrapVersion = "7.19.0"
  lazy val jacksonVersion = "2.12.6"
  lazy val playJsonVersion = "2.9.2"

  val compile = Seq(
    "uk.gov.hmrc"                       %% "bootstrap-backend-play-28"              % bootstrapVersion,
    "com.typesafe.play"                 %% "play-json"                              % playJsonVersion,
    "com.typesafe.play"                 %% "play-json-joda"                         % playJsonVersion,
    "com.beachape"                      %% "enumeratum-play-json"                   % enumeratumVersion,
    "uk.gov.hmrc.mongo"                 %% "hmrc-mongo-work-item-repo-play-28"      % hmrcMongoVersion,

    "com.fasterxml.jackson.module"      %% "jackson-module-scala"           % jacksonVersion,
    "com.fasterxml.jackson.core"        % "jackson-annotations"             % jacksonVersion,
    "com.fasterxml.jackson.core"        % "jackson-databind"                % jacksonVersion,
    "com.fasterxml.jackson.core"        % "jackson-core"                    % jacksonVersion,
    "com.fasterxml.jackson.dataformat"  % "jackson-dataformat-yaml"         % jacksonVersion,
    "com.fasterxml.jackson.datatype"    % "jackson-datatype-jsr310"         % jacksonVersion,
    "org.typelevel"                     %% "cats-core"                      % "2.4.2",
    "io.swagger.parser.v3"              % "swagger-parser"                  % "2.1.9"
      excludeAll(
      ExclusionRule("com.fasterxml.jackson.core", "jackson-databind"),
      ExclusionRule("com.fasterxml.jackson.core", "jackson-core"),
      ExclusionRule("com.fasterxml.jackson.core", "jackson-annotations"),
      ExclusionRule("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml"),
      ExclusionRule("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310")
    )
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"   % bootstrapVersion    % "test, it",
    "org.pegdown"             %  "pegdown"                  % "1.6.0"             % "test, it",
    "org.mockito"             %% "mockito-scala-scalatest"  % "1.7.1"           % "test, it",
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.36.8"            % "test, it",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"  % hmrcMongoVersion    % "test, it",
    "com.github.tomakehurst"  % "wiremock-jre8-standalone"  % "2.27.1"            % "test, it",
    "org.scalacheck"          %% "scalacheck"               % scalaCheckVersion   % "test, it"
  )
}
