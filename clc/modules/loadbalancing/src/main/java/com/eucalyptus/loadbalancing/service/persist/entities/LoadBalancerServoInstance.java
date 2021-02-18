/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.loadbalancing.service.persist.entities;

import java.util.Calendar;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerServoInstanceView;

/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 */
@Entity
@PersistenceContext(name = "eucalyptus_loadbalancing")
@Table(name = "metadata_servo_instance")
public class LoadBalancerServoInstance extends AbstractPersistent
    implements LoadBalancerServoInstanceView {
  private static Logger LOG = Logger.getLogger(LoadBalancerServoInstance.class);

  private static final long serialVersionUID = 1L;

  public enum STATE {
    Pending, InService, Error, OutOfService, Retired
  }

  public enum DNS_STATE {
    Registered, Deregistered, None
  }

  @ManyToOne
  @JoinColumn(name = "metadata_zone_fk", nullable = true)
  private LoadBalancerZone zone = null;

  @ManyToOne
  @JoinColumn(name = "metadata_group_fk", nullable = true)
  private LoadBalancerSecurityGroup security_group = null;

  @ManyToOne
  @JoinColumn(name = "metadata_asg_fk", nullable = true)
  private LoadBalancerAutoScalingGroup autoscaling_group = null;

  @Column(name = "metadata_instance_id", nullable = false, unique = true)
  private String instanceId = null;

  @Column(name = "metadata_state", nullable = false)
  private String state = null;

  @Column(name = "metadata_address", nullable = true)
  private String address = null;

  @Column(name = "metadata_private_ip", nullable = true)
  private String privateIp = null;

  @Column(name = "metadata_dns_state", nullable = true)
  private String dnsState = null;

  @Column(name = "metadata_certificate_expiration_date", nullable = true)
  private Date certificateExpirationDate = null;

  @Column(name = "metadata_activity_failure_count", nullable = true)
  private Integer activityFailureCount = null;

  @Column(name = "metadata_activity_failure_update_time", nullable = true)
  private Date activityFailureUpdateTime = null;

  private LoadBalancerServoInstance() {
  }

  private LoadBalancerServoInstance(final LoadBalancerZone lbzone) {
    this.state = STATE.Pending.name();
    this.zone = lbzone;
  }

  private LoadBalancerServoInstance(final LoadBalancerZone lbzone,
      final LoadBalancerSecurityGroup group) {
    this.state = STATE.Pending.name();
    this.zone = lbzone;
    this.security_group = group;
  }

  public static LoadBalancerServoInstance newInstance(final LoadBalancerZone lbzone,
      final LoadBalancerSecurityGroup group,
      final LoadBalancerAutoScalingGroup as_group,
      final int certExpirationDays,
      String instanceId) {
    final LoadBalancerServoInstance instance = new LoadBalancerServoInstance(lbzone, group);
    instance.setInstanceId(instanceId);
    instance.setAutoScalingGroup(as_group);
    instance.dnsState = DNS_STATE.None.name();
    final Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    cal.add(Calendar.DATE, certExpirationDays);
    instance.setCertificateExpiration(cal.getTime());
    return instance;
  }

  public static LoadBalancerServoInstance named(final LoadBalancerZone lbzone) {
    final LoadBalancerServoInstance sample = new LoadBalancerServoInstance(lbzone);
    return sample;
  }

  public static LoadBalancerServoInstance named(String instanceId) {
    final LoadBalancerServoInstance sample = new LoadBalancerServoInstance();
    sample.instanceId = instanceId;
    return sample;
  }

  public static LoadBalancerServoInstance named() {
    return new LoadBalancerServoInstance();
  }

  public static LoadBalancerServoInstance withState(String state) {
    final LoadBalancerServoInstance sample = new LoadBalancerServoInstance();
    sample.state = state;
    return sample;
  }

  public void setInstanceId(String id) {
    this.instanceId = id;
  }

  public String getInstanceId() {
    return this.instanceId;
  }

  public void setState(STATE update) {
    this.state = update.name();
  }

  public STATE getState() {
    return Enum.valueOf(STATE.class, this.state);
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getAddress() {
    return this.address;
  }

  public void setSecurityGroup(LoadBalancerSecurityGroup group) {
    this.security_group = group;
  }

  public void setAutoScalingGroup(LoadBalancerAutoScalingGroup group) {
    this.autoscaling_group = group;
  }

  public void setAvailabilityZone(LoadBalancerZone zone) {
    this.zone = zone;
  }

  public LoadBalancerZone getAvailabilityZone() {
    return this.zone;
  }

  public LoadBalancerAutoScalingGroup getAutoScalingGroup() {
    return this.autoscaling_group;
  }

  public String getPrivateIp() {
    return this.privateIp;
  }

  public void setPrivateIp(final String ipAddr) {
    this.privateIp = ipAddr;
  }

  public void setDnsState(final DNS_STATE dnsState) {
    this.dnsState = dnsState.toString();
  }

  public DNS_STATE getDnsState() {
    return Enum.valueOf(DNS_STATE.class, this.dnsState);
  }

  public void setCertificateExpiration(final Date expirationDate) {
    this.certificateExpirationDate = expirationDate;
  }

  public Date getCertificateExpiration() {
    return this.certificateExpirationDate;
  }

  public boolean isCertificateExpired() {
    if (this.certificateExpirationDate == null) {
      return false;
    }
    if ((new Date()).after(this.certificateExpirationDate)) {
      return true;
    } else {
      return false;
    }
  }

  public int getActivityFailureCount() {
    if (this.activityFailureCount == null) {
      return 0;
    } else {
      return this.activityFailureCount;
    }
  }

  public void setActivityFailureCount(final int count) {
    this.activityFailureCount = count;
  }

  public Date getActivityFailureUpdateTime() {
    return this.activityFailureUpdateTime;
  }

  public void setActivityFailureUpdateTime(final Date updateTime) {
    this.activityFailureUpdateTime = updateTime;
  }

  @Override
  public String toString() {
    String id = this.instanceId == null ? "unassigned" : this.instanceId;
    return String.format("Servo-instance (%s) for %s", id, this.zone.getName());
  }
}
