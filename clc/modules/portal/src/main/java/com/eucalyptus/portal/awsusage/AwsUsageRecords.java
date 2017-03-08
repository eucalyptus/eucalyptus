/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.portal.awsusage;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.portal.workflow.AwsUsageRecord;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


public abstract class AwsUsageRecords {
  private static Logger LOG=
          Logger.getLogger(AwsUsageRecords.class );
  final static AwsUsageRecords instance = new AwsUsageHourlyRecordsEntity();
  public static final AwsUsageRecords getInstance() {
    return instance;
  }

  public abstract AwsUsageHourlyRecordBuilder newRecord(final String accountNumber);
  public abstract void append(final Collection<AwsUsageRecord> records);
  public abstract Collection<AwsUsageRecord> queryHourly( final String accountNumber, final String service,
                                                   final String operation, final String usageType,
                                                   final Date startDate, final Date endDate);
  public Collection<AwsUsageRecord> queryDaily( final String accountNumber, final String service,
                                                         final String operation, final String usageType,
                                                         final Date startDate, final Date endDate ) {
    final List<AwsUsageRecord> hourlyRecords = Lists.newArrayList(
            queryHourly(accountNumber, service, operation, usageType, startDate, endDate)
    );

    final Calendar calDay = Calendar.getInstance();
    for (int i = 0; i< hourlyRecords.size(); i++) {
      final AwsUsageRecord firstRecord = hourlyRecords.get(i);
      if (firstRecord.getUsageValue() == null )
        continue;

      calDay.setTime(firstRecord.getStartTime());
      int day = calDay.get(Calendar.DAY_OF_MONTH);
      long aggregatedValue = Long.parseLong(firstRecord.getUsageValue());
      final String svc = firstRecord.getService();
      final String op = firstRecord.getOperation();
      final String type = firstRecord.getUsageType();
      final String res = firstRecord.getResource();

      for (int j = i+1; j < hourlyRecords.size(); j++) {
        final AwsUsageRecord curRecord = hourlyRecords.get(j);
        calDay.setTime(curRecord.getStartTime());

        if (day != calDay.get(Calendar.DAY_OF_MONTH)) {
          // assumption: record is ordered in time
          break;
        } else {
          if ( (svc != null && !svc.equals(curRecord.getService()))
                  || (op != null && !op.equals(curRecord.getOperation()))
                  || (type != null && !type.equals(curRecord.getUsageType()))
                  || (res != null && !res.equals(curRecord.getResource()))) {
            continue;
          }

          // when reached here, it's the same record type in the same day
          if (curRecord.getUsageValue() != null) {
            aggregatedValue += Long.parseLong(curRecord.getUsageValue());
            curRecord.setUsageValue(null);
          }
        }
      }
      firstRecord.setUsageValue(String.format("%d", aggregatedValue));
    }

    final List<AwsUsageRecord> dailyRecords =
            hourlyRecords.stream()
            .filter( rr -> rr.getUsageValue() != null )
            .collect(Collectors.toList());

    dailyRecords.stream()
            .forEach( rr -> {
              final Date day = getBeginningOfDay(rr.getStartTime());
              rr.setStartTime(day);
              rr.setEndTime(getNextDay(day));
            });

    return dailyRecords;
  }

  public  Collection<AwsUsageRecord> queryMonthly( final String accountNumber, final String service,
                                                   final String operation, final String usageType,
                                                   final Date startDate, final Date endDate ) {
    final List<AwsUsageRecord> dailyRecords =
            Lists.newArrayList(queryDaily(accountNumber, service, operation, usageType, startDate, endDate));

    final Calendar calMonth = Calendar.getInstance();
    for (int i = 0; i< dailyRecords.size(); i++) {
      final AwsUsageRecord firstRecord = dailyRecords.get(i);
      if (firstRecord.getUsageValue() == null )
        continue;

      calMonth.setTime(firstRecord.getStartTime());
      int month = calMonth.get(Calendar.MONTH);
      long aggregatedValue = Long.parseLong(firstRecord.getUsageValue());
      final String svc = firstRecord.getService();
      final String op = firstRecord.getOperation();
      final String type = firstRecord.getUsageType();
      final String res = firstRecord.getResource();

      for (int j = i+1; j < dailyRecords.size(); j++) {
        final AwsUsageRecord curRecord = dailyRecords.get(j);
        calMonth.setTime(curRecord.getStartTime());

        if (month != calMonth.get(Calendar.MONTH)) {
          // assumption: record is ordered in time
          break;
        } else {
          if ( (svc != null && !svc.equals(curRecord.getService()))
                  || (op != null && !op.equals(curRecord.getOperation()))
                  || (type != null && !type.equals(curRecord.getUsageType()))
                  || (res != null && !res.equals(curRecord.getResource()))) {
            continue;
          }

          // when reached here, it's the same record type in the same month
          if (curRecord.getUsageValue() != null) {
            aggregatedValue += Long.parseLong(curRecord.getUsageValue());
            curRecord.setUsageValue(null);
          }
        }
      }
      firstRecord.setUsageValue(String.format("%d", aggregatedValue));
    }

    final List<AwsUsageRecord> monthlyRecords =
            dailyRecords.stream()
                    .filter( rr -> rr.getUsageValue() != null )
                    .collect(Collectors.toList());

    monthlyRecords.stream()
            .forEach( rr -> {
              final Date month = getFirstDayOfMonth(rr.getStartTime());
              rr.setStartTime(month);
              rr.setEndTime(getNextMonth(month));
            });

    return monthlyRecords;
  }

  private static Date getFirstDayOfMonth(final Date time) {
    final Calendar c = Calendar.getInstance();
    c.setTime(time);
    c.set(Calendar.DAY_OF_MONTH, 1);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTime();
  }

  private static Date getNextMonth(final Date time) {
    final Calendar c = Calendar.getInstance();
    c.setTime(time);
    c.set(Calendar.MONTH, c.get(Calendar.MONTH) + 1);
    return c.getTime();
  }

  private static Date getBeginningOfDay(final Date time) {
    final Calendar c = Calendar.getInstance();
    c.setTime(time);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTime();
  }

  private static Date getNextDay(final Date time) {
    final Calendar c = Calendar.getInstance();
    c.setTime(time);
    c.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH) + 1);
    return c.getTime();
  }

  public abstract void purge(final String accountNumber, final Date beginning);


  public static abstract class AwsUsageHourlyRecordBuilder {
    AwsUsageRecord instance = null;
    private AwsUsageHourlyRecordBuilder(final String accountId) {
      instance = init(accountId);
    }

    protected abstract AwsUsageRecord init(final String accountId);

    AwsUsageHourlyRecordBuilder withService(final String service) {
      instance.setService(service);
      return this;
    }

    AwsUsageHourlyRecordBuilder withOperation(final String operation) {
      instance.setOperation(operation);
      return this;
    }

    AwsUsageHourlyRecordBuilder withResource(final String resource) {
      instance.setResource(resource);
      return this;
    }

    AwsUsageHourlyRecordBuilder withUsageType(final String usageType) {
      instance.setUsageType(usageType);
      return this;
    }

    AwsUsageHourlyRecordBuilder withStartTime(final Date startTime) {
      instance.setStartTime(startTime);
      return this;
    }

    AwsUsageHourlyRecordBuilder withEndTime(final Date endTime) {
      instance.setEndTime(endTime);
      return this;
    }

    AwsUsageHourlyRecordBuilder withUsageValue(final String usageValue) {
      instance.setUsageValue(usageValue);
      return this;
    }

    AwsUsageRecord build() {
      return instance;
    }
  }

  private static class AwsUsageHourlyRecordsEntity extends AwsUsageRecords {
    @Override
    public AwsUsageHourlyRecordBuilder newRecord(String accountNumber) {
      return new AwsUsageHourlyRecordBuilder(accountNumber) {
        @Override
        protected AwsUsageRecord init(String accountId) {
          return new AwsUsageRecordEntity(accountId);
        }
      };
    }

    @Override
    public void append(Collection<AwsUsageRecord> records) {
      try(final TransactionResource db=Entities.transactionFor(AwsUsageRecordEntity.class )){
        records.stream().forEach( r -> Entities.persist(r));
        db.commit();
      }catch(final Exception ex){
        LOG.error("Failed to add records", ex);
      }
    }

    @Override
    public Collection<AwsUsageRecord> queryHourly(String accountNumber, String service, String operation, String usageType, Date startDate, Date endDate) {
      try (final TransactionResource db = Entities.transactionFor(AwsUsageRecordEntity.class)) {
        Criteria criteria = Entities.createCriteria(AwsUsageRecordEntity.class);

        if (accountNumber != null) {
          criteria = criteria.add(Restrictions.eq("ownerAccountNumber", accountNumber));
        }

        if (service != null) {
          criteria = criteria.add(Restrictions.eq("service", service));
        }

        if (operation != null) {
          criteria = criteria.add(Restrictions.eq("operation", operation));
        }

        if (usageType != null) {
          criteria = criteria.add(Restrictions.eq("usageType", operation));
        }

        if (startDate != null) {
          criteria = criteria.add(Restrictions.ge("endTime", startDate));
        }

        if (endDate != null) {
          criteria = criteria.add(Restrictions.le("endTime", endDate));
        }

        final List<AwsUsageRecordEntity> entities = (List<AwsUsageRecordEntity>) criteria.list();
        return entities.stream()
                .map(e -> (AwsUsageRecord) e)
                .collect(Collectors.toList());
      } catch (final Exception ex) {
        LOG.error("Failed to query aws usage record entity", ex);
        return Lists.newArrayList();
      }
    }

    @Override
    public void purge(String accountNumber, Date beginning) {

    }
  }
}
