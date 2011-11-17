package com.eucalyptus.context;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import javax.security.auth.Subject;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.local.DefaultLocalClientChannelFactory;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.mule.api.MuleEvent;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Contract;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.ws.server.Statistics;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class Context {
  private static Logger                LOG       = Logger.getLogger( Context.class );
  private final String                 correlationId;
  private Long                         creationTime;
  private BaseMessage                  request   = null;
  private final MappingHttpRequest     httpRequest;
  private final Channel                channel;
  private WeakReference<MuleEvent>     muleEvent = new WeakReference<MuleEvent>( null );
  private User                         user      = null;
  private Subject                      subject   = null;
  private Map<Contract.Type, Contract> contracts = Maps.newHashMap( );
  
  protected Context( String dest, final BaseMessage msg ) {
    this.correlationId = msg.getCorrelationId( );
    this.creationTime = System.nanoTime( );
    this.httpRequest = new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.GET, dest ) {
      {
        this.setCorrelationId( msg.getCorrelationId( ) );
        this.message = msg;
      }
    };
    this.channel = new DefaultLocalClientChannelFactory( ).newChannel( Channels.pipeline( ) );
    this.user = Principals.systemUser( );
    EventRecord.caller( Context.class, EventType.CONTEXT_CREATE, this.correlationId, this.channel.toString( ) ).debug( );
  }
  
  protected Context( MappingHttpRequest httpRequest, Channel channel ) {
    UUID uuid = UUID.randomUUID( );
    Statistics.startRequest( channel );
    this.correlationId = uuid.toString( );
    this.creationTime = System.nanoTime( );
    this.httpRequest = httpRequest;
    this.channel = channel;
    EventRecord.caller( Context.class, EventType.CONTEXT_CREATE, this.correlationId, this.channel.toString( ) ).debug( );
  }
  
  public Channel getChannel( ) {
    return check( this.channel );
  }
  
  public InetAddress getRemoteAddress( ) {
    if ( this.getChannel( ) != null ) {
      if ( this.getChannel( ).getRemoteAddress( ) instanceof InetSocketAddress ) {
        return ( ( InetSocketAddress ) this.getChannel( ).getRemoteAddress( ) ).getAddress( );
      }
    }
    throw new IllegalContextAccessException( "Attempt to access socket address information when no associated socket exists." );
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
      EventRecord.caller( Context.class, EventType.CONTEXT_MSG, this.correlationId, msg.toSimpleString( ) ).debug( );
      this.request = msg;
    }
  }
  
  public BaseMessage getRequest( ) {
    if ( this.request == null && this.httpRequest != null && this.httpRequest.getMessage( ) != null ) {
      this.request = ( BaseMessage ) this.httpRequest.getMessage( );
    }
    return check( this.request );
  }
  
  public void setUser( User user ) {
    if ( user != null ) {
      EventRecord.caller( Context.class, EventType.CONTEXT_USER, this.correlationId, user.getUserId( ) ).debug( );
      this.user = user;
    }
  }
  
  public UserFullName getUserFullName( ) {
    return UserFullName.getInstance( this.getUser( ) );
  }
  
  public OwnerFullName getEffectiveUserFullName( ) {
    String effectiveUserId = this.getRequest( ).getEffectiveUserId( );
    if ( this.getRequest( ) != null && Principals.systemFullName( ).getUserName( ).equals( effectiveUserId ) ) {
      return Principals.systemFullName( );
      /** system **/
    } else if ( this.getRequest( ) == null || effectiveUserId == null ) {
      return Principals.nobodyFullName( );
      /** unset **/
    } else if ( !effectiveUserId.equals( this.getUserFullName( ).getUserName( ) ) ) {
      try {
        return UserFullName.getInstance( Accounts.lookupUserByName( effectiveUserId ) );
      } catch ( RuntimeException ex ) {
        LOG.error( ex );
        return UserFullName.getInstance( this.getUser( ) );
      } catch ( AuthException ex ) {
        LOG.error( ex, ex );
        return UserFullName.getInstance( this.getUser( ) );
      }
    } else {
      return UserFullName.getInstance( this.getUser( ) );
    }
  }
  
  public boolean hasAdministrativePrivileges( ) {
    return Principals.systemFullName().equals( this.getEffectiveUserFullName( ) ) || this.getUser( ).isSystemAdmin( );
  }
  
  public User getUser( ) {
    return check( this.user );
  }
  
  void setMuleEvent( MuleEvent event ) {
    if ( event != null && this.muleEvent.get( ) == null ) {
//      LOG.debug( EventType.CONTEXT_EVENT + " associated event context found for " + this.correlationId + " other corrId: " + event.getId( ) );
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
      this.subject = subject;
    }
  }
  
  void clear( ) {
    if ( this.muleEvent != null ) {
      this.muleEvent.clear( );
      this.muleEvent = null;
    }
    this.contracts.clear( );
  }
  
  private final static <TYPE> TYPE check( final TYPE obj ) {
    if ( obj == null ) {
      StackTraceElement steMethod = Thread.currentThread( ).getStackTrace( )[1];
      StackTraceElement steCaller = Thread.currentThread( ).getStackTrace( )[2];
      LOG.error( "Accessing context field when it is null: " + steMethod.getMethodName( ) + " from " + steCaller );
    }
    return obj;
  }
  
  public Map<Contract.Type, Contract> getContracts( ) {
    return this.contracts;
  }
  
  public Account getAccount( ) {
    try {
      return this.user.getAccount( );
    } catch ( AuthException ex ) {
      LOG.error( ex, ex );
      throw new IllegalStateException( "Context populated with ill-defined user:  no corresponding account found.", ex );
    }
  }
  
}
