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

import java.util.ArrayList;
import com.google.common.collect.Lists;

public class DescribeSnapshotAttributeResponseType extends BlockSnapshotMessage {

  private String snapshotId;
  private ArrayList<CreateVolumePermissionItemType> createVolumePermission = Lists.newArrayList( );
  private ArrayList<String> productCodes = Lists.newArrayList( );

  public boolean hasCreateVolumePermissions( ) {
    return this.createVolumePermission != null && !this.createVolumePermission.isEmpty( );
  }

  public boolean hasProductCodes( ) {
    return this.productCodes != null && !this.productCodes.isEmpty( );
  }

  public String getSnapshotId( ) {
    return snapshotId;
  }

  public void setSnapshotId( String snapshotId ) {
    this.snapshotId = snapshotId;
  }

  public ArrayList<CreateVolumePermissionItemType> getCreateVolumePermission( ) {
    return createVolumePermission;
  }

  public void setCreateVolumePermission( ArrayList<CreateVolumePermissionItemType> createVolumePermission ) {
    this.createVolumePermission = createVolumePermission;
  }

  public ArrayList<String> getProductCodes( ) {
    return productCodes;
  }

  public void setProductCodes( ArrayList<String> productCodes ) {
    this.productCodes = productCodes;
  }
}
