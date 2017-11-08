/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.util.async;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.function.Supplier;
import com.eucalyptus.component.ServiceConfiguration;
import java.util.function.Function;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.ThrowingFunction;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.CallerContext;

/**
 * Factory for interface based service clients.
 *
 * For synchronous calls the interface should return BaseMessage, for
 * asynchronous calls return CheckedListenableFuture&lt;BaseMessage>.
 */
public class AsyncProxy {

  /**
   * Create a client that uses Topology to resolve an endpoint, propagates the contextual identity
   */
  public static <T> T client( final Class<T> clientInterface ) {
    return client( clientInterface, ThrowingFunction.undeclared( request -> {
      final CallerContext callerContext = new CallerContext( Contexts.lookup( ) );
      callerContext.apply( request );
      return request;
    } ) );
  }

  /**
   * Create a system privileged client
   */
  public static <T> T privilegedClient( final Class<T> clientInterface ) {
    final Function<String,String> idLookup =
        FUtils.memoizeLast( ThrowingFunction.undeclared( Accounts::lookupAccountIdByAlias ) );
    return client( clientInterface, ThrowingFunction.undeclared( request -> {
      request.setUserId( idLookup.apply( AccountIdentifiers.SYSTEM_ACCOUNT ) );
      request.markPrivileged( );
      return request;
    } ) );
  }

  /**
   * Create a client using the given message transform
   */
  @SuppressWarnings( "unchecked" )
  public static <T> T client(
      final Class<T> clientInterface,
      final Function<BaseMessage,BaseMessage> messageTransform
  ) {
    final ComponentPart componentPart = Ats.from( clientInterface ).get( ComponentPart.class );
    if ( componentPart == null ) {
      throw new IllegalArgumentException( "Client interface must have @ComponentPart annotation: " + clientInterface );
    }
    return client( clientInterface, messageTransform, () -> Topology.lookup( componentPart.value( ) )  );
  }

  /**
   * Create a client that uses the given endpoint and transform
   */
  public static <T> T client(
      final Class<T> clientInterface,
      final Function<BaseMessage,BaseMessage> messageTransform,
      final ServiceConfiguration configuration
  ) {
    return client( clientInterface, messageTransform, () -> configuration );
  }

  /**
   * Create a client that the endpoint from the given supplier
   */
  @SuppressWarnings( "unchecked" )
  public static <T> T client(
      final Class<T> clientInterface,
      final Function<BaseMessage,BaseMessage> messageTransform,
      final Supplier<ServiceConfiguration> configurationSupplier
  ) {
    return (T) Proxy.newProxyInstance(
        AsyncProxy.class.getClassLoader( ),
        new Class<?>[]{ clientInterface },
        ( target, method, arguments ) -> {
          if ( method.isDefault( ) ) {
            final Class<?> declaringClass = method.getDeclaringClass( );
            final Constructor<MethodHandles.Lookup> constructor =
                MethodHandles.Lookup.class.getDeclaredConstructor( Class.class, int.class );
            constructor.setAccessible( true );
            return constructor.
                newInstance( declaringClass, MethodHandles.Lookup.PRIVATE ).
                unreflectSpecial( method, declaringClass ).
                bindTo( target ).
                invokeWithArguments( arguments );
          }
          if ( arguments.length != 1 || !(arguments[0] instanceof BaseMessage) ) {
            throw new IllegalArgumentException( "Expected one argument of type BaseMessage: " + method );
          }
          final BaseMessage request = messageTransform.apply( (BaseMessage) arguments[0] );
          final Class<?> returnType = method.getReturnType( );
          if ( CheckedListenableFuture.class.isAssignableFrom( returnType ) ) {
            return AsyncRequests.dispatch( configurationSupplier.get( ), request );
          } else if ( BaseMessage.class.isAssignableFrom( returnType ) ) {
            return AsyncRequests.sendSync( configurationSupplier.get( ), request );
          } else {
            throw new IllegalArgumentException( "Unexpected return type: " + method );
          }
        }
    );
  }
}
