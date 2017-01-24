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
package com.eucalyptus.portal;

import static com.eucalyptus.util.RestrictedTypes.getIamActionByMessageType;
import java.util.Set;
import java.util.function.Function;
import javax.inject.Inject;

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
    try {
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
    final ViewUsageResult result = MockReports.getInstance().generateAwsUsageReport(request);
    response.setResult(result);
    return response;
  }

  public ViewMonthlyUsageResponseType viewMonthlyUsage(final ViewMonthlyUsageType request) throws PortalServiceException {
    final Context context = checkAuthorized( );
    final ViewMonthlyUsageResponseType response = request.getReply();
    final ViewMonthlyUsageResult result =  MockReports.getInstance().generateMonthlyReport(request);
    response.setResult(result);
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
