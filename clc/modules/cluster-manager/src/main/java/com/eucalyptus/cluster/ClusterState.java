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
 ************************************************************************/

package com.eucalyptus.cluster;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import org.apache.log4j.Logger;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.address.AddressingConfiguration;
import com.eucalyptus.cluster.callback.UnassignAddressCallback;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.async.AsyncRequests;
import edu.ucsb.eucalyptus.msgs.ClusterAddressInfo;

public class ClusterState {
  private static Logger                                       LOG                   = Logger.getLogger( ClusterState.class );
  private final String                                        clusterName;
  private Integer                                             addressCapacity;
  private Boolean                                             publicAddressing      = false;
  private Boolean                                             addressingInitialized = false;
  
  ClusterState( String clusterName ) {
    this.clusterName = clusterName;
  }
    
  public Boolean hasPublicAddressing( ) {
    return this.publicAddressing;
  }
  
  public Boolean isAddressingInitialized( ) {
    return this.addressingInitialized;
  }
  
  public void setAddressingInitialized( Boolean addressingInitialized ) {
    this.addressingInitialized = addressingInitialized;
  }
  
  public void setPublicAddressing( Boolean publicAddressing ) {
    this.publicAddressing = publicAddressing;
  }
  
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    
    ClusterState cluster = ( ClusterState ) o;
    
    if ( !this.getClusterName( ).equals( cluster.getClusterName( ) ) ) return false;
    
    return true;
  }
  
  @Override
  public int hashCode( ) {
    return this.getClusterName( ).hashCode( );
  }
  
  public String getClusterName( ) {
    return clusterName;
  }
  
  public Integer getAddressCapacity( ) {
    return addressCapacity;
  }
  
  public void setAddressCapacity( Integer addressCapacity ) {
    this.addressCapacity = addressCapacity;
  }
  
  @Override
  public String toString( ) {
    return String.format( "ClusterState [addressCapacity=%s, clusterName=%s]", this.addressCapacity, this.clusterName );
  }
  
}
