/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.context;

import java.net.InetAddress;
import java.util.Map;
import javax.security.auth.Subject;
import org.jboss.netty.channel.Channel;
import com.eucalyptus.auth.Contract;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
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
  public User getUser( ) {
    return this.delegate.getUser();
  }

  @Override
  public Account getAccount( ) {
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
  public Channel getChannel( ) {
    return this.delegate.getChannel( );
  }

  @Override
  public InetAddress getRemoteAddress( ) {
    return this.delegate.getRemoteAddress( );
  }

  @Override
  public MappingHttpRequest getHttpRequest( ) {
    return this.delegate.getHttpRequest( );
  }

  @Override
  public String getCorrelationId( ) {
    return this.delegate.getCorrelationId( );
  }

  @Override
  public Long getCreationTime( ) {
    return this.delegate.getCreationTime( );
  }

  @Override
  public void setRequest( BaseMessage msg ) {
    this.delegate.setRequest( msg );
  }

  @Override
  public BaseMessage getRequest( ) {
    return this.delegate.getRequest( );
  }

  @Override
  public void setUser( User user ) {
    this.delegate.setUser( user );
  }

  @Override
  public String getServiceName( ) {
    return this.delegate.getServiceName( );
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
}
