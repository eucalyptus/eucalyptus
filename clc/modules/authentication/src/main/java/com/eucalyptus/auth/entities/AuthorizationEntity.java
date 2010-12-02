package com.eucalyptus.auth.entities;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Condition;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.entities.AbstractPersistent;

/**
 * Database authorization entity. A single row of authorization table represents a decomposed
 * unit of the  policy statement. Each authorization contains only one action and one resource
 * pattern. And conditions are not included in the authorization record.
 * 
 * @author wenye
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_auth" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class AuthorizationEntity extends AbstractPersistent implements Authorization, Serializable {

  @Transient
  private static final long serialVersionUID = 1L;

  @Transient
  private static Logger LOG = Logger.getLogger( AuthorizationEntity.class );

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
  
  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "Authorization(" );
    sb.append( "ID=" ).append( this.getId( ) ).append( ", " );
    sb.append( "effect=" ).append( this.effect ).append( ", " );
    sb.append( "type=" ).append( this.type ).append( ", " );
    sb.append( "notAction=" ).append( this.isNotAction( ) ).append( ", " );
    sb.append( "actions=" ).append( this.getActions( ) ).append( ", " );
    sb.append( "notResource=" ).append( this.isNotResource( ) ).append( ", " );
    sb.append( "resources=" ).append( this.getResources( ) );
    sb.append( ")" );
    return sb.toString( );
  }
  
  @Override
  public EffectType getEffect( ) {
    return this.effect;
  }

  @Override
  public List<? extends Condition> getConditions( ) {
    return this.statement.getConditions( );
  }
  
  public void setStatement( StatementEntity statement ) {
    this.statement = statement;
  }

  @Override
  public Boolean isNotAction( ) {
    return this.notAction;
  }

  @Override
  public Boolean isNotResource( ) {
    return this.notResource;
  }

  @Override
  public String getType( ) {
    return this.type;
  }

  @Override
  public Set<String> getActions( ) {
    return this.actions;
  }

  @Override
  public Set<String> getResources( ) {
    return this.resources;
  }

  @Override
  public Group getGroup( ) {
    return this.statement.policy.group;
  }
  
}
