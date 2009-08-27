/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
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
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.msgs.*;

import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.ClusterMessageQueue;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.ws.client.Client;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListSet;

public class NodeCertCallback extends QueuedEventCallback<GetKeysType> implements Runnable {

  private static Logger LOG = Logger.getLogger( NodeCertCallback.class );

  private static int SLEEP_TIMER = 30 * 1000;
  private NavigableSet<GetKeysType> requests = null;
  private NavigableSet<NodeCertInfo> results = null;
  
  
  public NodeCertCallback( ClusterConfiguration config ) {
    super( config );
    this.requests = new ConcurrentSkipListSet<GetKeysType>();
    this.results = new ConcurrentSkipListSet<NodeCertInfo>();
  }

  public void process( final Client cluster, final GetKeysType msg ) throws Exception {
    try {
      GetKeysResponseType reply = ( GetKeysResponseType ) cluster.send( msg );
      NodeCertInfo certInfo = reply.getCerts();
      if ( certInfo != null ) {
        certInfo.setServiceTag( certInfo.getServiceTag().replaceAll( "EucalyptusGL", "EucalyptusNC" ) );
        if ( certInfo.getCcCert() != null && certInfo.getCcCert().length() > 0 ) {
          certInfo.setCcCert( new String( Base64.decode( certInfo.getCcCert() ) ) );
        }
        if ( certInfo.getNcCert() != null && certInfo.getNcCert().length() > 0 ) {
          certInfo.setNcCert( new String( Base64.decode( certInfo.getNcCert() ) ) );
        }
        results.add( certInfo );
      }
      requests.remove( msg );
    }
    catch ( Exception e ) {
      LOG.error( e );
    }
  }

  @Override
  public void notifyHandler() {
    if ( requests.isEmpty() ) {
      super.notifyHandler();
    }
  }

  public void run() {
    do {
      Cluster cluster = Clusters.getInstance( ).lookup( this.getConfig( ).getName( ) );
      if ( !cluster.getNodeTags().isEmpty() ) {
        LOG.debug( "Querying all known service tags:" );
        for ( String serviceTag : cluster.getNodeTags() ) {
          LOG.debug( "- " + serviceTag );
          GetKeysType msg = new GetKeysType( serviceTag.replaceAll( "EucalyptusNC", "EucalyptusGL" ) );
          this.requests.add( msg );
          cluster.getMessageQueue().enqueue( new QueuedLogEvent( this, msg ) );
        }
        this.waitForEvent();
        //TODO: FIXME
//        cluster.updateNodeCerts( results );
        this.results.clear();
      }
    } while ( !this.isStopped() && this.sleep( SLEEP_TIMER ) );
  }

}
