/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
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
