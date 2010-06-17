package com.eucalyptus.auth.api;

import java.util.List;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.login.WrappedCredentials;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.google.common.collect.Lists;

public abstract class BaseLoginModule<CB extends WrappedCredentials> implements LoginModule {
  private static Logger   LOG           = Logger.getLogger( BaseLoginModule.class );
  private boolean         authenticated = false;
  private CallbackHandler callbackHandler;
  private Object          credential;
  private List<Group>     groups = Lists.newArrayList( );
  private User            principal;
  private Subject         subject;
  private CB              wrappedCredentials;
  
  @Override
  public boolean abort( ) throws LoginException {
    LOG.debug( "Login aborted." );
    this.reset( );
    return true;
  }
  
  @Override
  public final boolean commit( ) throws LoginException {
    if ( !this.isAuthenticated( ) ) {
      return false;
    }
    this.getSubject( ).getPrincipals( ).add( this.getPrincipal( ) );
    this.getSubject( ).getPrincipals( ).addAll( this.getGroups( ) );
    this.getSubject( ).getPublicCredentials( ).add( this.getCredential( ) );
    try {
      Contexts.lookup( this.getWrappedCredentials( ).getCorrelationId( ) ).setUser( this.getPrincipal( ) );
      Contexts.lookup( this.getWrappedCredentials( ).getCorrelationId( ) ).setSubject( this.getSubject( ) );
    } catch ( final NoSuchContextException e ) {
      BaseLoginModule.LOG.debug( e, e );
      this.authenticated = false;
    }
    return this.authenticated;
  }
  
  public CallbackHandler getCallbackHandler( ) {
    return this.callbackHandler;
  }
  
  public Object getCredential( ) {
    return this.credential;
  }
  
  public List<Group> getGroups( ) {
    return this.groups;
  }
  
  public User getPrincipal( ) {
    return this.principal;
  }
  
  public Subject getSubject( ) {
    return this.subject;
  }
  
  public CB getWrappedCredentials( ) {
    return this.wrappedCredentials;
  }
  
  public abstract boolean accepts( );
  
  @Override
  public void initialize( final Subject subject, final CallbackHandler callbackHandler, final Map<String, ?> sharedState, final Map<String, ?> options ) {
    this.subject = subject;
    this.callbackHandler = callbackHandler;
    if ( this.accepts( ) ) {
      this.wrappedCredentials = ( CB ) callbackHandler;
    } else {
      this.wrappedCredentials = null;
    }
  }
  
  private boolean isAuthenticated( ) {
    return this.authenticated;
  }
  
  @Override
  public boolean login( ) throws LoginException {
    if ( this.wrappedCredentials == null ) {
      return false;
    }
    try {
      this.setAuthenticated( this.authenticate( this.wrappedCredentials ) );
    } catch ( final Exception e ) {
      LOG.debug( e, e );
      this.setAuthenticated( false );
      throw new LoginException( e.getMessage( ) );
    }
    return this.isAuthenticated( );
  }
  
  public abstract boolean authenticate( CB credentials ) throws Exception;
  
  @Override
  public boolean logout( ) throws LoginException {
    this.baseReset( );
    this.reset( );
    return true;
  }
  
  public void reset( ) {}
  
  private void setAuthenticated( final boolean authenticated ) {
    this.authenticated = authenticated;
  }
  
  public void setCredential( final Object credential ) {
    this.credential = credential;
  }
  
  public void setPrincipal( final User principal ) {
    this.principal = principal;
  }
  
  public void setWrappedCredentials( final CB wrappedCredentials ) {
    this.wrappedCredentials = wrappedCredentials;
  }
  
  private void baseReset( ) {
    if ( this.principal != null ) {
      this.subject.getPrincipals( ).remove( this.principal );
      this.principal = null;
    }
    if ( this.getCredential( ) != null ) {
      this.getSubject( ).getPublicCredentials( ).remove( this.getCredential( ) );
      this.credential = null;
    }
    this.wrappedCredentials = null;
    this.authenticated = false;
    this.callbackHandler = null;
    this.groups = Lists.newArrayList( );
  }
  
}
