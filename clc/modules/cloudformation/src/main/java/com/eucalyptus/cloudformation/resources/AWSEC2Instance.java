/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudformation.resources;

import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.util.async.AsyncRequests;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.DescribeInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeInstancesType;
import edu.ucsb.eucalyptus.msgs.RunInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesType;

/**
 * Created by ethomas on 12/18/13.
 */
public class AWSEC2Instance extends Resource {

  public AWSEC2Instance() {
    setType("AWS::EC2::Instance");
  }

  @Override
  public void create() throws Exception {
    String imageId = getPropertiesJSON().getString("ImageId"); // TODO: check more fields (later)
    RunInstancesType runInstancesType = new RunInstancesType();
    runInstancesType.setImageId(imageId);
    runInstancesType.setMinCount(1);
    runInstancesType.setMaxCount(1);
    final ComponentId componentId = ComponentIds.lookup(Eucalyptus.class);
    ServiceConfiguration configuration;
    if ( componentId.isAlwaysLocal() ||
      ( BootstrapArgs.isCloudController() && componentId.isCloudLocal() && !componentId.isRegisterable() ) ) {
      configuration = ServiceConfigurations.createEphemeral(componentId);
    } else {
      configuration = Topology.lookup(Eucalyptus.class);
    }
    runInstancesType.setEffectiveUserId(getOwnerUserId());
    RunInstancesResponseType runInstancesResponseType = AsyncRequests.<RunInstancesType,RunInstancesResponseType> sendSync(configuration, runInstancesType);
    setPhysicalResourceId(runInstancesResponseType.getRsvInfo().getInstancesSet().get(0).getInstanceId());
    for (int i=0;i<24;i++) { // sleeping for 5 seconds 24 times... (2 minutes)
      Thread.sleep(5000L);
      DescribeInstancesType describeInstancesType = new DescribeInstancesType();
      describeInstancesType.setInstancesSet(Lists.newArrayList(getPhysicalResourceId()));
      describeInstancesType.setEffectiveUserId(getOwnerUserId());
      DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.<DescribeInstancesType,DescribeInstancesResponseType> sendSync(configuration, describeInstancesType);
      if ("running".equals(describeInstancesResponseType.getReservationSet().get(0).getInstancesSet().get(0).getStateName())) {
        return;
      }
    }
    throw new Exception("Timeout");
  }


  @Override
  public void delete() throws Exception {
    if (getPhysicalResourceId() == null) return;
    TerminateInstancesType terminateInstancesType = new TerminateInstancesType();
    terminateInstancesType.setInstancesSet(Lists.newArrayList(getPhysicalResourceId()));
    final ComponentId componentId = ComponentIds.lookup(Eucalyptus.class);
    ServiceConfiguration configuration;
    if ( componentId.isAlwaysLocal() ||
      ( BootstrapArgs.isCloudController() && componentId.isCloudLocal() && !componentId.isRegisterable() ) ) {
      configuration = ServiceConfigurations.createEphemeral(componentId);
    } else {
      configuration = Topology.lookup(Eucalyptus.class);
    }
    terminateInstancesType.setEffectiveUserId(getOwnerUserId());
    AsyncRequests.<TerminateInstancesType,TerminateInstancesResponseType> sendSync(configuration, terminateInstancesType);
    boolean terminated = false;
    for (int i=0;i<24;i++) { // sleeping for 5 seconds 24 times... (2 minutes)
      Thread.sleep(5000L);
      DescribeInstancesType describeInstancesType = new DescribeInstancesType();
      describeInstancesType.setInstancesSet(Lists.newArrayList(getPhysicalResourceId()));
      describeInstancesType.setEffectiveUserId(getOwnerUserId());
      DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.<DescribeInstancesType,DescribeInstancesResponseType> sendSync(configuration, describeInstancesType);
      if ("terminated".equals(describeInstancesResponseType.getReservationSet().get(0).getInstancesSet().get(0).getStateName())) {
        terminated = true;
        break;
      }
    }
    if (!terminated) throw new Exception("Timeout");
    // terminate one more time, just to make sure it is gone
    terminateInstancesType = new TerminateInstancesType();
    terminateInstancesType.setInstancesSet(Lists.newArrayList(getPhysicalResourceId()));
    terminateInstancesType.setEffectiveUserId(getOwnerUserId());
    AsyncRequests.<TerminateInstancesType,TerminateInstancesResponseType> sendSync(configuration, terminateInstancesType);
    terminated = false;
    for (int i=0;i<24;i++) { // sleeping for 5 seconds 24 times... (2 minutes)
      Thread.sleep(5000L);
      DescribeInstancesType describeInstancesType = new DescribeInstancesType();
      describeInstancesType.setInstancesSet(Lists.newArrayList(getPhysicalResourceId()));
      describeInstancesType.setEffectiveUserId(getOwnerUserId());
      DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.<DescribeInstancesType,DescribeInstancesResponseType> sendSync(configuration, describeInstancesType);
      if (describeInstancesResponseType.getReservationSet().size() == 0) {
        terminated = true;
        break;
      }
    }
    if (!terminated) throw new Exception("Timeout");
  }

  @Override
  public void rollback() throws Exception {
    delete();
  }

  @Override
  public Object referenceValue() {
    return getPhysicalResourceId();
  }
}
