/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
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
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.keys;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.auth.principal.FakePrincipals;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.KeyPair;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.HasOwningAccount;

@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_keypair" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class SshKeyPair extends UserMetadata<SshKeyPair.State> implements KeyPair {
  enum State { available, removing }
  @Column( name = "metadata_keypair_user_keyname", unique = true )
  String                   uniqueName;                                                                 //bogus field to enforce uniqueness
  @Lob
  @Column( name = "metadata_keypair_public_key" )
  String                   publicKey;
  @Column( name = "metadata_keypair_finger_print" )
  String                   fingerPrint;
  @Transient
  private FullName          fullName;
  @Transient
  public static String     NO_KEY_NAME = "";
  @Transient
  public static SshKeyPair NO_KEY      = new SshKeyPair( FakePrincipals.NOBODY_USER_ERN, "", "", "" );
  
  public SshKeyPair( ) {}
  
  public SshKeyPair( UserFullName user ) {
    super( user );
  }
  
  public SshKeyPair( UserFullName user, String keyName ) {
    super( user, keyName );
    this.uniqueName = this.getFullName( ).toString( );
  }
  
  public SshKeyPair( UserFullName user, String keyName, String publicKey, String fingerPrint ) {
    this( user, keyName );
    this.publicKey = publicKey;
    this.fingerPrint = fingerPrint;
  }
  
  public String getUniqueName( ) {
    return this.uniqueName;
  }

  public void setUniqueName( String uniqueName ) {
    this.uniqueName = uniqueName;
  }

  public String getPublicKey( ) {
    return this.publicKey;
  }

  public void setPublicKey( String publicKey ) {
    this.publicKey = publicKey;
  }

  public String getFingerPrint( ) {
    return this.fingerPrint;
  }

  public void setFingerPrint( String fingerPrint ) {
    this.fingerPrint = fingerPrint;
  }

  @Override
  public String toString( ) {
    return String.format( "SshKeyPair:%s:fingerPrint=%s", this.uniqueName, this.fingerPrint );
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = super.hashCode( );
    result = prime * result + ( ( this.uniqueName == null )
      ? 0
      : this.uniqueName.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) {
      return true;
    }
    if ( !super.equals( obj ) ) {
      return false;
    }
    if ( getClass( ) != obj.getClass( ) ) {
      return false;
    }
    SshKeyPair other = ( SshKeyPair ) obj;
    if ( this.uniqueName == null ) {
      if ( other.uniqueName != null ) {
        return false;
      }
    } else if ( !this.uniqueName.equals( other.uniqueName ) ) {
      return false;
    }
    return true;
  }
  
  
  @Override
  public String getName( ) {
    return this.getDisplayName( );
  }
  
  @Override
  public String getPartition( ) {
    return ComponentIds.lookup( Eucalyptus.class ).name( );
  }
  
  @Override
  public FullName getFullName( ) {
    return this.fullName == null ? this.fullName = FullName.create.vendor( "euca" )
      .region( ComponentIds.lookup( Eucalyptus.class ).name( ) )
      .namespace( this.getOwnerAccountId( ) )
      .relativeId( "keypair", this.getDisplayName( ) ) : this.fullName;
  }
  @Override
  public int compareTo( KeyPair that ) {
    return this.getFullName( ).toString( ).compareTo( that.getFullName( ).toString( ) );
  }

}
