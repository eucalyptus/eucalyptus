package com.eucalyptus.cluster.callback;

import java.util.Date;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.async.MessageCallback;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import edu.ucsb.eucalyptus.msgs.GetConsoleOutputResponseType;
import edu.ucsb.eucalyptus.msgs.GetConsoleOutputType;
import edu.ucsb.eucalyptus.msgs.GetPasswordDataResponseType;
import edu.ucsb.eucalyptus.msgs.GetPasswordDataType;

public class PasswordDataCallback extends MessageCallback<GetConsoleOutputType,GetConsoleOutputResponseType> {
  
  private static Logger LOG = Logger.getLogger( ConsoleOutputCallback.class );
  private final GetPasswordDataType msg;
  public PasswordDataCallback( GetPasswordDataType msg ) {
    this.msg = msg;
    GetConsoleOutputType consoleOutput = new GetConsoleOutputType( ).regardingUserRequest( msg );
    consoleOutput.setInstanceId( msg.getInstanceId( ) );
    this.setRequest( consoleOutput );
  }
  
  @Override
  public void initialize( GetConsoleOutputType msg )  {}
  
  @Override
  public void fire( GetConsoleOutputResponseType reply )  {
    VmInstance vm = VmInstances.lookup( this.getRequest( ).getInstanceId( ) );
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
    try {
      Contexts.response( rep );
    } catch ( Exception ex1 ) {
      LOG.error( ex1 , ex1 );
    }
  }


  @Override
  public void fireException( Throwable e ) {
    LOG.debug( LogUtil.subheader( this.getRequest( ).toString( "eucalyptus_ucsb_edu" ) ) );
    LOG.debug( e, e );
  }

}
