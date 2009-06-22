package edu.ucsb.eucalyptus.transport.client;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;

import java.util.UUID;

@ChannelPipelineCoverage("all")
public class SoapHandler extends MessageStackHandler {
    private static Logger LOG = Logger.getLogger( SoapHandler.class );

  static final String WSA_NAMESPACE = "http://www.w3.org/2005/08/addressing";
  static final String WSA_NAMESPACE_PREFIX = "wsa";
  static final String WSA_MESSAGE_ID = "MessageID";
  static final String WSA_RELATES_TO = "RelatesTo";
  static final String WSA_RELATES_TO_RELATIONSHIP_TYPE = "RelationshipType";
  static final String WSA_TO = "To";
  static final String WSA_REPLY_TO = "ReplyTo";
  static final String WSA_FROM = "From";
  static final String WSA_FAULT_TO = "FaultTo";
  static final String WSA_ACTION = "Action";
  static final String EPR_SERVICE_NAME = "ServiceName";
  static final String EPR_REFERENCE_PARAMETERS = "ReferenceParameters";

  //:: worthwile to make this configurable later? :://
  private SOAPFactory soapFactory = OMAbstractFactory.getSOAP11Factory();

  public void outgoingMessage( final MessageEvent event ) throws Exception {
    Object o = event.getMessage();
    if( o instanceof MappingHttpRequest ) {
      MappingHttpRequest httpRequest = (MappingHttpRequest) o;
      //:: assert sourceElem != null :://
      httpRequest.setEnvelope( soapFactory.getDefaultEnvelope() );
      httpRequest.getEnvelope().getBody().addChild( httpRequest.getSourceElem() );
      String action = httpRequest.getSourceElem().getLocalName();
      httpRequest.addHeader( "SOAPAction", action );
      SOAPHeader header = httpRequest.getEnvelope().getHeader();

      OMNamespace wsaNs = OMAbstractFactory.getOMFactory().createOMNamespace( WSA_NAMESPACE, WSA_NAMESPACE_PREFIX );
      SOAPHeaderBlock wsaToHeader = header.addHeaderBlock( WSA_TO, wsaNs );
      wsaToHeader.setText( httpRequest.getUri() );
      SOAPHeaderBlock wsaActionHeader = header.addHeaderBlock( WSA_ACTION, wsaNs );
      wsaActionHeader.setText( action );
      SOAPHeaderBlock wsaMsgId = header.addHeaderBlock( WSA_MESSAGE_ID, wsaNs );
      wsaMsgId.setText( "urn:uuid:" + UUID.randomUUID().toString().replaceAll( "-", "" ).toUpperCase() );
    }
  }

}
