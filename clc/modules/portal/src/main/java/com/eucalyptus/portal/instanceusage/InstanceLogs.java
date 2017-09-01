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

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.portal.Ec2ReportsInvalidParameterException;
import com.eucalyptus.portal.common.model.InstanceUsageFilter;
import com.eucalyptus.portal.common.model.InstanceUsageFilters;
import com.eucalyptus.portal.workflow.InstanceLog;
import com.eucalyptus.portal.workflow.InstanceTag;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public abstract class InstanceLogs {
  private static Logger LOG =
          Logger.getLogger(InstanceLogs.class );
  final static InstanceLogs instance = new InstanceHourRecordsEntity();

  public static final InstanceLogs getInstance() {
    return instance;
  }
  public abstract InstanceLogBuilder newRecord(final String accountNumber);
  public abstract void append(final Collection<InstanceLog> records);
  public abstract List<InstanceHourLog> queryHourly(String accountId, Date rangeStart, Date rangeEnd, InstanceUsageFilters filters)
          throws Ec2ReportsInvalidParameterException;
  public List<InstanceHourLog> queryDaily(String accountId, Date rangeStart, Date rangeEnd, InstanceUsageFilters filters)
          throws Ec2ReportsInvalidParameterException
  {
    final List<InstanceHourLog> hourlyLogs = queryHourly(accountId, rangeStart, rangeEnd, filters);
    final Map<String, List<InstanceHourLog>> groupById = hourlyLogs.stream()
            .collect(
                    groupingBy(l -> l.getInstanceId(),
                            mapping(Function.identity(),
                                    collectingAndThen(
                                            Collectors.toList(),
                                            l -> l.stream().sorted(Comparator.comparing(InstanceHourLog::getLogTime)).collect(Collectors.toList()))
                            )
                    )
            );

    final Map<String, List<InstanceHourLog>> dailyLog = Maps.newHashMap();
    for (final String instanceId : groupById.keySet()) {
      dailyLog.put(instanceId, Lists.newArrayList());
      final List<InstanceHourLog> instanceLogs = groupById.get(instanceId);
      int day = 32; // list is sorted by log time
      for (int i = 0; i < instanceLogs.size(); i++) {
        final InstanceHourLog log = instanceLogs.get(i);
        if (getDay(log.getLogTime()) != day) {  // it's a new day
          // sum hours
          day = getDay(log.getLogTime());
          long hours = log.getHours();
          int j;
          for (j = i + 1; j < instanceLogs.size() && day == getDay(instanceLogs.get(j).getLogTime()); j++) {
            hours+=instanceLogs.get(j).getHours();
          }
          log.setHours(hours);
          dailyLog.get(instanceId).add(log);
          i = j-1;
        }
      }
    }

    dailyLog.values().stream()
            .forEach(li -> li.stream().forEach(
                    l -> l.setLogTime(firstHourOfDay(l.getLogTime()))
            )
    );
    // the order of records doesn't matter
    return dailyLog.values().stream()
            .flatMap(l -> l.stream())
            .collect(Collectors.toList());
  }

  public List<InstanceHourLog> queryMonthly(String accountId, Date rangeStart, Date rangeEnd, InstanceUsageFilters filters)
          throws Ec2ReportsInvalidParameterException
  {
    final List<InstanceHourLog> dailyLogs = queryDaily(accountId, rangeStart, rangeEnd, filters);
    final Map<String, List<InstanceHourLog>> groupById = dailyLogs.stream()
            .collect(
                    groupingBy(l -> l.getInstanceId(),
                            mapping(Function.identity(),
                                    collectingAndThen(
                                            Collectors.toList(),
                                            l -> l.stream().sorted(Comparator.comparing(InstanceHourLog::getLogTime)).collect(Collectors.toList()))
                            )
                    )
            );

    final Map<String, List<InstanceHourLog>> monthlyLog = Maps.newHashMap();
    for (final String instanceId : groupById.keySet()) {
      monthlyLog.put(instanceId, Lists.newArrayList());
      final List<InstanceHourLog> instanceLogs = groupById.get(instanceId);
      int month = 13; // list is sorted by log time
      for (int i = 0; i < instanceLogs.size(); i++) {
        final InstanceHourLog log = instanceLogs.get(i);
        if (getMonth(log.getLogTime()) != month) {  // it's a new month
          // sum hours
          month = getMonth(log.getLogTime());
          long hours = log.getHours();
          int j;
          for (j = i + 1; j < instanceLogs.size() && month == getMonth(instanceLogs.get(j).getLogTime()); j++) {
            hours+=instanceLogs.get(j).getHours();
          }
          log.setHours(hours);
          monthlyLog.get(instanceId).add(log);
          i = j-1;
        }
      }
    }

    monthlyLog.values().stream()
            .forEach(li -> li.stream().forEach(
                    l -> l.setLogTime(firstDayOfMonth(l.getLogTime()))
                    )
            );
    // the order of records doesn't matter
    return monthlyLog.values().stream()
            .flatMap(l -> l.stream())
            .collect(Collectors.toList());
  }

  private static int getDay(final Date time) {
    final Calendar c = Calendar.getInstance();
    c.setTime(time);
    return c.get(Calendar.DAY_OF_MONTH);
  }

  private static int getMonth(final Date time) {
    final Calendar c = Calendar.getInstance();
    c.setTime(time);
    return c.get(Calendar.MONTH);
  }

  private static Date firstHourOfDay(final Date time) {
    final Calendar c = Calendar.getInstance();
    c.setTime(time);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTime();
  }

  private static Date firstDayOfMonth(final Date time) {
    final Calendar c = Calendar.getInstance();
    c.setTime(time);
    c.set(Calendar.DAY_OF_MONTH, 1);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTime();
  }

  public static abstract class InstanceLogBuilder {
    Optional<InstanceLog> instance = Optional.empty();
    private InstanceLogBuilder(final String accountId) {
      instance = Optional.of(init());
      instance.get().setAccountId(accountId);
    }

    protected abstract InstanceLog init();

    public InstanceLogBuilder withInstanceType(final String instanceType) {
      if (this.instance.isPresent()) {
        this.instance.get().setInstanceType(instanceType);
      }
      return this;
    }

    public InstanceLogBuilder withPlatform(final String platform) {
      if (this.instance.isPresent()) {
        this.instance.get().setPlatform(platform);
      }
      return this;
    }

    public InstanceLogBuilder withAvailabilityZone(final String availabilityZone) {
      if (this.instance.isPresent()) {
        this.instance.get().setAvailabilityZone(availabilityZone);
      }
      return this;
    }

    public InstanceLogBuilder withRegion(final String region) {
      if (this.instance.isPresent()) {
        this.instance.get().setRegion(region);
      }
      return this;
    }

    public InstanceLogBuilder withInstanceId(final String instanceId) {
      if (this.instance.isPresent()) {
        this.instance.get().setInstanceId(instanceId);
      }
      return this;
    }

    public InstanceLogBuilder withLogTime(final Date time) {
      if (this.instance.isPresent()) {
        this.instance.get().setLogTime(time);
      }
      return this;
    }

    public InstanceLogBuilder addTag(final String tagKey, final String tagValue) {
      if (this.instance.isPresent()) {
        this.instance.get().getTags().add(new InstanceTag() {
          private String key;
          private String value;
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
        });
      }
      return this;
    }

    public InstanceLogBuilder empty() {
      this.instance = Optional.empty();
      return this;
    }

    public InstanceLogBuilder merge(final InstanceLogBuilder other) {
      if ( this.instance.isPresent() &&
              other.instance.isPresent() ) {
        final InstanceLog thisObj = this.instance.get();
        final InstanceLog otherObj = other.instance.get();
        if (otherObj.getPlatform()!=null) {
          thisObj.setPlatform(otherObj.getPlatform());
        }
        if (otherObj.getAvailabilityZone()!=null) {
          thisObj.setAvailabilityZone(otherObj.getAvailabilityZone());
        }
        if (otherObj.getInstanceType()!=null) {
          thisObj.setInstanceType(otherObj.getInstanceType());
        }
        if (otherObj.getTags() != null && !otherObj.getTags().isEmpty() ) {
          otherObj.getTags().stream().forEach( t -> thisObj.addTag(t) );
        }
      } else {
        this.instance = Optional.empty();
      }
      return this;
    }

    public Optional<InstanceLog> build() {
      return this.instance;
    }
  }

  public static class InstanceHourRecordsEntity extends InstanceLogs {
    @Override
    public InstanceLogBuilder newRecord(String accountNumber) {
      return new InstanceLogBuilder(accountNumber) {
        @Override
        protected InstanceLog init() {
          return new InstanceLogEntity();
        }

        @Override
        public InstanceLogBuilder addTag(final String tagKey, final String tagValue) {
          if (this.instance.isPresent()) {
            this.instance.get().addTag(new InstanceTagEntity(tagKey, tagValue));
          }
          return this;
        }
      };
    }

    @Override
    public void append(Collection<InstanceLog> records) {
      try(final TransactionResource db = Entities.transactionFor(InstanceLogEntity.class )){
        records.stream().forEach( r -> Entities.persist(r));
        db.commit();
      }catch(final Exception ex){
        LOG.error("Failed to add instance usage log", ex);
      }
    }

    @Override
    public List<InstanceHourLog> queryHourly(final String accountNumber, final Date rangeStart, final Date rangeEnd, final InstanceUsageFilters filters)
            throws Ec2ReportsInvalidParameterException {
      try (final TransactionResource db = Entities.transactionFor(InstanceLogEntity.class)) {
        Criteria criteria = Entities.createCriteria(InstanceLogEntity.class);

        if (accountNumber != null) {
          criteria = criteria.add(Restrictions.eq("ownerAccountNumber", accountNumber));
        }
        if (rangeStart != null) {
          criteria = criteria.add(Restrictions.ge("logTime", rangeStart));
        }
        if (rangeEnd != null) {
          criteria = criteria.add(Restrictions.le("logTime", rangeEnd));
        }

        if (filters!= null && filters.getMember()!=null) {
          final Map<String, List<InstanceUsageFilter>> filtersByType =
                  filters.getMember().stream()
                          .filter( f -> f.getType() != null && f.getKey() != null )
                          .collect(Collectors.groupingBy( InstanceUsageFilter::getType, toList()));
          for (final String type : filtersByType.keySet()) {
            Criterion criterion = null;
            for (final InstanceUsageFilter filter : filtersByType.get(type)) {
              if ("instancetype".equals(type.toLowerCase()) || "instance_type".equals(type.toLowerCase())) {
                criterion = criterion == null ? Restrictions.eq("instanceType", filter.getKey() )
                        : Restrictions.or(criterion,  Restrictions.eq("instanceType", filter.getKey() ) );
              } else if ("platform".equals(type.toLowerCase()) || "platforms".equals(type.toLowerCase())) {
                criterion = criterion == null ? Restrictions.eq("platform", filter.getKey() )
                        : Restrictions.or(criterion,  Restrictions.eq("platform", filter.getKey() ) );
              } else if ("availabilityzone".equals(type.toLowerCase()) || "availability_zone".equals(type.toLowerCase())) {
                criterion = criterion == null ? Restrictions.eq("availabilityZone", filter.getKey() )
                        : Restrictions.or(criterion,  Restrictions.eq("availabilityZone", filter.getKey() ) );
              }
            }
            if (criterion != null)
              criteria = criteria.add(criterion);
          }
        }

        List<InstanceLogEntity> results = (List<InstanceLogEntity>) criteria.list();
        if (filters!= null && filters.getMember()!=null) {
          // key: tag_key, value: set of tag values
          final Map<String, Set<String>> tagFilters =
                  filters.getMember().stream()
                          .filter ( f -> f.getType() != null )
                          .filter( f -> "tag".equals(f.getType().toLowerCase()) || "tags".equals(f.getType().toLowerCase()) )
                          .collect( Collectors.groupingBy(
                                  InstanceUsageFilter::getKey, Collectors.mapping(
                                          InstanceUsageFilter::getValue, toSet()
                                  )) );
          if (!tagFilters.isEmpty()) {
            results = results.stream()
                    .filter(l -> l.getTags().stream()
                            .filter(t -> tagFilters.containsKey(t.getKey()) && tagFilters.get(t.getKey()).contains(t.getValue()))
                            .findAny()
                            .isPresent()
                    ).collect(Collectors.toList());
          }
        }
        return results.stream()
                .map( l -> new InstanceHourLogImpl(l, 1))
                .collect(Collectors.toList());
      } catch ( final Exception ex) {
        LOG.error("Failed to query instance log", ex);
        return Lists.newArrayList();
      }
    }
  }

  private static class SimpleInstanceLog implements InstanceLog {
    private String accountId;
    private String instanceId;
    private String instanceType;
    private String platform;
    private String region;
    private String availabilityZone;
    private Date logTime;
    private List<InstanceTag> tags = Lists.newArrayList();

    public SimpleInstanceLog() {
    }

    @Override
    public String getAccountId() {
      return accountId;
    }

    @Override
    public void setAccountId(String accountId) {
      this.accountId = accountId;
    }

    @Override
    public String getInstanceId() {
      return instanceId;
    }

    @Override
    public void setInstanceId(String instanceId) {
      this.instanceId = instanceId;
    }

    @Override
    public String getInstanceType() {
      return instanceType;
    }

    @Override
    public void setInstanceType(String instanceType) {
      this.instanceType = instanceType;
    }

    @Override
    public String getPlatform() {
      return platform;
    }

    @Override
    public void setPlatform(String platform) {
      this.platform = platform;
    }

    @Override
    public String getRegion() {
      return region;
    }

    @Override
    public void setRegion(String region) {
      this.region = region;
    }

    @Override
    public String getAvailabilityZone() {
      return availabilityZone;
    }

    @Override
    public void setAvailabilityZone(String az) {
      this.availabilityZone = az;
    }

    @Override
    public List<InstanceTag> getTags() {
      return tags;
    }

    @Override
    public void addTag(InstanceTag tag) {
      tags.add(new SimpleInstanceTag(tag));
    }

    @Override
    public Date getLogTime() {
      return logTime;
    }

    @Override
    public void setLogTime(Date date) {
      this.logTime = date;
    }
  }

  private static class SimpleInstanceTag implements InstanceTag {

    private String tagKey;
    private String tagValue;

    public SimpleInstanceTag() {
    }

    public SimpleInstanceTag(String tagKey, String tagValue) {
      this.tagKey = tagKey;
      this.tagValue = tagValue;
    }

    public SimpleInstanceTag(InstanceTag tag) {
      this.tagKey = tag.getKey();
      this.tagValue = tag.getValue();
    }
    @Override
    public String getKey() {
      return tagKey;

    }
    @Override
    public void setKey(String key) {
      this.tagKey = key;
    }

    @Override
    public String getValue() {
      return tagValue;
    }
    @Override
    public void setValue(String value) {
      this.tagValue = value;
    }
  }
}
