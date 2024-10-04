import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import bloop.integrations.sbt.BloopDefaults
import uk.gov.hmrc.DefaultBuildSettings

val appName = "integration-catalogue"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.5.1"



lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    routesImport                     += "uk.gov.hmrc.integrationcatalogue.controllers.binders._",
    libraryDependencies              ++= AppDependencies.compile ++ AppDependencies.test,
    Test / unmanagedSourceDirectories += baseDirectory(_ / "test-common").value
  )
  .settings(scoverageSettings)
  .settings(scalacOptions := scalacOptions.value.diff(Seq("-Wunused:all")))
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(scalacOptions += "-Wconf:msg=Flag.*repeatedly:s")


lazy val scoverageSettings = {
    import scoverage.ScoverageKeys
    Seq(
      // Semicolon-separated list of regexs matching classes to exclude
      ScoverageKeys.coverageExcludedPackages := ",.*\\.models\\..*,uk\\.gov\\.hmrc\\.BuildInfo,.*\\.MongoFormatters,.*\\.package,.*\\.Routes,.*\\.RoutesPrefix,,Module;GraphiteStartUp,.*\\.Reverse[^.]*",
      ScoverageKeys.coverageMinimumStmtTotal := 95,
      ScoverageKeys.coverageFailOnMinimum := true,
      ScoverageKeys.coverageHighlighting := true,
      Test / parallelExecution := false
  )
}

lazy val it = (project in file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.it)
  .settings(scalacOptions += "-Wconf:msg=Flag.*repeatedly:s")
