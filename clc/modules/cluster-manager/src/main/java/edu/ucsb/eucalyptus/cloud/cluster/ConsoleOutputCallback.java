package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.msgs.*;

import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.ws.client.Client;
import com.eucalyptus.ws.util.Messaging;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import java.util.Date;

public class ConsoleOutputCallback extends QueuedEventCallback<GetConsoleOutputType> {

  private static Logger LOG = Logger.getLogger( ConsoleOutputCallback.class );

  public ConsoleOutputCallback( final ClusterConfiguration clusterConfig ) {
    super( clusterConfig );
  }

  public void process( final Client cluster, final GetConsoleOutputType msg ) throws Exception {
    GetConsoleOutputResponseType reply = ( GetConsoleOutputResponseType ) cluster.send( msg );
    VmInstance vm = VmInstances.getInstance().lookup( msg.getInstanceId() );
    String output = null;
    try {
      output = new String( Base64.decode( reply.getOutput().getBytes() ) );
      if ( !"EMPTY".equals( output ) )
        vm.getConsoleOutput().append( output );
    } catch ( ArrayIndexOutOfBoundsException e1 ) {}
    reply.setInstanceId( msg.getInstanceId() );
    reply.setTimestamp( new Date() );
    reply.setOutput( new String( Base64.encode( vm.getConsoleOutput().toString().getBytes() ) ) );
    Messaging.dispatch( "vm://ReplyQueue", reply );
  }

}
