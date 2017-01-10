/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 * <p/>
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.auth.util;

import java.util.concurrent.TimeUnit;
import com.eucalyptus.auth.AuthenticationLimitProvider;

/**
 *
 */
public class TestAuthenticationLimitProvider implements AuthenticationLimitProvider {

  @Override
  public long getDefaultPasswordExpirySpi( ) {
    return TimeUnit.DAYS.toMillis( 60 );
  }

  @Override
  public int getAccessKeyLimitSpi( ) {
    return 2;
  }

  @Override
  public int getSigningCertificateLimitSpi( ) {
    return 2;
  }

  @Override
  public int getPolicyAttachmentLimitSpi( ) {
    return 10;
  }

  @Override
  public int getPolicySizeLimitSpi( ) {
    return 16384;
  }

  @Override
  public boolean getUseValidatingPolicyParserSpi( ) {
    return true;
  }
}
