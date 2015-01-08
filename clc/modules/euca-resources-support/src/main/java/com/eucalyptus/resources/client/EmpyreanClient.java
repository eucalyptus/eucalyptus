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
package com.eucalyptus.resources.client;

import java.util.List;

import com.eucalyptus.empyrean.DescribeServicesResponseType;
import com.eucalyptus.empyrean.DescribeServicesType;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.empyrean.EmpyreanMessage;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.resources.EucalyptusActivityException;
import com.eucalyptus.util.DispatchingClient;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Callback.Checked;
import com.eucalyptus.util.async.CheckedListenableFuture;
/**
 * @author Sang-Min Park
 *
 */
public class EmpyreanClient {
  private static EmpyreanClient _instance = null;
  private EmpyreanClient(){ }
  public static EmpyreanClient getInstance(){
    if(_instance == null)
      _instance = new EmpyreanClient();
    return _instance;
  }
  
  private class EmpyreanSystemContext extends AbstractClientContext<EmpyreanMessage, Empyrean> {
    private EmpyreanSystemContext() {
      super(null, Empyrean.class);
    }
  }

  private class EucalyptusDescribeServicesTask extends
  EucalyptusClientTask<EmpyreanMessage, Empyrean> {
    private String componentType = null;
    private List<ServiceStatusType> services = null;

    private EucalyptusDescribeServicesTask(final String componentType) {
      this.componentType = componentType;
    }

    private DescribeServicesType describeServices() {
      final DescribeServicesType req = new DescribeServicesType();
      req.setByServiceType(this.componentType);
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<EmpyreanMessage, Empyrean> context,
        Checked<EmpyreanMessage> callback) {
      final DispatchingClient<EmpyreanMessage, Empyrean> client = context
          .getClient();
      client.dispatch(describeServices(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<EmpyreanMessage, Empyrean> context,
        EmpyreanMessage response) {
      // TODO Auto-generated method stub
      final DescribeServicesResponseType resp = (DescribeServicesResponseType) response;
      this.services = resp.getServiceStatuses();
    }

    public List<ServiceStatusType> getServiceDetais() {
      return this.services;
    }
  }

  public List<ServiceStatusType> describeServices(final String componentType) {
    // LOG.info("calling describe-services -T "+componentType);
    final EucalyptusDescribeServicesTask serviceTask = new EucalyptusDescribeServicesTask(
        componentType);
    final CheckedListenableFuture<Boolean> result = serviceTask
        .dispatch(new EmpyreanSystemContext());
    try {
      if (result.get()) {
        return serviceTask.getServiceDetais();
      } else
        throw new EucalyptusActivityException("failed to describe services");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

}
