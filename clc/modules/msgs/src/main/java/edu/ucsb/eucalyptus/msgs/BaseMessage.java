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
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.FakePrincipals;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ComponentMessage;
import com.eucalyptus.component.Topology;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.empyrean.ServiceId;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.HasFullName;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

public class BaseMessage {
  @Transient
  private static Logger        LOG      = Logger.getLogger( BaseMessage.class );
  private String               correlationId;
  private String               userId;
  private String               effectiveUserId;
  private Boolean              _return  = true;
  private String               statusMessage;
  private Integer              _epoch;//NOTE:GRZE: intentionally violating naming conventions to avoid shadowing/conflicts
  private ArrayList<ServiceId> _services = Lists.newArrayList( );//NOTE:GRZE: intentionally violating naming conventions to avoid shadowing/conflicts
  
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
    this.effectiveUserId = copy.getEffectiveUserId( );
    this.correlationId = copy.getCorrelationId( );
  }
  
  public String getCorrelationId( ) {
    if ( this.correlationId == null ) {
      Logger.getLogger( "EXHAUST" ).error( Exceptions.filterStackTrace( new RuntimeException( "Creating UUID for message which did not have it set correctly: "
                                                                                              + this.getClass( ) ) ) );
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
  
  @Deprecated
  public String getUserId( ) {
    return this.userId;
  }
  
  public Boolean get_return( ) {
    return this._return;
  }
  
  public <TYPE extends BaseMessage> TYPE markFailed( ) {
    this._return = false;
    return ( TYPE ) this;
  }
  
  public <TYPE extends BaseMessage> TYPE markPrivileged( ) {
    this.effectiveUserId = FakePrincipals.systemUser().getName( );
    return ( TYPE ) this;
  }
  
  public <TYPE extends BaseMessage> TYPE markUnprivileged( ) {
    this.effectiveUserId = this.userId;
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
   * 
   * @param <TYPE>
   * @return
   */
  public <TYPE extends BaseMessage> TYPE regarding( ) {
    regarding( null, null );
    return ( TYPE ) this;
  }
  
  public <TYPE extends BaseMessage> TYPE regarding( BaseMessage msg ) {
    return ( TYPE ) regarding( msg, String.format( "%f", Math.random( ) ).substring( 2 ) );
  }
  
  public <TYPE extends BaseMessage> TYPE regardingUserRequest( BaseMessage msg ) {
    return ( TYPE ) regardingUserRequest( msg, String.format( "%f", Math.random( ) ).substring( 2 ) );
  }
  
  public <TYPE extends BaseMessage> TYPE regarding( BaseMessage msg, String subCorrelationId ) {
    String corrId = null;
    if ( msg == null ) {
      this.correlationId = UUID.randomUUID( ).toString( );
    } else {
      corrId = msg.correlationId;
    }
    if ( subCorrelationId == null ) {
      subCorrelationId = String.format( "%f", Math.random( ) ).substring( 2 );
    }
    this.userId = FakePrincipals.systemFullName().getUserName( );
    this.effectiveUserId = FakePrincipals.systemFullName().getUserName( );
    this.correlationId = corrId + "-" + subCorrelationId;
    return ( TYPE ) this;
  }
  
  public <TYPE extends BaseMessage> TYPE regardingUserRequest( BaseMessage msg, String subCorrelationId ) {
    this.correlationId = msg.getCorrelationId( ) + "-" + subCorrelationId;
    this.userId = msg.userId;
    return ( TYPE ) this;
  }
  
  @Deprecated
  /** this cannot work correctly anymore **/
  private boolean isAdministrator( ) {
//    return ( FakePrincipals.SYSTEM_USER_ERN.getUserName( ).equals( this.effectiveUserId ) ) || this.getUser( ).isSystemAdmin( )
//           || this.getUser( ).isSystemInternal( );
    throw new RuntimeException( "This method is deprecated: use com.eucalyptus.context.Contexts.lookup().hasAdministrativePrivileges() instead." );
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
    Class targetClass = Classes.findAncestor( this, new Predicate<Class>( ) {
      @Override
      public boolean apply( Class arg0 ) {
        return !arg0.isAnonymousClass( );
      }
    } );
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
      reply.correlationId = this.correlationId;
    } catch ( Exception e ) {
      Logger.getLogger( BaseMessage.class ).debug( e, e );
      throw new TypeNotPresentException( this.correlationId, e );
    }
    return reply;
  }
  
  public String toSimpleString( ) {
    StringBuilder buf = new StringBuilder( );
    buf.append( this.getClass( ).getSimpleName( ) )
       .append( ":" ).append( this.correlationId )
       .append( ":" ).append( this.userId )
       .append( ":" ).append( this.effectiveUserId )
       .append( ":return=" ).append( this.get_return( ) )
       .append( ":status=" ).append( this.getStatusMessage( ) );
    return buf.toString( );
  }
  
  /**
   * @return the epoch
   */
  public Integer get_epoch( ) {
    return this._epoch;
  }
  
  /**
   * @param epoch the epoch to set
   */
  public void set_epoch( Integer epoch ) {
    this._epoch = epoch;
  }
  
  /**
   * @return the services
   */
  public ArrayList<ServiceId> get_services( ) {
    return this._services;
  }

  /**
   * @deprecated use get_services( ) as needed, this old name presents a potential naming conflict
   * @see #get_services()
   */
  @Deprecated
  public ArrayList<ServiceId> getBaseServices( ) {
    return this._services;
  }
  
  /**
   * @param services the services to set
   */
  public void set_services( ArrayList<ServiceId> services ) {
    this._services = services;
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
    if ( user == null ) {
      this.setUser( FakePrincipals.nobodyUser() );
    } else {
      this.userId = user.getName( );
      this.effectiveUserId = ( user.isSystemAdmin( ) || user.isSystemInternal( ) )
        ? FakePrincipals.systemUser().getName( )
        : user.getName( );
    }
    return this;
  }
  
  /**
   * @deprecated
   * @see {@link Context#getAccount()}
   */
  public Account getAccount( ) {
    throw new RuntimeException( "This method is deprecated: use com.eucalyptus.context.Contexts.lookup().getAccount() instead." );
  }
  
  /**
   * @deprecated
   * @see {@link Context#getUser()}
   */
  @Deprecated
  public User getUser( ) {
    throw new RuntimeException( "This method is deprecated: use com.eucalyptus.context.Contexts.lookup().getUser() instead." );
  }
  
  @Deprecated
  private UserFullName getUserErn( ) {
    throw new RuntimeException( "This method is deprecated: use com.eucalyptus.context.Contexts.lookup().getUserFullName() instead." );
  }
}
