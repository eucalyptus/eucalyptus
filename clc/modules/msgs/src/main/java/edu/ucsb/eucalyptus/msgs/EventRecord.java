package edu.ucsb.eucalyptus.msgs;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.system.LogLevels;
import com.google.common.collect.Lists;

public class EventRecord extends EucalyptusMessage {
  private static Logger            LOG    = Logger.getLogger( EventRecord.class );
  private static EucalyptusMessage BOGUS  = getBogusMessage( );
  
  private String                   logger;
  private String                   component;
  private Long                     timestamp;
  private String                   eventUserId;
  private String                   eventCorrelationId;
  private String                   eventId;
  private String                   other;
  private String                   caller;
  private ArrayList                others = Lists.newArrayList( );
  
  private EventRecord( final String component, final String eventUserId, final String eventCorrelationId, final String eventId, final String other, int distance ) {
    this.timestamp = TimeUnit.MILLISECONDS.convert( System.nanoTime( ), TimeUnit.NANOSECONDS );
    this.component = component;
    this.eventUserId = eventUserId;
    this.eventCorrelationId = eventCorrelationId;
    this.eventId = eventId;
    this.others.add( other );
    if ( LogLevels.DEBUG ) {
      StackTraceElement ste = Thread.currentThread( ).getStackTrace( )[distance];
      if ( ste != null )
        this.logger = ste.getClassName( );
      if ( ste != null && ste.getFileName( ) != null ) {
        this.caller = String.format( "%s.%s.%s", ste.getFileName( ).replaceAll( "\\.\\w*\\b", "" ), ste.getMethodName( ), ste.getLineNumber( ) );
      } else {
        this.caller = "unknown";
      }
    } else {
      this.logger = Thread.currentThread( ).getStackTrace( )[distance].getClassName( );
    }
  }
  
  private static EucalyptusMessage getBogusMessage( ) {
    EucalyptusMessage hi = new EucalyptusMessage( );
    hi.setUserId( null );
    hi.setEffectiveUserId( null );
    hi.setCorrelationId( null );
    return hi;
  }
  
  public EventRecord( ) {}
  
  public EventRecord info( ) {
    LOG.getLogger( this.logger ).info( this );
    return this;
  }
  public EventRecord error( ) {
    LOG.getLogger( this.logger ).info( this );
    return this;
  }
  public EventRecord warn( ) {
    LOG.getLogger( this.logger ).warn( this );
    return this;
  }
  
  public EventRecord next( ) {
    this.others.add( NEXT );
    return this;
  }
  
  public EventRecord append( Object... obj ) {
    for ( Object o : obj ) {
      this.others.add( o == null ? ISNULL : o.toString( ) );
    }
    return this;
  }
  
  private static final String ISNULL = "NULL";
  private static final String NEXT   = "\n";
  private transient String    lead;
  
  private String leadIn( ) {
    return lead == null ? ( lead = String.format( ":%010d:%s:%s:%s:%s:", this.timestamp, this.component,
                                                  ( ( this.eventCorrelationId != null ) ? this.eventCorrelationId : "" ),
                                                  ( ( this.eventUserId != null ) ? this.eventUserId : "" ), this.eventId ) ) : lead;
  }
  
  public String toString( ) {
    String ret = this.leadIn( );
    for ( Object o : this.others ) {
      ret += ":" + o.toString( );
    }
    return ret.replaceAll( "::*", ":" ).replaceAll( NEXT, NEXT + this.leadIn( ) );
  }
  
  public static EventRecord create( final String component, final String eventUserId, final String eventCorrelationId, final Object eventName, final String other, int dist ) {
    return new EventRecord( component, eventUserId, eventCorrelationId, eventName.toString( ), getMessageString( other ), 3 + dist );
  }
  
  public static EventRecord here( final Class component, final Object eventName, final String... other ) {
    EucalyptusMessage msg = tryForMessage( );
    return create( component.getSimpleName( ), msg.getUserId( ), msg.getCorrelationId( ), eventName.toString( ), getMessageString( other ), 1 );
  }
  
  public static EventRecord here( final String component, final Object eventName, final String... other ) {
    EucalyptusMessage msg = tryForMessage( );
    return create( component, msg.getUserId( ), msg.getCorrelationId( ), eventName.toString( ), getMessageString( other ), 1 );
  }
  
  public static EventRecord here( final Component component, final Object eventName, final String... other ) {
    EucalyptusMessage msg = tryForMessage( );
    return create( component.name( ), msg.getUserId( ), msg.getCorrelationId( ), eventName.toString( ), getMessageString( other ), 1 );
  }
  
  public static EventRecord caller( final Class component, final Object eventName, final Object... other ) {
    EucalyptusMessage msg = tryForMessage( );
    return create( component.getSimpleName( ), msg.getUserId( ), msg.getCorrelationId( ), eventName.toString( ), getMessageString( other ), 2 );
  }
  
  private static String getMessageString( final Object... other ) {
    StringBuffer last = new StringBuffer( );
    for ( Object x : other ) {
      last.append( ":" ).append( x.toString( ) );
    }
    return last.length( ) > 1 ? last.substring( 1 ) : last.toString( );
  }
  
  private static EucalyptusMessage tryForMessage( ) {
    EucalyptusMessage msg = null;
    MuleEvent event = RequestContext.getEvent( );
    if ( event != null ) {
      if ( event.getMessage( ) != null && event.getMessage( ).getPayload( ) != null && event.getMessage( ).getPayload( ) instanceof EucalyptusMessage ) {
        msg = ( ( EucalyptusMessage ) event.getMessage( ).getPayload( ) );
      }
    }
    return msg == null ? BOGUS : msg;
  }
  
}
