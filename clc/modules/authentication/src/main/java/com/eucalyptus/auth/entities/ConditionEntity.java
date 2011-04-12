package com.eucalyptus.auth.entities;

import java.io.Serializable;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import org.hibernate.annotations.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.entities.AbstractPersistent;
import com.google.common.collect.Sets;

/**
 * Database condition entity. A single row of condition table represents a decomposed
 * condition from the owning statement. Each condition contains only one condition type
 * and one condition key.
 * 
 * @author wenye
 *
 */
@Entity @javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_condition" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class ConditionEntity extends AbstractPersistent implements Serializable {

  @Transient
  private static final long serialVersionUID = 1L;

  // The condition type
  @Column( name = "auth_condition_type" )
  String type;
  
  // The key
  @Column( name = "auth_condition_key" )
  String key;
  
  // The values to be compared with the key
  @ElementCollection
  @CollectionTable( name = "auth_condition_value_list" )
  @Column( name = "auth_condition_values" )
  Set<String> values;
  
  // The owning statement
  @ManyToOne
  @JoinColumn( name = "auth_condition_owning_statement" )
  StatementEntity statement;
  
  public ConditionEntity( ) {
    this.values = Sets.newHashSet( );
  }

  public ConditionEntity( String type, String key, Set<String> values ) {
    this.type = type;
    this.key = key;
    this.values = values;
  }
  
  public static ConditionEntity newInstanceWithId( final String id ) {
    ConditionEntity c = new ConditionEntity( );
    c.setId( id );
    return c;
  }

  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "Condition(" );
    sb.append( "ID=" ).append( this.getId( ) ).append( ", " );
    sb.append( "type=" ).append( this.getType( ) ).append( ", " );
    sb.append( "key=" ).append( this.getKey( ) );
    sb.append( ")" );
    return sb.toString( );
  }
  
  public String getType( ) {
    return this.type;
  }

  public void setType( String type ) {
    this.type = type;
  }
  
  public String getKey( ) {
    return this.key;
  }

  public void setKey( String key ) {
    this.key = key;
  }
  
  public Set<String> getValues( ) {
    return this.values;
  }
  
  public StatementEntity getStatement( ) {
    return this.statement;
  }
  
  public void setStatement( StatementEntity statement ) {
    this.statement = statement;
  }

  /**
   * NOTE:IMPORTANT: this method has default visibility (rather than public) only for the sake of
   * supporting currently hand-coded proxy classes. Don't share this value with the user.
   *
   * TODO: remove this if possible.
   * 
   * @return
   * @see {@link AbstractPersistent#getId()}
   */
  public String getConditionId( ) {
    return this.getId( );
  }

}
