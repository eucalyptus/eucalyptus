package com.eucalyptus.empyrean;

import java.util.Hashtable;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.ServiceUnavailableException;
import javax.naming.spi.InitialContextFactory;
import org.apache.log4j.Logger;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.jndi.BitronixContext;
import bitronix.tm.resource.ResourceRegistrar;

public class EmpyreanContext implements Context, InitialContextFactory {
  private static Logger LOG = Logger.getLogger( EmpyreanContext.class );

  private BitronixContext ctx = new BitronixContext( );
  public EmpyreanContext( ) {}
  
  @Override
  public Context getInitialContext( Hashtable<?, ?> environment ) throws NamingException {
    return new EmpyreanContext( );
  }

  public Object lookup( Name name ) throws NamingException {
    return this.lookup( name );
  }

  public Object lookup( String name ) throws NamingException {
    if( EmpyreanTransactionManager.JNDI_NAME.equals( name.toString( ) ) ) {
      return EmpyreanTransactionManager.getTm( );
    } else if( name.toString( ).contains( "eucalyptus" ) ) {
      return EmpyreanTransactionManager.getTm( );
    } else {
      return this.ctx.lookup( name );
    }
  }

  public void bind( Name name, Object obj ) throws NamingException {
    this.ctx.bind( name, obj );
  }

  public void bind( String name, Object obj ) throws NamingException {
    this.ctx.bind( name, obj );
  }

  public void rebind( Name name, Object obj ) throws NamingException {
    this.ctx.rebind( name, obj );
  }

  public void rebind( String name, Object obj ) throws NamingException {
    this.ctx.rebind( name, obj );
  }

  public void unbind( Name name ) throws NamingException {
    this.ctx.unbind( name );
  }

  public void unbind( String name ) throws NamingException {
    this.ctx.unbind( name );
  }

  public void rename( Name oldName, Name newName ) throws NamingException {
    this.ctx.rename( oldName, newName );
  }

  public void rename( String oldName, String newName ) throws NamingException {
    this.ctx.rename( oldName, newName );
  }

  public NamingEnumeration<NameClassPair> list( Name name ) throws NamingException {
    return this.ctx.list( name );
  }

  public NamingEnumeration<NameClassPair> list( String name ) throws NamingException {
    return this.ctx.list( name );
  }

  public NamingEnumeration<Binding> listBindings( Name name ) throws NamingException {
    return this.ctx.listBindings( name );
  }

  public NamingEnumeration<Binding> listBindings( String name ) throws NamingException {
    return this.ctx.listBindings( name );
  }

  public void destroySubcontext( Name name ) throws NamingException {
    this.ctx.destroySubcontext( name );
  }

  public void destroySubcontext( String name ) throws NamingException {
    this.ctx.destroySubcontext( name );
  }

  public Context createSubcontext( Name name ) throws NamingException {
    return this.ctx.createSubcontext( name );
  }

  public Context createSubcontext( String name ) throws NamingException {
    return this.ctx.createSubcontext( name );
  }

  public Object lookupLink( Name name ) throws NamingException {
    return this.ctx.lookupLink( name );
  }

  public Object lookupLink( String name ) throws NamingException {
    return this.ctx.lookupLink( name );
  }

  public NameParser getNameParser( Name name ) throws NamingException {
    return this.ctx.getNameParser( name );
  }

  public NameParser getNameParser( String name ) throws NamingException {
    return this.ctx.getNameParser( name );
  }

  public Name composeName( Name name, Name prefix ) throws NamingException {
    return this.ctx.composeName( name, prefix );
  }

  public String composeName( String name, String prefix ) throws NamingException {
    return this.ctx.composeName( name, prefix );
  }

  public Object addToEnvironment( String propName, Object propVal ) throws NamingException {
    return this.ctx.addToEnvironment( propName, propVal );
  }

  public Object removeFromEnvironment( String propName ) throws NamingException {
    return this.ctx.removeFromEnvironment( propName );
  }

  public Hashtable<?, ?> getEnvironment( ) throws NamingException {
    return this.ctx.getEnvironment( );
  }

  public void close( ) throws NamingException {
    this.ctx.close( );
  }

  public String getNameInNamespace( ) throws NamingException {
    return this.ctx.getNameInNamespace( );
  }
  
}
