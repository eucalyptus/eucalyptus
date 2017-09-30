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
import java.util.Collection;
import java.util.Collections;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ReservationInfoType extends EucalyptusData {

  private String reservationId;
  private String ownerId;
  private ArrayList<GroupItemType> groupSet = Lists.newArrayList( );
  private ArrayList<RunningInstancesItemType> instancesSet = Lists.newArrayList( );

  public ReservationInfoType( String reservationId, String ownerId, Collection<GroupItemType> groupIdsToNames ) {
    this.reservationId = reservationId;
    this.ownerId = ownerId;
    this.groupSet.addAll( groupIdsToNames );
    Collections.sort( this.groupSet );
  }

  public ReservationInfoType( ) {
  }

  public String getReservationId( ) {
    return reservationId;
  }

  public void setReservationId( String reservationId ) {
    this.reservationId = reservationId;
  }

  public String getOwnerId( ) {
    return ownerId;
  }

  public void setOwnerId( String ownerId ) {
    this.ownerId = ownerId;
  }

  public ArrayList<GroupItemType> getGroupSet( ) {
    return groupSet;
  }

  public void setGroupSet( ArrayList<GroupItemType> groupSet ) {
    this.groupSet = groupSet;
  }

  public ArrayList<RunningInstancesItemType> getInstancesSet( ) {
    return instancesSet;
  }

  public void setInstancesSet( ArrayList<RunningInstancesItemType> instancesSet ) {
    this.instancesSet = instancesSet;
  }
}
