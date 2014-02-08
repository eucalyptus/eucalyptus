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

import java.io.IOException;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.log4j.Logger;

import com.eucalyptus.util.EucalyptusCloudException;

public enum ImportImageManifest implements ImageManifest {
	INSTANCE;
	
	private static Logger LOG = Logger.getLogger( ImportImageManifest.class );

	@Override
	public FileType getFileType() {
		// TODO: return actual type from import manifest
		return FileType.RAW;
	}

	@Override
	public String getPartsPath() {
		return "/manifest/import/parts/part";
	}

	@Override
	public String getPartUrlElement() {
		return "get-url";
	}

	@Override
	public boolean signPartUrl() {
		return false;
	}

	@Override
	public String getSizePath() {
		return "/manifest/import/size";
	}

	@Override
	public String getManifest(String location) throws EucalyptusCloudException {
		LOG.debug("Downloading " + location);
		HttpClient client = new HttpClient();
		client.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());
		GetMethod method = new GetMethod(location);
		String s = null;
		try {
			client.executeMethod(method);
			s = method.getResponseBodyAsString();
			if (s == null) {
				throw new EucalyptusCloudException("Can't download manifest from " + location + " content is null");
			}
		} catch(IOException ex) {
			throw new EucalyptusCloudException("Can't download manifest from " + location, ex);
		} finally {
			method.releaseConnection();
		}
		return s;
	}

	@Override
	public String getBaseBucket(String location) {
		throw new UnsupportedOperationException();
	}
}
