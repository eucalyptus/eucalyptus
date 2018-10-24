/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class FpgaImage extends EucalyptusData {

  private java.util.Date createTime;
  private String description;
  private String fpgaImageGlobalId;
  private String fpgaImageId;
  private String name;
  private String ownerAlias;
  private String ownerId;
  private PciId pciId;
  private ProductCodeList productCodes;
  private Boolean isPublic;
  private String shellVersion;
  private FpgaImageState state;
  private ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>( );
  private java.util.Date updateTime;

  public java.util.Date getCreateTime( ) {
    return createTime;
  }

  public void setCreateTime( final java.util.Date createTime ) {
    this.createTime = createTime;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( final String description ) {
    this.description = description;
  }

  public String getFpgaImageGlobalId( ) {
    return fpgaImageGlobalId;
  }

  public void setFpgaImageGlobalId( final String fpgaImageGlobalId ) {
    this.fpgaImageGlobalId = fpgaImageGlobalId;
  }

  public String getFpgaImageId( ) {
    return fpgaImageId;
  }

  public void setFpgaImageId( final String fpgaImageId ) {
    this.fpgaImageId = fpgaImageId;
  }

  public String getName( ) {
    return name;
  }

  public void setName( final String name ) {
    this.name = name;
  }

  public String getOwnerAlias( ) {
    return ownerAlias;
  }

  public void setOwnerAlias( final String ownerAlias ) {
    this.ownerAlias = ownerAlias;
  }

  public String getOwnerId( ) {
    return ownerId;
  }

  public void setOwnerId( final String ownerId ) {
    this.ownerId = ownerId;
  }

  public PciId getPciId( ) {
    return pciId;
  }

  public void setPciId( final PciId pciId ) {
    this.pciId = pciId;
  }

  public ProductCodeList getProductCodes( ) {
    return productCodes;
  }

  public void setProductCodes( final ProductCodeList productCodes ) {
    this.productCodes = productCodes;
  }

  public Boolean getPublic( ) {
    return isPublic;
  }

  public void setPublic( final Boolean isPublic ) {
    this.isPublic = isPublic;
  }

  public String getShellVersion( ) {
    return shellVersion;
  }

  public void setShellVersion( final String shellVersion ) {
    this.shellVersion = shellVersion;
  }

  public FpgaImageState getState( ) {
    return state;
  }

  public void setState( final FpgaImageState state ) {
    this.state = state;
  }

  public ArrayList<ResourceTag> getTagSet( ) {
    return tagSet;
  }

  public void setTagSet( ArrayList<ResourceTag> tagSet ) {
    this.tagSet = tagSet;
  }

  public java.util.Date getUpdateTime( ) {
    return updateTime;
  }

  public void setUpdateTime( final java.util.Date updateTime ) {
    this.updateTime = updateTime;
  }

}
