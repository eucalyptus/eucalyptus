/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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

import static com.eucalyptus.compute.common.CloudMetadata.InternetGatewayMetadata;
import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.auth.principal.OwnerFullName;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_internet_gateways", indexes = {
    @Index( name = "metadata_internet_gateways_account_id_idx", columnList = "metadata_account_id" ),
    @Index( name = "metadata_internet_gateways_display_name_idx", columnList = "metadata_display_name" ),
} )
public class InternetGateway extends AbstractOwnedPersistent implements InternetGatewayMetadata {

  private static final long serialVersionUID = 1L;

  protected InternetGateway( ) {
  }

  protected InternetGateway( final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
  }

  public static InternetGateway create( final OwnerFullName owner,
                                        final String name ) {
    return new InternetGateway( owner, name );
  }

  public static InternetGateway exampleWithOwner( final OwnerFullName owner ) {
    return new InternetGateway( owner, null );
  }

  public static InternetGateway exampleWithName( final OwnerFullName owner, final String name ) {
    return new InternetGateway( owner, name );
  }

  @ManyToOne
  @JoinColumn( name = "metadata_vpc_id" )
  private Vpc vpc;

  @OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "internetGateway" )
  private Collection<InternetGatewayTag> tags;

  public Vpc getVpc() {
    return vpc;
  }

  public void setVpc( final Vpc vpc ) {
    this.vpc = vpc;
  }
}
