package com.eucalyptus.empyrean;

import java.util.Properties;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import bitronix.tm.Configuration;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.jndi.BitronixContext;
import com.eucalyptus.bootstrap.SystemIds;
import com.eucalyptus.system.SubDirectory;

public class EmpyreanTransactionManager extends org.mortbay.component.AbstractLifeCycle implements org.jboss.cache.transaction.TransactionManagerLookup, org.hibernate.transaction.TransactionManagerLookup {
//  public static final String JNDI_NAME = "eucalyptusTransactionManager";
  private static Logger             LOG = Logger.getLogger( EmpyreanTransactionManager.class );
  private static Context            ctx = getContext( );
  private static TransactionManager tm;

  static Context getContext( ) {
    if ( ctx != null ) {
      return ctx;
    } else {
      synchronized ( EmpyreanTransactionManager.class ) {
        if ( ctx == null ) {
          ctx = configureTm( );
          try {
            tm = ( TransactionManager ) ctx.lookup( TransactionManagerServices.getConfiguration( ).getJndiUserTransactionName( ) );
          } catch ( NamingException ex ) {
            LOG.error( ex , ex );
          }
        }
        return ctx;
      }
    }
  }
  
  public TransactionManager getTransactionManager( ) throws Exception {
    return tm;
  }
  
  private static BitronixContext configureTm( ) {
    Configuration tm_conf = TransactionManagerServices.getConfiguration( );
    tm_conf.setServerId( SystemIds.createCloudUniqueName( "transaction-manager" ) );
    tm_conf.setAsynchronous2Pc( false );
    tm_conf.setLogPart1Filename( SubDirectory.TX.toString( ) + "/btm1.tx" );
    tm_conf.setLogPart2Filename( SubDirectory.TX.toString( ) + "/btm2.tx" );
//    tm_conf.setJndiUserTransactionName( JNDI_NAME );
    LOG.debug( "Setting up transaction manager: " + tm_conf.getJndiUserTransactionName( ) );
    return new BitronixContext( );
  }
  
  public String getUserTransactionName( ) {
    return TransactionManagerServices.getConfiguration( ).getJndiUserTransactionName( );
  }
  
  public Object getTransactionIdentifier( Transaction transaction ) {
    return transaction;
  }
  
  @Override
  public TransactionManager getTransactionManager( Properties arg0 ) throws HibernateException {
    return tm;
  }
  
//  @Override
//  protected void doStart( ) throws Exception {
//    InitialContext ic = new InitialContext( );
//    Context env = ( Context ) ic.lookup( "java:comp/env" );
//    LOG.debug( "Unbinding " + this.getUserTransactionName( ) );
//    env.bind( getUserTransactionName( ), tm );
//  }
//  
//  @Override
//  protected void doStop( ) throws Exception {
//    InitialContext ic = new InitialContext( );
//    Context env = ( Context ) ic.lookup( "java:comp/env" );
//    LOG.debug( "Unbinding " + this.getUserTransactionName( ) );
//    env.unbind( getUserTransactionName( ) );
//  }
}
