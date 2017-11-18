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

package com.eucalyptus.context;

import static java.util.Collections.unmodifiableMap;
import com.eucalyptus.auth.AuthContextSupplier;
import static com.google.common.collect.Maps.newHashMap;

import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.ws.server.MessageStatistics;
import edu.ucsb.eucalyptus.msgs.EvaluatedIamConditionKey;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.security.auth.Subject;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.local.DefaultLocalClientChannelFactory;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Contract;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.CollectionUtils;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.BaseCallerContext;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class Context {
  private static Logger                LOG       = Logger.getLogger( Context.class );
  private String                       correlationId;
  private Long                         creationTime;
  private BaseMessage                  request   = null;
  private final MappingHttpRequest     httpRequest;
  private final Channel                channel;
  private final boolean                channelManaged;
  private UserPrincipal                user      = null;
  private Subject                      subject   = null;
  private Map<Contract.Type, Contract> contracts = null;
  private Boolean isSystemAdmin;
  private Boolean isSystemUser;

  Context( ) {
    this.correlationId = null;
    this.httpRequest = null;
    this.channel = null;
    this.channelManaged = false;
  }

  protected Context( String dest, final BaseMessage msg ) {
    this.correlationId = msg.getCorrelationId( );
    this.creationTime = System.nanoTime();
    this.httpRequest = new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.GET, dest ) {
      {
        this.setCorrelationId( msg.getCorrelationId( ) );
        this.message = msg;
      }
    };
    this.channel = new DefaultLocalClientChannelFactory( ).newChannel( Channels.pipeline( new SimpleChannelHandler( ) ) );
    this.channelManaged = true;
    this.user = Principals.systemUser( );
    EventRecord.caller( Context.class, EventType.CONTEXT_CREATE, this.correlationId, this.channel.toString( ) ).debug( );
  }
  
  protected Context( MappingHttpRequest httpRequest, Channel channel ) {
    UUID uuid = UUID.randomUUID( );
    MessageStatistics.startRequest(channel);
    this.correlationId = uuid.toString( );
    this.creationTime = System.nanoTime();
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
  
  public void setCorrelationId(final String corrId){
    this.correlationId = corrId;
  }
  
  public String getCorrelationId( ) {
    return this.correlationId;
  }
  
  public Long getCreationTime( ) {
    return this.creationTime;
  }
  
  public BaseMessage getRequest( ) {
    initRequest();
    return check( this.request );
  }
  
  public void setUser( UserPrincipal user ) {
    if ( user != null ) {
      EventRecord.caller( Context.class, EventType.CONTEXT_USER, this.correlationId, user.getUserId( ) ).debug( );
      this.user = user;
    }
  }

  public UserFullName getUserFullName( ) {
    return UserFullName.getInstance( this.getUser( ) );
  }

  /**
   * Context is system privileged acting as user.
   */
  public boolean isPrivileged() {
    return false;
  }

  /**
   * Context is privileged to perform any operation.
   */
  public boolean hasAdministrativePrivileges( ) {
    if ( isSystemAdmin == null ) {
      isSystemAdmin = this.getUser( ).isSystemAdmin( );
    }
    return isSystemAdmin;
  }

  /**
   * Context may be permitted to perform some administrative actions.
   *
   * Policy should be evaluated to determine actual permissions.
   */
  public boolean isAdministrator( ) {
    if ( isSystemUser == null ) {
      isSystemUser = this.getUser( ).isSystemUser();
    }
    return isSystemUser;
  }

  /**
   * Context uses impersonation.
   *
   * <p>This does not imply any privilege. Do not use this for authorization.</p>
   *
   * @return true if context identity is impersonated
   * @see #isPrivileged
   */
  public boolean isImpersonated( ) {
    return false;
  }

  /**
   * Evaluate IAM condition keys that are context sensitive.
   *
   * @return The evaluated keys.
   */
  public Map<String,String> evaluateKeys( ) throws AuthException {
    return Permissions.evaluateHostKeys( );
  }

  public UserPrincipal getUser( ) {
    return check( this.user );
  }

  public AuthContextSupplier getAuthContext( ) {
    return () -> Permissions.createAuthContext( getUser( ), Collections.emptyMap( ), Principals.typedSet( getUser( ) ) );
  }
  
  @Nullable
  public Subject getSubject( ) {
    return check( this.subject );
  }
  
  public void setSubject( Subject subject ) {
    if ( subject != null ) {
      this.subject = subject;
    }
  }
  
  void clear( ) {
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

  public String getAccountNumber( ) {
    return getUser( ).getAccountNumber( );
  }

  public String getAccountAlias( ) {
    return getUser( ).getAccountAlias();
  }

  public AccountFullName getAccount( ) {
    return AccountFullName.getInstance( getAccountNumber() );
  }
  
  static Context maybeImpersonating( Context ctx ) {
    ctx.initRequest();
    if ( ctx.request != null ) {
      final String userId = Optional.fromNullable( ctx.request.getUserId( ) )
          .or( Optional.fromNullable( ctx.request.getEffectiveUserId( ) ) )
          .orNull( );
      if ( userId != null &&
           !Principals.isFakeIdentify(userId) &&
           ctx.hasAdministrativePrivileges( ) ) {
        try {
          final String originalContextId = ctx.request.hasRequestId( ) ?
              ctx.request.getCorrelationId( ).substring( 0, ctx.request.getCorrelationId( ).indexOf( "::" ) ) :
              null;
          final UserPrincipal user;
          if ( Accounts.isAccountNumber( userId ) ) {
            user = Accounts.lookupCachedPrincipalByAccountNumber( userId );
          } else if ( Contexts.exists( originalContextId ) &&
              Contexts.lookup( originalContextId ).getUser( ).getAuthenticatedId( ).equals( userId ) ) {
            user = Contexts.lookup( originalContextId ).getUser( );
          } else if ( Accounts.isRoleIdentifier( userId ) ) {
            user = Accounts.lookupCachedPrincipalByRoleId( userId, null );
          } else {
            user = Accounts.lookupCachedPrincipalByUserId( userId, null );
          }
          return createImpersona( ctx, user );
        } catch ( AuthException ex ) {
          return ctx;
        } catch ( NoSuchContextException e ) {
          throw Exceptions.toUndeclared( new AuthException( e ) );
        }
      }
    }
    return ctx;
  }

  private static Context createImpersona( final Context ctx, final UserPrincipal user ) {
    return new DelegatingContextSupport( ctx ) {
      private Boolean isSystemAdmin;
      private Boolean isSystemUser;
      private Subject subject = new Subject( );
      private Map<String,String> evaluatedKeys;

      @Override
      public UserPrincipal getUser( ) {
        return user;
      }
      
      @Override
      public AccountFullName getAccount( ) {
        return AccountFullName.getInstance( getAccountNumber( ) );
      }

      @Override
      public String getAccountAlias() {
        return getUser( ).getAccountAlias( );
      }

      @Override
      public String getAccountNumber() {
        return getUser( ).getAccountNumber( );
      }

      @Override
      public UserFullName getUserFullName( ) {
        return UserFullName.getInstance( user );
      }

      @Override
      public boolean isPrivileged( ) {
        return Principals.systemUser( ).getName( ).equals( ctx.request.getEffectiveUserId( ) );
      }

      @Override
      public boolean isAdministrator( ) {
        if ( isSystemUser == null ) {
          isSystemUser = this.getUser( ).isSystemUser( );
        }
        return isSystemUser;
      }

      @Override
      public boolean hasAdministrativePrivileges( ) {
        if ( isSystemAdmin == null ) {
          isSystemAdmin = user.isSystemAdmin( );
        }
        return isSystemAdmin;
      }

      @Override
      public boolean isImpersonated( ) {
        return true;
      }

      @Override
      public Subject getSubject( ) {
        return subject;
      }

      @Override
      public void setSubject( final Subject subject ) {
        this.subject = subject;
      }

      @Override
      public Map<String, String> evaluateKeys( ) throws AuthException {
        if ( evaluatedKeys == null ) {
          final BaseCallerContext context = super.getRequest( ).getCallerContext( );
          if ( context == null ) {
            evaluatedKeys = Collections.emptyMap( );
          } else {
            evaluatedKeys = CollectionUtils.putAll(
                context.getEvaluatedKeys( ),
                Maps.<String,String>newHashMap( ),
                EvaluatedIamConditionKey.key( ),
                EvaluatedIamConditionKey.value( ) );
          }
        }
        return evaluatedKeys;
      }

      @Override
      public AuthContextSupplier getAuthContext( ) {
        return () -> Permissions.createAuthContext( getUser( ), evaluateKeys( ), Principals.typedSet( getUser( ) ) );
      }
    };
  }
}
