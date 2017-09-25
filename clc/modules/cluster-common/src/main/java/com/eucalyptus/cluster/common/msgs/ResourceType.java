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

public class ResourceType extends EucalyptusData {

  private VmTypeInfo instanceType;
  private int maxInstances;
  private int availableInstances;

  public String toString( ) {
    return "ResourceType " + String.valueOf( instanceType ) + " " + String.valueOf( availableInstances ) + " / " + String.valueOf( maxInstances );
  }

  public VmTypeInfo getInstanceType( ) {
    return instanceType;
  }

  public void setInstanceType( VmTypeInfo instanceType ) {
    this.instanceType = instanceType;
  }

  public int getMaxInstances( ) {
    return maxInstances;
  }

  public void setMaxInstances( int maxInstances ) {
    this.maxInstances = maxInstances;
  }

  public int getAvailableInstances( ) {
    return availableInstances;
  }

  public void setAvailableInstances( int availableInstances ) {
    this.availableInstances = availableInstances;
  }
}
