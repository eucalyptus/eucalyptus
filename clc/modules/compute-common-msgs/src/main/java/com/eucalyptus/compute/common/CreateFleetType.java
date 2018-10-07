/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import java.util.Date;
import javax.annotation.Nonnull;
import com.eucalyptus.binding.HttpEmbedded;


public class CreateFleetType extends ComputeMessage {

  private String clientToken;
  private String excessCapacityTerminationPolicy;
  @Nonnull
  private FleetLaunchTemplateConfigListRequest launchTemplateConfigs;
  private OnDemandOptionsRequest onDemandOptions;
  private Boolean replaceUnhealthyInstances;
  private SpotOptionsRequest spotOptions;
  @HttpEmbedded( multiple = true )
  private ArrayList<ResourceTagSpecification> tagSpecification = new ArrayList<ResourceTagSpecification>( );

  @Nonnull
  private TargetCapacitySpecificationRequest targetCapacitySpecification;
  private Boolean terminateInstancesWithExpiration;
  private String type;
  private Date validFrom;
  private Date validUntil;

  public String getClientToken( ) {
    return clientToken;
  }

  public void setClientToken( final String clientToken ) {
    this.clientToken = clientToken;
  }

  public String getExcessCapacityTerminationPolicy( ) {
    return excessCapacityTerminationPolicy;
  }

  public void setExcessCapacityTerminationPolicy( final String excessCapacityTerminationPolicy ) {
    this.excessCapacityTerminationPolicy = excessCapacityTerminationPolicy;
  }

  public FleetLaunchTemplateConfigListRequest getLaunchTemplateConfigs( ) {
    return launchTemplateConfigs;
  }

  public void setLaunchTemplateConfigs( final FleetLaunchTemplateConfigListRequest launchTemplateConfigs ) {
    this.launchTemplateConfigs = launchTemplateConfigs;
  }

  public OnDemandOptionsRequest getOnDemandOptions( ) {
    return onDemandOptions;
  }

  public void setOnDemandOptions( final OnDemandOptionsRequest onDemandOptions ) {
    this.onDemandOptions = onDemandOptions;
  }

  public Boolean getReplaceUnhealthyInstances( ) {
    return replaceUnhealthyInstances;
  }

  public void setReplaceUnhealthyInstances( final Boolean replaceUnhealthyInstances ) {
    this.replaceUnhealthyInstances = replaceUnhealthyInstances;
  }

  public SpotOptionsRequest getSpotOptions( ) {
    return spotOptions;
  }

  public void setSpotOptions( final SpotOptionsRequest spotOptions ) {
    this.spotOptions = spotOptions;
  }


  public ArrayList<ResourceTagSpecification> getTagSpecification( ) {
    return tagSpecification;
  }

  public void setTagSpecification( ArrayList<ResourceTagSpecification> tagSpecification ) {
    this.tagSpecification = tagSpecification;
  }

  public TargetCapacitySpecificationRequest getTargetCapacitySpecification( ) {
    return targetCapacitySpecification;
  }

  public void setTargetCapacitySpecification( final TargetCapacitySpecificationRequest targetCapacitySpecification ) {
    this.targetCapacitySpecification = targetCapacitySpecification;
  }

  public Boolean getTerminateInstancesWithExpiration( ) {
    return terminateInstancesWithExpiration;
  }

  public void setTerminateInstancesWithExpiration( final Boolean terminateInstancesWithExpiration ) {
    this.terminateInstancesWithExpiration = terminateInstancesWithExpiration;
  }

  public String getType( ) {
    return type;
  }

  public void setType( final String type ) {
    this.type = type;
  }

  public Date getValidFrom( ) {
    return validFrom;
  }

  public void setValidFrom( final Date validFrom ) {
    this.validFrom = validFrom;
  }

  public Date getValidUntil( ) {
    return validUntil;
  }

  public void setValidUntil( final Date validUntil ) {
    this.validUntil = validUntil;
  }

}
