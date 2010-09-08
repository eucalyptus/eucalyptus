package edu.ucsb.eucalyptus.msgs;

import java.io.ByteArrayOutputStream;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.JiBXException;

public class BaseMessage {
  String                correlationId;
  String                userId;
  String                effectiveUserId;
  Boolean               _return;
  String                statusMessage;
  
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
  public String getCorrelationId( ) {
    return this.correlationId;
  }
  public void setCorrelationId( String correlationId ) {
    this.correlationId = correlationId;
  }
  public String getUserId( ) {
    return this.userId;
  }
  public void setUserId( String userId ) {
    this.userId = userId;
  }
  public void setEffectiveUserId( String effectiveUserId ) {
    this.effectiveUserId = effectiveUserId;
  }
  public Boolean get_return( ) {
    return this._return;
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
  
  public String getEffectiveUserId( ) {
    if ( isAdministrator( ) ) return "eucalyptus";
    return effectiveUserId;
  }
  
  public <TYPE extends BaseMessage> TYPE regarding( ) {
    this.userId = "eucalyptus";
    this.effectiveUserId = "eucalyptus";
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
    this.userId = msg.getUserId( );
    this.effectiveUserId = msg.getEffectiveUserId( );
    return ( TYPE ) this;
  }
  
  public boolean isAdministrator( ) {
    return "eucalyptus".equals( this.effectiveUserId );
  }
  
  public String toString( ) {
    String str = this.toString( "msgs_eucalyptus_com" );
    str = ( str != null ) ? str : this.toString( "eucalyptus_ucsb_edu" );
    str = ( str != null ) ? str : "Failed to bind message of type: " + this.getClass( ).getName( ) + " at "
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
//    while ( !targetClass.getSimpleName( ).endsWith( "Type" ) && BaseMessage.class.equals( targetClass ) ) {
//      targetClass = targetClass.getSuperclass( );
//    }
    try {
      IBindingFactory bindingFactory = BindingDirectory.getFactory( namespace, targetClass );
      IMarshallingContext mctx = bindingFactory.createMarshallingContext( );
      mctx.setIndent( 2 );
      mctx.marshalDocument( this, "UTF-8", null, temp );
    } catch ( JiBXException e ) {
      Logger.getLogger(BaseMessage.class).debug( e, e );
      return null;
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
      Logger.getLogger(BaseMessage.class).debug( e, e );
      throw new TypeNotPresentException( correlationId, e );
    }
    reply.setCorrelationId( this.getCorrelationId( ) );
    reply.setUserId( this.getUserId( ) );
    reply.setEffectiveUserId( this.getEffectiveUserId( ) );
    return reply;
  }
  
  public String toSimpleString( ) {
    return String.format("%s:%s:%s:%s:%s:%s", this.getClass( ).getSimpleName( ), this.getCorrelationId( ), this.getUserId( ), this.getEffectiveUserId( ), this.get_return( ), this.getStatusMessage( ) );
  }
}
