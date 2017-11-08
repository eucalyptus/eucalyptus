/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.cluster.callback.reporting;


import com.eucalyptus.cloudwatch.common.msgs.MetricDatum;
import com.google.common.base.Predicate;

public class AbsoluteMetricQueueItem {
  private final long itemCreated = System.currentTimeMillis( );
  private String accountId;
  private MetricDatum metricDatum;
  private String namespace;
  public String getAccountId() {
    return accountId;
  }
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }
  public MetricDatum getMetricDatum() {
    return metricDatum;
  }
  public void setMetricDatum(MetricDatum metricDatum) {
    this.metricDatum = metricDatum;
  }
  public String getNamespace() {
    return namespace;
  }
  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public static Predicate<AbsoluteMetricQueueItem> createdBefore( final long timestamp ) {
    return new Predicate<AbsoluteMetricQueueItem>( ) {
      @Override
      public boolean apply( final AbsoluteMetricQueueItem absoluteMetricQueueItem ) {
        return absoluteMetricQueueItem.itemCreated < timestamp;
      }
    };
  }
}
