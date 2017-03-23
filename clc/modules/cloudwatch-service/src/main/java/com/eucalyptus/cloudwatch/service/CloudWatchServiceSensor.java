/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.cloudwatch.service;

import static com.eucalyptus.util.RestrictedTypes.getIamActionByMessageType;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.AuthContext;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.cloudwatch.common.msgs.CloudWatchMessage;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.context.ServiceAdvice;
import com.eucalyptus.reporting.Counter;
import com.eucalyptus.reporting.Counter.Counted;
import com.google.common.collect.ImmutableSet;

/**
 *
 */
@ComponentNamed
public class CloudWatchServiceSensor extends ServiceAdvice {

  private static final Set<String> countedActions = ImmutableSet.of(
      "getmetricstatistics",
      "listmetrics",
      "putmetricdata"
  );

  private static final Counter<CloudWatchMessage,Counted> counter =
      new Counter<>( 60_000, 15, CloudWatchServiceSensor::messageToCountedItem );

  @Override
  protected void afterService( @Nonnull final Object request, @Nullable final Object response ) throws Exception {
    if ( request instanceof CloudWatchMessage ) {
      counter.count( (CloudWatchMessage) request );
    }
  }

  private static Counted messageToCountedItem( final CloudWatchMessage message ) {
    final String action = getIamActionByMessageType( message );
    if ( countedActions.contains( action ) ) try {
      final AuthContext authContext = Contexts.lookup( message.getCorrelationId( ) ).getAuthContext( ).get( );
      return new Counted( authContext.getAccountNumber( ), action );
    } catch ( AuthException | NoSuchContextException ignore ) {
    }
    return null;
  }
}
