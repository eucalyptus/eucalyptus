/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.imaging.manifest;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.objectstorage.client.EucaS3Client;
import com.eucalyptus.objectstorage.client.EucaS3ClientFactory;
import com.eucalyptus.util.EucalyptusCloudException;

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
	public boolean signPartUrl() {
		return true;
	}

	@Override
	public String getSizePath() {
		return "/manifest/image/bundled_size";
	}

	@Override
	public String getManifest(String location) throws EucalyptusCloudException {
		String cleanLocation = location.replaceAll( "^/*", "" );
		int index = cleanLocation.indexOf( '/' );
		String bucketName = cleanLocation.substring( 0, index );
		String manifestKey = cleanLocation.substring( index + 1 );
        try {
            EucaS3Client s3Client = EucaS3ClientFactory.getEucaS3Client(Accounts.lookupSystemAdmin());
            return s3Client.getObjectContent(bucketName, manifestKey);
        } catch (Exception e) {
			throw new EucalyptusCloudException( "Failed to read manifest file: " + bucketName + "/" + manifestKey, e );
		}
	}

	@Override
	public String getBaseBucket(String location) {
		String cleanLocation = location.replaceAll( "^/*", "" );
		int index = cleanLocation.indexOf( '/' );
		return cleanLocation.substring( 0, index );
	}
}
