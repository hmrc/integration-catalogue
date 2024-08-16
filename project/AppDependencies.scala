import sbt._

object AppDependencies {

  lazy val scalaCheckVersion = "1.14.0"
  lazy val enumeratumVersion = "1.8.0"
  lazy val hmrcMongoVersion = "2.2.0"
  lazy val bootstrapVersion = "9.3.0"
  lazy val jacksonVersion = "2.17.1"
  lazy val playJsonVersion = "2.10.5"
  lazy val internalAuthVersion = "3.0.0"

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
    "org.typelevel"                     %% "cats-core"                      % "2.10.0",
    "io.swagger.parser.v3"              % "swagger-parser"                  % "2.1.22"
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
    "org.scalacheck" %% "scalacheck" % "1.18.0" % Test,
    "org.scalatestplus" %% "mockito-4-11" % "3.2.17.0" % Test,
    "org.scalatestplus" %% "scalacheck-1-17" % "3.2.18.0" % Test
  )

  val it = Seq.empty

}
