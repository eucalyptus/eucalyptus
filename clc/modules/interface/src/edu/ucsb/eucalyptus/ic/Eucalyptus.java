/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.ic;

import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import edu.ucsb.eucalyptus.constants.EventType;
import edu.ucsb.eucalyptus.msgs.DescribeRegionsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeRegionsType;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.EventRecord;
import edu.ucsb.eucalyptus.msgs.RegionInfoType;
import edu.ucsb.eucalyptus.transport.OverloadedWebserviceMethod;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import edu.ucsb.eucalyptus.util.Messaging;
import org.apache.log4j.Logger;

public class Eucalyptus {

  private static Logger LOG = Logger.getLogger( Eucalyptus.class );

  @OverloadedWebserviceMethod( actions = {
      "AllocateAddress", "AssociateAddress", "AuthorizeSecurityGroupIngress",
      "ConfirmProductInstance", "CreateKeyPair", "CreateSecurityGroup",
      "DeleteKeyPair", "DeleteSecurityGroup", "DeregisterImage",
      "DescribeAddresses", "DescribeAvailabilityZones", "DescribeImageAttribute",
      "DescribeImages", "DescribeInstances", "DescribeKeyPairs",
      "DescribeResources", "DescribeSecurityGroups", "DisassociateAddress",
      "EucalyptusErrorMessage", "GetConsoleOutput", "ModifyImageAttribute",
      "RebootInstances", "RegisterImage", "ReleaseAddress",
      "ResetImageAttribute", "RevokeSecurityGroupIngress",
      "RunInstances", "TerminateInstances", "AddCluster",
      "CreateVolume", "CreateSnapshot", "DeleteVolume", "DeleteSnapshot",
      "DescribeVolumes", "DescribeSnapshots", "AttachVolume", "DetachVolume",
      "DescribeRegions",
      "BundleInstance","DescribeBundleTasks","CancelBundleTask",
      "DescribeReservedInstances","DescribeReservedInstancesOfferings","PurchaseReservedInstancesOffering" } )
  public EucalyptusMessage handle( EucalyptusMessage msg ) {
    if( msg instanceof DescribeRegionsType ) {
      DescribeRegionsResponseType reply = (DescribeRegionsResponseType ) msg.getReply();
      try {
        SystemConfiguration config = EucalyptusProperties.getSystemConfiguration();
        reply.getRegionInfo().add(new RegionInfoType( "Eucalyptus", config.getStorageUrl().replaceAll( "Walrus", "Eucalyptus" )));
        reply.getRegionInfo().add(new RegionInfoType( "Walrus", config.getStorageUrl()));
      } catch ( EucalyptusCloudException e ) {}
      return reply;
    }
    LOG.info( EventRecord.create( this.getClass().getSimpleName(), msg.getUserId(), msg.getCorrelationId(), EventType.MSG_RECEIVED, msg.getClass().getSimpleName() ) );
    long startTime = System.currentTimeMillis();
    Messaging.dispatch( "vm://RequestQueue", msg );
    EucalyptusMessage reply = null;
    reply = ReplyQueue.getReply( msg.getCorrelationId() );
    LOG.info( EventRecord.create( this.getClass().getSimpleName(), msg.getUserId(), msg.getCorrelationId(), EventType.MSG_SERVICED, ( System.currentTimeMillis() - startTime ) ) );
    if ( reply == null )
      return new EucalyptusErrorMessageType( this.getClass().getSimpleName(), msg, "Received a NULL reply" );
    return reply;
  }

}
