/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.portal;

import static com.eucalyptus.util.RestrictedTypes.getIamActionByMessageType;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.inject.Inject;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.objectstorage.client.EucaS3Client;
import com.eucalyptus.portal.monthlyreport.MonthlyReports;
import com.eucalyptus.portal.workflow.AwsUsageRecord;
import com.eucalyptus.portal.awsusage.AwsUsageRecords;
import com.eucalyptus.portal.common.model.*;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.AuthContextSupplier;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.portal.common.TagClient;
import com.eucalyptus.portal.common.policy.PortalPolicySpec;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

/**
 *
 */
@SuppressWarnings( "unused" )
@ComponentNamed
public class PortalService {

  private static final Logger logger = Logger.getLogger( PortalService.class );

  private static final Map<String,String> AWS_USAGE_SERVICE_MAP =
      ImmutableMap.<String,String>builder()
      .put( "ec2", "AmazonEC2" )
      .put( "s3", "AmazonS3" )
      .put( "cloudwatch", "AmazonCloudWatch" )
      .build( );

  private static final Map<String,Map<String,String>> AWS_USAGE_USAGE_TYPE_MAPS =
      ImmutableMap.<String,Map<String,String>>builder()
      .put( "AmazonCloudWatch", ImmutableMap.of( "request", "Request-NoCharge" ) )
      .build( );

  private final BillingAccounts billingAccounts;
  private final BillingInfos billingInfos;
  private final TagClient tagClient;

  @Inject
  public PortalService(
          final BillingAccounts billingAccounts,
          final BillingInfos billingInfos,
          final TagClient tagClient
  ) {
    this.billingAccounts = billingAccounts;
    this.billingInfos = billingInfos;
    this.tagClient = tagClient;
  }

  public ModifyAccountResponseType modifyAccount( final ModifyAccountType request ) throws PortalServiceException {
    final Context context = checkAuthorized( );
    final ModifyAccountResponseType response = request.getReply( );
    if ( request.getUserBillingAccess( ) != null ) {
      Function<BillingAccount,BillingAccount> updater = account -> {
        account.setUserAccessEnabled( request.getUserBillingAccess( ) );
        return account;
      };
      try {
        try {
          response.getResult( ).setAccountSettings( billingAccounts.updateByAccount(
                  context.getAccountNumber( ),
                  context.getAccount( ),
                  account -> TypeMappers.transform( updater.apply( account ), AccountSettings.class )
          ) );
        } catch ( PortalMetadataNotFoundException e ) {
          final BillingAccount billingAccount = updater.apply( billingAccounts.defaults( ) );
          billingAccount.setOwner( context.getUserFullName( ) );
          billingAccount.setDisplayName( context.getAccountNumber( ) );
          response.getResult( ).setAccountSettings(
                  billingAccounts.save(
                          billingAccount,
                          TypeMappers.lookupF( BillingAccount.class, AccountSettings.class )
                  ) );
        }
      } catch ( Exception e ) {
        throw handleException( e );
      }
    }
    return response;
  }

  public ViewAccountResponseType viewAccount( final ViewAccountType request ) throws PortalServiceException {
    final Context context = checkAuthorized( );
    final ViewAccountResponseType response = request.getReply( );
    try {
      response.getResult( ).setAccountSettings( billingAccounts.lookupByAccount(
              context.getAccountNumber( ),
              context.getAccount( ),
              TypeMappers.lookupF( BillingAccount.class, AccountSettings.class )
      ) );
    } catch ( PortalMetadataNotFoundException e ) {
      response.getResult( ).setAccountSettings(
              TypeMappers.transform( billingAccounts.defaults( ), AccountSettings.class )
      );
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return response;
  }

  public ModifyBillingResponseType modifyBilling( final ModifyBillingType request ) throws PortalServiceException {
    final Context context = checkAuthorized( );
    final ModifyBillingResponseType response = request.getReply( );
    Function<BillingInfo,BillingInfo> updater = info -> {
      info.setBillingReportsBucket( request.getReportBucket( ) );
      info.setDetailedBillingEnabled( MoreObjects.firstNonNull( request.getDetailedBillingEnabled( ), false ) );
      if ( request.getActiveCostAllocationTags( ) != null ) {
        info.setActiveCostAllocationTags( request.getActiveCostAllocationTags( ) );
      }
      return info;
    };
    final Predicate<String> testBucket = (bucket) -> {
      try {
        final EucaS3Client s3c = BucketUploadableActivities.getS3Client();
        PutObjectRequest req = new PutObjectRequest(bucket, "aws-programmatic-access-test-object",
                new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8)),
                new ObjectMetadata())
                .withCannedAcl(CannedAccessControlList.BucketOwnerFullControl);
        s3c.putObject(req);
        return true;
      } catch (final Exception ex) {
        ;
      }
      return false;
    };

    try {
      if (request.getReportBucket()!=null && !testBucket.test(request.getReportBucket()) ) {
        throw new PortalInvalidParameterException("Requested bucket is not accessible by billing");
      }

      try {
        response.getResult( ).setBillingSettings( billingInfos.updateByAccount(
                context.getAccountNumber( ),
                context.getAccount( ),
                info -> TypeMappers.transform( updater.apply( info ), BillingSettings.class )
        ) );
      } catch ( PortalMetadataNotFoundException e ) {
        final BillingInfo billingInfo = updater.apply( billingInfos.defaults( ) );
        billingInfo.setOwner( context.getUserFullName( ) );
        billingInfo.setDisplayName( context.getAccountNumber( ) );
        response.getResult( ).setBillingSettings(
                billingInfos.save(
                        billingInfo,
                        TypeMappers.lookupF( BillingInfo.class, BillingSettings.class )
                ) );
      }
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return response;
  }

  public ViewBillingResponseType viewBilling( final ViewBillingType request ) throws PortalServiceException {
    final Context context = checkAuthorized( );
    final ViewBillingResponseType response = request.getReply( );
    try {
      response.getResult( ).setBillingSettings( billingInfos.lookupByAccount(
              context.getAccountNumber( ),
              context.getAccount( ),
              TypeMappers.lookupF( BillingInfo.class, BillingSettings.class )
      ) );
    } catch ( PortalMetadataNotFoundException e ) {
      response.getResult( ).setBillingSettings(
              TypeMappers.transform( billingInfos.defaults( ), BillingSettings.class )
      );
    } catch ( Exception e ) {
      throw handleException( e );
    }
    try {
      final Set<String> inactiveTagKeys = Sets.newTreeSet( );
      inactiveTagKeys.addAll( tagClient.getTagKeys( new GetTagKeysType( ).markPrivileged( ) ).getResult( ).getKeys( ) );
      inactiveTagKeys.removeAll( response.getResult( ).getBillingSettings( ).getActiveCostAllocationTags( ) );
      response.getResult( ).getBillingMetadata( ).setInactiveCostAllocationTags(
              Lists.newArrayList( Ordering.from( String.CASE_INSENSITIVE_ORDER ).sortedCopy( inactiveTagKeys ) )
      );
    } catch ( Exception e ) {
      logger.error( "Error loading tag keys", e );
    }
    return response;
  }

  public ViewUsageResponseType viewUsage(final ViewUsageType request) throws PortalServiceException {
    final Context context = checkAuthorized( );
    final ViewUsageResponseType response = request.getReply();

    final Function<ViewUsageType, Optional<PortalServiceException>> requestVerifier = (req) -> {
      final String granularity = req.getReportGranularity() != null ?
              req.getReportGranularity().toLowerCase() : null;
      if (granularity==null) {
        return Optional.of(new PortalInvalidParameterException("Granularity must be specified"));
      }
      if (!Sets.newHashSet("hourly", "hour", "daily", "day", "monthly", "month").contains(granularity)) {
        return Optional.of(new PortalInvalidParameterException("Can't recognize granularity. Valid values are hourly, daily, and monthly"));
      }

      final String service = req.getServices() != null ? req.getServices().toLowerCase() : null;
      if (service == null) {
        return Optional.of(new PortalInvalidParameterException("Service name must be specified"));
      } else if (!Sets.newHashSet("ec2", "s3", "cloudwatch").contains(service)) {
        return Optional.of(new PortalInvalidParameterException("Can't recognize service name. Supported services are ec2, s3, and cloudwatch"));
      }

      if (req.getTimePeriodFrom() == null) {
        return Optional.of(new PortalInvalidParameterException("Beginning time period must be specified"));
      }
      if (req.getTimePeriodTo() == null) {
        return Optional.of(new PortalInvalidParameterException("Ending time period must be specified"));
      }

      return Optional.empty();
    };

    final Function<ViewUsageType, ViewUsageType> requestFormatter = (req) -> {
      final String service = req.getServices().toLowerCase();
      if ( AWS_USAGE_SERVICE_MAP.containsKey( service ) ) {
        req.setServices( AWS_USAGE_SERVICE_MAP.get( service ) );
      }

      final String operation = req.getOperations();
      if (operation != null && "all".equals(operation.toLowerCase())) {
        req.setOperations(null);
      }

      if ( "all".equalsIgnoreCase( request.getUsageTypes( ) ) ) {
        req.setUsageTypes( null );
      } else if ( AWS_USAGE_USAGE_TYPE_MAPS.containsKey( request.getServices( ) ) ) {
        final Map<String,String> usageTypeMapForService = AWS_USAGE_USAGE_TYPE_MAPS.get( request.getServices( ) );
        if ( usageTypeMapForService.containsKey( request.getUsageTypes( ) ) ) {
          request.setUsageTypes( usageTypeMapForService.get( request.getUsageTypes( ) ) );
        }
      }
      return req;
    };

    final Optional<PortalServiceException> error = requestVerifier.apply(request);
    if (error.isPresent()) {
      throw error.get();
    }
    final ViewUsageType req = requestFormatter.apply(request);
    final String service = req.getServices();
    final String operation = req.getOperations();
    final String usageType = req.getUsageTypes();
    final String granularity = req.getReportGranularity();
    final Date periodBegin = req.getTimePeriodFrom();
    final Date periodEnd = req.getTimePeriodTo();
    final List<AwsUsageRecord> records = Lists.newArrayList();
    if (granularity != null && granularity.startsWith("hour")) {
      records.addAll(AwsUsageRecords.getInstance().queryHourly( context.getAccountNumber(), service,
              operation, usageType, periodBegin, periodEnd));
    } else if (granularity != null && (granularity.startsWith("day") || granularity.startsWith("dai"))) {
      records.addAll(AwsUsageRecords.getInstance().queryDaily( context.getAccountNumber(), service,
              operation, usageType, periodBegin, periodEnd));
    } else if (granularity != null && granularity.startsWith("month")) {
      records.addAll(AwsUsageRecords.getInstance().queryMonthly( context.getAccountNumber(), service,
              operation, usageType, periodBegin, periodEnd));
    } else {
      throw new PortalInvalidParameterException("Valid report granularity are hourly, daily or monthly");
    }
    
    final Function<AwsUsageRecord, String> formatter = (r) -> {
      final StringBuilder sb = new StringBuilder();
      final DateFormat df = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
      //Service, Operation, UsageType, Resource, StartTime, EndTime, UsageValue
      //AmazonEC2,Unknown,CW:AlarmMonitorUsage,,11/01/16 00:00:00,11/02/16 00:00:00,48
      sb.append(r.getService()!=null ? r.getService() + "," : ",");
      sb.append(r.getOperation()!=null ? r.getOperation() + "," : ",");
      sb.append(r.getUsageType()!=null ? r.getUsageType() + "," : ",");
      sb.append(r.getResource()!=null ? r.getResource() + "," : ",");
      sb.append(r.getStartTime()!=null ? df.format(r.getStartTime()) + "," : ",");
      sb.append(r.getEndTime()!=null ? df.format(r.getEndTime()) + "," : ",");
      sb.append(r.getUsageValue()!=null ? r.getUsageValue(): "");
      return sb.toString();
    };

    response.setResult(new ViewUsageResult());
    final StringBuilder sb = new StringBuilder();
    sb.append("Service, Operation, UsageType, Resource, StartTime, EndTime, UsageValue");
    final Optional<String> data = records.stream()
            .map (formatter)
            .reduce((l1, l2) -> String.format("%s\n%s", l1, l2));
    if (data.isPresent()) {
      sb.append("\n");
      sb.append(data.get());
    }
    response.getResult().setData(sb.toString());
    return response;
  }

  public ViewMonthlyUsageResponseType viewMonthlyUsage(final ViewMonthlyUsageType request) throws PortalServiceException {
    final Context context = checkAuthorized( );
    final ViewMonthlyUsageResponseType response = request.getReply();

    final Predicate<ViewMonthlyUsageType> requestVerifier = (req) -> {
      final String year = req.getYear();
      final String month = req.getMonth();
      if (! Pattern.matches("2[0-9][0-9][0-9]", year)) // Do EUCA exists in year 3000?
        return false;
      if (! Pattern.matches("[0-1]?[0-9]", month)) {
        return false;
      }
      try {
        final int nMonth = Integer.parseInt(month);
        if (! (nMonth >= 1 && nMonth <= 12))
          return false;
      } catch (final NumberFormatException ex) {
        return false;
      }
      return true;
    };

    if (!requestVerifier.test(request))
      throw new PortalInvalidParameterException("Invalid year and month requested");

    final String year;
    final String month;
    try {
      year = String.format("%d", Integer.parseInt(request.getYear()));
      month = String.format("%d", Integer.parseInt(request.getMonth()));
    } catch (final NumberFormatException ex) {
      throw new PortalInvalidParameterException("Invalid year and month requested");
    }
    response.setResult( new ViewMonthlyUsageResult());
    final StringBuilder sb = new StringBuilder();
    sb.append("\"InvoiceID\",\"PayerAccountId\",\"LinkedAccountId\",\"RecordType\",\"RecordID\",\"BillingPeriodStartDate\"," +
            "\"BillingPeriodEndDate\",\"InvoiceDate\",\"PayerAccountName\",\"LinkedAccountName\",\"TaxationAddress\"," +
            "\"PayerPONumber\",\"ProductCode\",\"ProductName\",\"SellerOfRecord\",\"UsageType\",\"Operation\",\"RateId\"," +
            "\"ItemDescription\",\"UsageStartDate\",\"UsageEndDate\",\"UsageQuantity\",\"BlendedRate\",\"CurrencyCode\"," +
            "\"CostBeforeTax\",\"Credits\",\"TaxAmount\",\"TaxType\",\"TotalCost\"");
    try {
      final Optional<String> data = MonthlyReports.getInstance()
              .lookupReport(AccountFullName.getInstance(context.getAccountNumber()), year, month).stream()
              .reduce((l1, l2) -> String.format("%s\n%s", l1, l2));
      if (data.isPresent()) {
        sb.append("\n");
        sb.append(data.get());
      }
    } catch (final NoSuchElementException ex) {
      ;
    }
    response.getResult().setData(sb.toString());
    return response;
  }

  protected static Context checkAuthorized( ) throws PortalServiceException {
    final Context ctx = Contexts.lookup( );
    final AuthContextSupplier requestUserSupplier = ctx.getAuthContext( );
    if ( !Permissions.isAuthorized(
            PortalPolicySpec.VENDOR_PORTAL,
            "",
            "",
            null,
            getIamActionByMessageType( ),
            requestUserSupplier ) ) {
      throw new PortalServiceUnauthorizedException(
              "UnauthorizedOperation",
              "You are not authorized to perform this operation." );
    }
    return ctx;
  }

  /**
   * Method always throws, signature allows use of "throw handleException ..."
   */
  private static PortalServiceException handleException( final Exception e  ) throws PortalServiceException {
    Exceptions.findAndRethrow( e, PortalServiceException.class );

    logger.error( e, e );

    final PortalServiceException exception = new PortalServiceException( "InternalError", String.valueOf(e.getMessage()) );
    if ( Contexts.lookup( ).hasAdministrativePrivileges() ) {
      exception.initCause( e );
    }
    throw exception;
  }
}
