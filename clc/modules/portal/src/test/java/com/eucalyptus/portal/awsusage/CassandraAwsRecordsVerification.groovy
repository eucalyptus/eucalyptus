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
package com.eucalyptus.portal.awsusage

import com.datastax.driver.core.SimpleStatement
import com.eucalyptus.portal.workflow.AwsUsageRecord
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Created by ethomas on 3/20/17.
 */
@CompileStatic
class CassandraAwsRecordsVerificationImpl {

  static Collection<AwsUsageRecordForEquality> queryHourlyLocally(Collection<AwsUsageRecordForEquality> sample,
                         String accountNumber, String service, String operation,
                         String usageType, Date startDate, Date endDate) {

    return sample.findAll { \
    it.ownerAccountNumber == accountNumber && it.service == service && \
     (operation == null || it.operation == operation) && \
     (usageType == null || it.usageType.startsWith(usageType)) && \
     (startDate == null || it.endTime >= startDate) && \
     (endDate == null || it.endTime <= endDate)
    };
  }

  @CompileStatic
  @EqualsAndHashCode
  @ToString
  static class AwsUsageRecordForEquality {
    String ownerAccountNumber;
    String service;
    String operation;
    String usageType;
    String resource;
    Date startTime;
    Date endTime;
    String usageValue;
  }

  static String verify() {
    if (!"cassandra".equals(CassandraSessionManager.DB_TO_USE)) {
      return "Error: not configured to use cassandra";
    }

// dates rounded to the hour
    Calendar NOWCAL = new GregorianCalendar();
    NOWCAL.setTime(new Date());
    NOWCAL.set(Calendar.MILLISECOND, 0);
    NOWCAL.set(Calendar.SECOND, 0);
    NOWCAL.set(Calendar.MINUTE, 0);

    Date NOW = NOWCAL.getTime();
    Date ONE_HOUR_AGO = new Date(NOW.getTime() - 3600 * 1000L);
    Date TWO_HOURS_AGO = new Date(ONE_HOUR_AGO.getTime() - 3600 * 1000L);

    Date ONE_HOUR_FROM_NOW = new Date(NOW.getTime() + 3600 * 1000L);

    String OPERATION1 = "operation1";
    String OPERATION2 = "operation2";

    String SERVICE1 = "service1";
    String SERVICE2 = "service2";

    String ACCOUNT1 = "test_1_" + UUID.randomUUID(); // I know this is not what account ids look like
    String ACCOUNT2 = "test_2_" + UUID.randomUUID();

    String RESOURCE1 = "resource1";
    String RESOURCE2 = "resource2";
    String RESOURCE3 = "resource3";
    String RESOURCE4 = "resource4";

    String USAGETYPE1 = "usageType1";
    String USAGETYPE2 = "usageType2";

    Collection<AwsUsageRecordForEquality> records = [
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT1, service: SERVICE1, operation: OPERATION1, usageType: USAGETYPE1,
          startTime: TWO_HOURS_AGO, endTime: NOW, usageValue: "1.0"),
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT1, service: SERVICE1, operation: OPERATION1, usageType: USAGETYPE2,
          resource: RESOURCE1, startTime: ONE_HOUR_AGO, endTime: ONE_HOUR_FROM_NOW, usageValue: "2.0"),
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT1, service: SERVICE1, operation: OPERATION1, usageType: USAGETYPE2,
          resource: RESOURCE2, startTime: TWO_HOURS_AGO, endTime: ONE_HOUR_AGO, usageValue: "3.0"),
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT1, service: SERVICE1, operation: OPERATION2, usageType: USAGETYPE1,
          startTime: ONE_HOUR_AGO, endTime: NOW, usageValue: "4.0"),
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT1, service: SERVICE1, operation: OPERATION2, usageType: USAGETYPE2,
          resource: RESOURCE1, startTime: ONE_HOUR_AGO, endTime: ONE_HOUR_FROM_NOW, usageValue: "5.0"),
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT1, service: SERVICE1, operation: OPERATION2, usageType: USAGETYPE2,
          resource: RESOURCE2, startTime: TWO_HOURS_AGO, endTime: NOW, usageValue: "6.0"),
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT1, service: SERVICE2, operation: OPERATION1, usageType: USAGETYPE1,
          startTime: NOW, endTime: NOW, usageValue: "7.0"),
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT1, service: SERVICE2, operation: OPERATION1, usageType: USAGETYPE2,
          resource: RESOURCE1, startTime: TWO_HOURS_AGO, endTime: ONE_HOUR_FROM_NOW, usageValue: "8.0"),
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT1, service: SERVICE2, operation: OPERATION1, usageType: USAGETYPE2,
          resource: RESOURCE2, startTime: ONE_HOUR_AGO, endTime: ONE_HOUR_AGO, usageValue: "9.0"),
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT1, service: SERVICE2, operation: OPERATION2, usageType: USAGETYPE1,
          startTime: ONE_HOUR_AGO, endTime: NOW, usageValue: "10.0"),
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT1, service: SERVICE2, operation: OPERATION2, usageType: USAGETYPE2,
          resource: RESOURCE1, startTime: TWO_HOURS_AGO, endTime: NOW, usageValue: "11.0"),
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT1, service: SERVICE2, operation: OPERATION2, usageType: USAGETYPE2,
          resource: RESOURCE2, startTime: NOW, endTime: ONE_HOUR_FROM_NOW, usageValue: "12.0"),
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT2, service: SERVICE1, operation: OPERATION1, usageType: USAGETYPE1,
          startTime: NOW, endTime: ONE_HOUR_FROM_NOW, usageValue: "13.0"),
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT2, service: SERVICE1, operation: OPERATION1, usageType: USAGETYPE2,
          resource: RESOURCE3, startTime: TWO_HOURS_AGO, endTime: ONE_HOUR_AGO, usageValue: "14.0"),
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT2, service: SERVICE1, operation: OPERATION1, usageType: USAGETYPE2,
          resource: RESOURCE4, startTime: ONE_HOUR_AGO, endTime: NOW, usageValue: "15.0"),
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT2, service: SERVICE1, operation: OPERATION2, usageType: USAGETYPE1,
          startTime: ONE_HOUR_AGO, endTime: NOW, usageValue: "16.0"),
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT2, service: SERVICE1, operation: OPERATION2, usageType: USAGETYPE2,
          resource: RESOURCE3, startTime: TWO_HOURS_AGO, endTime: ONE_HOUR_AGO, usageValue: "17.0"),
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT2, service: SERVICE1, operation: OPERATION2, usageType: USAGETYPE2,
          resource: RESOURCE4, startTime: NOW, endTime: ONE_HOUR_FROM_NOW, usageValue: "18.0"),
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT2, service: SERVICE2, operation: OPERATION1, usageType: USAGETYPE1,
          startTime: TWO_HOURS_AGO, endTime: NOW, usageValue: "19.0"),
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT2, service: SERVICE2, operation: OPERATION1, usageType: USAGETYPE2,
          resource: RESOURCE3, startTime: ONE_HOUR_AGO, endTime: ONE_HOUR_FROM_NOW, usageValue: "20.0"),
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT2, service: SERVICE2, operation: OPERATION1, usageType: USAGETYPE2,
          resource: RESOURCE4, startTime: NOW, endTime: ONE_HOUR_FROM_NOW, usageValue: "21.0"),
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT2, service: SERVICE2, operation: OPERATION2, usageType: USAGETYPE1,
          startTime: ONE_HOUR_AGO, endTime: NOW, usageValue: "22.0"),
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT2, service: SERVICE2, operation: OPERATION2, usageType: USAGETYPE2,
          resource: RESOURCE3, startTime: NOW, endTime: ONE_HOUR_FROM_NOW, usageValue: "23.0"),
      new AwsUsageRecordForEquality
        (ownerAccountNumber: ACCOUNT2, service: SERVICE2, operation: OPERATION2, usageType: USAGETYPE2,
          resource: RESOURCE4, startTime: TWO_HOURS_AGO, endTime: NOW, usageValue: "24.0"),
    ];

    def makeAwsUsageRecord = { AwsUsageRecordForEquality it ->
      AwsUsageRecords.instance.newRecord(it.ownerAccountNumber)
        .withService(it.service)
        .withOperation(it.operation)
        .withUsageType(it.usageType)
        .withResource(it.resource)
        .withStartTime(it.startTime)
        .withEndTime(it.endTime)
        .withUsageValue(it.usageValue)
        .build()
    };

    def makeAwsUsageRecordForEquality = { AwsUsageRecord it ->
      new AwsUsageRecordForEquality
        (ownerAccountNumber: it.ownerAccountNumber, service: it.service, operation: it.operation, usageType: it.usageType,
          resource: it.resource, startTime: it.startTime, endTime: it.endTime, usageValue: it.usageValue);
    };

    try {
      AwsUsageRecords.getInstance().append(records.collect(makeAwsUsageRecord));
      for (String accountId: [ACCOUNT1, ACCOUNT2]) {
        for (String service : [SERVICE1, SERVICE2]) {
          for (String operation : [OPERATION1, OPERATION2, null]) {
            for (String usageType : [USAGETYPE1, USAGETYPE1, "usageType*", null]) {
              for (Date startDate : [TWO_HOURS_AGO, ONE_HOUR_AGO, NOW, ONE_HOUR_FROM_NOW, null]) {
                for (Date endDate : [TWO_HOURS_AGO, ONE_HOUR_AGO, NOW, ONE_HOUR_FROM_NOW, null]) {
                  assert (
                    queryHourlyLocally(records, accountId, service, operation, usageType, startDate, endDate).toSet() ==
                    AwsUsageRecords.getInstance().queryHourly(accountId, service, operation, usageType, startDate, endDate).collect(makeAwsUsageRecordForEquality).toSet()
                  )
                }
              }
            }
          }
        }
      }
      for (String accountId: [ACCOUNT1, ACCOUNT2]) {
        for (String service : [SERVICE1, SERVICE2]) {
          CassandraSessionManager.doWithSession{ it.execute(
            new SimpleStatement("DELETE FROM eucalyptus_billing.aws_records where account_id=? AND service=?", accountId, service)
          ) }
          for (String resource: [RESOURCE1, RESOURCE2, RESOURCE3, RESOURCE4]) {
            CassandraSessionManager.doWithSession{ it.execute(
              new SimpleStatement("DELETE FROM eucalyptus_billing.aws_records_by_resource where account_id=? AND service=? AND resource=?", accountId, service, resource)
            ) }
          }
        }
      }
    } catch (Exception e) {
      return "Error: " + e.getMessage() == null ? e.getClass().getName() : e.getMessage();
    }
    return "Success"

  }
}

CassandraAwsRecordsVerificationImpl.verify();