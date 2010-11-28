package com.eucalyptus.auth.policy;

import java.net.SocketAddress;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class RequestContext {

  public interface ContextAdaptor {
    
    public User getRequestUser( );
    public Class<? extends BaseMessage> getRequestMessageClass( );
    public SocketAddress getRemoteAddress( );
    
  }
  
  private static class DefaultContextAdaptor implements ContextAdaptor {
    
    @Override
    public User getRequestUser( ) {
      Context requestContext = Contexts.lookup( );
      return requestContext.getUser( );
    }
    
    @Override
    public Class<? extends BaseMessage> getRequestMessageClass( ) {
      Context requestContext = Contexts.lookup( );
      return requestContext.getRequest( ).getClass( );
    }
    
    @Override
    public SocketAddress getRemoteAddress( ) {
      Context requestContext = Contexts.lookup( );
      return requestContext.getChannel( ).getRemoteAddress( );
    }
    
  }
  
  private static ContextAdaptor adaptor = new DefaultContextAdaptor( );
  
  /**
   * For testing.
   * @param adaptor
   */
  public static void setAdaptor( ContextAdaptor adaptor ) {
    RequestContext.adaptor = adaptor;
  }

  public static User getRequestUser( ) {
    return adaptor.getRequestUser( );
  }
  
  public static Class<? extends BaseMessage> getRequestMessageClass( ) {
    return adaptor.getRequestMessageClass( );
  }
  
  public static SocketAddress getRemoteAddress( ) {
    return adaptor.getRemoteAddress( );
  }

}
