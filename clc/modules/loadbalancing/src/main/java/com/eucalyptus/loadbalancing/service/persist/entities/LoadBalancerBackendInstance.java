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
package com.eucalyptus.loadbalancing.service.persist.entities;

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.loadbalancing.activities.EucalyptusActivityTasks;
import com.eucalyptus.loadbalancing.service.InternalFailure400Exception;
import com.eucalyptus.loadbalancing.service.InvalidEndPointException;
import com.eucalyptus.loadbalancing.service.LoadBalancingException;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerBackendInstanceView;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park
 */
@Entity
@PersistenceContext(name = "eucalyptus_loadbalancing")
@Table(name = "metadata_backend_instance")
public class LoadBalancerBackendInstance extends UserMetadata<LoadBalancerBackendInstance.STATE>
    implements LoadBalancerBackendInstanceView {
  private static Logger LOG = Logger.getLogger(LoadBalancerBackendInstance.class);

  private static final long serialVersionUID = 1L;

  public enum STATE {
    InService, OutOfService, Unknown, Error
  }

  @ManyToOne()
  @JoinColumn(name = "metadata_loadbalancer_fk")
  private LoadBalancer loadbalancer = null;

  @ManyToOne()
  @JoinColumn(name = "metadata_zone_fk")
  private LoadBalancerZone zone = null;

  @Column(name = "reason_code", nullable = true)
  private String reasonCode = null;

  @Column(name = "description", nullable = true)
  private String description = null;

  @Column(name = "ip_address", nullable = true)
  private String ipAddress = null;

  @Column(name = "partition", nullable = true)
  private String partition = null;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "instance_update_timestamp", nullable = true)
  private Date instanceUpdateTimestamp = null;

  private LoadBalancerBackendInstance() {
    super(null, null);
  }

  private LoadBalancerBackendInstance(
      final OwnerFullName userFullName,
      final LoadBalancer lb,
      final LoadBalancerZone zone,
      final String vmId,
      final String ipAddress
  ) throws LoadBalancingException {
    super(userFullName, vmId);
    this.loadbalancer = lb;
    this.setState(STATE.OutOfService);
    this.setReasonCode("ELB");
    this.setDescription("Instance registration is still in progress.");
    this.updateInstanceStateTimestamp();
    this.setPartition(zone.getName());
    this.setIpAddress(ipAddress);
    this.setAvailabilityZone(zone);
  }

  public static LoadBalancerBackendInstance newInstance(
      final OwnerFullName userFullName,
      final LoadBalancer lb,
      final LoadBalancerZone zone,
      final String vmId,
      final String ipAddress
  ) throws LoadBalancingException {
    return new LoadBalancerBackendInstance(userFullName, lb, zone, vmId, ipAddress);
  }

  public static LoadBalancerBackendInstance named(final LoadBalancer lb, final String vmId) {
    LoadBalancerBackendInstance instance = new LoadBalancerBackendInstance();
    instance.setOwner(null);
    instance.setDisplayName(vmId);
    instance.setLoadBalancer(lb);
    instance.setState(null);
    instance.setStateChangeStack(null);
    instance.getUniqueName();
    return instance;
  }

  public static LoadBalancerBackendInstance named() {
    return new LoadBalancerBackendInstance();
  }

  public String getInstanceId() {
    return this.getDisplayName();
  }

  private void setLoadBalancer(final LoadBalancer lb) {
    this.loadbalancer = lb;
  }

  public LoadBalancer getLoadBalancer() {
    return this.loadbalancer;
  }

  public void setBackendState(final STATE state) {
    this.setState(state);
  }

  public STATE getBackendState() {
    return this.getState();
  }

  public String getReasonCode() {
    return this.reasonCode;
  }

  public void setReasonCode(final String reason) {
    this.reasonCode = reason;
  }

  public String getDescription() {
    return this.description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public void setAvailabilityZone(final LoadBalancerZone zone) {
    this.zone = zone;
  }

  public LoadBalancerZone getAvailabilityZone() {
    return this.zone;
  }

  public void updateInstanceStateTimestamp() {
    final long currentTime = System.currentTimeMillis();
    this.instanceUpdateTimestamp = new Date(currentTime);
  }

  public Date instanceStateLastUpdated() {
    return this.instanceUpdateTimestamp;
  }

  @Override
  public String getPartition() {
    return this.partition;
  }

  public void setPartition(final String partition) {
    this.partition = partition;
  }

  public String getIpAddress() {
    return this.ipAddress;
  }

  public void setIpAddress(final String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public Date getInstanceUpdateTimestamp() {
    return instanceUpdateTimestamp;
  }

  public void setInstanceUpdateTimestamp(final Date instanceUpdateTimestamp) {
    this.instanceUpdateTimestamp = instanceUpdateTimestamp;
  }

  @Override
  public FullName getFullName() {
    return FullName.create.vendor("euca")
        .region(ComponentIds.lookup(Eucalyptus.class).name())
        .namespace(this.getOwnerAccountNumber())
        .relativeId("loadbalancer-backend-instance", Objects.toString(this.getDisplayName(), ""));
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj.getClass() != LoadBalancerBackendInstance.class) {
      return false;
    }
    final LoadBalancerBackendInstance other = (LoadBalancerBackendInstance) obj;

    if (this.loadbalancer == null) {
      if (other.loadbalancer != null) {
        return false;
      }
    } else if (this.loadbalancer.getOwnerUserId() == null) {
      if (other.loadbalancer.getOwnerUserId() != null) {
        return false;
      }
    } else if (!this.loadbalancer.getOwnerUserId().equals(other.loadbalancer.getOwnerUserId())) {
      return false;
    }

    if (this.loadbalancer == null) {
      if (other.loadbalancer != null) {
        return false;
      }
    } else if (this.loadbalancer.getDisplayName() == null) {
      if (other.loadbalancer.getDisplayName() != null) {
        return false;
      }
    } else if (!this.loadbalancer.getDisplayName().equals(other.loadbalancer.getDisplayName())) {
      return false;
    }

    if (this.displayName == null) {
      if (other.displayName != null) {
        return false;
      }
    } else if (!this.displayName.equals(other.displayName)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result =
        prime * result + ((this.loadbalancer == null || this.loadbalancer.getOwnerUserId() == null)
            ? 0
            : this.loadbalancer.getOwnerUserId().hashCode());
    result =
        prime * result + ((this.loadbalancer == null || this.loadbalancer.getDisplayName() == null)
            ? 0
            : this.loadbalancer.getDisplayName().hashCode());
    result = prime * result + ((this.displayName == null)
        ? 0
        : this.displayName.hashCode());
    return result;
  }

  @Override
  public String toString() {
    return String.format("%s backend instance - %s", this.loadbalancer, this.getDisplayName());
  }

  @Override
  protected String createUniqueName() {
    return (this.loadbalancer != null && this.getDisplayName() != null)
        ? this.loadbalancer.getOwnerAccountNumber()
        + ":"
        + this.loadbalancer.getDisplayName()
        + ":"
        + this.getDisplayName()
        : null;
  }
}
