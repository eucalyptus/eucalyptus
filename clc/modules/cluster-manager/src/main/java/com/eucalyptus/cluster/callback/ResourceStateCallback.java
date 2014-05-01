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

import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.node.NodeController;
import com.eucalyptus.node.Nodes;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.vmtypes.VmType;
import com.eucalyptus.vmtypes.VmTypes;
import com.google.common.base.Joiner;
import edu.ucsb.eucalyptus.msgs.DescribeResourcesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeResourcesType;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;
import org.apache.log4j.Logger;

import java.util.concurrent.Callable;

public class ResourceStateCallback extends StateUpdateMessageCallback<Cluster, DescribeResourcesType, DescribeResourcesResponseType> {
  private static Logger LOG = Logger.getLogger( ResourceStateCallback.class );
  
  public ResourceStateCallback( ) {
    super( new DescribeResourcesType( ) {
      {
        regarding( );
        for ( VmType arg0 : VmTypes.list( ) ) {
          getInstanceTypes( ).add( new VmTypeInfo( arg0.getName( ), arg0.getMemory( ), arg0.getDisk( ), arg0.getCpu( ), "sda1" ) {
            {
              this.setSwap( "sda2", 512 * 1024l * 1024l );
            }
          } );
        }
      }
    } );
  }
  
  /**
   * @see com.eucalyptus.util.async.MessageCallback#fire(edu.ucsb.eucalyptus.msgs.BaseMessage)
   * @param reply
   */
  @Override
  public void fire( final DescribeResourcesResponseType reply ) {
    final Cluster cluster = this.getSubject();
    cluster.getNodeState().update( reply.getResources( ) );
    LOG.debug( "Adding node service tags: " + Joiner.on( ", " ).join( reply.getNodes() ) );
      Callable<Boolean> updateNodes = new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
      try {
            Nodes.updateNodeInfo( cluster.getConfiguration(), reply.getNodes() );
            return true;
      } catch ( Exception e ) {
            LOG.error( e, e );
        LOG.trace( e, e );
            return false;
      }
      }
      };
      //GRZE: submit the node controller state updates in a separate thread to ensure it doesn't interfere with the Cluster state machine.
      Threads.enqueue( NodeController.class, ResourceStateCallback.class, updateNodes );
  }
  
  /**
   * @see com.eucalyptus.cluster.callback.StateUpdateMessageCallback#fireException(com.eucalyptus.util.async.FailedRequestException)
   * @param t
   */
  @Override
  public void fireException( FailedRequestException t ) {
    LOG.debug( "Request to " + this.getSubject( ).getName( ) + " failed: " + t.getMessage( ) );
  }

}
