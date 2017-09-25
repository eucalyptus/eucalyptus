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

import java.util.ArrayList;
import com.google.common.base.Joiner;

public class VmDescribeResponseType extends CloudClusterMessage {

  private String originCluster;
  private ArrayList<VmInfo> vms = new ArrayList<VmInfo>( );

  public String toString( ) {
    return this.getClass( ).getSimpleName( ) + " " + Joiner.on( "\n" + this.getClass( ).getSimpleName( ) + " " ).join( vms.iterator( ) );
  }

  public String getOriginCluster( ) {
    return originCluster;
  }

  public void setOriginCluster( String originCluster ) {
    this.originCluster = originCluster;
  }

  public ArrayList<VmInfo> getVms( ) {
    return vms;
  }

  public void setVms( ArrayList<VmInfo> vms ) {
    this.vms = vms;
  }
}
