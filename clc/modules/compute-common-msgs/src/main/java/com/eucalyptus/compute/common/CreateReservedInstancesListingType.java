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

public class CreateReservedInstancesListingType extends ReservedInstanceMessage {

  private String reservedInstancesId;
  private Integer instanceCount;
  @HttpEmbedded( multiple = true )
  private ArrayList<PriceScheduleRequestSetItemType> priceSchedules;
  private String clientToken;

  public String getReservedInstancesId( ) {
    return reservedInstancesId;
  }

  public void setReservedInstancesId( String reservedInstancesId ) {
    this.reservedInstancesId = reservedInstancesId;
  }

  public Integer getInstanceCount( ) {
    return instanceCount;
  }

  public void setInstanceCount( Integer instanceCount ) {
    this.instanceCount = instanceCount;
  }

  public ArrayList<PriceScheduleRequestSetItemType> getPriceSchedules( ) {
    return priceSchedules;
  }

  public void setPriceSchedules( ArrayList<PriceScheduleRequestSetItemType> priceSchedules ) {
    this.priceSchedules = priceSchedules;
  }

  public String getClientToken( ) {
    return clientToken;
  }

  public void setClientToken( String clientToken ) {
    this.clientToken = clientToken;
  }
}
