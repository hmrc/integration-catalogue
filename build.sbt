import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import bloop.integrations.sbt.BloopDefaults
import uk.gov.hmrc.DefaultBuildSettings

val appName = "integration-catalogue"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.12"



lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .settings(
    routesImport                     += "uk.gov.hmrc.integrationcatalogue.controllers.binders._",
    libraryDependencies              ++= AppDependencies.compile ++ AppDependencies.test,
    Test / unmanagedSourceDirectories += baseDirectory(_ / "test-common").value
  )
  .settings(scoverageSettings)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)


lazy val scoverageSettings = {
    import scoverage.ScoverageKeys
    Seq(
      // Semicolon-separated list of regexs matching classes to exclude
      ScoverageKeys.coverageExcludedPackages := ";.*\\.domain\\.models\\..*;uk\\.gov\\.hmrc\\.BuildInfo;.*\\.Routes;.*\\.RoutesPrefix;;Module;GraphiteStartUp;.*\\.Reverse[^.]*",
      ScoverageKeys.coverageMinimumStmtTotal := 96,
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
