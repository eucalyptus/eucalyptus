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
import com.eucalyptus.auth.crypto.CryptoProviders;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;

@Entity
@PersistenceContext( name = "eucalyptus_general" )
@Table( name = "Users" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class UserInfo {
  
  @Id
  @GeneratedValue
  @Column( name = "user_id" )
  private Long    id = -1l;
  @Column( name = "user_name" )
  private String  userName;
  @Column( name = "user_email" )
  private String  email;
  @Column( name = "user_real_name" )
  private String  realName;
  @Column( name = "user_reservation_id" )
  private Long    reservationId;
  @Column( name = "user_b_crypted_password" )
  private String  bCryptedPassword;
  @Column( name = "user_telephone_number" )
  private String  telephoneNumber;
  @Column( name = "user_affiliation" )
  private String  affiliation;
  @Column( name = "user_project_description" )
  private String  projectDescription;
  @Column( name = "user_project_pi_name" )
  private String  projectPIName;
  @Column( name = "user_confirmation_code" )
  private String  confirmationCode;
  @Column( name = "user_certificate_code" )
  private String  certificateCode;
  @Column( name = "user_is_approved" )
  private Boolean isApproved;
  @Column( name = "user_is_confirmed" )
  private Boolean isConfirmed;
  @Column( name = "user_is_enabled" )
  private Boolean isEnabled;
  @Column( name = "user_is_admin" )
  private Boolean isAdministrator;
  @Column( name = "password_expires" )
  private Long    passwordExpires;
  @Column( name = "user_temporary_password" )
  private String  temporaryPassword;
  
  @Transient
  private static String BOGUS_ENTRY = "N/A";
  public UserInfo( ) {}
  
  public UserInfo( String userName, Boolean admin, String confirmationCode, String certificateCode, String oneTimePass ) {
    this( userName, BOGUS_ENTRY, admin, confirmationCode, certificateCode, oneTimePass );
    this.isApproved = true;
    this.isConfirmed = true;
    this.isEnabled = true;
    this.bCryptedPassword = BOGUS_ENTRY;
  }
  
  public UserInfo( String userName, String email, Boolean admin, String confirmationCode, String certificateCode, String oneTimePass ) {
    this.isApproved = true;
    this.isConfirmed = false;
    this.isEnabled = false;    
    this.reservationId = 0l;
    this.passwordExpires = 0l;    

    this.userName = userName;
    this.isAdministrator = admin;
    this.email = email;
    this.confirmationCode = confirmationCode;
    this.certificateCode = certificateCode;
    this.bCryptedPassword = oneTimePass;

    this.realName = BOGUS_ENTRY;
    this.telephoneNumber = BOGUS_ENTRY;
    this.affiliation = BOGUS_ENTRY;
    this.projectDescription = BOGUS_ENTRY;
    this.projectPIName = BOGUS_ENTRY;
  }  
  
  public static UserInfo generateAdmin( ) {
    return UserInfo.generateAdmin( "admin" );
  }
  public static UserInfo generateAdmin( String userName ) {
    return new UserInfo( userName, "", "Eucalyptus Administrator", 0l, 
                         CryptoProviders.generateHashedPassword( userName ), "", "", "", "", 
                         CryptoProviders.generateSessionToken( userName ), 
                         CryptoProviders.generateSessionToken( userName ), 
                         true, true, true, true, 0l );
  }
  public UserInfo( String userName, String email, String realName, Long reservationId, String bCryptedPassword, String telephoneNumber, String affiliation,
                   String projectDescription, String projectPIName, String confirmationCode, String certificateCode, Boolean isApproved, Boolean isConfirmed,
                   Boolean isEnabled, Boolean isAdministrator, Long passwordExpires ) {
    super( );
    this.userName = userName;
    this.email = email;
    this.realName = realName;
    this.reservationId = reservationId;
    this.bCryptedPassword = bCryptedPassword;
    this.telephoneNumber = telephoneNumber;
    this.affiliation = affiliation;
    this.projectDescription = projectDescription;
    this.projectPIName = projectPIName;
    this.confirmationCode = confirmationCode;
    this.certificateCode = certificateCode;
    this.isApproved = isApproved;
    this.isConfirmed = isConfirmed;
    this.isEnabled = isEnabled;
    this.isAdministrator = isAdministrator;
    this.passwordExpires = passwordExpires;
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
  
  public String getBCryptedPassword( ) {
    return bCryptedPassword;
  }
  
  public void setBCryptedPassword( String bCryptedPassword ) {
    this.bCryptedPassword = bCryptedPassword;
  }
  
  public String getConfirmationCode( ) {
    return confirmationCode;
  }
  
  public void setConfirmationCode( String confirmationCode ) {
    this.confirmationCode = confirmationCode;
  }
  
  public String getCertificateCode( ) {
    return certificateCode;
  }
  
  public void setCertificateCode( String certificateCode ) {
    this.certificateCode = certificateCode;
  }
  
  public Boolean isAdministrator( ) {
    return isAdministrator;
  }
  
  public void setIsAdministrator( Boolean administrator ) {
    isAdministrator = administrator;
  }
  
  public Long getPasswordExpires( ) {
    return passwordExpires;
  }
  
  public void setPasswordExpires( Long passwordExpires ) {
    this.passwordExpires = passwordExpires;
  }
  
  public String getTemporaryPassword( ) {
    return this.temporaryPassword;
  }
  
  public void setTemporaryPassword( String password ) {
    this.temporaryPassword = password;
  }
  
  public Boolean isApproved( ) {
    return isApproved;
  }
  
  public void setIsApproved( Boolean approved ) {
    isApproved = approved;
  }
  
  public Boolean isConfirmed( ) {
    return isConfirmed;
  }
  
  public void setIsConfirmed( Boolean confirmed ) {
    isConfirmed = confirmed;
  }
  
  public Boolean isEnabled( ) {
    return isEnabled;
  }
  
  public void setIsEnabled( Boolean enabled ) {
    isEnabled = enabled;
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
  
  public Long getReservationId( ) {
    return reservationId++;
  }
  
  public void setReservationId( Long reservationId ) {
    this.reservationId = reservationId;
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
  
  public static UserInfo named( String userId ) throws EucalyptusCloudException {
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>( );
    UserInfo user = null;
    try {
      user = db.getUnique( new UserInfo( userId ) );
      db.commit( );
    } catch ( Throwable t ) {
      db.commit( );
    }
    return user;
  }
}
