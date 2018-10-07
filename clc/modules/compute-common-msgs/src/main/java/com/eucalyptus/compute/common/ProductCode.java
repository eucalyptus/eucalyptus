/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ProductCode extends EucalyptusData {

  private String productCodeId;
  private String productCodeType;

  public String getProductCodeId( ) {
    return productCodeId;
  }

  public void setProductCodeId( final String productCodeId ) {
    this.productCodeId = productCodeId;
  }

  public String getProductCodeType( ) {
    return productCodeType;
  }

  public void setProductCodeType( final String productCodeType ) {
    this.productCodeType = productCodeType;
  }

}
