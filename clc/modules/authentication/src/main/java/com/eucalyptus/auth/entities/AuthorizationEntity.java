package com.eucalyptus.auth.entities;

import java.io.Serializable;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import org.hibernate.annotations.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.auth.principal.Authorization.EffectType;
import com.eucalyptus.entities.AbstractPersistent;

/**
 * Database authorization entity. A single row of authorization table represents a decomposed
 * unit of the  policy statement. Each authorization contains only one action and one resource
 * pattern. And conditions are not included in the authorization record.
 * 
 * @author wenye
 *
 */
@Entity @javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_auth" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class AuthorizationEntity extends AbstractPersistent implements Serializable {

  @Transient
  private static final long serialVersionUID = 1L;

  // The effect of the authorization
  @Enumerated( EnumType.STRING )
  @Column( name = "auth_auth_effect" )
  EffectType effect;

  // The type of resource this authorization applies to, used to restrict search.
  @Column( name = "auth_auth_type" )
  String type;
  
  // If action list is negated, i.e. NotAction
  @Column( name = "auth_auth_not_action" )
  Boolean notAction;
  
  @ElementCollection
  @CollectionTable( name = "auth_auth_action_list" )
  @Column( name = "auth_auth_actions" )
  Set<String> actions;
  
  // If resource list is negated, i.e. NotResource
  @Column( name = "auth_auth_not_resource" )
  Boolean notResource;
  
  @ElementCollection
  @CollectionTable( name = "auth_auth_resource_list" )
  @Column( name = "auth_auth_resources" )
  Set<String> resources;

  // The owning statement
  @ManyToOne
  @JoinColumn( name = "auth_auth_owning_statement" )
  StatementEntity statement;
  
  public AuthorizationEntity( ) {
  }

  public AuthorizationEntity( EffectType effect, String type, Set<String> actions, Boolean notAction, Set<String> resources, Boolean notResource ) {
    this.effect = effect;
    this.type = type;
    this.notAction = notAction;
    this.actions = actions;
    this.notResource = notResource;
    this.resources = resources;
  }
  
  public AuthorizationEntity( String type ) {
    this.type = type;
  }

  public static AuthorizationEntity newInstanceWithId( final String id ) {
    AuthorizationEntity a = new AuthorizationEntity( );
    a.setId( id );
    return a;
  }

  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "Authorization(" );
    sb.append( "ID=" ).append( this.getId( ) ).append( ", " );
    sb.append( "effect=" ).append( this.effect ).append( ", " );
    sb.append( "type=" ).append( this.type ).append( ", " );
    sb.append( "notAction=" ).append( this.isNotAction( ) ).append( ", " );
    sb.append( "notResource=" ).append( this.isNotResource( ) );
    sb.append( ")" );
    return sb.toString( );
  }
  
  public EffectType getEffect( ) {
    return this.effect;
  }

  public void setEffect( EffectType effect ) {
    this.effect = effect;
  }
  
  public StatementEntity getStatement( ) {
    return this.statement;
  }
  
  public void setStatement( StatementEntity statement ) {
    this.statement = statement;
  }

  public Boolean isNotAction( ) {
    return this.notAction;
  }

  public void setNotAction( Boolean notAction ) {
    this.notAction = notAction;
  }
  
  public Boolean isNotResource( ) {
    return this.notResource;
  }
  
  public void setNotResource( Boolean notResource ) {
    this.notResource = notResource;
  }

  public String getType( ) {
    return this.type;
  }

  public void setType( String type ) {
    this.type = type;
  }
  
  public Set<String> getActions( ) {
    return this.actions;
  }

  public Set<String> getResources( ) {
    return this.resources;
  }

  /**
   * NOTE:IMPORTANT: this method has public visibility (rather than public) only for the sake of
   * supporting currently hand-coded proxy classes. Don't share this value with the user.
   * 
   * TODO: remove this if possible.
   * 
   * @return
   * @see {@link AbstractPersistent#getId()}
   */
  public String getAuthorizationId( ) {
    return this.getId( );
  }
}
