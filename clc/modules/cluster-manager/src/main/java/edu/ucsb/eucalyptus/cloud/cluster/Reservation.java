/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 *
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */

package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.msgs.ReservationInfoType;
import org.apache.log4j.Logger;

import com.eucalyptus.util.HasName;

import java.util.*;
import java.util.zip.Adler32;

public class Reservation implements HasName {
  private static Logger LOG = Logger.getLogger( Reservation.class );

  private String ownerId;
  private String reservationId;
  private Long rsvId;
  private int minCount;
  private int maxCount;
  private int launchIndexBase = 0;
  private List<String> vmInstances;

  public Reservation( String ownerId, String reservationId)
  {
    this(ownerId,0l,0,0,0);
    this.reservationId = reservationId;
  }

  public Reservation( String ownerId, Long reservationId, int minCount, int maxCount, int baseMac )
  {
    this.ownerId = ownerId;
    this.rsvId = reservationId;

    //:: DOCUMENT: reservation ID creation:://
    Adler32 hash = new Adler32();
    hash.reset();
    hash.update( ( ownerId + Long.toHexString( reservationId ) ).getBytes() );
    this.reservationId = String.format( "r-%08X", hash.getValue() );

    this.minCount = minCount;
    this.maxCount = maxCount;
    this.vmInstances = new ArrayList<String>();
  }

  public String getName()
  {
    return this.getReservationId();
  }

  public boolean isEmpty()
  {
    return this.vmInstances.isEmpty();
  }

  public boolean contains( String instanceId )
  {
    return this.vmInstances.contains( instanceId );
  }

  public boolean removeInstance( String instanceId )
  {
    return this.vmInstances.remove( instanceId );
  }

  public int addInstance( String vmId )
  {
    this.vmInstances.add( vmId );
    return this.launchIndexBase++;
  }

  public int getMaxCount()
  {
    return maxCount;
  }

  public int getMinCount()
  {
    return minCount;
  }

  public String getOwnerId()
  {
    return ownerId;
  }

  public String getReservationId()
  {
    return reservationId;
  }

  public Long getRsvId()
  {
    return rsvId;
  }

  public ReservationInfoType getAsReservationInfoType()
  {
    ReservationInfoType rsvInfo = new ReservationInfoType();
    for( String vmId : this.vmInstances )
    {
      VmInstance vm = VmInstances.getInstance().lookup( vmId );
      rsvInfo.getInstancesSet().add( vm.getAsRunningInstanceItemType() );
    }
    rsvInfo.setOwnerId( this.getOwnerId() );
    rsvInfo.setReservationId( this.getReservationId() );
    return rsvInfo;
  }

  public int compareTo( final Object o )
  {
    Reservation that = (Reservation) o;
    return this.getRsvId().compareTo( that.getRsvId() );
  }


}
