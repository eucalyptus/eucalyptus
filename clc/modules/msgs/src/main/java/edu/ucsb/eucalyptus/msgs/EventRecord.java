package edu.ucsb.eucalyptus.msgs;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.util.DebugUtil;

import edu.ucsb.eucalyptus.constants.EventType;

public class EventRecord extends EucalyptusMessage {
  
  private static EucalyptusMessage BOGUS = getBogusMessage();

  String component;
  String service;
  long timestamp;
  String eventUserId;
  String eventCorrelationId;
  String eventId;
  String other;
  String caller;
  private EventRecord(final String component, final String eventUserId, final String eventCorrelationId, final String eventId, final String other, int distance ) {
    this.timestamp = System.currentTimeMillis();
    this.component = component;
    this.eventUserId = eventUserId;
    this.eventCorrelationId = eventCorrelationId;
    this.eventId = eventId;
    this.other = ":" + other;
    if( DebugUtil.DEBUG ) {
      StackTraceElement ste = Thread.currentThread().getStackTrace( )[distance];
      if( ste != null && ste.getFileName( ) != null ) {
        this.caller = String.format( "%s.%s.%s", ste.getFileName( ).replaceAll( "\\.\\w*\\b", "" ), ste.getMethodName( ), ste.getLineNumber( ) );
      } else {
        this.caller = "unknown";
      }
    } else {
      this.caller = "";
    }
  }
  
  private static EucalyptusMessage getBogusMessage( ) {
    EucalyptusMessage hi = new EucalyptusMessage( );
    hi.setUserId( "eucalyptus" );
    hi.setCorrelationId( "eucalyptus" );
    return hi;
  }

  public EventRecord() {
  }
  
  public String toString() {
    return String.format(":%7.4f:%s:uid:%s:%s:%s%s:", 
                         this.timestamp / 1000.0f, 
                         this.component, 
                         this.eventUserId, 
                         this.eventCorrelationId, 
                         this.eventId, 
                         this.other != null ? this.other : "",
                         this.caller 
                         ).replaceAll("::*",":");
  }

  public static EventRecord create( final String component, final String eventUserId, final String eventCorrelationId, final Object eventName, final String other, int dist ) {
    return new EventRecord( component, eventUserId, eventCorrelationId, eventName.toString( ), getMessageString(other), 3 + dist );
  }
  public static EventRecord here( final Class component, final Object eventName, final String... other) {
    EucalyptusMessage msg = tryForMessage( );
    return create( component.getSimpleName( ), msg.getUserId( ), msg.getCorrelationId( ), eventName.toString( ), getMessageString( other ), 1 );
  }
  public static EventRecord here( final String component, final Object eventName, final String... other) {
    EucalyptusMessage msg = tryForMessage( );
    return create( component, msg.getUserId( ), msg.getCorrelationId( ), eventName.toString( ), getMessageString( other ), 1 );
  }
  public static EventRecord here( final Component component, final Object eventName, final String... other) {
    EucalyptusMessage msg = tryForMessage( );
    return create( component.name( ), msg.getUserId( ), msg.getCorrelationId( ), eventName.toString( ), getMessageString( other ), 1 );
  }
  public static EventRecord caller( final Class component, final Object eventName, final String... other) {
    EucalyptusMessage msg = tryForMessage( );
    return create( component.getSimpleName( ), msg.getUserId( ), msg.getCorrelationId( ), eventName.toString( ), getMessageString( other ), 2 );
  }
  private static String getMessageString( final String... other ) {
    StringBuffer last = new StringBuffer();
    for(String x : other) {
      last.append( ":" ).append(x);
    }
    return last.length()>1?last.substring( 1 ):last.toString( );
  }
  private static EucalyptusMessage tryForMessage( ) {
    EucalyptusMessage msg = null;
    MuleEvent event = RequestContext.getEvent( );
    if( event != null ) {
      if( event.getMessage( ) != null && event.getMessage( ).getPayload( ) != null && event.getMessage( ).getPayload( ) instanceof EucalyptusMessage ) {
        msg = ((EucalyptusMessage) event.getMessage( ).getPayload( ) ); 
      }
    }
    return msg==null?BOGUS:msg;
  }

}

