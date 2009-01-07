package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.transport.client.Client;
import org.apache.log4j.Logger;

import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;

public class NodeLogCallback extends QueuedEventCallback<GetLogsType> implements Runnable {

  private static Logger LOG = Logger.getLogger( NodeLogCallback.class );
  private static int SLEEP_TIMER = 60 * 1000;
  private Cluster parent;
  private NavigableSet<NodeLogInfo> results = null;
  private NavigableSet<GetLogsType> requests = null;

  public NodeLogCallback( final Cluster parent )
  {
    this.parent = parent;
    this.results = new ConcurrentSkipListSet<NodeLogInfo>();
    this.requests = new ConcurrentSkipListSet<GetLogsType>();
  }

  public void process( final Client cluster, final GetLogsType msg ) throws Exception
  {
    //:: TODO-1.4: enable this again for testing :://
//    try
//    {
//      GetLogsResponseType reply = ( GetLogsResponseType ) cluster.send( msg );
//      NodeLogInfo logInfo = reply.getLogs();
//      logInfo.setServiceTag( logInfo.getServiceTag().replaceAll( "EucalyptusGL", "EucalyptusNC" ) );
    //:: REMEMBER TO DO BASE64 DECODE HERE ::/
//      results.add( logInfo );
      requests.remove( msg );
//    }
//    catch ( AxisFault axisFault )
//    {
//      LOG.error( axisFault, axisFault );
//    }
  }

  @Override
  protected void notifyHandler() {
    if( requests.isEmpty() ) super.notifyHandler();    
  }

  public void run()
  {
    do
    {
      if ( !this.parent.getNodeTags().isEmpty() )
      {
        for ( String serviceTag : this.parent.getNodeTags() )
        {
          GetLogsType msg = new GetLogsType( serviceTag.replaceAll( "EucalyptusNC", "EucalyptusGL" ) );
          this.requests.add( msg );
          this.parent.getMessageQueue().enqueue( new QueuedLogEvent( this, msg ) );
        }
        this.waitForEvent();
        this.parent.updateNodeLogs( results );
        this.results.clear();
      }
    } while ( !this.isStopped() && this.sleep( SLEEP_TIMER ) );
  }

}
