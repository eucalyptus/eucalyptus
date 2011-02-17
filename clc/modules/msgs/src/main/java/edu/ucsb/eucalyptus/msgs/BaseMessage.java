package edu.ucsb.eucalyptus.msgs;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.UUID;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.JiBXException;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.empyrean.ServiceInfoType;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.util.FullName;
import com.google.common.collect.Lists;

public class BaseMessage {
  @Transient
  User                       user;
  String                     correlationId;
  String                     userId;
  String                     effectiveUserId;
  Boolean                    _return      = true;
  String                     statusMessage;
  Integer                    epoch        = currentEpoch++;
  ArrayList<ServiceInfoType> services     = Lists.newArrayList( );
  private Account account;
  private static Integer     currentEpoch = 0;
  
  public BaseMessage( ) {
    super( );
    this.correlationId = UUID.randomUUID( ).toString( );
    this._return = true;
  }
  
  public BaseMessage( String userId ) {
    this( );
    this.userId = userId;
    this.effectiveUserId = userId;
    this.statusMessage = null;
  }
  
  public BaseMessage( BaseMessage copy ) {
    this( );
    this.setUserId( copy.getUserId( ) );
    this.effectiveUserId = copy.getEffectiveUserId( );
    this.correlationId = copy.getCorrelationId( );
  }
  
  public String getCorrelationId( ) {
    if ( this.correlationId == null ) {
      return ( this.correlationId = UUID.randomUUID( ).toString( ) );
    } else {
      return this.correlationId;
    }
  }
  
  public void setCorrelationId( String correlationId ) {
    this.correlationId = correlationId;
  }
  
  @Deprecated
  public void setUserId( String userId ) {
    this.userId = userId;
  }

  public String getUserId( ) {
    if( this.user == null ) {
      return "unknown";
    } else {
      return this.user.getId( );
    }
  }
  
  public Boolean get_return( ) {
    return this._return;
  }
  
  public <TYPE extends BaseMessage> TYPE markFailed( ) {
    this._return = false;
    return ( TYPE ) this;
  }
  
  public <TYPE extends BaseMessage> TYPE markPrivileged( ) {
    this.effectiveUserId = User.SYSTEM.getName( );
    return ( TYPE ) this;
  }

  public <TYPE extends BaseMessage> TYPE markUnprivileged( ) {
    this.effectiveUserId = this.user.getName( );
    return ( TYPE ) this;
  }

  public void set_return( Boolean return1 ) {
    this._return = return1;
  }
  
  public String getStatusMessage( ) {
    return this.statusMessage;
  }
  
  public void setStatusMessage( String statusMessage ) {
    this.statusMessage = statusMessage;
  }
  
  @Deprecated
  public void setEffectiveUserId( String effectiveUserId ) {
    this.effectiveUserId = effectiveUserId;
  }

  public String getEffectiveUserId( ) {
    return this.effectiveUserId;
  }
  
  /**
   * Creates a default SYSTEM generated message.
   * @param <TYPE>
   * @return
   */
  public <TYPE extends BaseMessage> TYPE regarding( ) {
    this.setUser( User.SYSTEM );
    return ( TYPE ) this;
  }
  
  public <TYPE extends BaseMessage> TYPE regarding( BaseMessage msg ) {
    return ( TYPE ) regarding( msg, String.format( "%f", Math.random( ) ).substring( 2 ) );
  }
  
  public <TYPE extends BaseMessage> TYPE regardingUserRequest( BaseMessage msg ) {
    return ( TYPE ) regardingUserRequest( msg, String.format( "%f", Math.random( ) ).substring( 2 ) );
  }
  
  public <TYPE extends BaseMessage> TYPE regarding( BaseMessage msg, String subCorrelationId ) {
    this.correlationId = msg.getCorrelationId( ) + "-" + subCorrelationId;
    return ( TYPE ) regarding( );
  }
  
  public <TYPE extends BaseMessage> TYPE regardingUserRequest( BaseMessage msg, String subCorrelationId ) {
    this.correlationId = msg.getCorrelationId( ) + "-" + subCorrelationId;
    this.setUser( msg.getUser( ) );
    return ( TYPE ) this;
  }
  
  public boolean isAdministrator( ) {
    return ( User.SYSTEM.getName( ).equals( this.effectiveUserId ) ) || this.user.isSystemAdmin( ) || this.user.isSystemInternal( );
  }
  
  public String toString( ) {
    String str = this.toString( "msgs_eucalyptus_com" );
    str = ( str != null )
      ? str
      : this.toString( "eucalyptus_ucsb_edu" );
    str = ( str != null )
      ? str
      : "Failed to bind message of type: " + this.getClass( ).getName( ) + " at "
                                  + Thread.currentThread( ).getStackTrace( )[1].toString( );
    return str;
  }
  
  /**
   * Get the XML form of the message.
   * 
   * @param namespace
   * @return String representation of the object, null if binding fails.
   */
  public String toString( String namespace ) {
    ByteArrayOutputStream temp = new ByteArrayOutputStream( );
    Class targetClass = this.getClass( );
    try {
      IBindingFactory bindingFactory = BindingDirectory.getFactory( namespace, targetClass );
      IMarshallingContext mctx = bindingFactory.createMarshallingContext( );
      mctx.setIndent( 2 );
      mctx.marshalDocument( this, "UTF-8", null, temp );
    } catch ( JiBXException e ) {
      Logger.getLogger( BaseMessage.class ).debug( e, e );
    } catch ( Throwable e ) {
      Logger.getLogger( BaseMessage.class ).error( e, e );
    }
    return temp.toString( );
  }
  
  public <TYPE extends BaseMessage> TYPE getReply( ) {
    Class msgClass = this.getClass( );
    while ( !msgClass.getSimpleName( ).endsWith( "Type" ) ) {
      msgClass = msgClass.getSuperclass( );
    }
    TYPE reply = null;
    String replyType = msgClass.getName( ).replaceAll( "Type", "" ) + "ResponseType";
    try {
      Class responseClass = ClassLoader.getSystemClassLoader( ).loadClass( replyType );
      reply = ( TYPE ) responseClass.newInstance( );
    } catch ( Exception e ) {
      Logger.getLogger( BaseMessage.class ).debug( e, e );
      throw new TypeNotPresentException( this.correlationId, e );
    }
    reply.setCorrelationId( this.getCorrelationId( ) );
    reply.setUser( this.user );
    return reply;
  }
  
  public String toSimpleString( ) {
    return String.format( "%s:%s:%s:%s:%s:%s", this.getClass( ).getSimpleName( ), this.getCorrelationId( ), this.account.getName( ), this.getUser( ).getName( ), this.effectiveUserId,
                          this.get_return( ), this.getStatusMessage( ) );
  }
  
  /**
   * @return the epoch
   */
  public Integer getBaseEpoch( ) {
    return this.epoch;
  }
  
  /**
   * @param epoch the epoch to set
   */
  public void setBaseEpoch( Integer epoch ) {
    this.epoch = epoch;
  }
  
  /**
   * @return the services
   */
  public ArrayList<ServiceInfoType> getBaseServices( ) {
    return this.services;
  }
  
  /**
   * @param services the services to set
   */
  public void setBaseServices( ArrayList<ServiceInfoType> services ) {
    this.services = services;
  }
  
  /**
   * Get the message from within a ChannelEvent. Returns null if no message found.
   * 
   * @param <T>
   * @param e
   * @return message or null if no msg.
   */
  public static <T extends BaseMessage> T extractMessage( ChannelEvent e ) {
    if ( e instanceof MessageEvent ) {
      final MessageEvent msge = ( MessageEvent ) e;
      MappingHttpMessage msgHttp = null;
      if ( msge.getMessage( ) instanceof BaseMessage ) {
        return ( T ) msge.getMessage( );
      } else if ( msge.getMessage( ) instanceof MappingHttpMessage
                  && ( msgHttp = ( MappingHttpMessage ) msge.getMessage( ) ).getMessage( ) instanceof BaseMessage ) {
        return ( T ) msgHttp.getMessage( );
      } else {
        return null;
      }
    } else {
      return null;
    }
  }
  
  public BaseMessage setUser( User user ) {
    if( user == null ) {
      this.account = null;
      this.user = null;
      this.userId = null;
      this.effectiveUserId = null;
    } else {
      try {
        this.account = user.getAccount( );
      } catch ( AuthException ex ) {
      }
      this.user = user;
      this.userId = user.getName( );
      this.effectiveUserId = this.isAdministrator( ) ? User.SYSTEM.getName( ) : user.getName( );
    }
    return this;
  }

  public User getUser( ) {
    return this.user;
  }

  public FullName getUserErn( ) {
    return UserFullName.get( this.user );
  }
}
