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

import java.util.function.Function;
import javax.annotation.Nullable;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.portal.common.model.BillingSettings;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

/**
 *
 */
public interface BillingInfos {

  default BillingInfo defaults( ) {
    final BillingInfo billingInfo = new BillingInfo( );
    billingInfo.setDetailedBillingEnabled( false );
    billingInfo.setActiveCostAllocationTags( Lists.newArrayList( ) );
    return billingInfo;
  }

  <T> T lookupByAccount( String accountNumber,
                         @Nullable OwnerFullName ownerFullName,
                         Function<? super BillingInfo,T> transform ) throws PortalMetadataException;

  <T> T updateByAccount( String accountNumber,
                         @Nullable OwnerFullName ownerFullName,
                         Function<? super BillingInfo,T> updateTransform ) throws PortalMetadataException;

  <T> T save( BillingInfo info,
              Function<? super BillingInfo,T> transform ) throws PortalMetadataException;

  @TypeMapper
  public enum BillingInfoToBillingSettingsTransform implements CompatFunction<BillingInfo,BillingSettings> {
    INSTANCE;

    @Override
    public BillingSettings apply( final BillingInfo info ) {
      return info == null ?
          null :
          new BillingSettings( )
              .withReportBucket( info.getBillingReportsBucket( ) )
              .withDetailedBillingEnabled( Optional.of( info.getDetailedBillingEnabled( ) ).or( Boolean.FALSE ) )
              .withActiveCostAllocationTags(
                  Ordering.from( String.CASE_INSENSITIVE_ORDER ).sortedCopy( info.getActiveCostAllocationTags( ) )
              );
    }
  }
}
