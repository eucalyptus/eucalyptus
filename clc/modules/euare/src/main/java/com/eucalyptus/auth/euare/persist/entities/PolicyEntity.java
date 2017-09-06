/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.euare.persist.entities;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Type;
import com.eucalyptus.entities.AbstractPersistent;

/**
 * Database policy entity.
 */
@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_policy", indexes = {
    @Index( name = "auth_policy_owning_group_idx", columnList = "auth_policy_owning_group" ),
    @Index( name = "auth_policy_owning_role_idx", columnList = "auth_policy_owning_role" )
} )
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
  @Type(type="text")
  String text;
  
  // The owning group
  @ManyToOne( fetch = FetchType.LAZY  )
  @JoinColumn( name = "auth_policy_owning_group" )
  GroupEntity group;

  // The owning role
  @ManyToOne( fetch = FetchType.LAZY )
  @JoinColumn( name = "auth_policy_owning_role" )
  RoleEntity role;

  // The role owning this assume role policy
  @OneToOne( fetch = FetchType.LAZY, mappedBy = "assumeRolePolicy" )
  RoleEntity assumeRole;

  public PolicyEntity( ) {
  }
  
  public PolicyEntity( String name ) {
    this.name = name;
  }

  public PolicyEntity( String name, String version, String text ) {
    this( name );
    this.policyVersion = version;
    this.text = text;
  }

  public static PolicyEntity create( final String name,
                                     final String policyVersion,
                                     final String text ) {
    return new PolicyEntity( name, policyVersion, text );
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
  
  public GroupEntity getGroup( ) {
    return this.group;
  }
  
  public void setGroup( GroupEntity group ) {
    this.group = group;
  }

  public RoleEntity getRole() {
    return role;
  }

  public void setRole( RoleEntity role ) {
    this.role = role;
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
