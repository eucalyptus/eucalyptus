package com.eucalyptus.net.util;

import com.google.common.collect.Lists;

public class ClusterAddressInfo {
  int orphanCount;
  String address;
  String instanceIp;
  
  public ClusterAddressInfo( String address ) {
    this.address = address;
  }

  public static List<ClusterAddressInfo> fromLists( List<String> addresses, List<String> instanceIps ) {
    return addresses.collect{ new ClusterAddressInfo( it ) }.eachWithIndex{ it, i -> it.instanceIp = instanceIps[ i ] }
  }

  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.address == null ) ? 0 : this.address.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this.is( obj ) ) return true;
    if ( obj == null ) return false;
    if ( !getClass( ).equals( obj.getClass( ) ) ) return false;
    ClusterAddressInfo other = ( ClusterAddressInfo ) obj;
    if ( this.address == null ) {
      if ( other.address != null ) return false;
    } else if ( !this.address.equals( other.address ) ) return false;
    return true;
  }  
  
}
