/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.auth;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.util.Parameters;
import com.google.common.base.Function;

/**
 *
 */
public final class RegionService {
  private final String regionName;
  private final String serviceType;
  private final String serviceEndpoint;

  public RegionService(
      @Nonnull final String regionName,
      @Nonnull final String serviceType,
      @Nonnull final String serviceEndpoint ) {
    Parameters.checkParam( "regionName", regionName, not( isEmptyOrNullString( ) ) );
    Parameters.checkParam( "serviceType", serviceType, not( isEmptyOrNullString( ) ) );
    Parameters.checkParam( "serviceEndpoint", serviceEndpoint, not( isEmptyOrNullString( ) ) );
    this.regionName = regionName;
    this.serviceType = serviceType;
    this.serviceEndpoint = serviceEndpoint;
  }

  @Nonnull
  public String getRegionName( ) {
    return regionName;
  }

  @Nonnull
  public String getServiceType( ) {
    return serviceType;
  }

  @Nonnull
  public String getServiceEndpoint( ) {
    return serviceEndpoint;
  }

  public static Function<RegionService,String> regionName( ) {
    return StringProperties.REGION_NAME;
  }

  public static Function<RegionService,String> serviceType( ) {
    return StringProperties.SERVICE_TYPE;
  }

  public static Function<RegionService,String> serviceEndpoint( ) {
    return StringProperties.SERVICE_ENDPOINT;
  }

  private enum StringProperties implements Function<RegionService,String> {
    REGION_NAME {
      @Nullable
      @Override
      public String apply( @Nullable final RegionService regionService ) {
        return regionService == null ? null : regionService.getRegionName( );
      }
    },
    SERVICE_TYPE {
      @Nullable
      @Override
      public String apply( @Nullable final RegionService regionService ) {
        return regionService == null ? null : regionService.getServiceType( );
      }
    },
    SERVICE_ENDPOINT {
      @Nullable
      @Override
      public String apply( @Nullable final RegionService regionService ) {
        return regionService == null ? null : regionService.getServiceEndpoint( );
      }
    },
  }
}
