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
package com.eucalyptus.compute.common;

public class DescribeVolumeAttributeResponseType extends BlockVolumeMessage {

  private String volumeId;
  private Boolean autoEnableIO;
  private Boolean productCodes;

  public boolean hasAutoEnableIO( ) {
    return this.autoEnableIO != null;
  }

  public boolean hasProductCodes( ) {
    return this.productCodes != null;
  }

  public String getVolumeId( ) {
    return volumeId;
  }

  public void setVolumeId( String volumeId ) {
    this.volumeId = volumeId;
  }

  public Boolean getAutoEnableIO( ) {
    return autoEnableIO;
  }

  public void setAutoEnableIO( Boolean autoEnableIO ) {
    this.autoEnableIO = autoEnableIO;
  }

  public Boolean getProductCodes( ) {
    return productCodes;
  }

  public void setProductCodes( Boolean productCodes ) {
    this.productCodes = productCodes;
  }
}
