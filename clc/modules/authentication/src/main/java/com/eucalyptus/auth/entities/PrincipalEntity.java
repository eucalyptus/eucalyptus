/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.auth.entities;

import static com.eucalyptus.auth.principal.Principal.PrincipalType;
import java.io.Serializable;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import com.eucalyptus.entities.AbstractPersistent;

/**
 *
 */
@Entity
@javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_principal" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class PrincipalEntity extends AbstractPersistent implements Serializable {
  private static final long serialVersionUID = 1L;

  @Column( name = "auth_principal_type", nullable = false )
  @Enumerated( EnumType.STRING )
  private PrincipalType type;

  @ElementCollection
  @CollectionTable( name = "auth_principal_value_list" )
  @Column( name = "auth_principal_value" )
  private Set<String> values;

  // If principal list is negated, i.e. NotPrincipal
  @Column( name = "auth_principal_not_principal", nullable = false )
  private Boolean notPrincipal;

  // The owning statement
  @ManyToOne
  @JoinColumn( name = "auth_principal_owning_statement", nullable = false )
  private StatementEntity statement;

  public PrincipalEntity() {
  }

  public PrincipalEntity( @Nonnull final Boolean notPrincipal,
                          @Nonnull final PrincipalType type,
                          @Nonnull final Set<String> values ) {
    this.notPrincipal = notPrincipal;
    this.type = type;
    this.values = values;
  }

  public static PrincipalEntity newInstanceWithId( final String id ) {
    final PrincipalEntity principalEntity = new PrincipalEntity( );
    principalEntity.setId( id );
    return principalEntity;
  }

  public PrincipalType getType() {
    return type;
  }

  public void setType( final PrincipalType type ) {
    this.type = type;
  }

  public Set<String> getValues() {
    return values;
  }

  public void setValues( final Set<String> values ) {
    this.values = values;
  }

  public Boolean isNotPrincipal() {
    return notPrincipal;
  }

  public void setNotPrincipal( final Boolean notPrincipal ) {
    this.notPrincipal = notPrincipal;
  }

  public StatementEntity getStatement() {
    return statement;
  }

  public void setStatement( final StatementEntity statement ) {
    this.statement = statement;
  }

  /**
   * NOTE:IMPORTANT: this method has public visibility (rather than public) only for the sake of
   * supporting currently hand-coded proxy classes. Don't share this value with the user.
   *
   * TODO: remove this if possible.
   *
   * @see {@link AbstractPersistent#getId()}
   */
  public String getPrincipalId( ) {
    return this.getId( );
  }

  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "Principal(" );
    sb.append( "ID=" ).append( this.getId( ) ).append( ", " );
    sb.append( "type=" ).append( this.getType() ).append( ", " );
    sb.append( "notPrincipal=" ).append( this.isNotPrincipal() ).append( ", " );
    sb.append( "values=" ).append( this.getValues() );
    sb.append( ")" );
    return sb.toString( );
  }
}
