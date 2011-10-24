package com.eucalyptus.ws.util;

import java.util.concurrent.ThreadFactory;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import com.eucalyptus.system.Threads;

public class ChannelUtil {
  private static Logger LOG = Logger.getLogger( ChannelUtil.class );
  
  static class SystemThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread( final Runnable r ) {
      return Threads.newThread( r, "channels" );
    }
  }
  
  public static ChannelFutureListener DISPATCH( Object o ) {
    return new DeferedWriter( o, ChannelFutureListener.CLOSE );
  }
  
  public static ChannelFutureListener WRITE_AND_CALLBACK( Object o, ChannelFutureListener callback ) {
    return new DeferedWriter( o, callback );
  }
  
  public static ChannelFutureListener WRITE( Object o ) {
    return new DeferedWriter( o, new ChannelFutureListener( ) {
      public void operationComplete( ChannelFuture future ) throws Exception {}
    } );
  }
  
  private static class DeferedWriter implements ChannelFutureListener {
    private Object                request;
    private ChannelFutureListener callback;
    
    DeferedWriter( final Object request, final ChannelFutureListener callback ) {
      this.callback = callback;
      this.request = request;
    }
    
    @Override
    public void operationComplete( ChannelFuture channelFuture ) {
      if ( channelFuture.isSuccess( ) ) {
        channelFuture.getChannel( ).write( request ).addListener( callback );
      } else {
        LOG.debug( channelFuture.getCause( ), channelFuture.getCause( ) );
        try {
          callback.operationComplete( channelFuture );
        } catch ( Exception e ) {
          LOG.debug( e, e );
        }
      }
    }
    
  }
  
}
