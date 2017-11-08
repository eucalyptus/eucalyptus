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

import java.io.IOException;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.log4j.Logger;

import com.eucalyptus.util.EucalyptusCloudException;

public enum ImportImageManifest implements ImageManifest {
  INSTANCE;

  private static Logger LOG = Logger.getLogger(ImportImageManifest.class);

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
  public String getDigestElement() {
    return null;
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
  public String getManifest(String location, int maximumSize)
      throws EucalyptusCloudException {
    HttpClient client = new HttpClient();
    client.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
        new DefaultHttpMethodRetryHandler());
    GetMethod method = new GetMethod(location);
    String s = null;
    try {
      client.executeMethod(method);
      s = method.getResponseBodyAsString(maximumSize);
      if (s == null) {
        throw new EucalyptusCloudException("Can't download manifest from "
            + location + " content is null");
      }
    } catch (IOException ex) {
      throw new EucalyptusCloudException("Can't download manifest from "
          + location, ex);
    } finally {
      method.releaseConnection();
    }
    return s;
  }

  @Override
  public String getPrefix(String location) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getBaseBucket(String location) {
    throw new UnsupportedOperationException();
  }
}
