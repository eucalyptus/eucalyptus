/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.compute.common.internal.account;

import static org.hamcrest.core.IsNull.notNullValue;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.compute.common.internal.blockstorage.Snapshot;
import com.eucalyptus.compute.common.internal.blockstorage.Volume;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.util.Parameters;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_identity_id_format", indexes = {
    @Index( name = "metadata_identity_id_format_acc_name_idx", columnList = "metadata_account_number,metadata_id_name" )
} )
public class IdentityIdFormat extends AbstractPersistent {
  private static final long serialVersionUID = 1L;

  public enum IdType{ account, user, role }

  public enum IdResource{
    instance( VmInstance.ID_PREFIX ),
    reservation( "r" ),
    snapshot( Snapshot.ID_PREFIX ),
    volume( Volume.ID_PREFIX ),
    ;

    private final String prefix;

    IdResource( final String prefix ) {
      this.prefix = prefix;
    }

    public String prefix( ) {
      return prefix;
    }
  }

  @Column( name = "metadata_account_number", nullable = false, updatable = false )
  private String accountNumber;

  @Column( name = "metadata_id_type", nullable = false, updatable = false )
  @Enumerated( EnumType.STRING )
  private IdType identityType;

  @Column( name = "metadata_id_name", updatable = false )
  private String identityFullName;

  @Column( name = "metadata_resource", nullable = false, updatable = false )
  @Enumerated( EnumType.STRING )
  private IdResource resource;

  @Column( name = "metadata_use_long_id", nullable = false, updatable = true )
  private Boolean useLongIdentifiers;

  protected IdentityIdFormat( ) {
  }

  /**
   * Create an identity id format for the given type/name.
   */
  public static IdentityIdFormat create(
      final String accountNumber,
      final IdType type,
      final String fullName,
      final IdResource resource,
      final boolean useLongIdentifiers ) {
    Parameters.checkParam( "accountNumber", accountNumber, notNullValue( ) );
    Parameters.checkParam( "type", type, notNullValue( ) );
    Parameters.checkParam( "fullName", fullName, notNullValue( ) );
    Parameters.checkParam( "resource", resource, notNullValue( ) );
    final IdentityIdFormat format = new IdentityIdFormat( );
    format.setAccountNumber( accountNumber );
    format.setIdentityType( type );
    format.setIdentityFullName( fullName );
    format.setResource( resource );
    format.setUseLongIdentifiers( useLongIdentifiers );
    return format;
  }

  public String getAccountNumber( ) {
    return accountNumber;
  }

  public void setAccountNumber( final String accountNumber ) {
    this.accountNumber = accountNumber;
  }

  public IdType getIdentityType( ) {
    return identityType;
  }

  public void setIdentityType( final IdType identityType ) {
    this.identityType = identityType;
  }

  public String getIdentityFullName( ) {
    return identityFullName;
  }

  public void setIdentityFullName( final String identityFullName ) {
    this.identityFullName = identityFullName;
  }

  public IdResource getResource( ) {
    return resource;
  }

  public void setResource( final IdResource resource ) {
    this.resource = resource;
  }

  public Boolean getUseLongIdentifiers( ) {
    return useLongIdentifiers;
  }

  public void setUseLongIdentifiers( final Boolean useLongIdentifiers ) {
    this.useLongIdentifiers = useLongIdentifiers;
  }
}
