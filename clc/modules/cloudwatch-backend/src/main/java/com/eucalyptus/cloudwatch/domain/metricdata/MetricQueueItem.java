package com.eucalyptus.cloudwatch.domain.metricdata;


import com.eucalyptus.cloudwatch.common.backend.msgs.MetricDatum;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;

public class MetricQueueItem {
  private String accountId;
  private MetricDatum metricDatum;
  private String namespace;
  private MetricType metricType;
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
  public MetricType getMetricType() {
    return metricType;
  }
  public void setMetricType(MetricType metricType) {
    this.metricType = metricType;
  }
}
