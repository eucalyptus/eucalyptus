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

import java.util.function.Function;
import javax.annotation.Nullable;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.portal.common.model.BillingSettings;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

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
              .withActiveCostAllocationTags( info.getActiveCostAllocationTags( ) );
    }
  }
}
