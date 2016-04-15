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

import java.util.Iterator;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.NetworkInfo;
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
      final NetworkInfo networkInfo
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
