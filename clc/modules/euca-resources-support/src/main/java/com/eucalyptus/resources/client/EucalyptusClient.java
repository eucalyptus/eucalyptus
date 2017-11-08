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
