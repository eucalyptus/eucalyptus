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

import org.apache.log4j.Logger;

import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Expendable;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.async.MessageCallback;

import com.eucalyptus.cluster.common.msgs.ClusterTerminateInstancesResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterTerminateInstancesType;

public class TerminateCallback extends MessageCallback<ClusterTerminateInstancesType,ClusterTerminateInstancesResponseType> implements Expendable<TerminateCallback> {
  
  private static Logger LOG = Logger.getLogger( TerminateCallback.class );
  private String        instanceId;
  
  public TerminateCallback( String instanceId ) {
    super( new ClusterTerminateInstancesType( instanceId ) );
    this.instanceId = instanceId;
  }
  
  @Override
  public void initialize( ClusterTerminateInstancesType msg ) {
    EventRecord.here( ClusterTerminateInstancesType.class, EventType.VM_TERMINATING, LogUtil.dumpObject( msg ) ).info( );
  }
  
  @Override
  public void fire( ClusterTerminateInstancesResponseType reply ) {
    EventRecord.here( ClusterTerminateInstancesType.class, EventType.VM_TERMINATED, LogUtil.dumpObject( reply  ) ).info( );
  }
  
  @Override
  public void fireException( Throwable e ) {
    LOG.debug( LogUtil.subheader( this.getRequest( ).toString( "eucalyptus_ucsb_edu" ) ) );
    LOG.debug( e, e );
  }

  @Override
  public boolean duplicateOf( TerminateCallback that ) {
    return this.getRequest( ).getInstancesSet( ).containsAll( that.getRequest( ).getInstancesSet( ) );
  }
  
}
