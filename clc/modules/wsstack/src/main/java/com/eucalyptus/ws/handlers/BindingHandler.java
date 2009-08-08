package com.eucalyptus.ws.handlers;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.ws.MappingHttpMessage;
import com.eucalyptus.ws.WebServicesException;
import com.eucalyptus.ws.binding.Binding;
import com.eucalyptus.ws.binding.BindingManager;

import edu.ucsb.eucalyptus.cloud.entities.UserInfo;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

@ChannelPipelineCoverage( "all" )
public class BindingHandler extends MessageStackHandler {
  private static Logger LOG = Logger.getLogger( BindingHandler.class );

  private Binding binding;
 
  public BindingHandler( ) {
    super( );
  }

  public BindingHandler( final Binding binding ) {
    this.binding = binding;
  }

  @Override
  public void incomingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpMessage ) {
      MappingHttpMessage httpMessage = ( MappingHttpMessage ) event.getMessage( );
      //:: TODO: need an index of message types based on name space :://
      Class msgType = Class.forName( "edu.ucsb.eucalyptus.msgs." + httpMessage.getOmMessage( ).getLocalName( ) + "Type" );
      EucalyptusMessage msg = null;
      OMElement elem = httpMessage.getOmMessage( );
      OMNamespace omNs = elem.getNamespace( );
      String namespace = omNs.getNamespaceURI( );
      try {
        this.binding = BindingManager.getBinding( BindingManager.sanitizeNamespace( namespace ) );
      } catch ( Exception e1 ) {
        if( this.binding == null ) {
          throw new WebServicesException(e1);
        }
      }
      try {
        msg = ( EucalyptusMessage ) this.binding.fromOM( httpMessage.getOmMessage( ), msgType );
		/*UserInfo user = new UserInfo("admin");
		user.setIsAdministrator(Boolean.TRUE);
		msg.setUserId( user.getUserName() );
		msg.setEffectiveUserId( user.isAdministrator() ? "eucalyptus" : user.getUserName() );*/

      } catch ( Exception e1 ) {
        LOG.fatal( "FAILED TO PARSE:\n" + httpMessage.getMessageString( ) );
        throw new WebServicesException(e1);
      }
      httpMessage.setMessage( msg );
    }
  }

  @Override
  public void outgoingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpMessage ) {
      MappingHttpMessage httpRequest = ( MappingHttpMessage ) event.getMessage( );
      if( httpRequest.getMessage( ) instanceof EucalyptusErrorMessageType ) {
        return;
      }
      Class targetClass = httpRequest.getMessage( ).getClass( );
      while ( !targetClass.getSimpleName( ).endsWith( "Type" ) )
        targetClass = targetClass.getSuperclass( );
      Class responseClass = Class.forName( targetClass.getName( ) );
      ctx.setAttachment( responseClass );
      OMElement omElem = this.binding.toOM( httpRequest.getMessage( ) );
      httpRequest.setOmMessage( omElem );
    }
  }

}
