package com.eucalyptus.empyrean;

import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.mortbay.naming.InitialContextFactory;
import org.mortbay.naming.NamingUtil;
import com.eucalyptus.bootstrap.SystemIds;
import com.eucalyptus.system.SubDirectory;
import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.Configuration;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.jndi.BitronixContext;

public class EmpyreanTransactionManager extends org.mortbay.component.AbstractLifeCycle implements org.jboss.cache.transaction.TransactionManagerLookup, org.hibernate.transaction.TransactionManagerLookup {
  
  private static Logger             LOG = Logger.getLogger( EmpyreanTransactionManager.class );
  private static Context            ctx;
  private static TransactionManager tm;
  
  private static Context getContext( ) {
    if ( ctx != null ) {
      return ctx;
    } else {
      synchronized ( EmpyreanTransactionManager.class ) {
        if ( ctx == null ) {
          tm = configureTm( );
          ctx = new BitronixContext( );
        }
        return ctx;
      }
    }
  }
  
  public TransactionManager getTransactionManager( ) throws Exception {
    return tm;
  }
  
  private static TransactionManager configureTm( ) {
    Configuration tm_conf = TransactionManagerServices.getConfiguration( );
    tm_conf.setServerId( SystemIds.createCloudUniqueName( "transaction-manager" ) );
    tm_conf.setAsynchronous2Pc( false );
    tm_conf.setLogPart1Filename( SubDirectory.DB.toString( ) + "/btm1.tx" );
    tm_conf.setLogPart2Filename( SubDirectory.DB.toString( ) + "/btm2.tx" );
    tm_conf.setJndiUserTransactionName( "eucalyptusTransactionManager" );
    return TransactionManagerServices.getTransactionManager( );
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
