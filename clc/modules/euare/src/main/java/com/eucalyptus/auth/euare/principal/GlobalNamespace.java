/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.auth.euare.principal;

/**
 *
 */
public enum GlobalNamespace {

  Account_Alias( "iam:account:alias" ),

  Signing_Certificate_Id( "iam:signing-certificate:id" ),
  ;

  private final String namespace;

  private GlobalNamespace( final String namespace ) {
    this.namespace = namespace;
  }

  public String getNamespace( ) {
    return namespace;
  }

  public static GlobalNamespace forNamespace( final String namespace ) {
    GlobalNamespace globalNamespace = null;
    for ( final GlobalNamespace candidateNamespace : values( ) ) {
      if ( candidateNamespace.getNamespace( ).equals( namespace ) ) {
        globalNamespace = candidateNamespace;
        break;
      }
    }
    if ( globalNamespace == null ) {
      throw new IllegalArgumentException( "Invalid namespace: " + namespace );
    }
    return globalNamespace;
  }
}
