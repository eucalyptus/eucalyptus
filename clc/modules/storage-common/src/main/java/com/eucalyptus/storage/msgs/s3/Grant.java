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

public class Grant extends EucalyptusData implements Comparable<Grant> {

  private Grantee grantee;
  private String permission;

  public Grant( ) {
  }

  public Grant( Grantee grantee, String permission ) {
    this.grantee = grantee;
    this.permission = permission;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final Grant grant = (Grant) o;
    return Objects.equals( getGrantee( ), grant.getGrantee( ) ) &&
        Objects.equals( getPermission( ), grant.getPermission( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getGrantee( ), getPermission( ) );
  }

  @Override
  public int compareTo( Grant grant ) {
    //sort by alpha on grantee and then on permission.
    String grantee1 = "";
    String grantee2 = "";
    if ( grantee != null ) {
      if ( "CanonicalUser".equals( grantee.getType( ) ) ) {
        grantee1 = grantee.getCanonicalUser( ).getID( );
      } else if ( "Group".equals( grantee.getType( ) ) ) {
        grantee1 = grantee.getGroup( ).getUri( ).toString( );
      } else if ( "AmazonCustomerByEmail".equals( grantee.getType( ) ) ) {
        grantee1 = grantee.getEmailAddress( );
      }
    } else {
      if ( grant.getGrantee( ) != null ) {
        return -1;
      }
    }


    if ( grant.getGrantee( ) != null ) {
      if ( "CanonicalUser".equals( grant.getGrantee( ).getType( ) ) ) {
        grantee2 = grant.getGrantee( ).getCanonicalUser( ).getID( );
      } else if ( "Group".equals( grant.getGrantee( ).getType( ) ) ) {
        grantee2 = grant.getGrantee( ).getGroup( ).getUri( ).toString( );
      } else if ( "AmazonCustomerByEmail".equals( grant.getGrantee( ).getType( ) ) ) {
        grantee2 = grant.getGrantee( ).getEmailAddress( );
      }
    } else {
      return 1;
    }

    int granteeCompare = grantee1.compareTo( grantee2 );

    if ( granteeCompare != 0 ) {
      return granteeCompare;
    } else {
      //Compare the permission.
      if ( permission != null ) {
        //String-compare. Specific ordering isn't important as long as it is consistent.
        return permission.compareTo( grant.getPermission( ) );
      } else if ( grant.getPermission( ) != null ) {
        //Anything beats null
        return 1;
      } else {
        //Both same grantee and same null permission.
        return 0;
      }

    }

  }

  public Grantee getGrantee( ) {
    return grantee;
  }

  public void setGrantee( Grantee grantee ) {
    this.grantee = grantee;
  }

  public String getPermission( ) {
    return permission;
  }

  public void setPermission( String permission ) {
    this.permission = permission;
  }
}
