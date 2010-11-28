package com.eucalyptus.auth.entities;

import java.io.Serializable;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.auth.principal.Condition;
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
@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_condition" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class ConditionEntity extends AbstractPersistent implements Condition, Serializable {

  @Transient
  private static final long serialVersionUID = 1L;

  @Transient
  private static Logger LOG = Logger.getLogger( ConditionEntity.class );
  
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
  
  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "Condition(" );
    sb.append( "ID=" ).append( this.getId( ) ).append( ", " );
    sb.append( "type=" ).append( this.getType( ) ).append( ", " );
    sb.append( "key=" ).append( this.getKey( ) ).append( ", " );
    sb.append( "values=" ).append( this.getValues( ) );
    sb.append( ")" );
    return sb.toString( );
  }
  
  @Override
  public String getType( ) {
    return this.type;
  }

  @Override
  public String getKey( ) {
    return this.key;
  }

  @Override
  public Set<String> getValues( ) {
    return this.values;
  }
  
  public void setStatement( StatementEntity statement ) {
    this.statement = statement;
  }
  
}
