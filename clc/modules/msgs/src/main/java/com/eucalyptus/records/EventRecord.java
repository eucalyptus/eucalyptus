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
 ************************************************************************/

package com.eucalyptus.records;

import org.apache.log4j.Logger;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

public class EventRecord extends BaseMessage {
  private static Logger            LOG   = Logger.getLogger( EventRecord.class );
  
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

  private static BaseMessage BOGUS  = getBogusMessage( );
  private static BaseMessage getBogusMessage( ) {
    EucalyptusMessage hi = new EucalyptusMessage( );
    hi.setCorrelationId( "" );
    hi.setUserId( "" );
    return hi;
  }
  private static BaseMessage tryForMessage( ) {
    BaseMessage msg = null;
    MuleEvent event = RequestContext.getEvent( );
    if ( event != null ) {
      if ( event.getMessage( ) != null && event.getMessage( ).getPayload( ) != null && event.getMessage( ).getPayload( ) instanceof BaseMessage ) {
        msg = ( ( BaseMessage ) event.getMessage( ).getPayload( ) );
      }
    }
    return msg == null ? BOGUS : msg;
  }

}
