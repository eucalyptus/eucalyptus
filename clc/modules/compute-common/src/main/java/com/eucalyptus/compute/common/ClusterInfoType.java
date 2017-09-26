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
import com.eucalyptus.util.CompatFunction;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

/** *******************************************************************************/
public class ClusterInfoType extends EucalyptusData {

  private String zoneName;
  private String zoneState;
  private String regionName;
  private ArrayList<String> messageSet = new ArrayList<String>( );

  public ClusterInfoType( ) {
  }

  public ClusterInfoType( String zoneName, String zoneState ) {
    this( zoneName, zoneState, "" );
  }

  public ClusterInfoType( String zoneName, String zoneState, String regionName ) {
    this.zoneName = zoneName;
    this.zoneState = zoneState;
    this.regionName = regionName;
  }

  public static CompatFunction<ClusterInfoType, String> zoneName( ) {
    return ClusterInfoType::getZoneName;
  }

  public String getZoneName( ) {
    return zoneName;
  }

  public void setZoneName( String zoneName ) {
    this.zoneName = zoneName;
  }

  public String getZoneState( ) {
    return zoneState;
  }

  public void setZoneState( String zoneState ) {
    this.zoneState = zoneState;
  }

  public String getRegionName( ) {
    return regionName;
  }

  public void setRegionName( String regionName ) {
    this.regionName = regionName;
  }

  public ArrayList<String> getMessageSet( ) {
    return messageSet;
  }

  public void setMessageSet( ArrayList<String> messageSet ) {
    this.messageSet = messageSet;
  }
}
