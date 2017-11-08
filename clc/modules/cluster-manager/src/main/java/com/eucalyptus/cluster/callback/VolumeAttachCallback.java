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

package com.eucalyptus.cluster.callback;

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.blockstorage.msgs.DetachStorageVolumeType;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.MessageCallback;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.compute.common.internal.vm.VmVolumeAttachment.AttachmentState;
import com.google.common.base.Function;
import com.eucalyptus.cluster.common.msgs.ClusterAttachVolumeResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterAttachVolumeType;

public class VolumeAttachCallback extends MessageCallback<ClusterAttachVolumeType, ClusterAttachVolumeResponseType> {
  private static Logger        LOG = Logger.getLogger( VolumeAttachCallback.class );
  
  public VolumeAttachCallback( ClusterAttachVolumeType request ) {
    super( request );
  }
  
  @Override
  public void initialize( ClusterAttachVolumeType msg ) {
    final String instanceId = this.getRequest( ).getInstanceId( );
    final String volumeId = this.getRequest( ).getVolumeId( );
    final Function<String, VmInstance> funcName = new Function<String, VmInstance>( ) {
      public VmInstance apply( final String input ) {
        VmInstance vm = VmInstances.lookup( instanceId );
        try {
          if ( !AttachmentState.attached.equals( vm.lookupVolumeAttachment( volumeId ).getAttachmentState( ) ) ) {
            VmInstances.updateVolumeAttachment( vm, volumeId, AttachmentState.attaching );
          }
        } catch ( Exception ex ) {
          VmInstances.updateVolumeAttachment( vm, volumeId, AttachmentState.attaching );
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
  public void fire( ClusterAttachVolumeResponseType reply ) {}
  
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
          VmInstances.removeVolumeAttachment( vm, VolumeAttachCallback.this.getRequest( ).getVolumeId( ) );
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
