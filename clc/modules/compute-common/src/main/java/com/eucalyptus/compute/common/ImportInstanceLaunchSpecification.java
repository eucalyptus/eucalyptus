/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class ImportInstanceLaunchSpecification extends EucalyptusData {

  private String architecture;
  @HttpEmbedded( multiple = true )
  @HttpParameterMapping( parameter = "GroupName" )
  private ArrayList<String> groupName = new ArrayList<String>( );
  @HttpEmbedded
  private UserData userData;
  private String instanceType;
  private InstancePlacement placement;
  private MonitoringInstance monitoring;
  private String subnetId;
  private String instanceInitiatedShutdownBehavior;
  private String privateIpAddress;
  private String keyName;

  public String getArchitecture( ) {
    return architecture;
  }

  public void setArchitecture( String architecture ) {
    this.architecture = architecture;
  }

  public ArrayList<String> getGroupName( ) {
    return groupName;
  }

  public void setGroupName( ArrayList<String> groupName ) {
    this.groupName = groupName;
  }

  public UserData getUserData( ) {
    return userData;
  }

  public void setUserData( UserData userData ) {
    this.userData = userData;
  }

  public String getInstanceType( ) {
    return instanceType;
  }

  public void setInstanceType( String instanceType ) {
    this.instanceType = instanceType;
  }

  public InstancePlacement getPlacement( ) {
    return placement;
  }

  public void setPlacement( InstancePlacement placement ) {
    this.placement = placement;
  }

  public MonitoringInstance getMonitoring( ) {
    return monitoring;
  }

  public void setMonitoring( MonitoringInstance monitoring ) {
    this.monitoring = monitoring;
  }

  public String getSubnetId( ) {
    return subnetId;
  }

  public void setSubnetId( String subnetId ) {
    this.subnetId = subnetId;
  }

  public String getInstanceInitiatedShutdownBehavior( ) {
    return instanceInitiatedShutdownBehavior;
  }

  public void setInstanceInitiatedShutdownBehavior( String instanceInitiatedShutdownBehavior ) {
    this.instanceInitiatedShutdownBehavior = instanceInitiatedShutdownBehavior;
  }

  public String getPrivateIpAddress( ) {
    return privateIpAddress;
  }

  public void setPrivateIpAddress( String privateIpAddress ) {
    this.privateIpAddress = privateIpAddress;
  }

  public String getKeyName( ) {
    return keyName;
  }

  public void setKeyName( String keyName ) {
    this.keyName = keyName;
  }
}
