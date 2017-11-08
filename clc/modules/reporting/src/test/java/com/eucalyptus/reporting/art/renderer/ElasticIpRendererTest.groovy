/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
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
