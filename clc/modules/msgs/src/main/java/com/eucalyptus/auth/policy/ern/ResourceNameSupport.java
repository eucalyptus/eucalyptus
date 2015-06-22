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
package com.eucalyptus.auth.policy.ern;

import com.google.common.base.Strings;

/**
 *
 */
public abstract class ResourceNameSupport extends Ern {

  private final String type;
  private final String id;

  public ResourceNameSupport( String vendor, String region, String account, String type, String id ) {
    super( vendor, region, account );
    this.type = type;
    this.id = id;
  }

  @Override
  public String getResourceType( ) {
    return qualifiedName( this.type );
  }

  @Override
  public String getResourceName( ) {
    return this.id;
  }

  protected String getType( ) {
    return type;
  }

  @Override
  public String toString( ) {
    return new StringBuilder( )
        .append( ARN_PREFIX )
        .append( getService( ) ).append( ':' )
        .append( Strings.nullToEmpty( getRegion( ) ) ).append( ':' )
        .append( Strings.nullToEmpty( getAccount( ) ) ).append( ':' )
        .append( getType( ) ).append( '/' )
        .append( getResourceName( ) ).toString( );
  }
}
