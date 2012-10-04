/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.records;

import org.apache.log4j.Logger;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

public class EventRecord extends BaseMessage {
  private static final Logger                LOG             = Logger.getLogger( EventRecord.class );
  private static final BaseMessage           BOGUS           = getBogusMessage( );
  private static final Supplier<BaseMessage> messageSupplier = getCurrentBaseMessageSupplier();
  
  private static Record create( final Class component, final EventClass eventClass, final EventType eventName, final String other, int dist ) {
    BaseMessage msg = tryForMessage( );
    StackTraceElement[] stack = Thread.currentThread( ).getStackTrace( );
    StackTraceElement ste = stack[dist+3<stack.length?dist+3:stack.length-1];
    String userFn = Bootstrap.isFinished( ) ? "" : "bootstrap";
    try {
      Context ctx = Contexts.lookup( msg.getCorrelationId( ) );
      userFn = ctx.getUserFullName( ).toString( );
    } catch ( Exception ex ) {
    }
    
    return new LogFileRecord( eventClass, eventName, component, ste, userFn, msg.getCorrelationId( ), other );
  }

  public static Record here( final Class component, final EventClass eventClass, final EventType eventName, final String... other ) {
    return create( component, eventClass, eventName, getMessageString( other ), 1 );
  }
    
  public static Record caller( final Class component, final EventClass eventClass, final EventType eventName, final Object... other ) {
    return create( component, eventClass, eventName, getMessageString( other ), 2 );
  }

  public static Record here( final Class component, final EventType eventName, final String... other ) {
    return create( component, EventClass.ORPHAN, eventName, getMessageString( other ), 1 );
  }
    
  public static Record caller( final Class component, final EventType eventName, final Object... other ) {
    return create( component, EventClass.ORPHAN, eventName, getMessageString( other ), 2 );
  }

  private static String getMessageString( final Object[] other ) {
    StringBuffer last = new StringBuffer( );
    if( other != null ) {
      for ( Object x : other ) {
        last.append( ":" ).append( x );
      }
    }
    return last.length( ) > 1 ? last.substring( 1 ) : last.toString( );
  }

  private static BaseMessage getBogusMessage( ) {
    EucalyptusMessage hi = new EucalyptusMessage( );
    hi.setCorrelationId( "" );
    hi.setUserId( "" );
    return hi;
  }

  private static BaseMessage tryForMessage( ) {
    return messageSupplier.get();
  }

  private static Supplier<BaseMessage> getCurrentBaseMessageSupplier() {
    try {
      MuleEvent.class.getName(); // fail if class not found
      return new Supplier<BaseMessage>(){
        @Override
        public BaseMessage get() {
          BaseMessage msg = null;
          MuleEvent event = RequestContext.getEvent();
          if ( event != null ) {
            if ( event.getMessage( ) != null && event.getMessage( ).getPayload( ) != null && event.getMessage( ).getPayload( ) instanceof BaseMessage ) {
              msg = ( ( BaseMessage ) event.getMessage( ).getPayload( ) );
            }
          }
          return msg == null ? BOGUS : msg;
        }
      };
    } catch ( final NoClassDefFoundError e ) {
      return Suppliers.ofInstance( BOGUS );
    }
  }
}
