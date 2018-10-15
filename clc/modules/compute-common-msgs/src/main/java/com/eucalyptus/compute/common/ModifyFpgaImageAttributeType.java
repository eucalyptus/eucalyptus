/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import javax.annotation.Nonnull;


public class ModifyFpgaImageAttributeType extends ComputeMessage {

  private String attribute;
  private String description;
  @Nonnull
  private String fpgaImageId;
  private LoadPermissionModifications loadPermission;
  private String name;
  private String operationType;
  private ProductCodeStringList productCodes;
  private UserGroupStringList userGroups;
  private UserIdStringList userIds;

  public String getAttribute( ) {
    return attribute;
  }

  public void setAttribute( final String attribute ) {
    this.attribute = attribute;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( final String description ) {
    this.description = description;
  }

  public String getFpgaImageId( ) {
    return fpgaImageId;
  }

  public void setFpgaImageId( final String fpgaImageId ) {
    this.fpgaImageId = fpgaImageId;
  }

  public LoadPermissionModifications getLoadPermission( ) {
    return loadPermission;
  }

  public void setLoadPermission( final LoadPermissionModifications loadPermission ) {
    this.loadPermission = loadPermission;
  }

  public String getName( ) {
    return name;
  }

  public void setName( final String name ) {
    this.name = name;
  }

  public String getOperationType( ) {
    return operationType;
  }

  public void setOperationType( final String operationType ) {
    this.operationType = operationType;
  }

  public ProductCodeStringList getProductCodes( ) {
    return productCodes;
  }

  public void setProductCodes( final ProductCodeStringList productCodes ) {
    this.productCodes = productCodes;
  }

  public UserGroupStringList getUserGroups( ) {
    return userGroups;
  }

  public void setUserGroups( final UserGroupStringList userGroups ) {
    this.userGroups = userGroups;
  }

  public UserIdStringList getUserIds( ) {
    return userIds;
  }

  public void setUserIds( final UserIdStringList userIds ) {
    this.userIds = userIds;
  }

}
