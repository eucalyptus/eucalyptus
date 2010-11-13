package com.eucalyptus.auth.entities;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Column;
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

  // The action pattern of the authorization
  @Column( name = "auth_auth_action_pattern" )
  String actionPattern;

  // The resource type of the authorization
  @Column( name = "auth_auth_resource_type" )
  String resourceType;
  
  // If the resource pattern is negated, i.e. NotResource
  @Column( name = "auth_auth_negative" )
  Boolean negative;
  
  // The resource pattern
  @Column( name = "auth_auth_resource_pattern" )
  String resourcePattern;

  // The owning statement
  @ManyToOne
  @JoinColumn( name = "auth_auth_owning_statement" )
  StatementEntity statement;
  
  public AuthorizationEntity( ) {
  }

  public AuthorizationEntity( EffectType effect, String actionPattern, String resourceType, String resourcePattern, Boolean negative ) {
    this.effect = effect;
    this.actionPattern = actionPattern;
    this.resourceType = resourceType;
    this.resourcePattern = resourcePattern;
    this.negative = negative;
  }
  
  public AuthorizationEntity( String resourceType ) {
    this.resourceType = resourceType;
  }
  
  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "Authorization(" );
    sb.append( "ID=" ).append( this.getId( ) ).append( ", " );
    sb.append( "actionPattern=" ).append( this.actionPattern ).append( ", " );
    sb.append( "effect=" ).append( this.effect ).append( ", " );
    sb.append( "resourceType=" ).append( this.resourceType ).append( ", " );
    sb.append( "resourcePattern=" ).append( this.resourcePattern ).append( ", " );
    sb.append( "negative=" ).append( this.negative );
    sb.append( ")" );
    return sb.toString( );
  }
  
  @Override
  public EffectType getEffect( ) {
    return this.effect;
  }

  @Override
  public String getActionPattern( ) {
    return this.actionPattern;
  }

  @Override
  public String getResourceType( ) {
    return this.resourceType;
  }

  @Override
  public String getResourcePattern( ) {
    return this.resourcePattern;
  }

  @Override
  public Boolean isNegative( ) {
    return this.negative;
  }
  
  @Override
  public List<? extends Condition> getConditions( ) {
    return this.statement.getConditions( );
  }
  
  public void setStatement( StatementEntity statement ) {
    this.statement = statement;
  }
  
}
