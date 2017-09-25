/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.auth.euare;

import org.apache.log4j.Logger;
import org.springframework.integration.annotation.Filter;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.euare.common.msgs.DownloadServerCertificateType;
import com.eucalyptus.auth.euare.common.msgs.EuareMessage;
import com.eucalyptus.auth.euare.common.msgs.EuareMessageWithDelegate;
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
