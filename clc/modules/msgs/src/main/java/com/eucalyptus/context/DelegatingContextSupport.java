/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.context;

import java.net.InetAddress;
import java.util.Map;
import javax.security.auth.Subject;
import org.jboss.netty.channel.Channel;
import com.eucalyptus.auth.AuthContextSupplier;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Contract;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.util.Wrapper;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 *
 */
abstract class DelegatingContextSupport extends Context implements Wrapper<Context> {
  private final Context delegate;

  protected DelegatingContextSupport( final Context delegate ) {
    this.delegate = delegate;
  }

  @Override
  public UserPrincipal getUser( ) {
    return this.delegate.getUser();
  }

  @Override
  public String getAccountAlias( ) {
    return delegate.getAccountAlias( );
  }

  @Override
  public String getAccountNumber( ) {
    return delegate.getAccountNumber( );
  }

  @Override
  public AccountFullName getAccount( ) {
    return this.delegate.getAccount();
  }

  @Override
  public UserFullName getUserFullName( ) {
    return this.delegate.getUserFullName();
  }

  @Override
  public boolean hasAdministrativePrivileges( ) {
    return this.delegate.hasAdministrativePrivileges();
  }

  @Override
  public boolean isAdministrator() {
    return this.delegate.isAdministrator();
  }

  @Override
  public Channel getChannel( ) {
    return this.delegate.getChannel();
  }

  @Override
  public InetAddress getRemoteAddress( ) {
    return this.delegate.getRemoteAddress();
  }

  @Override
  public MappingHttpRequest getHttpRequest( ) {
    return this.delegate.getHttpRequest();
  }

  @Override
  public String getCorrelationId( ) {
    return this.delegate.getCorrelationId( );
  }

  @Override
  public void setCorrelationId( final String corrId ) {
    this.delegate.setCorrelationId( corrId );
  }

  @Override
  public Long getCreationTime( ) {
    return this.delegate.getCreationTime();
  }

  @Override
  public BaseMessage getRequest( ) {
    return this.delegate.getRequest( );
  }

  @Override
  public void setUser( final UserPrincipal user ) {
    delegate.setUser( user );
  }

  public String toString( ) {
    return this.delegate.toString( );
  }

  @Override
  public Subject getSubject( ) {
    return this.delegate.getSubject( );
  }

  @Override
  public void setSubject( Subject subject ) {
    this.delegate.setSubject( subject );
  }

  @Override
  public Map<Contract.Type, Contract> getContracts( ) throws IllegalStateException {
    return this.delegate.getContracts( );
  }

  @Override
  public void setContracts( Map<Contract.Type, Contract> contracts ) {
    this.delegate.setContracts( contracts );
  }

  @Override
  public Context unwrap() {
    return delegate;
  }

  @Override
  public boolean isPrivileged( ) {
    return this.delegate.isPrivileged();
  }

  @Override
  public boolean isImpersonated( ) {
    return this.delegate.isImpersonated();
  }

  @Override
  public Map<String, String> evaluateKeys( ) throws AuthException {
    return this.delegate.evaluateKeys(); 
 }

  @Override
  public AuthContextSupplier getAuthContext( ) {
    return this.delegate.getAuthContext();
  }
}
