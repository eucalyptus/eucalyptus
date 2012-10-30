package com.eucalyptus.reporting.art.renderer

import org.junit.Test
import com.eucalyptus.reporting.art.entity.ReportArtEntity
import com.eucalyptus.reporting.ReportType
import com.eucalyptus.reporting.ReportFormat
import com.eucalyptus.reporting.art.entity.AvailabilityZoneArtEntity

/**
 * 
 */
class ComputeCapacityRendererTest extends RendererTestSupport {

  @Test
  void testCsvLayout() {
    assertCsvColumns( render( ReportType.CAPACITY, ReportFormat.CSV, art() ) )
  }

  @Test
  void testHtmlLayout() {
    assertHtmlColumns( render( ReportType.CAPACITY, ReportFormat.HTML, art() ) )
  }

  private ReportArtEntity art() {
    ReportArtEntity art = new ReportArtEntity( millis("2012-10-01T00:00:00"), millis("2012-10-10T00:00:00") )
    art.getUsageTotals().getComputeCapacityArtEntity().setInstancesAvailableForType("m1.small", 0)
    art.getUsageTotals().getComputeCapacityArtEntity().setInstancesTotalForType("m1.small", 0)
    AvailabilityZoneArtEntity zoneArt = new AvailabilityZoneArtEntity()
    art.getZones().put("zone1", zoneArt)
    zoneArt.getUsageTotals().getComputeCapacityArtEntity().setInstancesAvailableForType("m1.small", 0)
    zoneArt.getUsageTotals().getComputeCapacityArtEntity().setInstancesTotalForType("m1.small", 0)
    art
  }

}
