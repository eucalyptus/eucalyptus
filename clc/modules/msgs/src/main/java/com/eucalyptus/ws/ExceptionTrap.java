package com.eucalyptus.ws;

import java.beans.ExceptionListener;
import org.jboss.netty.channel.Channels;
import org.mule.DefaultExceptionStrategy;
import com.eucalyptus.BaseException;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;

public class ExceptionTrap extends DefaultExceptionStrategy implements ExceptionListener {

  @Override
  public void exceptionThrown( Exception e ) {
    String errMsg = "";
    boolean baseEx = false;
    for( Throwable t = e; t.getCause( ) != null; t = t.getCause( ) ) {
      errMsg += t.getMessage( ) + " ";      
      baseEx |= t instanceof BaseException;
    }
    try {
      Context ctx = Contexts.lookup( );
      BaseMessage request = ctx.getRequest( );
      BaseMessage reply = new EucalyptusErrorMessageType( ctx.getServiceName( ), errMsg ).regardingUserRequest( request );
      Channels.write( ctx.getChannel( ), reply );
    } catch ( NoSuchContextException e1 ) {
      super.exceptionThrown( e );
    }
  }

}
