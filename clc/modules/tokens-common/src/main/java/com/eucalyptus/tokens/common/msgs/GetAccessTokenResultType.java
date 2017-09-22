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
package com.eucalyptus.tokens.common.msgs;

import java.util.Date;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class GetAccessTokenResultType extends EucalyptusData {
  private CredentialsType credentials;

  public static GetAccessTokenResultType forCredentials(
      final String accessKeyId,
      final String secretAccessKey,
      final String sessionToken,
      final long expiryTime
  ) {
    final CredentialsType credentials = new CredentialsType( );
    credentials.setAccessKeyId( accessKeyId );
    credentials.setSecretAccessKey( secretAccessKey );
    credentials.setSessionToken( sessionToken );
    credentials.setExpiration( new Date( expiryTime ) );

    final GetAccessTokenResultType getAccessTokenResult = new GetAccessTokenResultType( );
    getAccessTokenResult.setCredentials( credentials );
    return getAccessTokenResult;
  }

  public CredentialsType getCredentials() {
    return credentials;
  }

  public void setCredentials( CredentialsType credentials ) {
    this.credentials = credentials;
  }
}
