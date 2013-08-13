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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.context;

import static java.util.Collections.unmodifiableMap;
import static com.google.common.collect.Maps.newHashMap;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import javax.security.auth.Subject;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.local.DefaultLocalClientChannelFactory;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.mule.api.MuleEvent;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Contract;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.ws.server.Statistics;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class Context {
  private static Logger                LOG       = Logger.getLogger( Context.class );
  private final String                 correlationId;
  private Long                         creationTime;
  private BaseMessage                  request   = null;
  private final MappingHttpRequest     httpRequest;
  private final Channel                channel;
  private final boolean                channelManaged;
  private WeakReference<MuleEvent>     muleEvent = new WeakReference<MuleEvent>( null );
  private User                         user      = null;
  private Subject                      subject   = null;
  private Map<Contract.Type, Contract> contracts = null;
  private Boolean                      isSystemAdmin;

  Context( ) {
    this.correlationId = null;
    this.httpRequest = null;
    this.channel = null;
    this.channelManaged = false;
  }

  protected Context( String dest, final BaseMessage msg ) {
    this.correlationId = msg.getCorrelationId( );
    this.creationTime = System.nanoTime( );
    this.httpRequest = new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.GET, dest ) {
      {
        this.setCorrelationId( msg.getCorrelationId( ) );
        this.message = msg;
      }
    };
    this.channel = new DefaultLocalClientChannelFactory( ).newChannel( Channels.pipeline( ) );
    this.channelManaged = true;
    this.user = Principals.systemUser( );
    EventRecord.caller( Context.class, EventType.CONTEXT_CREATE, this.correlationId, this.channel.toString( ) ).debug( );
  }
  
  protected Context( MappingHttpRequest httpRequest, Channel channel ) {
    UUID uuid = UUID.randomUUID( );
    Statistics.startRequest( channel );
    this.correlationId = uuid.toString( );
    this.creationTime = System.nanoTime( );
    this.httpRequest = httpRequest;
    this.channel = channel;
    this.channelManaged = false;
    EventRecord.caller( Context.class, EventType.CONTEXT_CREATE, this.correlationId, this.channel.toString( ) ).debug( );
  }
  
  public Channel getChannel( ) {
    return check( this.channel );
  }
  
  public InetAddress getRemoteAddress( ) {
    if ( this.getChannel( ) != null ) {
      if ( this.getChannel( ).getRemoteAddress( ) instanceof InetSocketAddress ) {
        return ( ( InetSocketAddress ) this.getChannel( ).getRemoteAddress( ) ).getAddress( );
      }
    }
    throw new IllegalContextAccessException( "Attempt to access socket address information when no associated socket exists." );
  }
  
  public MappingHttpRequest getHttpRequest( ) {
    return check( this.httpRequest );
  }
  
  public String getCorrelationId( ) {
    return this.correlationId;
  }
  
  public Long getCreationTime( ) {
    return this.creationTime;
  }
  
  public void setRequest( BaseMessage msg ) {
    if ( msg != null ) {
      EventRecord.caller( Context.class, EventType.CONTEXT_MSG, this.correlationId, msg.toSimpleString( ) ).debug( );
      this.request = msg;
    }
  }
  
  public BaseMessage getRequest( ) {
    initRequest();
    return check( this.request );
  }
  
  public void setUser( User user ) {
    if ( user != null ) {
      EventRecord.caller( Context.class, EventType.CONTEXT_USER, this.correlationId, user.getUserId( ) ).debug( );
      this.user = user;
    }
  }
  
  public UserFullName getUserFullName( ) {
    return UserFullName.getInstance( this.getUser( ) );
  }
  
  public boolean hasAdministrativePrivileges( ) {
    if ( isSystemAdmin == null ) {
      isSystemAdmin = this.getUser( ).isSystemAdmin( );
    }
    return isSystemAdmin;
  }
  
  public User getUser( ) {
    return check( this.user );
  }
  
  void setMuleEvent( MuleEvent event ) {
    if ( event != null && this.muleEvent.get( ) == null ) {
//      LOG.debug( EventType.CONTEXT_EVENT + " associated event context found for " + this.correlationId + " other corrId: " + event.getId( ) );
      this.muleEvent = new WeakReference<MuleEvent>( event );
    }
  }
  
  public String getServiceName( ) {
    MuleEvent e;
    if ( ( e = this.muleEvent.get( ) ) != null ) {
      return e.getFlowConstruct( ).getName( );
    } else {
      return this.httpRequest.getServicePath( ).replaceAll( "/services/", "" ).replaceAll( "[/?].+", "" );
    }
  }
  
  public Subject getSubject( ) {
    return check( this.subject );
  }
  
  public void setSubject( Subject subject ) {
    if ( subject != null ) {
      this.subject = subject;
    }
  }
  
  void clear( ) {
    if ( this.muleEvent != null ) {
      this.muleEvent.clear( );
      this.muleEvent = null;
    }
    if ( this.channelManaged ) {
      this.channel.close( );
    }
    this.contracts = null;
  }

  private void initRequest() {
    if ( this.request == null && this.httpRequest != null && this.httpRequest.getMessage( ) != null ) {
      this.request = ( BaseMessage ) this.httpRequest.getMessage( );
    }
  }

  private static <TYPE> TYPE check( final TYPE obj ) {
    if ( obj == null ) {
      StackTraceElement steMethod = Thread.currentThread( ).getStackTrace( )[1];
      StackTraceElement steCaller = Thread.currentThread( ).getStackTrace( )[2];
      LOG.error( "Accessing context field when it is null: " + steMethod.getMethodName( ) + " from " + steCaller );
    }
    return obj;
  }

  /**
   * @throws IllegalStateException If contracts have not been evaluated for this context.
   */
  public Map<Contract.Type, Contract> getContracts( ) throws IllegalStateException {
    if ( this.contracts == null ) throw new IllegalStateException("Contracts not available");
    return this.contracts;
  }

  public void setContracts( final Map<Contract.Type, Contract> contracts ) {
    this.contracts = unmodifiableMap(newHashMap(contracts));
  }

  public Account getAccount( ) {
    try {
      return this.user.getAccount( );
    } catch ( AuthException ex ) {
      LOG.error( ex, ex );
      throw new IllegalStateException( "Context populated with ill-defined user:  no corresponding account found.", ex );
    }
  }
  
  static Context maybeImpersonating( Context ctx ) {
    ctx.initRequest();
    if ( ctx.request != null ) {
      final String userId = ctx.request.getEffectiveUserId( );
      if ( userId != null &&
           !Principals.isFakeIdentify(userId) &&
           ctx.hasAdministrativePrivileges( ) ) {
        try {
          final User user = Accounts.lookupUserById( userId );
          return createImpersona( ctx, user );
        } catch ( AuthException ex ) {
          return ctx;
        }
      }
    }
    return ctx;
  }

  private static Context createImpersona( final Context ctx, final User user ) {
    return new DelegatingContextSupport( ctx ) {
      private Boolean isSystemAdmin;

      @Override
      public User getUser( ) {
        return user;
      }
      
      @Override
      public Account getAccount( ) {
        try {
          return user.getAccount( );
        } catch ( AuthException ex ) {
          LOG.error( ex, ex );
          throw new IllegalStateException( "Context populated with ill-defined user:  no corresponding account found.", ex );
        }
      }

      @Override
      public UserFullName getUserFullName( ) {
        return UserFullName.getInstance( user );
      }

      @Override
      public boolean hasAdministrativePrivileges( ) {
        if ( isSystemAdmin == null ) {
          isSystemAdmin = user.isSystemAdmin();
        }
        return isSystemAdmin;
      }
    };
  }
}
