/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;



public class FpgaImageAttribute extends EucalyptusData {

  private String description;
  private String fpgaImageId;
  private LoadPermissionList loadPermissions;
  private String name;
  private ProductCodeList productCodes;

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

  public LoadPermissionList getLoadPermissions( ) {
    return loadPermissions;
  }

  public void setLoadPermissions( final LoadPermissionList loadPermissions ) {
    this.loadPermissions = loadPermissions;
  }

  public String getName( ) {
    return name;
  }

  public void setName( final String name ) {
    this.name = name;
  }

  public ProductCodeList getProductCodes( ) {
    return productCodes;
  }

  public void setProductCodes( final ProductCodeList productCodes ) {
    this.productCodes = productCodes;
  }

}
