/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.storage.msgs.s3;

import java.util.Objects;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class Grantee extends EucalyptusData implements Comparable<Grantee> {

  private CanonicalUser canonicalUser;
  private Group group;
  private String type;
  private String emailAddress;

  public Grantee( ) {
  }

  public Grantee( CanonicalUser canonicalUser ) {
    this.canonicalUser = canonicalUser;
    this.type = "CanonicalUser";
  }

  public Grantee( Group group ) {
    this.group = group;
    this.type = "Group";
  }

  public Grantee( String emailAddress ) {
    this.emailAddress = emailAddress;
    this.type = "AmazonCustomerByEmail";
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final Grantee grantee = (Grantee) o;
    return Objects.equals( getCanonicalUser( ), grantee.getCanonicalUser( ) ) &&
        Objects.equals( getGroup( ), grantee.getGroup( ) ) &&
        Objects.equals( getType( ), grantee.getType( ) ) &&
        Objects.equals( getEmailAddress( ), grantee.getEmailAddress( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getCanonicalUser( ), getGroup( ), getType( ), getEmailAddress( ) );
  }

  /**
   * Returns the identifier string for the grantee. Maps as:
   * canonicalUser -> returns canonicalUserId
   * group -> returns group uri
   * email -> returns email address
   *
   * @return
   */
  public String getIdentifier( ) {
    if ( "CanonicalUser".equals( type ) ) {
      return ( this.canonicalUser != null ? this.canonicalUser.getID( ) : null );
    }

    if ( "Group".equals( type ) ) {
      return ( this.group != null ? this.group.getUri( ) : null );
    }

    if ( "AmazonCustomerByEmail".equals( type ) ) {
      return this.emailAddress;
    }

    return null;
  }

  @Override
  public int compareTo( Grantee grantee ) {
    String grantee1 = this.getIdentifier( );
    String grantee2 = "";
    if ( grantee != null ) {
      grantee2 = grantee.getIdentifier( );
    }


    if ( grantee1 != null && grantee2 != null ) {
      return grantee1.compareTo( grantee2 );
    } else {
      if ( grantee1 == null && grantee2 == null ) {
        return 0;
      } else {
        return grantee1 == null ? -1 : 1;
      }

    }

  }

  public CanonicalUser getCanonicalUser( ) {
    return canonicalUser;
  }

  public void setCanonicalUser( CanonicalUser canonicalUser ) {
    this.canonicalUser = canonicalUser;
  }

  public Group getGroup( ) {
    return group;
  }

  public void setGroup( Group group ) {
    this.group = group;
  }

  public String getType( ) {
    return type;
  }

  public void setType( String type ) {
    this.type = type;
  }

  public String getEmailAddress( ) {
    return emailAddress;
  }

  public void setEmailAddress( String emailAddress ) {
    this.emailAddress = emailAddress;
  }
}
