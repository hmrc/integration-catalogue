import sbt._

object AppDependencies {

  lazy val scalaCheckVersion = "1.14.0"
  lazy val enumeratumVersion = "1.7.2"
  lazy val hmrcMongoVersion = "1.7.0"
  lazy val bootstrapVersion = "8.4.0"
  lazy val jacksonVersion = "2.15.1"
  lazy val playJsonVersion = "2.9.4"
  lazy val internalAuthVersion = "1.9.0"

  val compile = Seq(
    "uk.gov.hmrc"                       %% "bootstrap-backend-play-30"              % bootstrapVersion,
    "com.typesafe.play"                 %% "play-json"                              % playJsonVersion,
    "com.beachape"                      %% "enumeratum-play-json"                   % enumeratumVersion,
    "uk.gov.hmrc.mongo"                 %% "hmrc-mongo-work-item-repo-play-30"      % hmrcMongoVersion,
    "uk.gov.hmrc"                       %% "internal-auth-client-play-30"           % internalAuthVersion,

    "com.fasterxml.jackson.module"      %% "jackson-module-scala"           % jacksonVersion,
    "com.fasterxml.jackson.core"        % "jackson-annotations"             % jacksonVersion,
    "com.fasterxml.jackson.core"        % "jackson-databind"                % jacksonVersion,
    "com.fasterxml.jackson.core"        % "jackson-core"                    % jacksonVersion,
    "com.fasterxml.jackson.dataformat"  % "jackson-dataformat-yaml"         % jacksonVersion,
    "com.fasterxml.jackson.datatype"    % "jackson-datatype-jsr310"         % jacksonVersion,
    "com.typesafe.play"                 %% "play-json-joda"                 % "2.10.4",
    "org.typelevel"                     %% "cats-core"                      % "2.9.0",
    "io.swagger.parser.v3"              % "swagger-parser"                  % "2.1.14"
      excludeAll(
      ExclusionRule("com.fasterxml.jackson.core", "jackson-databind"),
      ExclusionRule("com.fasterxml.jackson.core", "jackson-core"),
      ExclusionRule("com.fasterxml.jackson.core", "jackson-annotations"),
      ExclusionRule("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml"),
      ExclusionRule("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310")
    )
  )

  val test = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVersion % Test,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongoVersion % Test,
    "org.mockito" %% "mockito-scala" % "1.17.30" % Test,
    "org.scalacheck" %% "scalacheck" % "1.17.0" % Test,
    "org.scalatestplus" %% "scalacheck-1-17" % "3.2.17.0" % Test
  )

  val it = Seq.empty

}
