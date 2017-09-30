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
import com.eucalyptus.binding.HttpEmbedded;

public class ModifyReservedInstancesType extends ReservedInstanceMessage {

  private ArrayList<String> reservedInstancesId;
  private String clientToken;
  @HttpEmbedded( multiple = true )
  private ArrayList<ReservedInstancesConfigurationSetItemType> reservedInstancesConfigurationSetItemType;

  public ArrayList<String> getReservedInstancesId( ) {
    return reservedInstancesId;
  }

  public void setReservedInstancesId( ArrayList<String> reservedInstancesId ) {
    this.reservedInstancesId = reservedInstancesId;
  }

  public String getClientToken( ) {
    return clientToken;
  }

  public void setClientToken( String clientToken ) {
    this.clientToken = clientToken;
  }

  public ArrayList<ReservedInstancesConfigurationSetItemType> getReservedInstancesConfigurationSetItemType( ) {
    return reservedInstancesConfigurationSetItemType;
  }

  public void setReservedInstancesConfigurationSetItemType( ArrayList<ReservedInstancesConfigurationSetItemType> reservedInstancesConfigurationSetItemType ) {
    this.reservedInstancesConfigurationSetItemType = reservedInstancesConfigurationSetItemType;
  }
}
