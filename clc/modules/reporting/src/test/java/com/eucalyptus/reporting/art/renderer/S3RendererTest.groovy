package com.eucalyptus.reporting.art.renderer

import org.junit.Test
import com.eucalyptus.reporting.art.entity.ReportArtEntity
import com.eucalyptus.reporting.art.entity.AccountArtEntity
import com.eucalyptus.reporting.art.entity.UserArtEntity
import com.eucalyptus.reporting.ReportType
import com.eucalyptus.reporting.ReportFormat
import com.eucalyptus.reporting.art.entity.BucketUsageArtEntity

/**
 * 
 */
class S3RendererTest extends RendererTestSupport {

  @Test
  void testCsvLayout() {
    assertCsvColumns( render( ReportType.S3, ReportFormat.CSV, art() ) )
  }

  @Test
  void testHtmlLayout() {
    assertHtmlColumns( render( ReportType.S3, ReportFormat.HTML, art() ) )
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
    acc1user1Art.getBucketUsage().put("bucket1", new BucketUsageArtEntity())
    acc1user2Art.getBucketUsage().put("bucket2", new BucketUsageArtEntity())
    acc2user1Art.getBucketUsage().put("bucket3", new BucketUsageArtEntity())
    art
  }
}
