package com.eucalyptus.cloudwatch.domain.listmetrics;

import javax.persistence.Column;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;

import com.eucalyptus.cloudwatch.domain.dimension.AbstractPersistentWithDimensions;

@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_cloudwatch")
@Table(name="list_metrics")
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class ListMetric extends AbstractPersistentWithDimensions {
  @Override
  public String toString() {
    return "ListMetric [getAccountId()=" + getAccountId() + ", getNamespace()="
        + getNamespace() + ", getMetricName()=" + getMetricName()
        + ", getDimensions()=" + getDimensions() + "]";
  }

  public static final int MAX_DIM_NUM = 10;
  private static final Logger LOG = Logger.getLogger(ListMetric.class);
  public ListMetric() {
    super();
  }
  
  public String getAccountId() {
    return accountId;
  }
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }
  public String getNamespace() {
    return namespace;
  }
  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }
  public String getMetricName() {
    return metricName;
  }
  public void setMetricName(String metricName) {
    this.metricName = metricName;
  }

  @Column( name = "account_id" , nullable = false)
  private String accountId;
  @Column( name = "namespace" , nullable = false)
  private String namespace; 
  @Column( name = "metric_name" , nullable = false)
  private String metricName;
}