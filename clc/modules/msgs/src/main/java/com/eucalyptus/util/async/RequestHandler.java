package com.eucalyptus.util.async;

import org.jboss.netty.channel.ChannelUpstreamHandler;
import com.eucalyptus.component.ServiceConfiguration;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public interface RequestHandler<Q extends BaseMessage, R extends BaseMessage> extends ChannelUpstreamHandler {
  
  public abstract boolean fire( final ServiceConfiguration serviceConfig, final Q request );
  
}
