package com.eucalyptus.auth.policy;

import java.net.SocketAddress;
import java.util.Map;
import com.eucalyptus.auth.Contract;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class RequestContext {

  public interface ContextAdaptor {
    
    public User getRequestUser( );

    public BaseMessage getRequest( );
    
    public SocketAddress getRemoteAddress( );
    
    public Map<String, Contract> getContracts( ); 
    
  }
  
  private static class DefaultContextAdaptor implements ContextAdaptor {
    
    @Override
    public User getRequestUser( ) {
      Context requestContext = Contexts.lookup( );
      return requestContext.getUser( );
    }
    
    @Override
    public BaseMessage getRequest( ) {
      Context requestContext = Contexts.lookup( );
      return requestContext.getRequest( );
    }
    
    @Override
    public SocketAddress getRemoteAddress( ) {
      Context requestContext = Contexts.lookup( );
      return requestContext.getChannel( ).getRemoteAddress( );
    }

    @Override
    public Map<String, Contract> getContracts( ) {
      Context requestContext = Contexts.lookup( );
      return requestContext.getContracts( );
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
  
  public static BaseMessage getRequest( ) {
    return adaptor.getRequest( );
  }
  
  public static SocketAddress getRemoteAddress( ) {
    return adaptor.getRemoteAddress( );
  }
  
  public static Map<String, Contract> getContracts( ) {
    return adaptor.getContracts( );
  }

}
