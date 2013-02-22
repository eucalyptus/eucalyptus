package com.eucalyptus.cloudwatch.domain.metricdata;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;

import com.eucalyptus.cloudwatch.domain.dimension.AbstractPersistentWithDimensions;

import edu.emory.mathcs.backport.java.util.Arrays;
@MappedSuperclass
public abstract class MetricEntity extends AbstractPersistentWithDimensions {

  @Column( name = "account_id" , nullable = false)
  private String accountId;
  @Column( name = "user_id" , nullable = false)
  private String userId;
  @Column( name = "namespace" , nullable = false)
  private String namespace; 
  @Column( name = "metric_name" , nullable = false)
  private String metricName;
  @Column( name = "dimension_hash" , nullable = false)
  private String dimensionHash;
  @Column( name = "units" , nullable = false)
  @Enumerated( EnumType.STRING )
  private Units units;
  @Column( name = "metric_type" , nullable = false)
  @Enumerated( EnumType.STRING )
  private MetricType metricType;
  @Column( name = "timestamp", nullable = false) 
  private Date timestamp;
  @Column ( name = "sample_size", nullable = false)
  private Double sampleSize;
  @Column ( name = "sample_max", nullable = false)
  private Double sampleMax;
  @Column ( name = "sample_min", nullable = false)
  private Double sampleMin;
  @Column ( name = "sample_sum", nullable = false)
  private Double sampleSum;
  
  public enum MetricType {
  	Custom,
  	System
  }
  public enum Units { 
  	Seconds,
  	Microseconds,
    Milliseconds,
    Bytes,
    Kilobytes,
    Megabytes,
    Gigabytes,
    Terabytes,
    Bits,
    Kilobits,
    Megabits,
    Gigabits,
    Terabits,
    Percent,
    Count,
    BytesPerSecond,
    KilobytesPerSecond,
    MegabytesPerSecond,
    GigabytesPerSecond,
    TerabytesPerSecond,
    BitsPerSecond,
    KilobitsPerSecond,
    MegabitsPerSecond,
    GigabitsPerSecond,
    TerabitsPerSecond,
    CountPerSecond,
    None("");
  	private String value;
  	Units() {
  		this.value = name().replace("Per", "/");
  	}
  	Units(String value) {
  		this.value = value;
  	}
  	@Override
  	public String toString() {
  		return value;
  	}
  }

  public String getAccountId() {
		return accountId;
	}
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}
  public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
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
	public String getDimensionHash() {
		return dimensionHash;
	}
	public void setDimensionHash(String dimensionHash) {
		this.dimensionHash = dimensionHash;
	}
	public Units getUnits() {
		return units;
	}
	public void setUnits(Units units) {
		this.units = units;
	}
	public MetricType getMetricType() {
		return metricType;
	}
	public void setMetricType(MetricType metricType) {
		this.metricType = metricType;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public Double getSampleSize() {
		return sampleSize;
	}
	public void setSampleSize(Double sampleSize) {
		this.sampleSize = sampleSize;
	}
	public Double getSampleMax() {
		return sampleMax;
	}
	public void setSampleMax(Double sampleMax) {
		this.sampleMax = sampleMax;
	}
	public Double getSampleMin() {
		return sampleMin;
	}
	public void setSampleMin(Double sampleMin) {
		this.sampleMin = sampleMin;
	}
	public Double getSampleSum() {
		return sampleSum;
	}
	public void setSampleSum(Double sampleSum) {
		this.sampleSum = sampleSum;
	}
	@Override
  public String toString() {
	  return "MetricEntity [accountId=" + accountId + ", userId=" + userId
	      + ", namespace=" + namespace + ", metricName=" + metricName
	      + ", dimensionHash=" + dimensionHash + ", units=" + units
	      + ", metricType=" + metricType + ", timestamp=" + timestamp
	      + ", sampleSize=" + sampleSize + ", sampleMax=" + sampleMax
	      + ", sampleMin=" + sampleMin + ", sampleSum=" + sampleSum
	      + ", getDimensions()=" + getDimensions() + "]";
  }
}