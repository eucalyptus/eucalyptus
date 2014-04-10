/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.vm;

import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.cluster.ClusterConfiguration;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.entities.Entities;
import com.google.common.base.Optional;

public class MetadataRequest {
  private static Logger    LOG = Logger.getLogger( MetadataRequest.class );
  private final String     requestIp;
  private final String     metadataName;
  private final String     localPath;
  private final String     vmId;

  public MetadataRequest( String requestIp, String requestUrl, Optional<String> vmId ) {
    try {
      this.requestIp = requestIp;
      requestUrl = requestUrl.replaceAll( "[/]+", "/" );
      String[] path = requestUrl.split( "/", 2 );
      if ( path.length > 0 ) {
        this.metadataName = path[0];
        if ( path.length > 1 ) {
          this.localPath = path[1].replaceFirst( "^[/]*", "" );
        } else {
          this.localPath = "";
        }
      } else {
        this.metadataName = "";
        this.localPath = "";
      }
      this.vmId = vmId.orNull( );
    } finally {
      LOG.debug( ( this.vmId != null
                                  ? "Instance"
                                  : "External" )
                 + " Metadata: requestIp=" + this.requestIp
                 + " metadataName=" + this.metadataName
                 + " metadataPath=" + this.localPath
                 + " requestUrl=" + requestUrl );
    }
  }

  public boolean isInstance( ) {
    return vmId != null;
  }
  
  /**
   * @return the requestIp
   */
  public String getRequestIp( ) {
    return this.requestIp;
  }
  
  /**
   * @return the metadataName
   */
  public String getMetadataName( ) {
    return this.metadataName;
  }
  
  /**
   * @return the localPath
   */
  public String getLocalPath( ) {
    return this.localPath;
  }
  
  public String getVmInstanceId( ) {
    return this.vmId;
  }
  
  public boolean isSystem( ) {
    for ( ServiceConfiguration config : Components.lookup( ClusterController.class ).services( ) ) {
      if (  config.getHostName( ).equals( this.requestIp ) ) {
        return true;
      } else if ( config instanceof ClusterConfiguration && ( ( ClusterConfiguration ) config ).getSourceHostName( ).equals( this.requestIp ) ) {
        return true;
      }
    }
    if ( !Databases.isVolatile( ) ) {
      ClusterConfiguration cConfig = new ClusterConfiguration( );
      cConfig.setSourceHostName( this.requestIp );
      EntityTransaction db = Entities.get( ClusterConfiguration.class );
      
      try {
        ClusterConfiguration ccAddresses = Entities.uniqueResult( cConfig );
        if ( ccAddresses.getSourceHostName( ).equals( this.requestIp )
             || ccAddresses.getHostName( ).equals( this.requestIp ) ) {
          db.commit( );
          return true;
        } else {
          db.commit( );
        }
      } catch ( Exception e ) {
        LOG.debug( "Unable to find Cluster Controller request address.", e );
        db.rollback( );
      }
    }
    return false;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    final MetadataRequest that = (MetadataRequest) o;

    if ( localPath != null ? !localPath.equals( that.localPath ) : that.localPath != null ) return false;
    if ( metadataName != null ? !metadataName.equals( that.metadataName ) : that.metadataName != null ) return false;
    if ( requestIp != null ? !requestIp.equals( that.requestIp ) : that.requestIp != null ) return false;
    if ( vmId != null ? !vmId.equals( that.vmId ) : that.vmId != null ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = requestIp != null ? requestIp.hashCode() : 0;
    result = 31 * result + ( metadataName != null ? metadataName.hashCode() : 0 );
    result = 31 * result + ( localPath != null ? localPath.hashCode() : 0 );
    result = 31 * result + ( vmId != null ? vmId.hashCode() : 0 );
    return result;
  }
}
