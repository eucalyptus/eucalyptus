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
package com.eucalyptus.cloudformation.resources.standard.info;

import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.annotations.AttributeJson;
import com.google.common.base.MoreObjects;

public class AWSEC2VolumeResourceInfo extends ResourceInfo {

  @AttributeJson
  private String snapshotIdForDelete;

  public AWSEC2VolumeResourceInfo( ) {
    setType( "AWS::EC2::Volume" );
  }

  @Override
  public boolean supportsSnapshot( ) {
    return true;
  }

  @Override
  public boolean supportsTags( ) {
    return true;
  }

  public String getSnapshotIdForDelete( ) {
    return snapshotIdForDelete;
  }

  public void setSnapshotIdForDelete( String snapshotIdForDelete ) {
    this.snapshotIdForDelete = snapshotIdForDelete;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "snapshotIdForDelete", snapshotIdForDelete )
        .toString( );
  }
}
