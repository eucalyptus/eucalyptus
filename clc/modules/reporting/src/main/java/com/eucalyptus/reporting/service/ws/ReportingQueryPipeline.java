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
package com.eucalyptus.reporting.service.ws;

import static com.eucalyptus.auth.principal.TemporaryAccessKey.TemporaryKeyType;
import java.util.EnumSet;
import org.jboss.netty.channel.ChannelPipeline;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.component.id.Reporting;
import com.eucalyptus.ws.server.QueryPipeline;

@ComponentPart(Reporting.class)
public class ReportingQueryPipeline extends QueryPipeline {

  public ReportingQueryPipeline() {
    super(
        "reporting-query-pipeline",
        "/services/Reporting",
        EnumSet.allOf( TemporaryKeyType.class ) );
  }

  @Override
  public ChannelPipeline addHandlers( ChannelPipeline pipeline ) {
    super.addHandlers( pipeline );
    pipeline.addLast( "reporting-query-binding", new ReportingQueryBinding( ) );
    return pipeline;
  }
}
