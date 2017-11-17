/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
