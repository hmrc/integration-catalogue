package uk.gov.hmrc.integrationcatalogue.support

import com.kenshoo.play.metrics.Metrics
import org.scalatest.Suite
import play.api.Application

import scala.jdk.CollectionConverters._

trait MetricsTestSupport {
  self: Suite =>

  def app: Application

  def givenCleanMetricRegistry(): Unit = {
    val registry = app.injector.instanceOf[Metrics].defaultRegistry
    for (
      metric <- registry.getMetrics.keySet().iterator().asScala
    ) {
      registry.remove(metric)
    }
  }

}
