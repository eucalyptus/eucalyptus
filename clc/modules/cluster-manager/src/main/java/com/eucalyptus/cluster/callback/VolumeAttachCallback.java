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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.cluster.callback;

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.component.Dispatcher;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.MessageCallback;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.vm.VmVolumeAttachment.AttachmentState;
import com.eucalyptus.ws.client.ServiceDispatcher;
import com.google.common.base.Function;
import edu.ucsb.eucalyptus.msgs.AttachVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.AttachVolumeType;
import edu.ucsb.eucalyptus.msgs.AttachedVolume;
import edu.ucsb.eucalyptus.msgs.DetachStorageVolumeType;

public class VolumeAttachCallback extends MessageCallback<AttachVolumeType, AttachVolumeResponseType> {
  private static Logger        LOG = Logger.getLogger( VolumeAttachCallback.class );
  
  public VolumeAttachCallback( AttachVolumeType request ) {
    super( request );
  }
  
  @Override
  public void initialize( AttachVolumeType msg ) {
    final String instanceId = this.getRequest( ).getInstanceId( );
    final String volumeId = this.getRequest( ).getVolumeId( );
    final Function<String, VmInstance> funcName = new Function<String, VmInstance>( ) {
      public VmInstance apply( final String input ) {
        VmInstance vm = VmInstances.lookup( instanceId );
        try {
          if ( !AttachmentState.attached.equals( vm.lookupVolumeAttachment( volumeId ).getAttachmentState( ) ) ) {
            vm.updateVolumeAttachment( volumeId, AttachmentState.attaching );
          }
        } catch ( Exception ex ) {
          vm.updateVolumeAttachment( volumeId, AttachmentState.attaching );
        }
        return vm;
      }
    };
    try {
      Entities.asTransaction( VmInstance.class, funcName ).apply( this.getRequest( ).getInstanceId( ) );
    } catch ( NoSuchElementException e1 ) {
      LOG.error( "Failed to lookup volume attachment state in order to update: " + this.getRequest( ).getVolumeId( ) + " due to " + e1.getMessage( ), e1 );
    }
  }
  
  @Override
  public void fire( AttachVolumeResponseType reply ) {}
  
  @Override
  public void fireException( Throwable e ) {
    LOG.debug( e );
    Logs.extreme( ).error( e, e );
    LOG.debug( "Trying to remove invalid volume attachment " + this.getRequest( ).getVolumeId( ) + " from instance " + this.getRequest( ).getInstanceId( ) );
    try {
      VmInstance vm = VmInstances.lookup( this.getRequest( ).getInstanceId( ) );
      Partition partition = vm.lookupPartition( );
      ServiceConfiguration sc = Topology.lookup( Storage.class, partition );
      /** clean up SC session state **/
      try {
        LOG.debug( "Sending detach after async failure in attach volume: " + this.getRequest( ).getVolumeId( ) + " sc=" + sc );
        AsyncRequests.sendSync( sc, new DetachStorageVolumeType( this.getRequest( ).getVolumeId( ) ) );
      } catch ( Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
      }
      /** clean up internal attachment state **/
      final Function<String, VmInstance> removeVolAttachment = new Function<String, VmInstance>( ) {
        public VmInstance apply( final String input ) {
          VmInstance vm = VmInstances.lookup( input );
          vm.removeVolumeAttachment( VolumeAttachCallback.this.getRequest( ).getVolumeId( ) );
          return vm;
        }
      };
      Entities.asTransaction( VmInstance.class, removeVolAttachment ).apply( this.getRequest( ).getInstanceId( ) );
      LOG.debug( "Removed failed attachment: " + this.getRequest( ).getVolumeId( ) + " -> " + vm.getInstanceId( ) );
    } catch ( Exception e1 ) {
      LOG.error( e1 );
      Logs.extreme( ).error( e1, e1 );
    }
  }
  
}
