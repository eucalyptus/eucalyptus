/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.constants.*;
import edu.ucsb.eucalyptus.msgs.*;
import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;

import java.util.*;

public class VmInstance implements HasName {

  private static Logger LOG = Logger.getLogger( VmInstance.class );

  public static String DEFAULT_IP = "0.0.0.0";

  //:: parent ref :://
  private String reservationId;
  //:: unique index in reservation :://
  private int launchIndex;
  //:: my id :://
  private String instanceId;
  //:: owner :://
  private String ownerId;
  private String placement;

  private Date launchTime;
  private StopWatch stopWatch;

  private String userData;
  private String reason;
  private VmImageInfo imageInfo;
  private VmKeyInfo keyInfo;
  private VmTypeInfo vmTypeInfo;
  private List<Network> networks = new ArrayList<Network>();
  private VmState state;
  private StringBuffer consoleOutput;
  private List<AttachedVolume> volumes = new ArrayList<AttachedVolume>();
  private NetworkConfigType networkConfig;

  public VmInstance() {
    this.launchTime = new Date();
    this.state = VmState.PENDING;
    this.networkConfig = new NetworkConfigType();
    this.stopWatch = new StopWatch();
    this.stopWatch.start();
    this.consoleOutput = new StringBuffer();
    this.volumes = new ArrayList<AttachedVolume>();
  }

  public VmInstance( final String reservationId, final int launchIndex, final String instanceId, final String ownerId, final String placement, final String userData,
                     final VmImageInfo imageInfo, final VmKeyInfo keyInfo, final VmTypeInfo vmTypeInfo, final List<Network> networks ) {
    this();

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

    String mac = String.format( "%s:%s:%s:%s",
                                this.instanceId.substring( 2, 4 ),
                                this.instanceId.substring( 4, 6 ),
                                this.instanceId.substring( 6, 8 ),
                                this.instanceId.substring( 8, 10 ) );
    this.networkConfig.setMacAddress( "d0:0d:" + mac );
    this.networkConfig.setIgnoredMacAddress( "d0:0f:" + mac );
    this.networkConfig.setIpAddress( DEFAULT_IP );
    this.networkConfig.setIgnoredPublicIp( DEFAULT_IP );
  }

  public String getName() {
    return this.instanceId;
  }

  public boolean equals( Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    VmInstance vmInstance = ( VmInstance ) o;

    if ( !instanceId.equals( vmInstance.instanceId ) ) return false;

    return true;
  }

  public void setLaunchTime( final Date launchTime ) {
    this.launchTime = launchTime;
  }

  public int hashCode() {
    return instanceId.hashCode();
  }

  public String getReservationId() {
    return reservationId;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public int getLaunchIndex() {
    return launchIndex;
  }

  public String getPlacement() {
    return placement;
  }

  public Date getLaunchTime() {
    return launchTime;
  }

  public VmState getState() {
    return state;
  }

  public void setState( final VmState state ) {
    LOG.info( String.format( "%s state change: %s -> %s", this.getInstanceId(), this.getState(), state ) );
    this.state = state;
  }

  public String getUserData() {
    return userData;
  }

  public void setUserData( final String userData ) {
    this.userData = userData;
  }

  public RunningInstancesItemType getAsRunningInstanceItemType() {
    RunningInstancesItemType runningInstance = new RunningInstancesItemType();

    runningInstance.setAmiLaunchIndex( Integer.toString( this.launchIndex ) );
    runningInstance.setStateCode( Integer.toString( this.state.getCode() ) );
    runningInstance.setStateName( this.state.getName() );

    runningInstance.setInstanceId( this.instanceId );
    runningInstance.setImageId( this.imageInfo.getImageId() );
    runningInstance.setKernel( this.imageInfo.getKernelId() );
    runningInstance.setRamdisk( this.imageInfo.getRamdiskId() );

    runningInstance.setPrivateDnsName( this.getNetworkConfig().getIpAddress() );
    runningInstance.setDnsName( this.getNetworkConfig().getIgnoredPublicIp() );

    if ( this.getReason() != null || !"".equals( this.getReason() ) )
      runningInstance.setReason( this.getReason() );

    if ( this.getKeyInfo() != null )
      runningInstance.setKeyName( this.getKeyInfo().getName() );
    else
      runningInstance.setKeyName( "" );


    runningInstance.setInstanceType( this.getVmTypeInfo().getName() );
    runningInstance.setPlacement( this.placement );

    runningInstance.setLaunchTime( this.launchTime );

    return runningInstance;
  }

  @Override
  public String toString() {
    return "VmInstance{" +
           "reservationId='" + reservationId + '\'' +
           ", ownerId='" + ownerId + '\'' +
           ", instanceId='" + instanceId + '\'' +
           ", launchIndex=" + launchIndex +
           ", launchTime=" + launchTime +
           ", state=" + state +
           ", split=" + this.getSplitTime() / 1000f +
           ", reason='" + reason + '\'' +
           ", placement='" + placement + '\'' +
           ", networks=" + this.getNetworkNames() +
           "\n" + instanceId + ".networkConfig=" + networkConfig +
           "\n" + instanceId + ".imageInfo=" + imageInfo +
           "\n" + instanceId + ".vmTypeInfo=" + vmTypeInfo +
           "\n" + instanceId + ".keyInfo=" + keyInfo +
           '}';
  }

  public VmKeyInfo getKeyInfo() {
    return keyInfo;
  }

  public void setKeyInfo( final VmKeyInfo keyInfo ) {
    this.keyInfo = keyInfo;
  }

  public VmTypeInfo getVmTypeInfo() {
    return vmTypeInfo;
  }

  public void setVmTypeInfo( final VmTypeInfo vmTypeInfo ) {
    this.vmTypeInfo = vmTypeInfo;
  }

  public List<Network> getNetworks() {
    return networks;
  }

  public List<String> getNetworkNames() {
    List<String> nets = new ArrayList<String>();
    for ( Network net : this.getNetworks() )
      nets.add( net.getNetworkName() );
    return nets;
  }

  public void setNetworks( final List<Network> networks ) {
    this.networks = networks;
  }

  public NetworkConfigType getNetworkConfig() {
    return networkConfig;
  }

  public void setNetworkConfig( final NetworkConfigType networkConfig ) {
    this.networkConfig = networkConfig;
  }

  public VmImageInfo getImageInfo() {
    return imageInfo;
  }

  public void setImageInfo( final VmImageInfo imageInfo ) {
    this.imageInfo = imageInfo;
  }

  public List<AttachedVolume> getVolumes() {
    return volumes;
  }

  public void setVolumes( final List<AttachedVolume> volumes ) {
    this.volumes = volumes;
  }

  public String getByKey( String path ) {


    Map<String, String> m = new HashMap<String, String>();
    m.put( "ami-id", this.getImageInfo().getImageId() );
    m.put( "ami-launch-index", "" + this.getLaunchIndex() );
    m.put( "ami-manifest-path", this.getImageInfo().getImageLocation() );
    m.put( "hostname", this.getNetworkConfig().getIgnoredPublicIp() );
    m.put( "instance-id", this.getInstanceId() );
    m.put( "instance-type", this.getVmTypeInfo().getName() );
    m.put( "local-hostname", this.getNetworkConfig().getIpAddress() );
    m.put( "local-ipv4", this.getNetworkConfig().getIpAddress() );
    m.put( "public-hostname", this.getNetworkConfig().getIgnoredPublicIp() );
    m.put( "public-ipv4", this.getNetworkConfig().getIgnoredPublicIp() );
    m.put( "public-keys/", "0=" + this.getKeyInfo().getName() );
    m.put( "placement/", "availability-zone" );
    m.put( "reservation-id", this.getReservationId() );
    m.put( "ancestor-ami-ids", "none" );
    m.put( "kernel-id", this.getImageInfo().getKernelId() );
    m.put( "ramdisk-id", this.getImageInfo().getRamdiskId() );
    m.put( "security-groups", this.getNetworkNames().toString() );
    m.put( "block-device-mapping", "not yet supported." );
    m.put( "product-codes", "not yet supported." );

    if( path == null ) path = "";
    String dir = "";
    for( String entry : m.keySet() ) dir += entry + "\n";
    m.put("",dir);

    m.put( "public-keys/0", "openssh-key" );
    m.put( "public-keys/0/", "openssh-key" );
    m.put( "placement/availability-zone", this.getPlacement() );
    m.put( "public-keys/0/openssh-key", this.getKeyInfo().getValue() );
    m.put( "public-keys", "0=" + this.getKeyInfo().getName() );
    m.put( "placement", "availability-zone" );

    LOG.debug("Servicing metadata request:" + path + " -> " + m.get(path) );
    return m.get(path);
  }

  public int compareTo( final Object o ) {
    VmInstance that = ( VmInstance ) o;
    return this.getName().compareTo( that.getName() );
  }

  public synchronized long resetStopWatch() {
    this.stopWatch.stop();
    long ret = this.stopWatch.getTime();
    this.stopWatch.reset();
    this.stopWatch.start();
    return ret;
  }

  public synchronized long getSplitTime() {
    this.stopWatch.split();
    long ret = this.stopWatch.getSplitTime();
    this.stopWatch.unsplit();
    return ret;
  }

  public String getReason() {
    return reason;
  }

  public void setReason( final String reason ) {
    this.reason = reason;
  }

  public StringBuffer getConsoleOutput() {
    return consoleOutput;
  }

  public void setConsoleOutput( final StringBuffer consoleOutput ) {
    this.consoleOutput = consoleOutput;
  }
}
