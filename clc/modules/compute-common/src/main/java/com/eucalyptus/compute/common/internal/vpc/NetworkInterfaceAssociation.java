/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.compute.common.internal.vpc;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import com.eucalyptus.auth.principal.Principals;

/**
 *
 */
@Embeddable
public class NetworkInterfaceAssociation implements Serializable {

  private static final long serialVersionUID = 1L;

  protected NetworkInterfaceAssociation( ) {

  }

  protected NetworkInterfaceAssociation( final String associationId,
                                         final String allocationId,
                                         final String ipOwnerId,
                                         final String publicIp,
                                         final String publicDnsName ) {
    this.associationId = associationId;
    this.allocationId = allocationId;
    this.ipOwnerId = ipOwnerId;
    this.publicIp = publicIp;
    this.publicDnsName = publicDnsName;
  }

  public static NetworkInterfaceAssociation create( final String associationId,
                                                    final String allocationId,
                                                    final String ipOwnerId,
                                                    final String publicIp,
                                                    final String publicDnsName ) {
    return new NetworkInterfaceAssociation(
        associationId,
        allocationId,
        ipOwnerId,
        publicIp,
        publicDnsName
    );
  }

  @Column( name = "metadata_association_id" )
  private String associationId;

  @Column( name = "metadata_association_allocation_id" )
  private String allocationId;

  @Column( name = "metadata_association_ip_owner_id" )
  private String ipOwnerId;

  @Column( name = "metadata_association_address" )
  private String publicIp;

  @Column( name = "metadata_association_public_dns" )
  private String publicDnsName;

  public String getAssociationId( ) {
    return associationId;
  }

  public void setAssociationId( final String associationId ) {
    this.associationId = associationId;
  }

  public String getAllocationId( ) {
    return allocationId;
  }

  public void setAllocationId( final String allocationId ) {
    this.allocationId = allocationId;
  }

  public String getIpOwnerId( ) {
    return ipOwnerId;
  }

  public void setIpOwnerId( final String ipOwnerId ) {
    this.ipOwnerId = ipOwnerId;
  }

  public String getDisplayIpOwnerId( ) {
    return Principals.isFakeIdentityAccountNumber( ipOwnerId ) ?
        Principals.systemAccount( ).getName( ) :
        ipOwnerId;
  }

  public String getPublicIp( ) {
    return publicIp;
  }

  public void setPublicIp( final String publicIp ) {
    this.publicIp = publicIp;
  }

  public String getPublicDnsName( ) {
    return publicDnsName;
  }

  public void setPublicDnsName( final String publicDnsName ) {
    this.publicDnsName = publicDnsName;
  }
}
