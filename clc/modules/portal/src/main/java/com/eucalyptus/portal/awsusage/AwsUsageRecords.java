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
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Date;


public abstract class AwsUsageRecords {
  private static Logger LOG=
          Logger.getLogger(AwsUsageRecords.class );
  final static AwsUsageRecords instance = new AwsUsageHourlyRecordsEntity();
  public static final AwsUsageRecords getInstance() {
    return instance;
  }

  public abstract AwsUsageHourlyRecordBuilder newRecord(final String accountNumber);
  public abstract void append(final Collection<AwsUsageRecord> records);
  public abstract Collection<AwsUsageRecord> query(final String accountNumber, final String service,
                                                   final String operation, final String usageType,
                                                   final Date startDate, final Date endDate);
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
    public Collection<AwsUsageRecord> query(String accountNumber, String service, String operation, String usageType, Date startDate, Date endDate) {
      return Lists.newArrayList();
    }

    @Override
    public void purge(String accountNumber, Date beginning) {

    }
  }
}
