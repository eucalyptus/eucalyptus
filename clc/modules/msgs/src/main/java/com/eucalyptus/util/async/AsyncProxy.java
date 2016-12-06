/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.util.async;

import java.lang.reflect.Proxy;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.system.Ats;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.CallerContext;

/**
 * Factory for interface based service clients.
 *
 * For synchronous calls the interface should return BaseMessage, for
 * asynchronous calls return CheckedListenableFuture&lt;BaseMessage>.
 */
public class AsyncProxy {

  @SuppressWarnings( "unchecked" )
  public static <T> T client( final Class<T> clientInterface ) {
    final ComponentPart componentPart = Ats.from( clientInterface ).get( ComponentPart.class );
    if ( componentPart == null ) {
      throw new IllegalArgumentException( "Client interface must have @ComponentPart annotation: " + clientInterface );
    }
    return (T) Proxy.newProxyInstance(
        AsyncProxy.class.getClassLoader( ),
        new Class<?>[]{ clientInterface },
        ( target, method, arguments ) -> {
          if ( arguments.length != 1 || !(arguments[0] instanceof BaseMessage) ) {
            throw new IllegalArgumentException( "Expected one argument of type BaseMessage: " + method );
          }
          final BaseMessage request = (BaseMessage) arguments[0];
          final CallerContext callerContext = new CallerContext( Contexts.lookup( ) );
          callerContext.apply( request );
          final Class<?> returnType = method.getReturnType( );
          if ( CheckedListenableFuture.class.isAssignableFrom( returnType ) ) {
            return AsyncRequests.dispatch( Topology.lookup( componentPart.value( ) ), request );
          } else if ( BaseMessage.class.isAssignableFrom( returnType ) ) {
            return AsyncRequests.sendSync( Topology.lookup( componentPart.value( ) ), request );
          } else {
            throw new IllegalArgumentException( "Unexpected return type: " + method );
          }
        }
    );
  }
}
