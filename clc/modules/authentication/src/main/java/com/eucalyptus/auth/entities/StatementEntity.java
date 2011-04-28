package com.eucalyptus.auth.entities;

import java.io.Serializable;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import org.hibernate.annotations.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.entities.AbstractPersistent;

/**
 * Database statement entity.
 * 
 * Each policy statement is decomposed into a list of authorizations and a list of
 * conditions. Each authorization contains one action and one resource pattern.
 * Each condition contains one condition type and one key. The purpose of this
 * decomposition is to index the statements easily, to make lookup faster and to
 * match the staged processing of requests.
 * 
 * For example, the following statement
 * {
 *   "Effect":"Allow",
 *   "Action":["RunInstance", "DescribeImages"],
 *   "Resource":["emi-12345678", "emi-ABCDEFGH"],
 *   "Condition":{
 *     "IpAddress":{
 *       "aws:SourceIp":"10.0.0.0/24",
 *     }
 *     "DateLessThanEquals":[
 *       {
 *         "aws:CurrentTime":"2010-11-01T12:00:00Z",
 *       },
 *       {
 *         "aws:EpochTime":"1284562853",
 *       },
 *     ]
 *   }
 * }
 * is decomposed into a list of authorizations:
 * 
 * Effect  Action         ResourceType   ResourcePattern
 * -----------------------------------------------------
 * Allow   RunInstance    Image          emi-12345678
 * Allow   RunInstance    Image          emi-ABCDEFGH
 * Allow   DescribeImages Image          emi-12345678
 * Allow   DescribeImages Image          emi-ABCDEFGH
 * 
 * and a list of conditions:
 * 
 * Type                 Key              Value
 * -----------------------------------------------------
 * IpAddress            aws:SourceIp     10.0.0.0/24
 * DateLessThanEquals   aws:CurrentTime  2010-11-01T12:00:00Z
 * DateLessThanEquals   aws:EpochTime    1284562853
 * 
 * When each authorization is evaluated, the corresponding list of conditions
 * are also evaluated.
 * 
 * @author wenye
 *
 */
@Entity @javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_statement" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class StatementEntity extends AbstractPersistent implements Serializable {

  @Transient
  private static final long serialVersionUID = 1L;
  
  // Statement ID
  @Column( name = "auth_statement_sid" )
  String sid;
  
  // List of decomposed authorizations
  @OneToMany( cascade = { CascadeType.ALL }, mappedBy = "statement" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  List<AuthorizationEntity> authorizations;
  
  // List of decomposed conditions
  @OneToMany( cascade = { CascadeType.ALL }, mappedBy = "statement" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  List<ConditionEntity> conditions;
  
  // The owning policy
  @ManyToOne
  @JoinColumn( name = "auth_statement_owning_policy" )
  PolicyEntity policy;
  
  public StatementEntity( ) {
  }
  
  public StatementEntity( String sid ) {
    this.sid = sid;
  }
  
  public String getSid( ) {
    return this.sid;
  }
  
  public void setSid( String sid ) {
    this.sid = sid;
  }
  
  public List<AuthorizationEntity> getAuthorizations( ) {
    return this.authorizations;
  }
  
  public void setAuthorizations( List<AuthorizationEntity> authorizations ) {
    this.authorizations = authorizations;
  }
  
  public List<ConditionEntity> getConditions( ) {
    return this.conditions;
  }
  
  public void setConditions( List<ConditionEntity> conditions ) {
    this.conditions = conditions;
  }
  
  public PolicyEntity getPolicy( ) {
    return this.policy;
  }
  
  public void setPolicy( PolicyEntity policy ) {
    this.policy = policy;
  }
  
  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "ID=" ).append( this.getId( ) ).append( ", " );
    sb.append( "sid=" ).append( this.getSid( ) );
    return sb.toString( );
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
  public String getStatementId( ) {
    return this.getId( );
  }

}
