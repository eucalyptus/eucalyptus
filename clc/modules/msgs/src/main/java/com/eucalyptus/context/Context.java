package com.eucalyptus.context;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.UUID;
import javax.security.auth.Subject;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.mule.api.MuleEvent;
import com.eucalyptus.auth.Groups;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.records.EventType;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import com.eucalyptus.records.EventRecord;

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
    EventRecord.caller( Context.class, EventType.CONTEXT_CREATE, this.correlationId, this.channel.toString( ) ).debug();
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
      EventRecord.caller( Context.class, EventType.CONTEXT_MSG, this.correlationId, msg.getClass( ).getSimpleName( ) ).debug( );
      this.request = msg;
    }
  }
  
  public BaseMessage getRequest( ) {
    return check( this.request );
  }
  
  public void setUser( User user ) {
    if ( user != null ) {
      EventRecord.caller( Context.class, EventType.CONTEXT_USER, this.correlationId, user.getName( ) ).debug( );
      this.user = user;
    }
  }
  
  public User getUser( ) {
    return check( this.user );
  }
  
  public List<Group> getGroups( ) {
    return Groups.lookupUserGroups( this.getUser( ) );
  }

  public List<Authorization> getAuthorizations( ) {
    List<Authorization> auths = Lists.newArrayList( );
    for( Group g : this.getGroups( ) ) {
      auths.addAll( g.getAuthorizations( ) );
    }
    return auths;
  }

  
  void setMuleEvent( MuleEvent event ) {
    if ( event != null ) {
      EventRecord.caller( Context.class, EventType.CONTEXT_EVENT, this.correlationId, event.getId( ) ).debug( );
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
      EventRecord.caller( Context.class, EventType.CONTEXT_SUBJECT, this.correlationId, subject.getPrincipals( ).toString( ) ).debug( );
      this.subject = subject;
    }
  }
  
  void clear( ) {
    EventRecord.caller( Context.class, EventType.CONTEXT_CLEAR, this.correlationId, this.channel.toString( ) ).debug( );
    this.channel = null;
    this.httpRequest = null;
    if( this.muleEvent != null ) {
      this.muleEvent.clear( );
      this.muleEvent = null;
    }
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
