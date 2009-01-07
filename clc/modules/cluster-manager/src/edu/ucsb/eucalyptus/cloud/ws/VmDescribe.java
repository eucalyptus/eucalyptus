/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.ws;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.cluster.*;
import edu.ucsb.eucalyptus.cloud.entities.*;
import edu.ucsb.eucalyptus.msgs.*;

import java.util.*;

public class VmDescribe extends DescribeInstancesType implements Cloneable {

  private boolean isAdmin = false;
  private List<ReservationInfoType> reservations;

  public VmDescribe( DescribeInstancesType msg )
  {
    this.setCorrelationId( msg.getCorrelationId() );
    this.setEffectiveUserId( msg.getEffectiveUserId() );
    this.setUserId( msg.getUserId() );
    this.setInstancesSet( msg.getInstancesSet() );
    this.reservations = new ArrayList<ReservationInfoType>();
  }

  public void transform() throws EucalyptusInvalidRequestException
  {
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    UserInfo user = null;
    try
    {
      user = db.getUnique( new UserInfo( this.getUserId() ) );
      this.isAdmin = user.isAdministrator();
    }
    catch ( EucalyptusCloudException e )
    {
      db.rollback();
      throw new EucalyptusInvalidRequestException( e );
    }
    db.commit();
    if ( this.getInstancesSet() == null )
      this.setInstancesSet( new ArrayList<String>() );
  }

  public void apply() throws EucalyptusCloudException
  {
    for ( Reservation r : Reservations.getInstance().listValues() )
    {
      if( r.isEmpty() ) continue;
      if ( this.isAdmin || r.getOwnerId().equals( this.getUserId() ) )
        if ( this.getInstancesSet().isEmpty() )
          this.reservations.add( r.getAsReservationInfoType() );
        else if ( !this.getInstancesSet().isEmpty() )
        {
          ReservationInfoType rsvInfo = r.getAsReservationInfoType();
          ArrayList<RunningInstancesItemType> instList = new ArrayList<RunningInstancesItemType>();
          for( RunningInstancesItemType runInst : rsvInfo.getInstancesSet() )
          {
            if( this.getInstancesSet().contains( runInst.getInstanceId() ) )
              instList.add( runInst );
          }
          if( instList.isEmpty() ) continue;
          rsvInfo.setInstancesSet( instList );
          this.reservations.add( rsvInfo );
        }
    }
  }

  public void rollback()
  {

  }

  public DescribeInstancesResponseType getResult()
  {
    DescribeInstancesResponseType reply = (DescribeInstancesResponseType)this.getReply();
    reply.getReservationSet().addAll( this.reservations );
    return reply;
  }
}
