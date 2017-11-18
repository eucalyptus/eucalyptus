/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE.
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
