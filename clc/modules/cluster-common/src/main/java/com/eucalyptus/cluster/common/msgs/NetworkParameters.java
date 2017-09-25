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
package com.eucalyptus.cluster.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class NetworkParameters extends EucalyptusData {

  private String privateMacAddress;
  private String publicMacAddress;
  private int macLimit;
  private int vlan;

  public String getPrivateMacAddress( ) {
    return privateMacAddress;
  }

  public void setPrivateMacAddress( String privateMacAddress ) {
    this.privateMacAddress = privateMacAddress;
  }

  public String getPublicMacAddress( ) {
    return publicMacAddress;
  }

  public void setPublicMacAddress( String publicMacAddress ) {
    this.publicMacAddress = publicMacAddress;
  }

  public int getMacLimit( ) {
    return macLimit;
  }

  public void setMacLimit( int macLimit ) {
    this.macLimit = macLimit;
  }

  public int getVlan( ) {
    return vlan;
  }

  public void setVlan( int vlan ) {
    this.vlan = vlan;
  }
}
