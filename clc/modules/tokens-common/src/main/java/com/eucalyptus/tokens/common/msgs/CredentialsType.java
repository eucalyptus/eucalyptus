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

public class CredentialsType extends EucalyptusData {
  private String accessKeyId;
  private String secretAccessKey;
  private String sessionToken;
  private Date expiration;

  public CredentialsType() {
  }

  public CredentialsType( String accessKeyId, String secretAccessKey, String sessionToken, long expiration ) {
    this.accessKeyId = accessKeyId;
    this.secretAccessKey = secretAccessKey;
    this.sessionToken = sessionToken;
    this.expiration = new Date( expiration );
  }

  public String getAccessKeyId() {
    return accessKeyId;
  }

  public void setAccessKeyId( String accessKeyId ) {
    this.accessKeyId = accessKeyId;
  }

  public String getSecretAccessKey() {
    return secretAccessKey;
  }

  public void setSecretAccessKey( String secretAccessKey ) {
    this.secretAccessKey = secretAccessKey;
  }

  public String getSessionToken() {
    return sessionToken;
  }

  public void setSessionToken( String sessionToken ) {
    this.sessionToken = sessionToken;
  }

  public Date getExpiration() {
    return expiration;
  }

  public void setExpiration( Date expiration ) {
    this.expiration = expiration;
  }
}
