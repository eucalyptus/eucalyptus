package com.eucalyptus.reporting.art.renderer

import org.junit.Test
import com.eucalyptus.reporting.ReportType
import com.eucalyptus.reporting.ReportFormat
import com.eucalyptus.reporting.art.entity.ReportArtEntity
import com.eucalyptus.reporting.art.entity.AccountArtEntity
import com.eucalyptus.reporting.art.entity.UserArtEntity
import com.eucalyptus.reporting.art.entity.ElasticIpArtEntity
import com.eucalyptus.reporting.art.entity.ElasticIpUsageArtEntity

/**
 * 
 */
class ElasticIpRendererTest extends RendererTestSupport {

  @Test
  void testCsvLayout() {
    assertCsvColumns( render( ReportType.ELASTIC_IP, ReportFormat.CSV, art() ) )
  }

  @Test
  void testHtmlLayout() {
    assertHtmlColumns( render( ReportType.ELASTIC_IP, ReportFormat.HTML, art() ) )
  }

  private ReportArtEntity art() {
    ReportArtEntity art = new ReportArtEntity( millis("2012-10-01T00:00:00"), millis("2012-10-10T00:00:00") )
    AccountArtEntity account1Art = new AccountArtEntity()
    AccountArtEntity account2Art = new AccountArtEntity()
    art.getAccounts().put("a1", account1Art)
    art.getAccounts().put("a2", account2Art)
    UserArtEntity acc1user1Art = new UserArtEntity();
    UserArtEntity acc1user2Art = new UserArtEntity();
    UserArtEntity acc2user1Art = new UserArtEntity();
    account1Art.getUsers().put( "u1", acc1user1Art )
    account1Art.getUsers().put( "u2", acc1user2Art )
    account2Art.getUsers().put( "u1", acc2user1Art )
    acc1user1Art.getElasticIps().put("1.1.1.2", new ElasticIpArtEntity())
    acc1user1Art.getElasticIps().get("1.1.1.2").getInstanceAttachments()
        .put("i-00000001", new  ElasticIpUsageArtEntity() )
    acc2user1Art.getElasticIps().put("1.1.1.1", new ElasticIpArtEntity())
    art
  }
}
