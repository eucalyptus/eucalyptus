/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import com.eucalyptus.util.CompatFunction;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class VpcClassicLinkType extends EucalyptusData implements ResourceTagged {

  private String vpcId;
  private Boolean classicLinkEnabled;
  private ResourceTagSetType tagSet;

  public VpcClassicLinkType( ) {
  }

  public VpcClassicLinkType( final String vpcId ) {
    this.vpcId = vpcId;
    this.classicLinkEnabled = false;
  }

  public static CompatFunction<VpcClassicLinkType, String> id( ) {
    return new CompatFunction<VpcClassicLinkType, String>( ) {
      @Override
      public String apply( final VpcClassicLinkType vpcType ) {
        return vpcType.getVpcId( );
      }
    };
  }

  public String getVpcId( ) {
    return vpcId;
  }

  public void setVpcId( String vpcId ) {
    this.vpcId = vpcId;
  }

  public Boolean getClassicLinkEnabled() {
    return classicLinkEnabled;
  }

  public void setClassicLinkEnabled(Boolean classicLinkEnabled) {
    this.classicLinkEnabled = classicLinkEnabled;
  }

  public ResourceTagSetType getTagSet( ) {
    return tagSet;
  }

  public void setTagSet( ResourceTagSetType tagSet ) {
    this.tagSet = tagSet;
  }
}
