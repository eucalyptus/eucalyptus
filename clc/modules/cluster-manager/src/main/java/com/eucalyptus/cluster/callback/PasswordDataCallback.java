package com.eucalyptus.cluster.callback;

import java.util.Date;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.util.Messaging;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.GetConsoleOutputResponseType;
import edu.ucsb.eucalyptus.msgs.GetConsoleOutputType;
import edu.ucsb.eucalyptus.msgs.GetPasswordDataResponseType;
import edu.ucsb.eucalyptus.msgs.GetPasswordDataType;

public class PasswordDataCallback extends QueuedEventCallback<GetConsoleOutputType,GetConsoleOutputResponseType> {
  
  private static Logger LOG = Logger.getLogger( ConsoleOutputCallback.class );
  private final GetPasswordDataType msg;
  public PasswordDataCallback( GetPasswordDataType msg ) {
    this.msg = msg;
    GetConsoleOutputType consoleOutput = new GetConsoleOutputType( ).regardingUserRequest( msg );
    consoleOutput.setInstanceId( msg.getInstanceId( ) );
    this.setRequest( consoleOutput );
  }
  
  @Override
  public void prepare( GetConsoleOutputType msg ) throws Exception {}
  
  @Override
  public void verify( BaseMessage msg ) throws Exception {
    this.verify( ( GetConsoleOutputResponseType ) msg );
  }
  
  public void verify( GetConsoleOutputResponseType reply ) throws Exception {
    VmInstance vm = VmInstances.getInstance( ).lookup( this.getRequest( ).getInstanceId( ) );
    String output = null;
    try {
      output = new String( Base64.decode( reply.getOutput( ).getBytes( ) ) );
      if ( !"EMPTY".equals( output ) ) vm.setConsoleOutput( new StringBuffer().append( output ) );
    } catch ( ArrayIndexOutOfBoundsException e1 ) {}
    GetPasswordDataResponseType rep = this.msg.getReply( );
    rep.setInstanceId( this.getRequest( ).getInstanceId( ) );
    rep.setTimestamp( new Date( ) );
    if( vm.getPasswordData( ) != null ) {
      rep.setOutput( vm.getPasswordData( ) );
    } else {
      rep.setOutput( null );
    }
    Messaging.dispatch( "vm://ReplyQueue", rep );
  }


  @Override
  public void fail( Throwable e ) {
    LOG.debug( LogUtil.subheader( this.getRequest( ).toString( "eucalyptus_ucsb_edu" ) ) );
    LOG.debug( e, e );
  }

}
