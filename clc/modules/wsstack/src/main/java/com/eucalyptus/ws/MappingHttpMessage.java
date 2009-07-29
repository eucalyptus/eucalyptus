package com.eucalyptus.ws;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.jboss.netty.handler.codec.http.DefaultHttpMessage;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpVersion;

public abstract class MappingHttpMessage extends DefaultHttpMessage implements HttpMessage {

  private String       messageString;
  private SOAPEnvelope soapEnvelope;
  private OMElement    omMessage;
  private Object       message;

  protected MappingHttpMessage( HttpVersion version ) {
    super( version );
  }

  public SOAPEnvelope getSoapEnvelope( ) {
    return soapEnvelope;
  }

  public void setSoapEnvelope( SOAPEnvelope soapEnvelope ) {
    this.soapEnvelope = soapEnvelope;
  }

  public OMElement getOmMessage( ) {
    return omMessage;
  }

  public void setOmMessage( OMElement omMessage ) {
    this.omMessage = omMessage;
  }

  public Object getMessage( ) {
    return message;
  }

  public void setMessage( Object message ) {
    this.message = message;
  }

  public String getMessageString( ) {
    return messageString;
  }

  public void setMessageString( String messageString ) {
    this.messageString = messageString;
  }

}
