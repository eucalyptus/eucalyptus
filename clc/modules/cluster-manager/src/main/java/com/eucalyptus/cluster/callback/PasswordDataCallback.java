/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

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
