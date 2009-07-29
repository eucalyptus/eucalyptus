package edu.ucsb.eucalyptus.cloud.cluster;

import com.eucalyptus.ws.client.Client;
import org.apache.log4j.Logger;

public class QueuedEvent<TYPE> {
  private static Logger LOG = Logger.getLogger( QueuedEvent.class );

  private QueuedEventCallback<TYPE> callback;
  private TYPE event;

  public static <T> QueuedEvent<T> make( final QueuedEventCallback callback, final T event ) {
    return new QueuedEvent<T>(callback,event);
  }

  public QueuedEvent( final QueuedEventCallback callback, final TYPE event )
  {
    this.callback = callback;
    this.event = event;
  }

  public QueuedEventCallback getCallback()
  {
    return callback;
  }

  public TYPE getEvent()
  {
    return event;
  }

  public void trigger( Client cluster )
  {
    try
    {
      this.callback.process( cluster, this.event );
    }
    catch ( Exception e )
    {
      LOG.error( e );
      LOG.debug( e, e );
    }
  }

  @Override
  public boolean equals( final Object o )
  {
    if ( this == o ) return true;
    if ( !( o instanceof QueuedEvent ) ) return false;

    QueuedEvent that = ( QueuedEvent ) o;

    if ( !callback.equals( that.callback ) ) return false;
    if ( !event.equals( that.event ) ) return false;

    return true;
  }

  @Override
  public int hashCode()
  {
    int result = callback.hashCode();
    result = 31 * result + event.hashCode();
    return result;
  }
}
