/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.cluster.proxy;

import com.eucalyptus.cluster.common.ClusterController;
import com.eucalyptus.component.annotation.ComponentApi;
import com.eucalyptus.component.annotation.Description;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.InternalService;
import com.eucalyptus.component.annotation.Partition;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.ws.StackConfiguration;
import com.google.common.base.MoreObjects;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;

@Partition( value = { Eucalyptus.class } )
public class ProxyClusterController extends ComponentId {
  
  private static final long       serialVersionUID = 1L;
  public static final ComponentId INSTANCE         = new ProxyClusterController( );
  
  public ProxyClusterController( ) {
    super( "proxy-cluster" );
  }
  
  @Override
  public Integer getPort( ) {
    return 8774;
  }

  @Override
  public boolean isRegisterable( ) {
    return false;
  }

  @Override
  public String getServicePath( final String... pathParts ) {
    return "/axis2/services/EucalyptusCC";
  }
  
  @Override
  public String getInternalServicePath( final String... pathParts ) {
    return this.getServicePath( pathParts );
  }

  @Override
  public Bootstrap getClientBootstrap() {
    return super.getClientBootstrap( )
        .option( ChannelOption.CONNECT_TIMEOUT_MILLIS, MoreObjects.firstNonNull( StackConfiguration.CLUSTER_CONNECT_TIMEOUT_MILLIS, 3000 ) );
  }

  @Partition( ProxyClusterController.class )
  @InternalService
  public static class GatherLogService extends ComponentId {

    private static final long serialVersionUID = 1L;

    public GatherLogService( ) {
      super( "gatherlog" );
    }

    @Override
    public Integer getPort( ) {
      return 8774;
    }

    @Override
    public String getServicePath( final String... pathParts ) {
      return "/axis2/services/EucalyptusGL";
    }

    @Override
    public String getInternalServicePath( final String... pathParts ) {
      return this.getServicePath( pathParts );
    }

    @Override
    public Bootstrap getClientBootstrap() {
      return super.getClientBootstrap( )
          .option( ChannelOption.CONNECT_TIMEOUT_MILLIS, MoreObjects.firstNonNull( StackConfiguration.CLUSTER_CONNECT_TIMEOUT_MILLIS, 3000 ) );
    }
  }
}
