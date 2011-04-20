package com.eucalyptus.util.fsm;

import org.jboss.netty.channel.ChannelPipelineFactory;
import com.eucalyptus.component.ServiceEndpoint;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.async.Callback;
import com.eucalyptus.util.async.Callbacks;
import com.eucalyptus.util.async.RemoteCallback;
import com.eucalyptus.util.async.SubjectRemoteCallbackFactory;

public class FusedCallbackTransition<P extends HasName<P>, S extends Enum<S>, T extends Enum<T>> extends AbstractTransitionAction<P> {
  private final SubjectRemoteCallbackFactory<RemoteCallback, ServiceEndpoint> msgFactory;
  private final ChannelPipelineFactory                                        channelFactory;
  
  public FusedCallbackTransition( ChannelPipelineFactory channelFactory, SubjectRemoteCallbackFactory<RemoteCallback, ServiceEndpoint> msgFactory ) {
    this.msgFactory = msgFactory;
    this.channelFactory = channelFactory;
  }
  
  /**
   * @see com.eucalyptus.util.fsm.SplitTransition#enter(com.eucalyptus.util.HasName)
   * @param parent
   */
  @Override
  public void enter( P parent ) {}
  
  /**
   * @see com.eucalyptus.util.fsm.SplitTransition#leave(com.eucalyptus.util.HasName,
   *      com.eucalyptus.util.async.Callback.Completion)
   * @param parent
   * @param transitionCallback
   */
  @Override
  public final void leave( P parent, final Callback.Completion transitionCallback ) {
    Callbacks.newRequest( msgFactory.newInstance( ) ).then( new Callback.Completion( ) {
      
      @Override
      public void fire( ) {
        transitionCallback.fire( );
      }
      
      @Override
      public void fireException( Throwable t ) {
        transitionCallback.fireException( t );
      }
    } ).dispatch( parent.getName( ) );
  }
}
