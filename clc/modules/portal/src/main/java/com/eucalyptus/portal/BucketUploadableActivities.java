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
