/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package com.eucalyptus.auth;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@PersistenceContext( name = "eucalyptus_general" )
@Table( name = "Users" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class UserInfo {
  
  @Id
  @GeneratedValue
  @Column( name = "user_id" )
  Long          id          = -1l;
  
  @Column( name = "user_name" )
  String        userName;
  
  @Column( name = "user_email" )
  String        email;
  
  @Column( name = "user_real_name" )
  String        realName;
  
  @Column( name = "user_telephone_number" )
  String        telephoneNumber;
  
  @Column( name = "user_affiliation" )
  String        affiliation;
  
  @Column( name = "user_project_description" )
  String        projectDescription;
  
  @Column( name = "user_project_pi_name" )
  String        projectPIName;
  
  @Column( name = "user_is_approved" )
  Boolean       approved;
  
  @Column( name = "user_is_confirmed" )
  Boolean       confirmed;
  
  @Column( name = "password_expires" )
  Long          passwordExpires;
  
  @Column( name = "user_confirmation_code" )
  String        confirmationCode;
  
  @Transient
  public static String BOGUS_ENTRY = "n/a";
  
  public UserInfo( ) {}
  
  public UserInfo( String userName, String confirmationCode ) {
    this( userName, BOGUS_ENTRY, confirmationCode );
    this.approved = true;
    this.confirmed = true;
  }
  
  public UserInfo( String userName, String email, String confirmationCode ) {
    this.approved = true;
    this.confirmed = false;
    this.confirmationCode = confirmationCode;
    this.passwordExpires = 1000 * 60 * 60 * 24 * 365L;
    
    this.userName = userName;
    this.email = email;
    
    this.realName = userName;
    this.telephoneNumber = BOGUS_ENTRY;
    this.affiliation = BOGUS_ENTRY;
    this.projectDescription = BOGUS_ENTRY;
    this.projectPIName = BOGUS_ENTRY;
  }
  
  public UserInfo( String userName ) {
    this.userName = userName;
  }
  
  public Long getId( ) {
    return id;
  }
  
  public void setUserName( String userName ) {
    this.userName = userName;
  }
  
  public String getUserName( ) {
    return userName;
  }
  
  public String getAffiliation( ) {
    return affiliation;
  }
  
  public void setAffiliation( String affiliation ) {
    this.affiliation = affiliation;
  }
  
  public Long getPasswordExpires( ) {
    return passwordExpires;
  }
  
  public void setPasswordExpires( Long passwordExpires ) {
    this.passwordExpires = passwordExpires;
  }
  
  public Boolean isApproved( ) {
    return approved;
  }
  
  public void setApproved( Boolean approved ) {
    this.approved = approved;
  }
  
  public String getProjectDescription( ) {
    return projectDescription;
  }
  
  public void setProjectDescription( String projectDescription ) {
    this.projectDescription = projectDescription;
  }
  
  public String getProjectPIName( ) {
    return projectPIName;
  }
  
  public void setProjectPIName( String projectPIName ) {
    this.projectPIName = projectPIName;
  }
  
  public String getRealName( ) {
    return realName;
  }
  
  public void setRealName( String realName ) {
    this.realName = realName;
  }
  
  public String getTelephoneNumber( ) {
    return telephoneNumber;
  }
  
  public void setTelephoneNumber( String telephoneNumber ) {
    this.telephoneNumber = telephoneNumber;
  }
  
  public String getEmail( ) {
    return email;
  }
  
  public void setEmail( String email ) {
    this.email = email;
  }
  
  public Boolean getConfirmed( ) {
    return this.confirmed;
  }
  
  public void setConfirmed( Boolean confirmed ) {
    this.confirmed = confirmed;
  }
  
  public String getConfirmationCode( ) {
    return this.confirmationCode;
  }
  
  public void setConfirmationCode( String confirmationCode ) {
    this.confirmationCode = confirmationCode;
  }
  
  public Boolean getApproved( ) {
    return this.approved;
  }
  
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    
    UserInfo userInfo = ( UserInfo ) o;
    
    if ( !userName.equals( userInfo.userName ) ) return false;
    
    return true;
  }
  
  public int hashCode( ) {
    return userName.hashCode( );
  }
  
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "UserInfo [ ");
    sb.append( "affiliation = ").append( affiliation == null ? "null" : affiliation ).append( ", " );
    sb.append( "approved = ").append( approved == null ? "null" : approved ).append( ", " );
    sb.append( "confirmationCode = ").append( confirmationCode == null ? "null" : confirmationCode ).append( ", " );
    sb.append( "confirmed = ").append( confirmed == null ? "null" : confirmed ).append( ", " );
    sb.append( "email = ").append( email == null ? "null" : email ).append( ", " );
    sb.append( "passwordExpires = ").append( passwordExpires == null ? "null" : passwordExpires ).append( ", " );
    sb.append( "projectDescription = ").append( projectDescription == null ? "null" : projectDescription ).append( ", " );
    sb.append( "projectPIName = ").append( projectPIName == null ? "null" : projectPIName ).append( ", " );
    sb.append( "realName = ").append( realName == null ? "null" : realName ).append( ", " );
    sb.append( "telephoneNumber = ").append( telephoneNumber == null ? "null" : telephoneNumber ).append( ", " );
    sb.append( "userName = ").append( userName == null ? "null" : userName ).append( " ]" );
    return sb.toString( );
  }
}
