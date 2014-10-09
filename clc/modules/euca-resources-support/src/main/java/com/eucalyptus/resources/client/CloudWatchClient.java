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
