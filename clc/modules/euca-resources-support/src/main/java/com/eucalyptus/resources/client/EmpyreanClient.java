/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
