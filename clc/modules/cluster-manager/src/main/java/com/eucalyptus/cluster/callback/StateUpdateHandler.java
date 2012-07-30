/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.cluster.callback;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.async.MessageCallback;
import com.eucalyptus.util.async.RemoteCallback;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

public class StateUpdateHandler implements EventListener {
  private static Logger LOG = Logger.getLogger( StateUpdateHandler.class );
  private static final ConcurrentMap<String,StateUpdateHandler> clusterMap = Maps.newConcurrentMap( );
  private final ConcurrentMap<Class,AtomicBoolean> inflightMap = Maps.newConcurrentMap( );
  private final ConcurrentMap<Class,MessageCallback> callbackMap = Maps.newConcurrentMap( );
  private final Cluster cluster;
  
  public static void create( Cluster cluster, RemoteCallback callback ) {
    StateUpdateHandler handler = new StateUpdateHandler( cluster );
    StateUpdateHandler.clusterMap.putIfAbsent( cluster.getName( ), handler );
    handler = StateUpdateHandler.clusterMap.get( cluster.getName( ) );
    EventRecord.here( StateUpdateHandler.class, EventType.CLUSTER_STATE_HANDLER_REGISTERED, cluster.getName( ), callback.getClass( ).getCanonicalName( ) );
  }
  
  private StateUpdateHandler( Cluster cluster ) {
    this.cluster = cluster;
  }

  public boolean timedTrigger( Event c ) {
    if ( c instanceof ClockTick ) {
      return ( ( ClockTick ) c ).isBackEdge( );
    } else {
      return false;
    }
  }

  public void addCallback( MessageCallback cb ) {
    if( this.callbackMap.putIfAbsent( cb.getClass( ), cb ) == null ) {
      this.inflightMap.put( cb.getClass( ), new AtomicBoolean( false ) );
    } else {
      LOG.debug( "Ignoring addition of timed callback for cluster "+ this.cluster.getName( )+ " which is already configured: " + cb.getClass( ) );
    }
  }

  @Override
  public void fireEvent( Event event ) {
    if( this.timedTrigger( event ) ) {
      Iterables.all( callbackMap.keySet( ), new Predicate<Class>() {

        @Override
        public boolean apply( Class arg0 ) {
          if( StateUpdateHandler.this.inflightMap.get( arg0 ).compareAndSet( false, true ) ) {
            //TODO: RELEASE: wrap this up here.
          }
          return false;
        }} );
    }
  }

}
