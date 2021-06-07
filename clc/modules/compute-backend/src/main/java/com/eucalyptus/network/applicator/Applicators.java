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

import java.util.Iterator;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.common.Cluster;
import com.eucalyptus.cluster.common.broadcast.BNetworkInfo;
import com.google.common.collect.ImmutableList;

/**
 *
 */
public class Applicators {

  private static final Logger logger = Logger.getLogger( Applicators.class );

  private static final ImmutableList<Applicator> applicators = ImmutableList.of(
      new FileOutputApplicator( ),
      new AppliedVersionApplicator( ),
      new ClearDirtyPublicAddressesApplicator( ),
      new BroadcastingApplicator( )
  );

  public static void apply(
      final Iterable<Cluster> clusters,
      final BNetworkInfo networkInfo
  ) throws ApplicatorException {

    final ApplicatorContext context = new ApplicatorContext( clusters, networkInfo );

    final ApplicatorChain chain = chain( applicators.iterator( ) );

    chain.applyNext( context );
  }

  private static ApplicatorChain chain( final Iterator<Applicator> applicatorIterator ) {
    if ( applicatorIterator.hasNext( ) ) {
      final Applicator applicator = applicatorIterator.next( );
      final ApplicatorChain chain = chain( applicatorIterator );
      return context -> applicator.apply( context, chain );
    } else {
      return context -> logger.trace( "Applicator chain completed." );
    }
  }

}
