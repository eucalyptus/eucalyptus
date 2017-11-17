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
