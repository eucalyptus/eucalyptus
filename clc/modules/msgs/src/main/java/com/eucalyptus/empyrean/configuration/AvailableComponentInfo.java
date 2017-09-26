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
package com.eucalyptus.empyrean.configuration;

import java.util.ArrayList;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class AvailableComponentInfo extends EucalyptusData {

  /**
   * Info about component
   */
  private String componentName;
  private String componentCapitalizedName;
  private String description;
  private Boolean hasCredentials;
  /**
   * Info about its registration requirements
   */
  private Boolean registerable;
  private Boolean requiresName;
  private Boolean partitioned;
  private Boolean publicApiService;
  /**
   * Info about service groups
   */
  private ArrayList<String> serviceGroups = new ArrayList<String>( );
  private ArrayList<String> serviceGroupMembers = new ArrayList<String>( );

  public String getComponentName( ) {
    return componentName;
  }

  public void setComponentName( String componentName ) {
    this.componentName = componentName;
  }

  public String getComponentCapitalizedName( ) {
    return componentCapitalizedName;
  }

  public void setComponentCapitalizedName( String componentCapitalizedName ) {
    this.componentCapitalizedName = componentCapitalizedName;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public Boolean getHasCredentials( ) {
    return hasCredentials;
  }

  public void setHasCredentials( Boolean hasCredentials ) {
    this.hasCredentials = hasCredentials;
  }

  public Boolean getRegisterable( ) {
    return registerable;
  }

  public void setRegisterable( Boolean registerable ) {
    this.registerable = registerable;
  }

  public Boolean getRequiresName( ) {
    return requiresName;
  }

  public void setRequiresName( Boolean requiresName ) {
    this.requiresName = requiresName;
  }

  public Boolean getPartitioned( ) {
    return partitioned;
  }

  public void setPartitioned( Boolean partitioned ) {
    this.partitioned = partitioned;
  }

  public Boolean getPublicApiService( ) {
    return publicApiService;
  }

  public void setPublicApiService( Boolean publicApiService ) {
    this.publicApiService = publicApiService;
  }

  public ArrayList<String> getServiceGroups( ) {
    return serviceGroups;
  }

  public void setServiceGroups( ArrayList<String> serviceGroups ) {
    this.serviceGroups = serviceGroups;
  }

  public ArrayList<String> getServiceGroupMembers( ) {
    return serviceGroupMembers;
  }

  public void setServiceGroupMembers( ArrayList<String> serviceGroupMembers ) {
    this.serviceGroupMembers = serviceGroupMembers;
  }
}
