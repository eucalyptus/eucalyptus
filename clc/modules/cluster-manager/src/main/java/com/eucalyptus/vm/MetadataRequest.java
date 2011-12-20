/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.vm;

import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.cluster.ClusterConfiguration;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;

public class MetadataRequest {
  private static Logger    LOG = Logger.getLogger( MetadataRequest.class );
  private final String     requestIp;
  private final String     metadataName;
  private final String     localPath;
  private final VmInstance vm;
  
  public MetadataRequest( String requestIp, String requestUrl ) {
    super( );
    try {
      this.requestIp = requestIp;
      String[] path = requestUrl.replaceFirst( "/", "?" ).split( "\\?" );
      if ( path.length > 0 ) {
        this.metadataName = path[0];
        if ( path.length > 1 ) {
          this.localPath = path[1].replaceFirst( "^[/]*", "" ).replaceAll( "[/]+", "/" );
        } else {
          this.localPath = "";
        }
      } else {
        this.metadataName = "";
        this.localPath = "";
      }
      VmInstance findVm = null;
      if ( !Databases.isVolatile( ) ) {
        try {
          findVm = VmInstances.lookupByPublicIp( requestIp );
        } catch ( Exception ex2 ) {
          try {
            findVm = VmInstances.lookupByPrivateIp( requestIp );
          } catch ( Exception ex ) {
            Logs.exhaust( ).error( ex );
          }
        }
      } 
      this.vm = findVm;
    } finally {
      LOG.debug( ( this.vm != null
                                  ? "Instance"
                                  : "External" )
                 + " Metadata: requestIp=" + this.requestIp
                 + " metadataName=" + this.metadataName
                 + " metadataPath=" + this.localPath
                 + " requestUrl=" + requestUrl );
    }
  }
  
  public boolean isInstance( ) {
    return vm != null;
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
  
  public VmInstance getVmInstance( ) {
    return this.vm;
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
        LOG.debug( "Unable to find Cluster Controller request addresss.", e );
        db.rollback( );
      }
    }
    return false;
  }
}
