/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.compute.vpc;

import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.euare.events.AccountCreatedEvent;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateVpcResponseType;
import com.eucalyptus.compute.common.CreateVpcType;
import com.eucalyptus.compute.common.network.Networking;
import com.eucalyptus.compute.common.network.NetworkingFeature;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.concurrent.ListenableFuture;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.BaseCallerContext;
import edu.ucsb.eucalyptus.msgs.EvaluatedIamConditionKey;

/**
 *
 */
public class VpcNewAccountEventListener implements EventListener<AccountCreatedEvent> {

  private static final Logger logger = Logger.getLogger( VpcNewAccountEventListener.class );

  public static void register( ) {
    Listeners.register( AccountCreatedEvent.class, new VpcNewAccountEventListener() );
  }

  @Override
  public void fireEvent( final AccountCreatedEvent event ) {
    if ( VpcConfiguration.getDefaultVpc( ) && Networking.getInstance( ).supports( NetworkingFeature.Vpc ) ) try {
      final ServiceConfiguration config = Topology.lookup( Compute.class );
      final CreateVpcType createVpc = new CreateVpcType( );
      createVpc.setCidrBlock( event.getMessage( ) );
      createVpc.setUserId( Accounts.lookupSystemAdmin( ).getUserId( ) );
      createVpc.setCallerContext( new BaseCallerContext( Lists.newArrayList(
          new EvaluatedIamConditionKey( Keys.AWS_SOURCEIP, Internets.localHostAddress( ) )
      ) ) );
      final ListenableFuture<CreateVpcResponseType> dispatchFuture = AsyncRequests.dispatch( config, createVpc );
      dispatchFuture.addListener( new Runnable( ) {
        @Override
        public void run() {
          try {
            dispatchFuture.get( );
          } catch ( final InterruptedException e ) {
            logger.info( "Interrupted while creating default vpc for account " + event.getMessage( ), e );
          } catch ( final ExecutionException e ) {
            logger.error( "Error creating default vpc for account " + event.getMessage( ), e );
          }
        }
      } );
    } catch ( final Exception e ) {
      logger.error( "Error creating default vpc for account " + event.getMessage( ), e );
    }
  }
}
