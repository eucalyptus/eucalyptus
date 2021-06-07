/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class LaunchTemplateTagSpecificationRequest extends EucalyptusData {

  private String resourceType;

  @HttpParameterMapping( parameter = "Tag" )
  @HttpEmbedded( multiple = true )
  private ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>( );

  public String getResourceType( ) {
    return resourceType;
  }

  public void setResourceType( final String resourceType ) {
    this.resourceType = resourceType;
  }

  public ArrayList<ResourceTag> getTagSet( ) {
    return tagSet;
  }

  public void setTagSet( ArrayList<ResourceTag> tagSet ) {
    this.tagSet = tagSet;
  }

}
