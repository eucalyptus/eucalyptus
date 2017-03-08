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
package com.eucalyptus.portal.monthlyreport;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.entities.AbstractPersistentSupport;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.portal.PortalMetadataException;
import com.eucalyptus.portal.PortalMetadataNotFoundException;
import com.eucalyptus.portal.workflow.AwsUsageRecord;
import com.eucalyptus.portal.common.PortalMetadata;
import com.eucalyptus.portal.workflow.MonthlyUsageRecord;
import com.eucalyptus.util.Exceptions;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

@ComponentNamed
public class MonthlyReports extends AbstractPersistentSupport<PortalMetadata.BillingReportMetadata, MonthlyReport, PortalMetadataException> {
  private static Logger LOG     = Logger.getLogger( MonthlyReports.class );

  private static MonthlyReports instance = new MonthlyReports();
  public static MonthlyReports getInstance() {
    return instance;
  }

  protected MonthlyReports( ) {
    super( "monthly-report" );
  }

  @SuppressWarnings( "ThrowableResultOfMethodCallIgnored" )
  @Override
  protected PortalMetadataException notFoundException(String message, Throwable cause) {
    final PortalMetadataNotFoundException exception = Exceptions.findCause( cause, PortalMetadataNotFoundException.class );
    if ( exception != null ) {
      return exception;
    }
    return new PortalMetadataNotFoundException( message, cause );  }

  @Override
  protected PortalMetadataException metadataException(String message, Throwable cause) {
    final PortalMetadataException exception = Exceptions.findCause( cause, PortalMetadataException.class );
    if ( exception != null ) {
      return exception;
    }
    return new PortalMetadataException( message, cause );
  }

  @Override
  protected MonthlyReport exampleWithOwner(OwnerFullName ownerFullName) {
    final MonthlyReport report = new MonthlyReport( );
    report.setOwner( ownerFullName );
    return report;
  }

  @Override
  protected MonthlyReport exampleWithName(OwnerFullName ownerFullName, String name) {
    final MonthlyReport report = new MonthlyReport( );
    report.setOwner( ownerFullName );
    report.setDisplayName( name );
    return report;
  }

  public MonthlyReport exampleWithYearMonth(OwnerFullName ownerFullName, String year, String month) {
    return exampleWithName(ownerFullName, String.format("%s-%s", year, month));
  }

  public void createOrUpdate(final OwnerFullName owner, final String year, final String month, List<MonthlyReportEntry> entries) {
    final MonthlyReport example = exampleWithYearMonth(owner, year, month);
    try {
      MonthlyReport report = null;
      try (final TransactionResource db = Entities.transactionFor(MonthlyReport.class)) {
        try {
          report = Entities.uniqueResult(example);
        } catch (final NoSuchElementException ex) {
          Entities.persist(example);
          db.commit();
        }
      }
      try (final TransactionResource db = Entities.transactionFor(MonthlyReport.class)) {
        report = Entities.uniqueResult(example);
        final List<MonthlyReportEntry> currentRecords = report.getEntries();
        for (final MonthlyReportEntry e : entries ) {
          final Optional<MonthlyReportEntry> found = currentRecords.stream()
                  .filter(r ->
                          (r.getProductCode() != null ? r.getProductCode().equals(e.getProductCode()) : true) &&
                                  (r.getOperation() != null ? r.getOperation().equals(e.getOperation()) : true) &&
                                  (r.getUsageType() != null ? r.getUsageType().equals(e.getUsageType()): true)
                  ).findAny();
          if (found.isPresent()) {
            found.get().setUsageQuantity(e.getUsageQuantity());
          } else {
            report.addEntry(e);
          }
        }
        Entities.persist(report);
        db.commit();
      }
    }catch (final Exception ex) {
      LOG.error("Failed to create or update monthly report record", ex);
    }
  }

  public static Function<AwsUsageRecord, Optional<MonthlyReportEntry>> transform = (record) -> {
    final AccountFullName owner = AccountFullName.getInstance(record.getOwnerAccountNumber());
    MonthlyReportEntryBuilder builder = null;
    // service name changes
    if ("AmazonEC2".equals(record.getService())) {
      builder = MonthlyReportEntryBuilder.forEc2(owner);
    } else if ("AmazonS3".equals(record.getService())) {
      builder = MonthlyReportEntryBuilder.forS3(owner);
    }

    final Optional<MonthlyReportEntryType> optType = MonthlyReportEntryType.getType(record);
    if (!optType.isPresent()) {
      return Optional.empty(); /// this aws usage record has no corresponding monthly report entry
    }
    final MonthlyReportEntryType entryType = optType.get();
    builder = builder
            .withUsageType(record.getUsageType())
            .withOperation(entryType.getOperation().isPresent() ? entryType.getOperation().get() : null )
            .withUsageQuantity(entryType.getQuantity(record.getUsageValue()))
            .withUsageStartDate(record.getStartTime()) // that's monthly start and end date of aws usage report
            .withUsageEndDate(record.getEndTime())
            .withBillingPeriodStartDate(record.getStartTime())
            .withBillingPeriodEndDate(record.getEndTime());
    return Optional.of(builder.build());
  };

  public static Function<MonthlyUsageRecord, MonthlyReportEntry> instantiate = (record) -> {
    final AccountFullName owner = AccountFullName.getInstance(record.getPayerAccountId());
    MonthlyReportEntryBuilder builder = null;
    // service name changes
    if ("AmazonEC2".equals(record.getProductCode())) {
      builder = MonthlyReportEntryBuilder.forEc2(owner);
    } else if ("AmazonS3".equals(record.getProductCode())) {
      builder = MonthlyReportEntryBuilder.forS3(owner);
    }

    builder = builder
            .withUsageType(record.getUsageType())
            .withOperation(record.getOperation())
            .withUsageQuantity(record.getUsageQuantity())
            .withUsageStartDate(record.getUsageStartDate()) // that's monthly start and end date of aws usage report
            .withUsageEndDate(record.getUsageEndDate())
            .withBillingPeriodStartDate(record.getBillingPeriodStartDate())
            .withBillingPeriodEndDate(record.getBillingPeriodEndDate());
    return builder.build();
  };

  public static class MonthlyReportEntryBuilder {
    private MonthlyReportEntry entry = null;
    private MonthlyReportEntryBuilder() {
      this.entry = new MonthlyReportEntry();
    }

    public static MonthlyReportEntryBuilder withDefault() {
      final MonthlyReportEntryBuilder builder = new MonthlyReportEntryBuilder();
      return builder.withSellerOfRecord("Amazon Web Services, Inc.");
    }

    public static MonthlyReportEntryBuilder withDefault(final AccountFullName owner) {
      try {
        return withDefault()
                .withPayerAccountId(owner.getAccountNumber())
                .withPayerAccountName(Accounts.lookupAccountAliasById(owner.getAccountNumber()));
      } catch (final AuthException ex) {
        throw Exceptions.toUndeclared(ex);
      }
    }

    public static MonthlyReportEntryBuilder forEc2(final AccountFullName owner) {
      return withDefault(owner)
              .withProductCode("AmazonEC2")
              .withProductName("Amazon Elastic Compute Cloud");
    }

    public static MonthlyReportEntryBuilder forS3(final AccountFullName owner) {
      return withDefault(owner)
              .withProductCode("AmazonS3")
              .withProductName("Amazon Simple Storage Service");
    }

    public static MonthlyReportEntryBuilder forDataTransfer(final AccountFullName owner) {
      return withDefault(owner)
              .withProductCode("AWSDataTransfer")
              .withProductName("AWS Data Transfer");
    }

    public MonthlyReportEntryBuilder withPayerAccountId(final String accountId) {
      this.entry.setPayerAccountId(accountId);
      return this;
    }

    public MonthlyReportEntryBuilder withBillingPeriodStartDate(final Date date) {
      this.entry.setBillingPeriodStartDate(date);
      return this;
    }

    public MonthlyReportEntryBuilder withBillingPeriodEndDate(final Date date) {
      this.entry.setBillingPeriodEndDate(date);
      return this;
    }

    public MonthlyReportEntryBuilder withPayerAccountName(final String accountName) {
      this.entry.setPayerAccountName(accountName);
      return this;
    }

    public MonthlyReportEntryBuilder withProductCode(final String productCode) {
      this.entry.setProductCode(productCode);
      return this;
    }

    public MonthlyReportEntryBuilder withProductName(final String productName) {
      this.entry.setProductName(productName);
      return this;
    }

    public MonthlyReportEntryBuilder withSellerOfRecord(final String sellerOfRecord) {
      this.entry.setSellerOfRecord(sellerOfRecord);
      return this;
    }

    public MonthlyReportEntryBuilder withUsageType(final String usageType) {
      this.entry.setUsageType(usageType);
      return this;
    }

    public MonthlyReportEntryBuilder withUsageStartDate(final Date date) {
      this.entry.setUsageStartDate(date);
      return this;
    }

    public MonthlyReportEntryBuilder withUsageEndDate(final Date date) {
      this.entry.setUsageEndDate(date);
      return this;
    }

    public MonthlyReportEntryBuilder withUsageQuantity(final Double quantity) {
      this.entry.setUsageQuantity(quantity);
      return this;
    }

    public MonthlyReportEntryBuilder withOperation(final String operation) {
      this.entry.setOperation(operation);
      return this;
    }

    public MonthlyReportEntry build() {
      return this.entry;
    }
  }
}
