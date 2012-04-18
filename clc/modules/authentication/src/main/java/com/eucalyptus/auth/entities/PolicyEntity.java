package com.eucalyptus.auth.entities;

import java.io.Serializable;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import org.hibernate.annotations.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.entities.AbstractPersistent;

/**
 * Database policy entity.
 * 
 * @author wenye
 *
 */
@Entity @javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_policy" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class PolicyEntity extends AbstractPersistent implements Serializable {

  @Transient
  private static final long serialVersionUID = 1L;
  
  // The policy name
  @Column( name = "auth_policy_name" )
  String name;
  
  @Column( name = "auth_policy_version" )
  String policyVersion;
  
  // The original policy text in JSON
  @Column( name = "auth_policy_text" )
  @Lob
  String text;
  
  // The set of statements of this policy
  @OneToMany( cascade = { CascadeType.ALL }, mappedBy = "policy" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  List<StatementEntity> statements;
  
  // The owning group
  @ManyToOne
  @JoinColumn( name = "auth_policy_owning_group" )
  GroupEntity group;
  
  public PolicyEntity( ) {
  }
  
  public PolicyEntity( String name ) {
    this.name = name;
  }

  public PolicyEntity( String version, String text, List<StatementEntity> statements ) {
    this.policyVersion = version;
    this.text = text;
    this.statements = statements;
  }

  public static PolicyEntity newInstanceWithId( final String id ) {
    PolicyEntity p = new PolicyEntity( );
    p.setId( id );
    return p;
  }
  
  public String getText( ) {
    return this.text;
  }

  public void setText( String text ) {
    this.text = text;
  }
  
  public String getName( ) {
    return this.name;
  }
  
  public void setName( String name ) {
    this.name = name;
  }
  
  public List<StatementEntity> getStatements( ) {
    return this.statements;
  }
  
  public GroupEntity getGroup( ) {
    return this.group;
  }
  
  public void setGroup( GroupEntity group ) {
    this.group = group;
  }

  public String getPolicyVersion( ) {
    return this.policyVersion;
  }

  public void setPolicyVersion( String policyVersion ) {
    this.policyVersion = policyVersion;
  }
  
  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "ID=" ).append( this.getId( ) ).append( ", " );
    sb.append( "name=" ).append( this.getName( ) );
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
  public String getPolicyId( ) {
    return this.getId( );
  }

  
}
