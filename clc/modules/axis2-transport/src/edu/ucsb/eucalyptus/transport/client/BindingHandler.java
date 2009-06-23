package edu.ucsb.eucalyptus.transport.client;

import edu.ucsb.eucalyptus.transport.binding.Binding;
import org.apache.axiom.om.OMElement;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;

@ChannelPipelineCoverage("all")
public class BindingHandler extends MessageStackHandler {
    private static Logger LOG = Logger.getLogger( BindingHandler.class );

  private Binding binding;

  public BindingHandler( final Binding binding ) {
    this.binding = binding;
  }

  public void incomingMessage( final MessageEvent event ) throws Exception {
  }

  public void outgoingMessage( final MessageEvent event ) throws Exception {
    Object o = event.getMessage();
    if( o instanceof MappingHttpRequest ) {
      MappingHttpRequest httpRequest = (MappingHttpRequest) o;
      OMElement omElem = this.binding.toOM( httpRequest.getSourceObject() );
      httpRequest.setSourceElem( omElem );
    }
  }

}
