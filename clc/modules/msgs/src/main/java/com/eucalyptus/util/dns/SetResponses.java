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
package com.eucalyptus.util.dns;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.xbill.DNS.RRset;
import org.xbill.DNS.SetResponse;
import com.eucalyptus.util.Exceptions;

/**
 * Utility methods for working with SetResponse
 */
public class SetResponses {

  private static final Constructor<SetResponse> CONS_TYPE_RRSET;
  private static final Method METHOD_OF_TYPE;
  private static final Method METHOD_ADD_RRSET;

  static {
    try {
      CONS_TYPE_RRSET = SetResponse.class.getDeclaredConstructor( Integer.TYPE, RRset.class );
      CONS_TYPE_RRSET.setAccessible( true );

      METHOD_OF_TYPE = SetResponse.class.getDeclaredMethod( "ofType", Integer.TYPE );
      METHOD_OF_TYPE.setAccessible( true );

      METHOD_ADD_RRSET = SetResponse.class.getDeclaredMethod( "addRRset", RRset.class );
      METHOD_ADD_RRSET.setAccessible( true );
    } catch ( NoSuchMethodException e ) {
      throw Exceptions.toUndeclared( e );
    }
  }

  public enum SetResponseType {
    unknown( 0 ),
    nxdomain( 1 ),
    nxrrset( 2 ),
    delegation( 3 ),
    cname( 4 ),
    dname( 5 ),
    successful( 6 ),
    ;

    private final int type;

    SetResponseType( final int type ) {
      this.type = type;
    }

    public int type( ) {
      return type;
    }
  }

  public static SetResponse ofType( final SetResponseType type ) {
    try {
      return (SetResponse) METHOD_OF_TYPE.invoke( null, type.type( ) );
    } catch ( InvocationTargetException | IllegalAccessException e ) {
      throw Exceptions.toUndeclared( e );
    }
  }

  public static SetResponse newInstance( final SetResponseType type, final RRset set ) {
    try {
      return CONS_TYPE_RRSET.newInstance( type.type( ), set );
    } catch ( InvocationTargetException | IllegalAccessException | InstantiationException e ) {
      throw Exceptions.toUndeclared( e );
    }
  }

  public static void addRRset( final SetResponse response, final RRset set ) {
    try {
      METHOD_ADD_RRSET.invoke( response, set );
    } catch ( InvocationTargetException | IllegalAccessException e ) {
      throw Exceptions.toUndeclared( e );
    }
  }
}
