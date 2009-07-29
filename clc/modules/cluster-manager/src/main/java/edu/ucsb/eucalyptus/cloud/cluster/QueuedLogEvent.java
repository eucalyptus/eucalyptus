package edu.ucsb.eucalyptus.cloud.cluster;

public class QueuedLogEvent<TYPE> extends QueuedEvent<TYPE>{
  public QueuedLogEvent( final QueuedEventCallback callback, final TYPE event )
  {
    super( callback, event );
  }
}
