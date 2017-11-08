/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
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
