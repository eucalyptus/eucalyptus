/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

package com.eucalyptus.cluster.common.callback;

import static com.eucalyptus.compute.common.internal.vm.VmInstance.VmState.PENDING;
import static com.eucalyptus.compute.common.internal.vm.VmInstance.VmState.RUNNING;
import static com.eucalyptus.compute.common.internal.vm.VmInstance.VmState.SHUTTING_DOWN;
import static com.eucalyptus.compute.common.internal.vm.VmInstance.VmState.STOPPING;

import java.util.Set;

import org.apache.log4j.Logger;

import com.eucalyptus.cluster.common.Cluster;
import com.eucalyptus.cluster.common.vm.VmStateUpdate;
import com.eucalyptus.compute.common.internal.vm.VmInstances;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vm.VmInstance.VmState;
import com.eucalyptus.util.async.FailedRequestException;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;

import com.eucalyptus.cluster.common.msgs.VmDescribeResponseType;
import com.eucalyptus.cluster.common.msgs.VmDescribeType;

public class VmStateCallback extends StateUpdateMessageCallback<Cluster, VmDescribeType, VmDescribeResponseType> {
  private static Logger               LOG                       = Logger.getLogger( VmStateCallback.class );

  private final Supplier<Set<String>> initialInstances;
  
  public VmStateCallback( ) {
    super( new VmDescribeType( ).<VmDescribeType>regarding( ) );
    this.initialInstances = createInstanceSupplier( this, PENDING, RUNNING, SHUTTING_DOWN, STOPPING );
  }
  
  private static Supplier<Set<String>> createInstanceSupplier(
      final StateUpdateMessageCallback<Cluster, ?, ?> cb,
      final VmState... states
  ) {
    return Suppliers.memoize( new Supplier<Set<String>>( ) {
      @Override
      public Set<String> get( ) {
        return Sets.newHashSet( VmInstances.listWithProjection(
            VmInstances.instanceIdProjection( ),
            VmInstance.criterion( states ),
            VmInstance.nonNullNodeCriterion( ),
            VmInstance.zoneCriterion( cb.getSubject( ).getConfiguration( ).getPartition( ) )
        ) );
      }
    } );
  }

  @Override
  public void fireException( FailedRequestException t ) {
    LOG.debug( "Request to " + this.getSubject( ).getName( ) + " failed: " + t.getMessage( ) );
  }
  
  @Override
  public void fire( VmDescribeResponseType reply ) {
    try {
      this.getSubject( ).updateVmInfo( new VmStateUpdate( this.getSubject( ), initialInstances.get( ), reply.getVms( ) ) );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
  }

  @Override
  public void setSubject( Cluster subject ) {
    super.setSubject( subject );
    this.initialInstances.get( );
  }

}
