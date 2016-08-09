/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.auth.euare;

import org.apache.log4j.Logger;
import org.springframework.integration.annotation.Filter;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.euare.identity.region.RegionConfigurationManager;
import com.eucalyptus.auth.euare.identity.region.RegionConfigurations;
import com.eucalyptus.auth.euare.identity.region.RegionInfo;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.records.Logs;
import com.google.common.base.Optional;

/**
 *  Filter that is true for requests for a remote region
 */
@ComponentNamed
public class EuareRemoteRegionFilter {
  private static final Logger logger = Logger.getLogger( EuareRemoteRegionFilter.class );
  private static final RegionConfigurationManager regionConfigurationManager = new RegionConfigurationManager( );

  @Filter
  public boolean accept( final EuareMessage message ) {
    return isNonLocalRegion( getRegion( message ) );
  }

  static Optional<RegionInfo> getRegion( final EuareMessage request ) {
    Optional<RegionInfo> region = Optional.absent( );
    try {
      if ( request instanceof EuareMessageWithDelegate ) {
        final EuareMessageWithDelegate messageWithDelegate = (EuareMessageWithDelegate) request;
        final String delegateAliasOrNumber = messageWithDelegate.getDelegateAccount();
        if ( delegateAliasOrNumber != null ) {
          final String delegateNumber = Accounts.isAccountNumber( delegateAliasOrNumber ) ?
              delegateAliasOrNumber :
              Accounts.lookupAccountIdByAlias( delegateAliasOrNumber );
          region =
              regionConfigurationManager.getRegionByAccountNumber( delegateNumber );
        }
      }

      if ( !region.isPresent( ) && request instanceof DownloadServerCertificateType ) {
        final DownloadServerCertificateType downloadServerCertificateType = (DownloadServerCertificateType) request;
        final String certArn = downloadServerCertificateType.getCertificateArn( );
        if ( certArn != null ) {
          final String accountNumber = Ern.parse( certArn ).getAccount( );
          region = regionConfigurationManager.getRegionByAccountNumber( accountNumber );
        }
      } else if ( !region.isPresent( ) && request.getUserId( ) != null ) {
        if ( Accounts.isAccountNumber( request.getUserId( ) ) ) {
          region = regionConfigurationManager.getRegionByAccountNumber( request.getUserId( ) );
        } else {
          region = regionConfigurationManager.getRegionByIdentifier( request.getUserId( ) );
        }
      }

      if ( !region.isPresent( ) && Contexts.exists( ) ) {
        final Context context = Contexts.lookup( );
        final String userId = context.getUser( ).getUserId( );
        region = regionConfigurationManager.getRegionByIdentifier( userId );
      }
    } catch ( AuthException e ) {
      Logs.extreme( ).error( e, e ); // bad alias
    } catch ( Exception e ) {
      logger.error( e, e );
    }

    return region;
  }

  private static boolean isNonLocalRegion( final Optional<RegionInfo> regionInfo ) {
    return regionInfo.isPresent( ) &&
        !RegionConfigurations.getRegionName( ).asSet( ).contains( regionInfo.get( ).getName( ) );
  }
}
