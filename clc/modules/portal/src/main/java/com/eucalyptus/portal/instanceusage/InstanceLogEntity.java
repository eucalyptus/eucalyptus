/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.portal.instanceusage;

import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.portal.workflow.InstanceLog;
import com.eucalyptus.portal.workflow.InstanceTag;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.PersistenceContext;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@PersistenceContext( name = "eucalyptus_billing" )
@Table( name = "instance_log" )
public class InstanceLogEntity extends AbstractPersistent implements InstanceLog {
  @Column( name = "account_id", nullable = false )
  private String ownerAccountNumber;

  @Column( name = "instance_id", nullable = false )
  private String instanceId;

  @Column( name = "instance_type", nullable = false )
  private String instanceType;

  @Column( name = "platform", nullable = false )
  private String platform;

  @Column( name = "region" )
  private String region;

  @Column( name = "availability_zone", nullable = false )
  private String availabilityZone;

  @Column ( name = "log_time", nullable = false )
  private Date logTime;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable( name = "instance_tag",
          joinColumns = @JoinColumn( name = "instancelog_id", referencedColumnName = "id"))
  private Collection<InstanceTagEntity> tags = null;

  public InstanceLogEntity() { }
  public InstanceLogEntity(final String accountId) {
    this.ownerAccountNumber = accountId;
  }

  @Override
  public String getInstanceType() {
    return this.instanceType;
  }

  @Override
  public void setInstanceType(String instanceType) {
    this.instanceType = instanceType;
  }

  @Override
  public String getPlatform() {
    return this.platform;
  }

  @Override
  public void setPlatform(String platform) {
    this.platform = platform;
  }

  @Override
  public String getRegion() {
    return this.region;
  }

  @Override
  public void setRegion(String region) {
    this.region = region;
  }

  @Override
  public String getAvailabilityZone() {
    return this.availabilityZone;
  }

  @Override
  public void setAvailabilityZone(String az) {
    this.availabilityZone = az;
  }

  @Override
  public String getAccountId() {
    return this.ownerAccountNumber;
  }

  @Override
  public void setAccountId(String accountId) {
    this.ownerAccountNumber = accountId;
  }

  @Transient
  private ImmutableList<InstanceTag> immutableTags = null;

  @PostLoad
  private void onLoad(){
    immutableTags = ImmutableList.copyOf(
            tags != null ?
                    tags.stream()
                            .map( t -> new InstanceTag() {
                              private String key = t.getKey();
                              private String value = t.getValue();

                              @Override
                              public String getKey() {
                                return this.key;
                              }

                              @Override
                              public void setKey(String key) {
                                this.key = key;
                              }

                              @Override
                              public String getValue() {
                                return this.value;
                              }

                              @Override
                              public void setValue(String value) {
                                this.value = value;
                              }
                            })
                            .collect(Collectors.toList()) : Lists.newArrayList());
  }

  @Override
  public List<InstanceTag> getTags() {
    if (immutableTags == null) {
      onLoad();
    }
    return immutableTags;
  }

  @Override
  public void addTag(InstanceTag tag) {
    if (tags == null) {
      tags = Lists.newArrayList();
    }
    if (tag instanceof  InstanceTagEntity) {
      tags.add((InstanceTagEntity) tag);
    } else {
      final InstanceTagEntity tagEntity = new InstanceTagEntity(tag.getKey(), tag.getValue());
      tags.add(tagEntity);
    }
  }

  @Override
  public Date getLogTime() {
    return this.logTime;
  }

  @Override
  public void setLogTime(Date date) {
    this.logTime = date;
  }

  @Override
  public String getInstanceId() {
    return this.instanceId;
  }

  @Override
  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }
}
