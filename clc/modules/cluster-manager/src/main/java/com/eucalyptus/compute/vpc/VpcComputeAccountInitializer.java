/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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

import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateVpcType;
import com.eucalyptus.compute.common.internal.account.ComputeAccounts;
import com.eucalyptus.compute.common.network.Networking;
import com.eucalyptus.compute.common.network.NetworkingFeature;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.async.AsyncRequests;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.BaseCallerContext;
import edu.ucsb.eucalyptus.msgs.EvaluatedIamConditionKey;

/**
 *
 */
public class VpcComputeAccountInitializer implements ComputeAccounts.ComputeAccountInitializer {

  private final Logger logger = Logger.getLogger( VpcComputeAccountInitializer.class );

  @Override
  public void initialize( final String accountNumber ) {
    try {
      if ( VpcConfiguration.getDefaultVpc( ) && Networking.getInstance().supports( NetworkingFeature.Vpc ) ) {
        final ServiceConfiguration config = Topology.lookup( Compute.class );
        final CreateVpcType createVpc = new CreateVpcType( );
        createVpc.setCidrBlock( accountNumber );
        createVpc.setUserId( Accounts.lookupSystemAdmin().getUserId( ) );
        createVpc.setCallerContext( new BaseCallerContext( Lists.newArrayList(
            new EvaluatedIamConditionKey( Keys.AWS_SOURCEIP, Internets.localHostAddress() )
        ) ) );
        AsyncRequests.sendSync( config, createVpc );
      }
    } catch ( final Exception e ) {
      logger.error( "Error creating default vpc for account " + accountNumber, e );
    }
  }
}
