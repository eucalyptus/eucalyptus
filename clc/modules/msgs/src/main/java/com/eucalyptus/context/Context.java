package com.eucalyptus.context;

import java.lang.ref.WeakReference;
import java.util.UUID;
import javax.security.auth.Subject;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.mule.api.MuleEvent;
import com.eucalyptus.auth.User;
import com.eucalyptus.http.MappingHttpRequest;
import edu.ucsb.eucalyptus.constants.EventType;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EventRecord;

public class Context {
  private static Logger            LOG         = Logger.getLogger( Context.class );
  
  private String                   correlationId;
  private Long                     creationTime;
  private BaseMessage              request     = null;
  private MappingHttpRequest       httpRequest = null;
  private Channel                  channel     = null;
  private WeakReference<MuleEvent> muleEvent   = null;
  private User                     user        = null;
  private Subject                  subject     = null;
  
  protected Context( MappingHttpRequest httpRequest, Channel channel ) {
    UUID uuid = UUID.randomUUID( );
    this.correlationId = uuid.toString( );
    this.creationTime = System.nanoTime( );
    this.httpRequest = httpRequest;
    this.channel = channel;
    LOG.debug( EventRecord.caller( Context.class, EventType.CONTEXT_CREATE, this.correlationId, this.channel.toString( ) ) );
  }
  
  public Channel getChannel( ) {
    return check( this.channel );
  }
  
  public MappingHttpRequest getHttpRequest( ) {
    return check( this.httpRequest );
  }
  
  public String getCorrelationId( ) {
    return this.correlationId;
  }
  
  public Long getCreationTime( ) {
    return this.creationTime;
  }
  
  public void setRequest( BaseMessage msg ) {
    if ( msg != null ) {
      LOG.debug( EventRecord.caller( Context.class, EventType.CONTEXT_MSG, this.correlationId, msg.getClass( ).getSimpleName( ) ) );
      this.request = msg;
    }
  }
  
  public BaseMessage getRequest( ) {
    return check( this.request );
  }
  
  public void setUser( User user ) {
    if ( user != null ) {
      LOG.debug( EventRecord.caller( Context.class, EventType.CONTEXT_USER, this.correlationId, user.getUserName( ) ) );
      this.user = user;
    }
  }
  
  public User getUser( ) {
    return check( this.user );
  }
  
  void setMuleEvent( MuleEvent event ) {
    if ( event != null ) {
      LOG.debug( EventRecord.caller( Context.class, EventType.CONTEXT_EVENT, this.correlationId, event.getService( ).getName( ) ) );
      this.muleEvent = new WeakReference<MuleEvent>( event );
    }
  }
  
  public String getServiceName( ) {
    MuleEvent e = null;
    if ( ( e = this.muleEvent.get( ) ) != null ) {
      return e.getService( ).getName( );
    } else {
      return this.httpRequest.getServicePath( ).replaceAll( "/services/", "" ).replaceAll( "[/?].+", "" );
    }
  }
  
  public Subject getSubject( ) {
    return check( this.subject );
  }
  
  public void setSubject( Subject subject ) {
    if ( subject != null ) {
      LOG.debug( EventRecord.caller( Context.class, EventType.CONTEXT_SUBJECT, this.correlationId, subject.getPrincipals( ).toString( ) ) );
      this.subject = subject;
    }
  }
  
  public void clear( ) {
  //    Contexts.clear( this );
  //    LOG.debug( EventRecord.caller( Context.class, EventType.CONTEXT_CLEAR, this.correlationId, this.channel.toString( ) ) );
  //    this.channel.clear( );
  //    this.channel = null;
  //    this.httpRequest.clear( );
  //    this.httpRequest = null;
  //    this.muleEvent.clear( );
  //    this.muleEvent = null;
  }
  
  private final static <TYPE> TYPE check( final TYPE obj ) {
    if ( obj == null ) {
      StackTraceElement steMethod = Thread.currentThread( ).getStackTrace( )[1];
      StackTraceElement steCaller = Thread.currentThread( ).getStackTrace( )[2];
      LOG.error( "Accessing context field when it is null: " + steMethod.getMethodName( ) + " from " + steCaller );
    }
    return obj;
  }
  
}
