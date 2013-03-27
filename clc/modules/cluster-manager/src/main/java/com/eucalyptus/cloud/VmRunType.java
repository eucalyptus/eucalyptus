/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.cloud;

import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.bouncycastle.util.encoders.Base64;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.network.NetworkGroup;
import edu.ucsb.eucalyptus.cloud.VmKeyInfo;
import edu.ucsb.eucalyptus.msgs.CloudClusterMessage;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

public class VmRunType extends CloudClusterMessage {
  private static final long serialVersionUID = 1L;
  
  public static class Builder {
    private final VmRunType buildit = new VmRunType( );
    
    Builder( ) {

    }
    
    public VmRunType.Builder vmTypeInfo( final VmTypeInfo vmTypeInfo ) {
      this.buildit.setVmTypeInfo( vmTypeInfo );
      return this;
    }
    
    public VmRunType.Builder keyInfo( final VmKeyInfo keyInfo ) {
      this.buildit.setKeyInfo( keyInfo );
      return this;
    }
    
    public VmRunType.Builder vlan( final Integer vlan ) {
      this.buildit.setVlan( vlan );
      return this;
    }
    
    public VmRunType.Builder networkIndex( Long networkIndex ) {
      this.buildit.setNetworkIndex( networkIndex );
      return this;
    }
    
    public VmRunType.Builder launchIndex( final Integer launchIndex ) {
      this.buildit.setLaunchIndex( launchIndex );
      return this;
    }
    
    public Builder reservationId( final String rsvId ) {
      this.buildit.setReservationId( rsvId );
      return this;
    }
    
    public VmRunType.Builder platform( final String platform ) {
      this.buildit.setPlatform( platform );
      return this;
    }
    
    public VmRunType.Builder userData( final String userData ) {
      this.buildit.setUserData( userData );
      return this;
    }
    
    public VmRunType.Builder networkNames( List<NetworkGroup> list ) {
      for ( NetworkGroup g : list ) {
        this.buildit.getNetworkNames( ).add( g.getClusterNetworkName( ) );
      }
      return this;
    }
    
    public VmRunType.Builder naturalId( final String naturalId ) {
      this.buildit.setUuid( naturalId );
      return this;
    }
    
    public VmRunType.Builder instanceId( final String instanceId ) {
      this.buildit.setInstanceId( instanceId );
      this.buildit.setMacAddress( String.format( "d0:0d:%s:%s:%s:%s", instanceId.substring( 2, 4 ), instanceId.substring( 4, 6 ), instanceId.substring( 6, 8 ),
                                                 instanceId.substring( 8, 10 ) ) );
      return this;
    }
    
    public VmRunType create( ) {
      /** GRZE:NOTE: Nullables: userData, keyInfo **/
      checkParam( this.buildit.getInstanceId(), notNullValue() );
      checkParam( this.buildit.getLaunchIndex(), notNullValue() );
      checkParam( this.buildit.getMacAddress(), notNullValue() );
      checkParam( this.buildit.getNetworkNames().isEmpty(), is( false ) );
      checkParam( this.buildit.getNetworkIndex(), notNullValue() );
      checkParam( this.buildit.getPlatform(), notNullValue() );
      checkParam( this.buildit.getReservationId(), notNullValue() );
      checkParam( this.buildit.getUuid(), notNullValue() );
      checkParam( this.buildit.getVlan(), notNullValue() );
      checkParam( this.buildit.getVmTypeInfo(), notNullValue() );
      return this.buildit;
    }
    
    public VmRunType.Builder owner( UserFullName ownerFullName ) {
      this.buildit.setUserId( ownerFullName.getUserId( ) );
      this.buildit.setOwnerId( ownerFullName.getUserId( ) );
      this.buildit.setAccountId( ownerFullName.getAccountNumber( ) );
      return this;
    }
    
  }
  
  public static Builder builder( ) {
    return new Builder( );
  }
  
  /** these are for more convenient binding later on but really should be done differently... sigh **/
  
  private String       reservationId;
  @Nullable
  private String       userData;
  private String       platform;
  private Integer      maxCount     = 1;
  private Integer      minCount     = 1;
  private Integer      vlan;
  private Integer      launchIndex;
  private VmTypeInfo   vmTypeInfo;
  @Nullable
  private VmKeyInfo    keyInfo;
  private String       instanceId;
  private String       ownerId;
  private String       accountId;
  private String       uuid;
  private String       macAddress;
  private List<String> networkNames = new ArrayList<String>( );
  private Long         networkIndex;
  
  VmRunType( ) {}
  
  private VmRunType( final String reservationId, final String userData, final int amount,
                     final VmTypeInfo vmTypeInfo, final VmKeyInfo keyInfo, final String platform,
                     final List<String> instanceIds,
                     final int vlan, final List<String> networkNames, final List<String> networkIndexList, final List<String> uuids ) {
    this.reservationId = reservationId;
    this.userData = userData;
    this.vlan = vlan;
    this.vmTypeInfo = vmTypeInfo;
    this.keyInfo = keyInfo;
    this.networkNames = networkNames;
    this.platform = platform;
  }
  
  @Override
  public String toString( ) {
    return String.format(
                          "VmRunType [instanceIds=%s, keyInfo=%s, launchIndex=%s, amount=%s, networkIndexList=%s, networkNames=%s, reservationId=%s, userData=%s, vlan=%s, vmTypeInfo=%s]",
                          this.instanceId, this.keyInfo, this.launchIndex,
                          this.minCount, this.networkIndex, this.networkNames, this.reservationId,
                          this.userData, this.vlan, this.vmTypeInfo );
  }
  
  @Override
  public String toSimpleString( ) {
    return String.format( "%s %s networkIndex=%s vlan=%s", super.toSimpleString( ), this.instanceId, this.networkIndex, this.vlan );
  }

  void setReservationId( final String reservationId ) {
    this.reservationId = reservationId;
  }
  
  void setUserData( final String userData ) {
    if ( userData == null ) {
      this.userData = new String( Base64.encode( new byte[] {} ) );
    } else {
      this.userData = userData;
    }
  }
  
  void setPlatform( final String platform ) {
    this.platform = platform;
  }
  
  void setVlan( final int vlan ) {
    this.vlan = vlan;
  }
  
  void setLaunchIndex( final int launchIndex ) {
    this.launchIndex = launchIndex;
  }
  
  void setVmTypeInfo( final VmTypeInfo vmTypeInfo ) {
    this.vmTypeInfo = vmTypeInfo;
  }
  
  void setKeyInfo( final VmKeyInfo keyInfo ) {
    this.keyInfo = keyInfo;
  }
  
  void setNetworkNames( final List<String> networkNames ) {
    this.networkNames = networkNames;
  }
  
  void setInstanceId( final String instanceId ) {
    this.instanceId = instanceId;
  }
  
  public static long getSerialversionuid( ) {
    return serialVersionUID;
  }
  
  public String getReservationId( ) {
    return this.reservationId;
  }
  
  public String getUserData( ) {
    return this.userData;
  }
  
  public String getPlatform( ) {
    return this.platform;
  }
  
  public Integer getAmount( ) {
    return this.minCount;
  }
  
  public Integer getVlan( ) {
    return this.vlan;
  }
  
  public int getLaunchIndex( ) {
    return this.launchIndex;
  }
  
  public VmTypeInfo getVmTypeInfo( ) {
    return this.vmTypeInfo;
  }
  
  public VmKeyInfo getKeyInfo( ) {
    return this.keyInfo;
  }
  
  public String getInstanceId( ) {
    return this.instanceId;
  }
  
  public List<String> getNetworkNames( ) {
    return this.networkNames;
  }
  
  public Long getNetworkIndex( ) {
    return this.networkIndex;
  }
  
  void setVlan( final Integer vlan ) {
    this.vlan = vlan;
  }
  
  void setLaunchIndex( final Integer launchIndex ) {
    this.launchIndex = launchIndex;
  }
  
  void setUuid( final String uuid ) {
    this.uuid = uuid;
  }
  
  void setMacAddress( final String macAddress ) {
    this.macAddress = macAddress;
  }
  
  public String getUuid( ) {
    return this.uuid;
  }
  
  public String getMacAddress( ) {
    return this.macAddress;
  }
  
  void setNetworkIndex( final Long networkIndex ) {
    this.networkIndex = networkIndex;
  }
  
  public String getOwnerId( ) {
    return this.ownerId;
  }
  
  public void setOwnerId( String ownerId ) {
    this.ownerId = ownerId;
  }
  
  public String getAccountId( ) {
    return this.accountId;
  }
  
  public void setAccountId( String accountId ) {
    this.accountId = accountId;
  }
}
