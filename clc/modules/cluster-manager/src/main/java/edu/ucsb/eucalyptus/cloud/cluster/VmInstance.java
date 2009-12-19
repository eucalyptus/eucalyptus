/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */

package edu.ucsb.eucalyptus.cloud.cluster;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.util.EucalyptusProperties;
import com.eucalyptus.util.HasName;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.Network;
import edu.ucsb.eucalyptus.cloud.VmImageInfo;
import edu.ucsb.eucalyptus.cloud.VmKeyInfo;
import edu.ucsb.eucalyptus.constants.VmState;
import edu.ucsb.eucalyptus.msgs.AttachedVolume;
import edu.ucsb.eucalyptus.msgs.NetworkConfigType;
import edu.ucsb.eucalyptus.msgs.RunningInstancesItemType;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;
import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VmInstance implements HasName {
  private static Logger        LOG         = Logger.getLogger( VmInstance.class );

  public static String         DEFAULT_IP  = "0.0.0.0";
  private String               reservationId;
  private int                  launchIndex;
  private String               instanceId;
  private String               ownerId;
  private String               placement;
  private Date                 launchTime;
  private StopWatch            stopWatch;
  private volatile int                  networkIndex;

  private String               userData;
  private String               serviceTag;
  private String               reason;
  private VmImageInfo          imageInfo;
  private VmKeyInfo            keyInfo;
  private VmTypeInfo           vmTypeInfo;
  private List<Network>        networks    = Lists.newArrayList( );
  private VmState              state;
  private StringBuffer         consoleOutput;
  private List<AttachedVolume> volumes     = Lists.newArrayList( );
  private NetworkConfigType    networkConfig;
  private Boolean              privateNetwork;
  private List<String>         ancestorIds = Lists.newArrayList( );

  public VmInstance( ) {
    this.launchTime = new Date( );
    this.state = VmState.PENDING;
    this.networkConfig = new NetworkConfigType( );
    this.stopWatch = new StopWatch( );
    this.stopWatch.start( );
    this.consoleOutput = new StringBuffer( );
    this.volumes = new ArrayList<AttachedVolume>( );
  }

  public VmInstance( final String reservationId, final int launchIndex, final String instanceId, final String ownerId, final String placement, final String userData, final VmImageInfo imageInfo, final VmKeyInfo keyInfo, final VmTypeInfo vmTypeInfo, final List<Network> networks, String networkIndex ) {
    this( );

    this.reservationId = reservationId;
    this.launchIndex = launchIndex;
    this.instanceId = instanceId;
    this.ownerId = ownerId;
    this.placement = placement;
    this.userData = userData;
    this.imageInfo = imageInfo;
    this.keyInfo = keyInfo;
    this.vmTypeInfo = vmTypeInfo;
    this.networks = networks;
    this.networkIndex = Integer.parseInt( networkIndex );
    String mac = String.format( "%s:%s:%s:%s", this.instanceId.substring( 2, 4 ), this.instanceId.substring( 4, 6 ), this.instanceId.substring( 6, 8 ), this.instanceId.substring( 8, 10 ) );
    this.networkConfig.setMacAddress( "d0:0d:" + mac );
    this.networkConfig.setIgnoredMacAddress( "d0:0f:" + mac );
    this.networkConfig.setIpAddress( DEFAULT_IP );
    this.networkConfig.setIgnoredPublicIp( DEFAULT_IP );
  }

  public String getName( ) {
    return this.instanceId;
  }

  public boolean equals( Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;

    VmInstance vmInstance = ( VmInstance ) o;

    if ( !instanceId.equals( vmInstance.instanceId ) ) return false;

    return true;
  }

  public void setLaunchTime( final Date launchTime ) {
    this.launchTime = launchTime;
  }

  public int hashCode( ) {
    return instanceId.hashCode( );
  }

  public String getReservationId( ) {
    return reservationId;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public String getOwnerId( ) {
    return ownerId;
  }

  public int getLaunchIndex( ) {
    return launchIndex;
  }

  public String getPlacement( ) {
    return placement;
  }

  public Date getLaunchTime( ) {
    return launchTime;
  }

  public VmState getState( ) {
    return state;
  }

  public void setState( final VmState state ) {
    if ( this.getState( ) != null && !this.getState( ).equals( state ) ) {
      LOG.info( String.format( "%s state change: %s -> %s", this.getInstanceId( ), this.getState( ), state ) );
    }
    this.state = state;
  }

  public String getUserData( ) {
    return userData;
  }

  public void setUserData( final String userData ) {
    this.userData = userData;
  }

  public RunningInstancesItemType getAsRunningInstanceItemType( ) {
    RunningInstancesItemType runningInstance = new RunningInstancesItemType( );

    runningInstance.setAmiLaunchIndex( Integer.toString( this.launchIndex ) );
    runningInstance.setStateCode( Integer.toString( this.state.getCode( ) ) );
    runningInstance.setStateName( this.state.getName( ) );

    runningInstance.setInstanceId( this.instanceId );
    runningInstance.setImageId( this.imageInfo.getImageId( ) );
    runningInstance.setKernel( this.imageInfo.getKernelId( ) );
    runningInstance.setRamdisk( this.imageInfo.getRamdiskId( ) );
    runningInstance.setProductCodes( this.imageInfo.getProductCodes( ) );

    if( Component.dns.isLocal( ) ) {
      try {
        StringBuffer sb = new StringBuffer( ).append( "euca-" );
        sb.append( this.getNetworkConfig( ).getIpAddress( ).replaceAll( "\\.", "-" ) );
        sb.append( ".eucalyptus." );
        String dnsDomain = edu.ucsb.eucalyptus.util.EucalyptusProperties.getSystemConfiguration( ).getDnsDomain( );
        runningInstance.setDnsName( sb.toString( ) + dnsDomain );
        runningInstance.setPrivateDnsName( sb.toString() + "internal" ); 
      } catch ( Exception e ) {
        setRunningIpAddresses( runningInstance );
      }
    } else {
      setRunningIpAddresses( runningInstance );
    } 
    if ( this.getReason( ) != null || !"".equals( this.getReason( ) ) ) runningInstance.setReason( this.getReason( ) );

    if ( this.getKeyInfo( ) != null ) runningInstance.setKeyName( this.getKeyInfo( ).getName( ) );
    else runningInstance.setKeyName( "" );

    runningInstance.setInstanceType( this.getVmTypeInfo( ).getName( ) );
    runningInstance.setPlacement( this.placement );

    runningInstance.setLaunchTime( this.launchTime );

    return runningInstance;
  }

  private void setRunningIpAddresses( RunningInstancesItemType runningInstance ) {
    runningInstance.setPrivateDnsName( this.getNetworkConfig( ).getIpAddress( ) );
    if ( !VmInstance.DEFAULT_IP.equals( this.getNetworkConfig( ).getIgnoredPublicIp( ) ) ) {
      runningInstance.setDnsName( this.getNetworkConfig( ).getIgnoredPublicIp( ) );
    } else {
      runningInstance.setDnsName( this.getNetworkConfig( ).getIpAddress( ) );
    }
  }


  
  public VmKeyInfo getKeyInfo( ) {
    return keyInfo;
  }

  public void setKeyInfo( final VmKeyInfo keyInfo ) {
    this.keyInfo = keyInfo;
  }

  public VmTypeInfo getVmTypeInfo( ) {
    return vmTypeInfo;
  }

  public void setVmTypeInfo( final VmTypeInfo vmTypeInfo ) {
    this.vmTypeInfo = vmTypeInfo;
  }

  public List<Network> getNetworks( ) {
    return networks;
  }

  public List<String> getNetworkNames( ) {
    List<String> nets = new ArrayList<String>( );
    for ( Network net : this.getNetworks( ) )
      nets.add( net.getNetworkName( ) );
    return nets;
  }

  public void setNetworks( final List<Network> networks ) {
    this.networks = networks;
  }

  public NetworkConfigType getNetworkConfig( ) {
    return networkConfig;
  }

  public void setNetworkConfig( final NetworkConfigType networkConfig ) {
    this.networkConfig = networkConfig;
  }

  public VmImageInfo getImageInfo( ) {
    return imageInfo;
  }

  public void setImageInfo( final VmImageInfo imageInfo ) {
    this.imageInfo = imageInfo;
  }

  public List<AttachedVolume> getVolumes( ) {
    return volumes;
  }

  public void setVolumes( final List<AttachedVolume> volumes ) {
    this.volumes = volumes;
  }

  public List<String> getAncestorIds( ) {
    return ancestorIds;
  }

  public void setAncestorIds( final List<String> ancestorIds ) {
    this.ancestorIds = ancestorIds;
  }

  public String getByKey( String path ) {

    Map<String, String> m = new HashMap<String, String>( );
    m.put( "ami-id", this.getImageInfo( ).getImageId( ) );
    m.put( "product-codes", this.getImageInfo( ).getProductCodes( ).toString( ).replaceAll( "[\\Q[]\\E]", "" ).replaceAll( ", ", "\n" ) );
    m.put( "ami-launch-index", "" + this.getLaunchIndex( ) );
    m.put( "ancestor-ami-ids", this.getImageInfo( ).getAncestorIds( ).toString( ).replaceAll( "[\\Q[]\\E]", "" ).replaceAll( ", ", "\n" ) );

    m.put( "ami-manifest-path", this.getImageInfo( ).getImageLocation( ) );
    m.put( "hostname", this.getNetworkConfig( ).getIgnoredPublicIp( ) );
    m.put( "instance-id", this.getInstanceId( ) );
    m.put( "instance-type", this.getVmTypeInfo( ).getName( ) );
    m.put( "local-hostname", this.getNetworkConfig( ).getIpAddress( ) );
    m.put( "local-ipv4", this.getNetworkConfig( ).getIpAddress( ) );
    m.put( "public-hostname", this.getNetworkConfig( ).getIgnoredPublicIp( ) );
    m.put( "public-ipv4", this.getNetworkConfig( ).getIgnoredPublicIp( ) );
    m.put( "reservation-id", this.getReservationId( ) );
    m.put( "kernel-id", this.getImageInfo( ).getKernelId( ) );
    m.put( "ramdisk-id", this.getImageInfo( ).getRamdiskId( ) );
    m.put( "security-groups", this.getNetworkNames( ).toString( ).replaceAll( "[\\Q[]\\E]", "" ).replaceAll( ", ", "\n" ) );

    m.put( "block-device-mapping/", "emi\nephemeral\nroot\nswap" );
    m.put( "block-device-mapping/emi", "sda1" );
    m.put( "block-device-mapping/ami", "sda1" );
    m.put( "block-device-mapping/ephemeral", "sda2" );
    m.put( "block-device-mapping/swap", "sda3" );
    m.put( "block-device-mapping/root", "/dev/sda1" );

    m.put( "public-keys/", "0=" + this.getKeyInfo( ).getName( ) );
    m.put( "public-keys/0", "openssh-key" );
    m.put( "public-keys/0/", "openssh-key" );
    m.put( "public-keys/0/openssh-key", this.getKeyInfo( ).getValue( ) );

    m.put( "placement/", "availability-zone" );
    m.put( "placement/availability-zone", this.getPlacement( ) );

    if ( path == null ) path = "";
    String dir = "";
    for ( String entry : m.keySet( ) ) {
      if ( entry.contains( "/" ) && !entry.endsWith( "/" ) ) continue;
      dir += entry + "\n";
    }
    m.put( "", dir );

    LOG.debug( "Servicing metadata request:" + path + " -> " + m.get( path ) );
    if ( m.containsKey( path + "/" ) ) path += "/";
    return m.get( path ).replaceAll( "\n*\\z", "" );
  }

  public int compareTo( final Object o ) {
    VmInstance that = ( VmInstance ) o;
    return this.getName( ).compareTo( that.getName( ) );
  }

  public synchronized long resetStopWatch( ) {
    this.stopWatch.stop( );
    long ret = this.stopWatch.getTime( );
    this.stopWatch.reset( );
    this.stopWatch.start( );
    return ret;
  }

  public synchronized long getSplitTime( ) {
    this.stopWatch.split( );
    long ret = this.stopWatch.getSplitTime( );
    this.stopWatch.unsplit( );
    return ret;
  }

  public String getReason( ) {
    return reason;
  }

  public void setReason( final String reason ) {
    this.reason = reason;
  }

  public StringBuffer getConsoleOutput( ) {
    return consoleOutput;
  }

  public void setConsoleOutput( final StringBuffer consoleOutput ) {
    this.consoleOutput = consoleOutput;
  }

  public int getNetworkIndex( ) {
    return networkIndex;
  }

  public void setNetworkIndex( int networkIndex ) {
    this.networkIndex = networkIndex;
  }

  @Override
  public String toString( ) {
    return String.format(
                          "VmInstance [imageInfo=%s, instanceId=%s, keyInfo=%s, launchIndex=%s, launchTime=%s, networkConfig=%s, networkIndex=%s, networks=%s, ownerId=%s, placement=%s, privateNetwork=%s, reason=%s, reservationId=%s, state=%s, stopWatch=%s, userData=%s, vmTypeInfo=%s, volumes=%s]",
                          this.imageInfo, this.instanceId, this.keyInfo, this.launchIndex, this.launchTime,
                          this.networkConfig, this.networkIndex, this.networks, this.ownerId, this.placement,
                          this.privateNetwork, this.reason, this.reservationId, this.state, this.stopWatch,
                          this.userData, this.vmTypeInfo, this.volumes );
  }

  public void setServiceTag( String serviceTag ) {
    this.serviceTag = serviceTag;
  }

  public String getServiceTag( ) {
    return serviceTag;
  }

  public boolean hasPublicAddress() {
    NetworkConfigType conf = getNetworkConfig( );
    return conf != null && !( DEFAULT_IP.equals(conf.getIgnoredPublicIp( )) || conf.getIpAddress( ).equals( conf.getIgnoredPublicIp( ) ) );
  }
}
