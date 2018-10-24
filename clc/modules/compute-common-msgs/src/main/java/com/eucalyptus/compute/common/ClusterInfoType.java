/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import com.eucalyptus.util.CompatFunction;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


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
    return new CompatFunction<ClusterInfoType, String>( ) {
      @Override
      public String apply( final ClusterInfoType clusterInfoType ) {
        return clusterInfoType.getZoneName( );
      }
    };
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
