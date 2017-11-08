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

import com.eucalyptus.cloudwatch.common.CloudWatch;
import com.eucalyptus.cloudwatch.common.msgs.CloudWatchMessage;
import com.eucalyptus.cloudwatch.common.msgs.MetricData;
import com.eucalyptus.cloudwatch.common.msgs.PutMetricDataResponseType;
import com.eucalyptus.cloudwatch.common.msgs.PutMetricDataType;
import com.eucalyptus.resources.EucalyptusActivityException;
import com.eucalyptus.util.DispatchingClient;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Callback.Checked;
import com.eucalyptus.util.async.CheckedListenableFuture;

/**
 * @author Sang-Min Park
 *
 */
public class CloudWatchClient {
  private static CloudWatchClient _instance = null;
  private CloudWatchClient(){ }
  public static CloudWatchClient getInstance(){
    if(_instance == null)
      _instance = new CloudWatchClient();
    return _instance;
  }
  private class CloudWatchContext extends AbstractClientContext<CloudWatchMessage, CloudWatch> {
    private CloudWatchContext(final String userId){
      super(userId, CloudWatch.class);
    }
  }

  private class CloudWatchPutMetricDataTask extends
  EucalyptusClientTask<CloudWatchMessage, CloudWatch> {
    private MetricData metricData = null;
    private String namespace = null;

    private CloudWatchPutMetricDataTask(final String namespace,
        final MetricData data) {
      this.namespace = namespace;
      this.metricData = data;
    }

    private PutMetricDataType putMetricData() {
      final PutMetricDataType request = new PutMetricDataType();
      request.setNamespace(this.namespace);
      request.setMetricData(this.metricData);
      return request;
    }

    @Override
    void dispatchInternal(
        ClientContext<CloudWatchMessage, CloudWatch> context,
        Checked<CloudWatchMessage> callback) {
      final DispatchingClient<CloudWatchMessage, CloudWatch> client = context
          .getClient();
      client.dispatch(putMetricData(), callback);

    }

    @Override
    void dispatchSuccess(
        ClientContext<CloudWatchMessage, CloudWatch> context,
        CloudWatchMessage response) {
      // TODO Auto-generated method stub
      final PutMetricDataResponseType resp = (PutMetricDataResponseType) response;
    }
  }

  public void putCloudWatchMetricData(final String userId,
      final String namespace, final MetricData data) {
    final CloudWatchPutMetricDataTask task = new CloudWatchPutMetricDataTask(
        namespace, data);

    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new CloudWatchContext(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException(
            "failed to remove multi A records");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }
}
