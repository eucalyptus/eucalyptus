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

package com.eucalyptus.cluster.callback;

import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.compute.common.network.NetworkReportType;
import com.eucalyptus.compute.common.network.NetworkResourceReportType;
import com.eucalyptus.compute.common.network.Networking;
import com.eucalyptus.compute.common.network.UpdateNetworkResourcesType;
import com.eucalyptus.network.EdgeNetworking;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.FailedRequestException;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import edu.ucsb.eucalyptus.msgs.DescribeNetworksResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeNetworksType;
import edu.ucsb.eucalyptus.msgs.NetworkInfoType;

public class NetworkStateCallback extends StateUpdateMessageCallback<Cluster, DescribeNetworksType, DescribeNetworksResponseType> {
  private static Logger LOG = Logger.getLogger( NetworkStateCallback.class );
  
  public NetworkStateCallback( ) {
    super( new DescribeNetworksType( ) {
      {
        regarding( );
        setClusterControllers( Lists.newArrayList( Clusters.getInstance( ).getClusterAddresses( ) ) );
        setVmsubdomain( VmInstances.INSTANCE_SUBDOMAIN.substring( 1 ) );
        setNameserver( edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration.getSystemConfiguration( ).getNameserverAddress( ) );
        setDnsDomainName( SystemConfiguration.getSystemConfiguration( ).getDnsDomain( ).replaceAll("^\\.","") );
      }
    } );
  }
  
  /**
   * @see com.eucalyptus.util.async.MessageCallback#fire(edu.ucsb.eucalyptus.msgs.BaseMessage)
   * @param reply
   */
  @Override
  public void fire( final DescribeNetworksResponseType reply ) {
    EdgeNetworking.setReported( "EDGE".equals( reply.getMode( ) ) );
    UpdateNetworkResourcesType update = new UpdateNetworkResourcesType( );
    update.setCluster( this.getSubject( ).getName( ) );
    update.setResources( TypeMappers.transform( reply, NetworkResourceReportType.class ) );
    Networking.getInstance( ).update( update );
  }

  /**
   * @see com.eucalyptus.cluster.callback.StateUpdateMessageCallback#fireException(com.eucalyptus.util.async.FailedRequestException)
   * @param t
   */
  @Override
  public void fireException( FailedRequestException t ) {
    LOG.debug( "Request to " + this.getSubject( ).getName( ) + " failed: " + t.getMessage( ) );
  }

  @TypeMapper
  public enum DescribeNetworksResponseTypeToNetworkResourceReport implements Function<DescribeNetworksResponseType,NetworkResourceReportType> {
    INSTANCE;

    @Nullable
    @Override
    public NetworkResourceReportType apply( final DescribeNetworksResponseType response ) {
      final NetworkResourceReportType report = new NetworkResourceReportType( );
      report.setUseVlans( response.getUseVlans( ) );
      report.setMode( response.getMode( ) );
      report.setAddrsPerNet( response.getAddrsPerNet( ) );
      report.setAddrIndexMin( response.getAddrIndexMin( ) );
      report.setAddrIndexMax( response.getAddrIndexMax() );
      report.setVlanMin( response.getVlanMin( ) );
      report.setVlanMax( response.getVlanMax( ) );
      report.setVnetSubnet( response.getVnetSubnet( ) );
      report.setVnetNetmask( response.getVnetNetmask( ) );
      report.setPrivateIps( response.getPrivateIps( ) );
      report.setActiveNetworks( Lists.newArrayList( Iterables.transform(
          response.getActiveNetworks( ),
          TypeMappers.lookup( NetworkInfoType.class, NetworkReportType.class ) ) ) );
      return report;
    }
  }

  @TypeMapper
  public enum NetworkInfoTypeToNetworkReportType implements Function<NetworkInfoType,NetworkReportType> {
    INSTANCE;

    @Nullable
    @Override
    public NetworkReportType apply( final NetworkInfoType networkInfo ) {
      final NetworkReportType report = new NetworkReportType( );
      report.setUuid( networkInfo.getUuid() );
      report.setTag( networkInfo.getTag() );
      report.setNetworkName( networkInfo.getNetworkName() );
      report.setAccountNumber( networkInfo.getAccountNumber() );
      report.setAllocatedIndexes( networkInfo.getAllocatedIndexes() );
      return report;
    }
  }
}
