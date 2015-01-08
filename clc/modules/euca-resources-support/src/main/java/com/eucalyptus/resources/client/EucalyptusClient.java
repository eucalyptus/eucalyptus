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

import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.backend.DescribeInstanceTypesResponseType;
import com.eucalyptus.compute.common.backend.DescribeInstanceTypesType;
import com.eucalyptus.compute.common.VmTypeDetails;
import com.eucalyptus.resources.EucalyptusActivityException;
import com.eucalyptus.util.DispatchingClient;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Callback.Checked;
import com.eucalyptus.util.async.CheckedListenableFuture;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 * @author Sang-Min Park
 * 
 * The client for euca-specific service operations such as describeInstanceTypes
 *
 */
public class EucalyptusClient {
  private static EucalyptusClient _instance = null;
  private EucalyptusClient(){ }
  public static EucalyptusClient getInstance(){
    if(_instance == null)
      _instance = new EucalyptusClient();
    return _instance;
  }
  
  private class EucalyptusBaseSystemContext extends AbstractClientContext<BaseMessage, Eucalyptus> {
    private EucalyptusBaseSystemContext() {
      super(null, Eucalyptus.class);
    }
  }

  private class EucalyptusDescribeVMTypesTask extends
      EucalyptusClientTask<BaseMessage, Eucalyptus> {
    private List<VmTypeDetails> types = null;

    private DescribeInstanceTypesType describeVMTypes() {
      final DescribeInstanceTypesType req = new DescribeInstanceTypesType();
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<BaseMessage, Eucalyptus> context,
        Checked<BaseMessage> callback) {
      final DispatchingClient<BaseMessage, Eucalyptus> client = context
          .getClient();
      client.dispatch(describeVMTypes(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<BaseMessage, Eucalyptus> context,
        BaseMessage response) {
      final DescribeInstanceTypesResponseType resp = (DescribeInstanceTypesResponseType) response;
      this.types = resp.getInstanceTypeDetails();
    }

    public List<VmTypeDetails> getVMTypes() {
      return this.types;
    }
  }
  
  public List<VmTypeDetails> describeVMTypes() {
    final EucalyptusDescribeVMTypesTask task = new EucalyptusDescribeVMTypesTask();
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new EucalyptusBaseSystemContext());
    try {
      if (result.get()) {
        final List<VmTypeDetails> describe = task.getVMTypes();
        return describe;
      } else
        throw new EucalyptusActivityException("failed to describe the vm types");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }
}
