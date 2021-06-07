/*************************************************************************
 * Copyright 2008 Regents of the University of California
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package edu.ucsb.eucalyptus.msgs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.JiBXException;

import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.empyrean.ServiceId;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class BaseMessage implements BaseMessageMarker {
  private String               correlationId;
  private String               userId;
  private String               effectiveUserId;
  private BaseCallerContext    callerContext;
  private Boolean              _return   = true;
  private Integer              _epoch;                                           //NOTE:GRZE: intentionally violating naming conventions to avoid shadowing/conflicts
  private ArrayList<ServiceId> _services = Lists.newArrayList( );                //NOTE:GRZE: intentionally violating naming conventions to avoid shadowing/conflicts
  private ArrayList<ServiceId> _disabledServices = Lists.newArrayList( );                //NOTE:GRZE: intentionally violating naming conventions to avoid shadowing/conflicts
  private ArrayList<ServiceId> _notreadyServices = Lists.newArrayList( );                //NOTE:GRZE: intentionally violating naming conventions to avoid shadowing/conflicts
  private ArrayList<ServiceId> _stoppedServices = Lists.newArrayList( );                //NOTE:GRZE: intentionally violating naming conventions to avoid shadowing/conflicts

  public BaseMessage( ) {
    super( );
    this.correlationId = UUID.randomUUID( ).toString( );
    this._return = true;
  }
  
  public BaseMessage( String userId ) {
    this( );
    this.userId = userId;
    this.effectiveUserId = userId;
  }
  
  public BaseMessage( BaseMessage copy ) {
    this( );
    this.effectiveUserId = copy != null ? copy.getEffectiveUserId( ) : null;
    this.correlationId = copy != null ? copy.getCorrelationId( ) : null;
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

  public Boolean get_return( boolean ifError ) {
    return get_return( );
  }

  @SuppressWarnings( "unchecked" )
  public <TYPE extends BaseMessage> TYPE markWinning( ) {
    this._return = true;
    return ( TYPE ) this;
  }
  
  @SuppressWarnings( "unchecked" )
  public <TYPE extends BaseMessage> TYPE markFailed( ) {
    this._return = false;
    return ( TYPE ) this;
  }
  
  @SuppressWarnings( "unchecked" )
  public <TYPE extends BaseMessage> TYPE markPrivileged( ) {
    this.effectiveUserId = Principals.systemUser( ).getName( );
    return ( TYPE ) this;
  }
  
  @SuppressWarnings( "unchecked" )
  public <TYPE extends BaseMessage> TYPE markUnprivileged( ) {
    this.effectiveUserId = this.userId;
    return ( TYPE ) this;
  }
  
  public void set_return( Boolean return1 ) {
    this._return = return1;
  }
  
  @Deprecated
  public void setEffectiveUserId( String effectiveUserId ) {
    this.effectiveUserId = effectiveUserId;
  }
  
  public String getEffectiveUserId( ) {
    return this.effectiveUserId;
  }

  public BaseCallerContext getCallerContext( ) {
    return callerContext;
  }

  public void setCallerContext( final BaseCallerContext callerContext ) {
    this.callerContext = callerContext;
  }

  /**
   * Creates a default SYSTEM generated message.
   * 
   * @param <TYPE>
   * @return
   */
  public <TYPE extends BaseMessage> TYPE regarding( ) {
    regarding( null );
    return ( TYPE ) this;
  }
  
  public <TYPE extends BaseMessage> TYPE regarding( BaseMessage msg ) {
    this.correlationId = UUID.randomUUID( ).toString( );
    this.userId = Principals.systemFullName( ).getUserName( );
    this.effectiveUserId = Principals.systemFullName( ).getUserName( );
    
    return ( TYPE ) this;
  }
  
  public <TYPE extends BaseMessage> TYPE regardingUserRequest( BaseMessage msg ) {
    this.userId = msg.userId;
    return ( TYPE ) this;  
  }
  
  public <TYPE extends BaseMessage> TYPE lookupAndSetCorrelationId(){
    String corrId = null;
    try{
      corrId = Contexts.lookup().getCorrelationId();
    }catch(final Exception ex){
      corrId = Threads.getCorrelationId();
    }
    if(corrId != null && corrId.length()>=36){
      return this.regardingRequestId(corrId);
    }else
      return ( TYPE ) this;
  }
  
  public <TYPE extends BaseMessage> TYPE regardingRequestId(final String msgId){
    if(msgId!=null){
      String requestId = null;
      String postfix = null;
      if (! msgId.contains("::")){
        requestId = msgId;
        postfix = requestId;
      }
      else {
        requestId = msgId.substring(0, msgId.indexOf("::"));
        postfix = msgId.substring(msgId.indexOf("::")+2);
      }
      String uuid = null;
      try{
        String baseHex = postfix.substring(9,13);
        Integer baseHexInt = Integer.parseInt(baseHex, 16);
        Integer newHexInt = (baseHexInt+1) % 65536;
        String newHex = Integer.toHexString(newHexInt);
        while(newHex.length()<4) {
          newHex = "0"+newHex;
        }
        uuid = UUID.randomUUID( ).toString( );
        uuid = uuid.substring(0, 9) + newHex + uuid.substring(13);
      }catch(final Exception ex){
        uuid = UUID.randomUUID( ).toString( );  
      }
      this.correlationId = String.format("%s::%s", requestId, uuid);
    }
    return ( TYPE ) this;
  }
  
  public boolean hasRequestId(){
    return this.correlationId!=null && this.correlationId.indexOf("::") > 0;
  }
    
  public String toString( ) {
    String str = null;
    try {
      str = BaseMessages.toString( this );
    } catch ( IOException e ) {
    }
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
    Class targetClass = Iterables.find( Classes.classAncestors( this ), new Predicate<Class>( ) {
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
    } catch ( Exception e ) {
      Logger.getLogger( BaseMessage.class ).error( e, e );
    }
    return temp.toString( );
  }
  
  public <TYPE extends BaseMessage> TYPE getReply( ) {
    Class msgClass = this.getClass( );
    while ( !msgClass.getSimpleName( ).endsWith( "Type" ) ) {
      msgClass = msgClass.getSuperclass( );
    }
    String replyType = msgClass.getName( ).replaceAll( "Type$", "" ) + "ResponseType";
    try {
      Class<TYPE> responseClass = (Class<TYPE>) ClassLoader.getSystemClassLoader( ).loadClass( replyType );
      return reply( responseClass.newInstance() );
    } catch ( Exception e ) {
      Logger.getLogger( BaseMessage.class ).debug( e, e );
      throw new TypeNotPresentException( this.correlationId, e );
    }
  }
   protected <TYPE extends BaseMessage> TYPE reply( TYPE reply ) {
    reply.setCorrelationId( this.correlationId );
    return reply;
  }

  public String toSimpleString( ) {
    StringBuilder buf = new StringBuilder( );
    buf.append( this.getClass( ).getSimpleName( ) )
       .append( ":" ).append( this.correlationId )
       .append( ":return=" ).append( this.get_return( ) )
       .append( ":epoch=" ).append( this.get_epoch( ) );
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
      } else if ( msge.getMessage( ) instanceof Supplier
          && ( ( Supplier ) msge.getMessage( ) ).get( ) instanceof BaseMessage ) {
        return ( T ) ( ( Supplier ) msge.getMessage( ) ).get( );
      } else {
        return null;
      }
    } else {
      return null;
    }
  }
  
  public BaseMessage setUser( User user ) {
    if ( user == null ) {
      this.setUser( Principals.nobodyUser( ) );
    } else {
      this.userId = user.getName( );
      this.effectiveUserId = user.isSystemAdmin( )
        ? Principals.systemUser( ).getName( )
        : user.getName( );
    }
    return this;
  }

  public ArrayList<ServiceId> get_disabledServices( ) {
    return this._disabledServices;
  }

  public void set_disabledServices( ArrayList<ServiceId> _disabledServices ) {
    this._disabledServices = _disabledServices;
  }

  public ArrayList<ServiceId> get_notreadyServices( ) {
    return this._notreadyServices;
  }

  public void set_notreadyServices( ArrayList<ServiceId> _notreadyServices ) {
    this._notreadyServices = _notreadyServices;
  }

  public ArrayList<ServiceId> get_stoppedServices() {
    return this._stoppedServices;
  }

  public void set_stoppedServices( final ArrayList<ServiceId> _stoppedServices ) {
    this._stoppedServices = _stoppedServices;
  }
}
