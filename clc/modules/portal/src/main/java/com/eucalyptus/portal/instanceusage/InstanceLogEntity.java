/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
    immutableTags = ImmutableList.<InstanceTag>copyOf(
            tags != null ?
                    tags.stream()
                            .<InstanceTag>map( t -> new InstanceTag() {
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
                            .collect(Collectors.<InstanceTag>toList()) : Lists.<InstanceTag>newArrayList());
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
