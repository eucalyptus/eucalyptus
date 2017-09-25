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
