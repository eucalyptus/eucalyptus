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
        Principals.systemAccount( ).getAccountAlias( ) :
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
