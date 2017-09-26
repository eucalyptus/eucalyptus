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
package com.eucalyptus.objectstorage.client;

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.eucalyptus.auth.principal.BaseRole;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.tokens.RoleSecurityTokenAttributes;
import com.eucalyptus.auth.tokens.SecurityToken;
import com.eucalyptus.auth.tokens.SecurityTokenManager;
import com.eucalyptus.util.EucalyptusCloudException;

/**
 * This is how any internal eucalyptus component should get an s3 client and use it for object-storage access.
 */
public class EucaS3ClientFactory {

  public static EucaS3Client getEucaS3Client( AWSCredentials credentials ) throws NoSuchElementException {
    return new EucaS3Client( credentials, USE_HTTPS_DEFAULT );
  }

  public static EucaS3Client getEucaS3Client( AWSCredentials credentials, boolean https ) throws NoSuchElementException {
    return new EucaS3Client( credentials, https );
  }

  public static EucaS3Client getEucaS3Client( AWSCredentialsProvider credentials ) throws NoSuchElementException {
    return new EucaS3Client( credentials, USE_HTTPS_DEFAULT );
  }

  public static EucaS3Client getEucaS3Client( AWSCredentialsProvider credentials, boolean https ) throws NoSuchElementException {
    return new EucaS3Client( credentials, https );
  }

  public static EucaS3Client getEucaS3ClientByRole( final BaseRole role, final String sessionName, int durationInSec ) throws EucalyptusCloudException {
    EucaS3Client eucaS3Client;
    try {
      SecurityToken token = SecurityTokenManager.issueSecurityToken( role, RoleSecurityTokenAttributes.basic( sessionName ), durationInSec );
      eucaS3Client = EucaS3ClientFactory.getEucaS3Client( new BasicSessionCredentials( token.getAccessKeyId( ), token.getSecretKey( ), token.getToken( ) ) );
    } catch ( Exception e ) {
      LOG.error( "Failed to initialize eucalyptus object storage client due to " + e );
      throw new EucalyptusCloudException( "Failed to initialize eucalyptus object storage client", e );
    }
    return eucaS3Client;
  }

  public static EucaS3Client getEucaS3ClientForUser( User user, int durationInSec ) throws EucalyptusCloudException {
    EucaS3Client eucaS3Client;
    try {
      SecurityToken token = SecurityTokenManager.issueSecurityToken( user, durationInSec );
      eucaS3Client = EucaS3ClientFactory.getEucaS3Client( new BasicSessionCredentials( token.getAccessKeyId( ), token.getSecretKey( ), token.getToken( ) ) );
    } catch ( Exception e ) {
      LOG.error( "Failed to initialize eucalyptus object storage client due to " + e );
      throw new EucalyptusCloudException( "Failed to initialize eucalyptus object storage client", e );
    }
    return eucaS3Client;
  }

  public static boolean getUSE_HTTPS_DEFAULT( ) {
    return USE_HTTPS_DEFAULT;
  }

  public static boolean isUSE_HTTPS_DEFAULT( ) {
    return USE_HTTPS_DEFAULT;
  }

  public static void setUSE_HTTPS_DEFAULT( boolean USE_HTTPS_DEFAULT ) {
    EucaS3ClientFactory.USE_HTTPS_DEFAULT = USE_HTTPS_DEFAULT;
  }

  private static Logger LOG = Logger.getLogger( EucaS3ClientFactory.class );
  private static boolean USE_HTTPS_DEFAULT = false;
}
