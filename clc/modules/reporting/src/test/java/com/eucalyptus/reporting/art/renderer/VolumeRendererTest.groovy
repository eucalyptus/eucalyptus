package com.eucalyptus.reporting.art.renderer

import org.junit.Test
import com.eucalyptus.reporting.art.entity.ReportArtEntity
import com.eucalyptus.reporting.art.entity.AccountArtEntity
import com.eucalyptus.reporting.art.entity.UserArtEntity
import com.eucalyptus.reporting.ReportType
import com.eucalyptus.reporting.ReportFormat
import com.eucalyptus.reporting.art.entity.AvailabilityZoneArtEntity
import com.eucalyptus.reporting.art.entity.VolumeArtEntity
import com.eucalyptus.reporting.art.entity.VolumeUsageArtEntity

/**
 * 
 */
class VolumeRendererTest extends RendererTestSupport {

  @Test
  void testCsvLayout() {
    assertCsvColumns( render( ReportType.VOLUME, ReportFormat.CSV, art() ) )
  }

  @Test
  void testHtmlLayout() {
    assertHtmlColumns( render( ReportType.VOLUME, ReportFormat.HTML, art() ) )
  }

  private ReportArtEntity art() {
    ReportArtEntity art = new ReportArtEntity( millis("2012-10-01T00:00:00"), millis("2012-10-10T00:00:00") )
    AccountArtEntity account1Art = new AccountArtEntity()
    AccountArtEntity account2Art = new AccountArtEntity()
    AvailabilityZoneArtEntity zoneArt = new AvailabilityZoneArtEntity()
    art.getZones().put("Zone1", zoneArt )
    zoneArt.getAccounts().put("a1", account1Art)
    zoneArt.getAccounts().put("a2", account2Art)
    UserArtEntity acc1user1Art = new UserArtEntity();
    UserArtEntity acc1user2Art = new UserArtEntity();
    UserArtEntity acc2user1Art = new UserArtEntity();
    account1Art.getUsers().put( "u1", acc1user1Art )
    account1Art.getUsers().put( "u2", acc1user2Art )
    account2Art.getUsers().put( "u1", acc2user1Art )
    acc1user1Art.getVolumes().put( "vol-00000001", new VolumeArtEntity("vol-00000001") )
    acc1user2Art.getVolumes().put( "vol-00000002", new VolumeArtEntity("vol-00000002") )
    acc2user1Art.getVolumes().put( "vol-00000003", new VolumeArtEntity("vol-00000003") )
    acc2user1Art.getVolumes().put( "vol-00000004", new VolumeArtEntity("vol-00000004") )
    acc2user1Art.getVolumes().get( "vol-00000004" ).getInstanceAttachments()
        .put( "i-0000001", new VolumeUsageArtEntity() )
    art
  }
}
