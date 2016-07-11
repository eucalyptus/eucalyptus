/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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

  public enum IdResource{ instance, reservation, snapshot, volume }

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
