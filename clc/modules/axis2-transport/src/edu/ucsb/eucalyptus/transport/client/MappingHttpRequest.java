package edu.ucsb.eucalyptus.transport.client;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;

public class MappingHttpRequest extends DefaultHttpRequest {
  private Object sourceObject;
  private OMElement sourceElem;
  private SOAPEnvelope envelope;

  public MappingHttpRequest( final HttpVersion httpVersion, final HttpMethod httpMethod, final String uri, final Object source ) {
    super( httpVersion, httpMethod, uri );
    this.sourceObject = source;
  }

  public Object getSourceObject() {
    return sourceObject;
  }

  public void setSourceObject( final Object sourceObject ) {
    this.sourceObject = sourceObject;
  }

  public OMElement getSourceElem() {
    return sourceElem;
  }

  public void setSourceElem( final OMElement sourceElem ) {
    this.sourceElem = sourceElem;
  }

  public SOAPEnvelope getEnvelope() {
    return envelope;
  }

  public void setEnvelope( final SOAPEnvelope envelope ) {
    this.envelope = envelope;
  }
}
