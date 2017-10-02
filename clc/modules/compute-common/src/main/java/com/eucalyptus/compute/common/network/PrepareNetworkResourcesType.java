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
package com.eucalyptus.compute.common.network;

import java.util.ArrayList;
import com.google.common.collect.Lists;

public class PrepareNetworkResourcesType extends NetworkingMessage {

  private String availabilityZone;
  private String vpc;
  private String subnet;
  private ArrayList<NetworkResource> resources = Lists.newArrayList( );
  private ArrayList<NetworkFeature> features = Lists.newArrayList( );

  public PrepareNetworkResourcesType( ) {
  }

  public PrepareNetworkResourcesType(
      final String vpc,
      final String subnet,
      final ArrayList<NetworkResource> resources
  ) {
    this.vpc = vpc;
    this.subnet = subnet;
    this.resources = resources;
  }

  public String getAvailabilityZone( ) {
    return availabilityZone;
  }

  public void setAvailabilityZone( String availabilityZone ) {
    this.availabilityZone = availabilityZone;
  }

  public String getVpc( ) {
    return vpc;
  }

  public void setVpc( String vpc ) {
    this.vpc = vpc;
  }

  public String getSubnet( ) {
    return subnet;
  }

  public void setSubnet( String subnet ) {
    this.subnet = subnet;
  }

  public ArrayList<NetworkResource> getResources( ) {
    return resources;
  }

  public void setResources( ArrayList<NetworkResource> resources ) {
    this.resources = resources;
  }

  public ArrayList<NetworkFeature> getFeatures( ) {
    return features;
  }

  public void setFeatures( ArrayList<NetworkFeature> features ) {
    this.features = features;
  }
}
