/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
package com.eucalyptus.network.applicator;

import java.util.EnumSet;
import java.util.Optional;
import com.eucalyptus.cluster.NIProperty;
import com.eucalyptus.cluster.NetworkInfo;
import com.eucalyptus.network.NetworkMode;
import com.eucalyptus.util.TypedKey;
import com.google.common.collect.Iterables;

/**
 * An applicator that is active in specific network modes.
 */
public abstract class ModeSpecificApplicator implements Applicator {

  private static final TypedKey<NetworkMode> MODE_KEY = TypedKey.create( "NetworkMode" );
  private final EnumSet<NetworkMode> modes;

  protected ModeSpecificApplicator( final EnumSet<NetworkMode> modes ) {
    this.modes = modes;
  }

  @Override
  public final void apply( final ApplicatorContext context, final ApplicatorChain chain ) throws ApplicatorException {
    NetworkMode mode = context.getAttribute( MODE_KEY );
    if ( mode == null ) {
      mode = extractMode( context.getNetworkInfo( ) );
      context.setAttribute( MODE_KEY, mode );
    }

    if ( modes.contains( mode ) ) {
      modeApply( mode, context, chain );
    } else {
      chain.applyNext( context );
    }
  }

  protected abstract void modeApply(
      NetworkMode mode,
      ApplicatorContext context,
      ApplicatorChain chain
  ) throws ApplicatorException;

  private NetworkMode extractMode( final NetworkInfo networkInfo ) {
    final Optional<NIProperty> property =
        networkInfo.getConfiguration( ).getProperties( ).stream( )
            .filter( prop -> "mode".equals( prop.getName( ) ) )
            .findFirst( );

    return NetworkMode.fromString(
        property.map( prop -> Iterables.get( prop.getValues( ), 0 ) ).orElse( null ),
        NetworkMode.EDGE );
  }
}
