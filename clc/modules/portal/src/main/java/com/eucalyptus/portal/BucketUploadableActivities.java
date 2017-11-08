/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.tokens.SecurityTokenAWSCredentialsProvider;
import com.eucalyptus.objectstorage.client.EucaS3Client;
import com.eucalyptus.objectstorage.client.EucaS3ClientFactory;
import com.eucalyptus.portal.persist.PersistenceBillingInfos;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.util.Optional;

public abstract class BucketUploadableActivities {
  private static Logger LOG     =
          Logger.getLogger(  BucketUploadableActivities.class );
  private final BillingInfos billingInfos;
  protected BucketUploadableActivities() {
    this.billingInfos = new PersistenceBillingInfos();
  }

  public static EucaS3Client getS3Client () throws AuthException {
    try {
      final Role billingRole = Accounts.lookupRoleByName(
              Accounts.lookupAccountIdByAlias( AccountIdentifiers.BILLING_SYSTEM_ACCOUNT ),
              "BillingServiceWorkflow");
      final SecurityTokenAWSCredentialsProvider roleCredentialProvider =
              SecurityTokenAWSCredentialsProvider.forUserOrRole(Accounts.lookupPrincipalByRoleId(billingRole.getRoleId()));
      return EucaS3ClientFactory.getEucaS3Client(roleCredentialProvider);
    }catch (AuthException ex) {
      LOG.error("Failed to obtain credentials for billing", ex);
    }catch (Exception ex) {
      LOG.error("Failed to obtain credentials for billing", ex);
    }
    return null;
  }

  protected boolean upload(final String accountId, final String keyName, InputStream contents) throws S3UploadException {
    Optional<String> bucketName;
    try {
      bucketName = this.billingInfos.lookupByAccount(accountId, AccountFullName.getInstance(accountId),
              (info) -> info.getBillingReportsBucket() != null ? Optional.of(info.getBillingReportsBucket()) : Optional.empty());
    } catch (final Exception ex) {
      throw new S3UploadException("Failed to lookup user's bucket setting");
    }
    if (bucketName.isPresent()) {
      try {
        final EucaS3Client s3c = getS3Client();
        // this will throw error if bucket policy does not allow billing writing into the bucket
        if ( s3c!=null ) {
          final PutObjectRequest req =
                  new PutObjectRequest(bucketName.get(), keyName, contents, new ObjectMetadata())
                  .withCannedAcl(CannedAccessControlList.BucketOwnerFullControl);
          s3c.putObject(req);
          return true;
        }
      } catch (final AmazonServiceException ex) {
        throw new S3UploadException("Failed to upload due to S3 service error: " + ex.getErrorCode());
      } catch (final SdkClientException ex) {
        throw new S3UploadException("Failed to upload due to S3 client error", ex);
      } catch (final Exception ex) {
        throw new S3UploadException("Failed to upload report to bucket", ex);
      }
    }
    return false;
  }
}
