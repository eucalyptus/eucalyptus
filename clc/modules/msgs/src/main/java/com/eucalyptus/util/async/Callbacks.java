package com.eucalyptus.util.async;

import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipelineFactory;
import com.google.common.base.Predicate;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class Callbacks {
  private static Logger LOG = Logger.getLogger( Callbacks.class );
  
  public static Callback forCallable( final Callable callable ) {
    return new Callback( ) {
      
      @Override
      public void fire( Object t ) {
        try {
          callable.call( );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    };
  }
  
  public static Callback forRunnable( final Runnable runnable ) {
    return new Callback( ) {
      
      @Override
      public void fire( Object t ) {
        try {
          runnable.run( );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    };
  }
  
  public static <T extends RemoteCallback<Q, R>, Q extends BaseMessage, R extends BaseMessage> RemoteCallbackFactory<T> newMessageFactory( final Class<T> callbackClass ) {
    return new RemoteCallbackFactory( ) {
      @Override
      public T newInstance( ) {
        T cb = Callbacks.newInstance( callbackClass );
        return cb;
      }
    };
  }
  
  public static <P, T extends SubjectMessageCallback<P, Q, R>, Q extends BaseMessage, R extends BaseMessage> SubjectRemoteCallbackFactory<T, P> newSubjectMessageFactory( final Class<T> callbackClass, final P subject ) {
    return new SubjectRemoteCallbackFactory( ) {
      @Override
      public T newInstance( ) {
        T cb = Callbacks.newInstance( callbackClass );
        cb.setSubject( subject );
        return cb;
      }
      
      @Override
      public P getSubject( ) {
        return subject;
      }
    };
  }
  
  public static <T extends RemoteCallback> T newInstance( Class<T> callbackClass ) {
    try {
      T callback = callbackClass.newInstance( );
      return callback;
    } catch ( Throwable t ) {
      LOG.error( t, t );
      throw new RuntimeException( t );
    }
  }
  
  public static <A extends BaseMessage, B extends BaseMessage> Request<A, B> newRequest( final RemoteCallback<A, B> msgCallback ) {
    return new AsyncRequest( msgCallback ) {
      {
        setRequest( msgCallback.getRequest( ) );
      }
    };
  }
  
  public static <A extends BaseMessage, B extends BaseMessage> Request<A, B> newClusterRequest( final RemoteCallback<A, B> msgCallback ) {
    return newRequest( msgCallback );
  }
  
  public static <T> Callback<T> noop( ) {
    return new NoopCallback<T>( );
  }
  
  private static final class NoopCallback<T> implements Callback<T> {
    @Override
    public final void fire( T t ) {}
  }
  
}
