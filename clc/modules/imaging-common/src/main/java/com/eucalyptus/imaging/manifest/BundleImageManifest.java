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
package com.eucalyptus.imaging.manifest;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.objectstorage.client.EucaS3Client;
import com.eucalyptus.objectstorage.client.EucaS3ClientFactory;
import com.eucalyptus.util.EucalyptusCloudException;

import java.util.concurrent.TimeUnit;

public enum BundleImageManifest implements ImageManifest {
  INSTANCE;

  @Override
  public FileType getFileType() {
    return FileType.BUNDLE;
  }

  @Override
  public String getPartsPath() {
    return "/manifest/image/parts/part";
  }

  @Override
  public String getPartUrlElement() {
    return "filename";
  }

  @Override
  public String getDigestElement() {
    return "digest";
  }

  @Override
  public boolean signPartUrl() {
    return true;
  }

  @Override
  public String getSizePath() {
    return "/manifest/image/bundled_size";
  }

  @Override
  public String getManifest(String location, int maximumSize)
      throws EucalyptusCloudException {
    String cleanLocation = location.replaceAll("^/*", "");
    int index = cleanLocation.indexOf('/');
    String bucketName = cleanLocation.substring(0, index);
    String manifestKey = cleanLocation.substring(index + 1);
    try (final EucaS3Client s3Client = EucaS3ClientFactory.getEucaS3ClientForUser(
        Accounts.lookupSystemAccountByAlias( AccountIdentifiers.AWS_EXEC_READ_SYSTEM_ACCOUNT ),
        (int)TimeUnit.MINUTES.toSeconds( 15 ))) {
      return s3Client.getObjectContent(bucketName, manifestKey, maximumSize);
    } catch (Exception e) {
      throw new EucalyptusCloudException("Failed to read manifest file: "
          + bucketName + "/" + manifestKey, e);
    }
  }

  @Override
  public String getPrefix(String location) {
    final String cleanLocation = clean( location );
    final int index = cleanLocation.indexOf('/') + 1;
    final int endIndex = cleanLocation.lastIndexOf('/');
    return endIndex > index ?
        cleanLocation.substring( index, endIndex ) :
        "";
  }

  @Override
  public String getBaseBucket(String location) {
    final String cleanLocation = clean( location );
    final int index = cleanLocation.indexOf('/');
    return cleanLocation.substring(0, index);
  }

  private String clean( final String location ) {
    return location.replaceAll("^/*", "");
  }
}
