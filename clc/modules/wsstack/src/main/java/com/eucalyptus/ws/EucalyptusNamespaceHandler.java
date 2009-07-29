package com.eucalyptus.ws;

import com.eucalyptus.ws.server.NioHttpConnector;
import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;

public class EucalyptusNamespaceHandler extends AbstractMuleNamespaceHandler {

  public static final String[][] AXIS2_ATTRIBUTES = new String[][]{new String[]{}};

  public void init()
  {
    registerBeanDefinitionParser( "connector", new OrphanDefinitionParser( NioHttpConnector.class, true ) );
    registerMetaTransportEndpoints( NioHttpConnector.PROTOCOL );
  }

}
