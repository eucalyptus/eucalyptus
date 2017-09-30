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

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import net.sf.json.JSONObject;

public class DiskImageVolumeDescription extends EucalyptusData {

  private Integer size;
  private String id;

  public DiskImageVolumeDescription( ) {
  }

  public DiskImageVolumeDescription( Integer size, String id ) {
    this.size = size;
    this.id = id;
  }

  public DiskImageVolumeDescription( JSONObject obj ) {
    if ( obj != null ) {
      size = obj.optInt( "size" );
      id = obj.optString( "id", null );
    }

  }

  public JSONObject toJSON( ) {
    JSONObject obj = new JSONObject( );
    obj.put( "size", size );
    obj.put( "id", id );
    return obj;
  }

  public Integer getSize( ) {
    return size;
  }

  public void setSize( Integer size ) {
    this.size = size;
  }

  public String getId( ) {
    return id;
  }

  public void setId( String id ) {
    this.id = id;
  }
}
