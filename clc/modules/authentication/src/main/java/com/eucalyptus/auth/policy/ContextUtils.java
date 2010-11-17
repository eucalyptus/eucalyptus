package com.eucalyptus.auth.policy;

import java.net.SocketAddress;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class ContextUtils {

  public static User getRequestUser( ) {
    Context requestContext = Contexts.lookup( );
    return requestContext.getUser( );
  }
  
  public static Class<? extends BaseMessage> getRequestMessageClass( ) {
    Context requestContext = Contexts.lookup( );
    return requestContext.getRequest( ).getClass( );
  }
  
  public static SocketAddress getRemoteAddress( ) {
    Context requestContext = Contexts.lookup( );
    return requestContext.getChannel( ).getRemoteAddress( );
  }
  
}
