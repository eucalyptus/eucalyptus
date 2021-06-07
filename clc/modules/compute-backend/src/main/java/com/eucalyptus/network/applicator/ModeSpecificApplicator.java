/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.network.applicator;

import java.util.EnumSet;
import java.util.Optional;
import com.eucalyptus.cluster.common.broadcast.BNetworkInfo;
import com.eucalyptus.cluster.common.broadcast.BNIProperty;
import com.eucalyptus.network.NetworkMode;
import com.eucalyptus.util.TypedKey;
import com.google.common.collect.Iterables;
import io.vavr.collection.Array;
import io.vavr.control.Option;

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

  private NetworkMode extractMode( final BNetworkInfo networkInfo ) {
    final Option<BNIProperty> property =
        networkInfo.configuration( ).properties( )
            .filter( prop -> "mode".equals( prop.name( ) ) )
            .filter( BNIProperty.class::isInstance )
            .map( BNIProperty.class::cast )
            .headOption( );

    return NetworkMode.fromString(
        property.map( BNIProperty::values ).flatMap( Array::headOption ).getOrNull( ),
        NetworkMode.EDGE );
  }
}
