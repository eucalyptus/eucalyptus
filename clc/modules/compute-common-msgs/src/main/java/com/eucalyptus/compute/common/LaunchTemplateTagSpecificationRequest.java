/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class LaunchTemplateTagSpecificationRequest extends EucalyptusData {

  private String resourceType;
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
