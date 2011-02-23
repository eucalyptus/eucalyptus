package com.eucalyptus.util.async;

import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import com.eucalyptus.component.ServiceEndpoint;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public interface RequestHandler<Q extends BaseMessage, R extends BaseMessage> extends ChannelUpstreamHandler, ChannelDownstreamHandler {
  
  public abstract boolean fire( final ServiceEndpoint serviceEndpoint, final ChannelPipelineFactory factory, final Q request );
  
}
