/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. userList OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.auth;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.BaseAuthorization;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.AbstractPersistent;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import edu.emory.mathcs.backport.java.util.Collections;

@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_groups" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class GroupEntity extends AbstractPersistent implements Group {
  @Transient
  private static Logger   LOG      = Logger.getLogger( GroupEntity.class );
  @Column( name = "auth_group_name", unique = true )
  String                  name;
  
  @ManyToMany( cascade = CascadeType.PERSIST )
  @JoinTable( name = "auth_group_has_userList", joinColumns = { @JoinColumn( name = "auth_group_id" ) }, inverseJoinColumns = @JoinColumn( name = "auth_user_id" ) )
  @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
  List<UserEntity>        userList = new ArrayList<UserEntity>( );
  
  @OneToMany( cascade = CascadeType.ALL )
  @JoinTable( name = "auth_group_has_authorization", joinColumns = { @JoinColumn( name = "auth_group_id" ) }, inverseJoinColumns = @JoinColumn( name = "auth_authorization_id" ) )
  @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
  List<BaseAuthorization> authList = new ArrayList<BaseAuthorization>( );
  
  @Transient
  String                  timestamp;
  
  public GroupEntity( ) {}
  
  public GroupEntity( final String name ) {
    this.name = name;
  }
  
  public GroupEntity( final String name, final String timestamp ) {
    this.name = name;
    this.timestamp = timestamp;
  }
  
  public String getName( ) {
    return name;
  }
  
  public void setName( final String name ) {
    this.name = name;
  }
  
  public List<BaseAuthorization> getAuthList( ) {
    return this.authList;
  }
  
  public void setAuthList( List<BaseAuthorization> authorizations ) {
    this.authList = authorizations;
  }
  
  public List<UserEntity> getUserList( ) {
    return userList;
  }
  
  public void setUserList( final List<UserEntity> userList ) {
    this.userList = userList;
  }
  
  public String getTimestamp( ) {
    return timestamp;
  }
  
  public void setTimestamp( String timestamp ) {
    this.timestamp = timestamp;
  }
  
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    
    GroupEntity that = ( GroupEntity ) o;
    
    if ( !name.equals( that.name ) ) return false;
    
    return true;
  }
  
  @Override
  public int hashCode( ) {
    return name.hashCode( );
  }
  
  @Override
  public boolean addAuthorization( Authorization auth ) {
    if ( auth instanceof BaseAuthorization ) {
      return this.authList.add( ( BaseAuthorization ) auth );
    } else {
      throw new RuntimeException( "EID: Authorizations must extend BaseAuthorization." );
    }
  }
  
  @Override
  public boolean removeAuthorization( Authorization auth ) {
    if ( auth instanceof BaseAuthorization ) {
      return this.authList.remove( ( BaseAuthorization ) auth );
    } else {
      throw new RuntimeException( "EID: Authorizations must extend BaseAuthorization." );
    }
  }
  
  @Override
  public boolean addMember( Principal user ) {
    if ( user instanceof UserEntity ) {
      return this.userList.add( ( UserEntity ) user );
    } else {
      LOG.debug( "EID: GroupEntity only supports users of type UserEntity" );
      return false;
    }
  }
  
  @Override
  public boolean isMember( Principal member ) {
    if ( member instanceof UserEntity ) {
      return this.userList.contains( ( UserEntity ) member );
    } else {
      LOG.debug( "EID: GroupEntity only supports users of type UserEntity" );
      return false;
    }
  }
  
  @Override
  public Enumeration<? extends Principal> members( ) {
    return Iterators.asEnumeration( this.userList.iterator( ) );
  }
  
  @Override
  public boolean removeMember( Principal user ) {
    if ( user instanceof UserEntity ) {
      return this.userList.remove( ( UserEntity ) user );
    } else {
      LOG.debug( "EID: GroupEntity only supports users of type UserEntity" );
      return false;
    }
  }
  
  @Override
  public ImmutableList<Authorization> getAuthorizations( ) {
    return ImmutableList.copyOf( ( List ) this.authList );
  }
  
  @Override
  public ImmutableList<User> getMembers( ) {
    return ImmutableList.copyOf( ( List ) this.userList );
  }
  
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "GroupEntity [ " );
    sb.append( "name = " ).append( name == null ? "null" : name ).append( ", " );
    sb.append( "userList = " );
    for ( UserEntity u : userList ) {
      sb.append( u.getName( ) ).append( ", " );
    }
    sb.append( "authList = " );
    for ( BaseAuthorization auth : authList ) {
      sb.append( auth.getValue( ) ).append( ", " );
    }
    sb.append( "]" );
    return sb.toString( );
  }
}
