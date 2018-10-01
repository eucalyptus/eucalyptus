/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common.internal.network;

import java.io.Serializable;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import org.hibernate.annotations.Parent;

@Embeddable
public class NetworkCidr implements Serializable {

  private static final long serialVersionUID = 1L;

  @Parent
  private NetworkRule networkRule;

  @Column( name = "ipranges" )
  private String cidrIp;

  @Column( name = "network_rule_cidr_description" )
  private String description;

  NetworkCidr( ) {
  }

  public NetworkCidr( final String cidrIp,
                      final String description ) {
    this( null, cidrIp, description );
  }

  public NetworkCidr( @Nullable final NetworkRule networkRule,
                      final String cidrIp,
                      final String description ) {
    this.networkRule = networkRule;
    this.cidrIp = cidrIp;
    this.description = description;
  }

  public static NetworkCidr create( final String cidrIp ) {
    return create( cidrIp, null );
  }

  public static NetworkCidr create( final String cidrIp,
                                    final String description ) {
    return new NetworkCidr( cidrIp, description );
  }

  public String getCidrIp( ) {
    return cidrIp;
  }

  public void setCidrIp( final String cidrIp ) {
    this.cidrIp = cidrIp;
  }


  @Nullable
  public String getDescription( ) {
    return description;
  }

  public void setDescription( final String description ) {
    this.description = description;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( ( o == null ) || !this.getClass( ).equals( o.getClass( ) ) ) return false;

    final NetworkCidr that = (NetworkCidr) o;

    if ( this.cidrIp != null && that.cidrIp != null )
      return this.cidrIp.equals( that.cidrIp );

    return true;
  }

  @Override
  public int hashCode( ) {
    int result = 7;
    if ( this.cidrIp != null ) {
      result = this.cidrIp.hashCode( );
    }
    return result;
  }

  @Override
  public String toString( ) {
    return String.format( "NetworkCidr:cidrIp=%s", this.cidrIp );
  }

  private NetworkRule getNetworkRule( ) {
    return this.networkRule;
  }

  private void setNetworkRule( final NetworkRule networkRule ) {
    this.networkRule = networkRule;
  }
}