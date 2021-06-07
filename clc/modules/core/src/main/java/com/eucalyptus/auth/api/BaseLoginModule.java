/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.api;

import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.login.WrappedCredentials;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;

public abstract class BaseLoginModule<CB extends WrappedCredentials> implements LoginModule {
  private static Logger   LOG           = Logger.getLogger( BaseLoginModule.class );
  private boolean         authenticated = false;
  private CallbackHandler callbackHandler;
  private Object          credential;
  private UserPrincipal   principal;
  private Subject         subject;
  private CB              wrappedCredentials;
  
  @Override
  public boolean abort( ) throws LoginException {
    this.reset( );
    return true;
  }
  
  @Override
  public final boolean commit( ) throws LoginException {
    if ( !this.isAuthenticated( ) ) {
      return false;
    }
    this.getSubject( ).getPrincipals( ).add( this.getPrincipal( ) );
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

  public UserPrincipal getPrincipal( ) {
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
      if ( this.isAuthenticated( ) && ( principal == null || !principal.isEnabled( ) ) ) {
        throw new LoginException( "Access denied for user" );
      }
    } catch ( final Exception e ) {
      LOG.trace( e, e );
      this.setAuthenticated( false );
      throw e instanceof LoginException ?
          (LoginException) e :
          (LoginException) new LoginException( e.getMessage( ) ).initCause( e );
    }
    return this.isAuthenticated( );
  }

  /**
   * Authenticate the given credentials.
   *
   * @return true if the authentication succeeded, or false if this
   *              LoginModule should be ignored.
   * @throws Exception if the authentication fails
   */
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
  
  public void setPrincipal( final UserPrincipal principal ) {
    this.principal = principal;
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
    //this.groups = Lists.newArrayList( );
  }
  
}
