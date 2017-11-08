/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
