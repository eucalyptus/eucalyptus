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
package com.eucalyptus.cloudformation.template.url;

import com.eucalyptus.cloudformation.ValidationErrorException;
import com.google.common.net.InetAddresses;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.StringTokenizer;

/**
 * Created by ethomas on 9/21/14.
 */
public class S3Helper {

  public static BucketAndKey getBucketAndKeyFromUrl(URL url, String[] validServicePaths, String[] validHostBucketSuffixes, String[] validDomains) throws ValidationErrorException {
    if (url.getHost() == null || url.getPath() == null) {
      throw new ValidationErrorException("Invalid S3 " + url);
    }
    // We may not get every form of possible URLs right at this point, but we will allow 3 types
    // 1) http://<ip address>/<service_path>/bucket/key  (No address checks will be done)
    // 2) Valid external domain, including something with ".objectstorage" or ".walrus".  In this case bucket name is first, and key is path
    // 3) Valid external domain, must start with objectstorage or walrus.  In this case bucket name is part of the path
    // 4) Valid external domain, with no objectstorage or walrus.  In this case there must be a service path, and /bucket/key will be assumed to be there.
    if (InetAddresses.isInetAddress(url.getHost())) {
      // must end in valid service path...
      for (String validServicePath : validServicePaths) {
        if (url.getPath().startsWith(validServicePath)) {
          return getBucketAndKeyFromPath(url.getPath().substring(validServicePath.length()), url);
        }
      }
      throw new ValidationErrorException("Invalid S3 url" + url + ", contains IP address but no OSG service path");
    }

    for (String validDomain : validDomains) {
      // DNS case insensitive
      String hostLower = url.getHost().toLowerCase();
      String validDomainLower = validDomain.toLowerCase();
      if (hostLower.endsWith(validDomainLower)) {
        // check bucket style...
        for (String validHostBucketSuffix : validHostBucketSuffixes) {
          String validHostNameBucketNameSuffixLower = validHostBucketSuffix.toLowerCase();
          if (hostLower.contains("." + validHostNameBucketNameSuffixLower)) {
            String bucket = hostLower.substring(0, hostLower.indexOf("." + validHostNameBucketNameSuffixLower));
            return getBucketAndKeyFromPath(bucket + "/" + url.getPath(), url);
          }
        }
        // check beginnings
        for (String validHostBucketSuffix : validHostBucketSuffixes) {
          String validHostNameBucketNameSuffixLower = validHostBucketSuffix.toLowerCase();
          if (hostLower.startsWith(validHostNameBucketNameSuffixLower + ".")) {
            return getBucketAndKeyFromPath(url.getPath(), url);
          }
        }
        // check service path
        for (String validServicePath : validServicePaths) {
          if (url.getPath().startsWith(validServicePath)) {
            return getBucketAndKeyFromPath(url.getPath().substring(validServicePath.length()), url);
          }
        }
      }
    }
    throw new ValidationErrorException("Invalid S3 url " + url + ", does not match any known S3 domains");
  }

  private static BucketAndKey getBucketAndKeyFromPath(String path, URL url) throws ValidationErrorException {
    try {
      StringTokenizer stok = new StringTokenizer(path, "/");
      String bucket = URLDecoder.decode(stok.nextToken());
      String delimiter = "";
      StringBuilder keyBuilder = new StringBuilder();
      do {
        keyBuilder.append(delimiter).append(URLDecoder.decode(stok.nextToken()));
        delimiter = "/";
      } while (stok.hasMoreTokens()); // must have at least one token for key, otherwise not valid
      return new BucketAndKey(bucket, keyBuilder.toString());

    } catch (Exception ex) {
      throw new ValidationErrorException("S3 URL " + url + " parses to invalid bucket/key pairs.");
    }
  }

  public static class BucketAndKey {
    String bucket;
    String key;
    BucketAndKey(String bucket, String key) {
      this.bucket = bucket;
      this.key = key;
    }
    public String getBucket() {
      return bucket;
    }

    public String getKey() {
      return key;
    }

    @Override
    public String toString() {
      return "BucketAndKey{" +
        "bucket='" + bucket + '\'' +
        ", key='" + key + '\'' +
        '}';
    }
  }

//  public static void main(String[] args) throws MalformedURLException, ValidationErrorException {
//    String[] validDomains = new String[]{"h-14.autoqa.qa1.eucalyptus-systems.com"};
//    String[] validServicePaths = new String[]{"/services/objectstorage","/services/walrus"};
//    String[] validHostNameBucketNameSuffixes = new String[]{"walrus","objectstorage"};
//    System.out.println(getBucketAndKeyFromUrl(new URL("http://10.0.0.10/services/objectstorage/path/to/my/key/a+b"), validServicePaths, validHostNameBucketNameSuffixes, validDomains));
//    System.out.println(getBucketAndKeyFromUrl(new URL("http://10.0.0.10/services/walrus/path/to/my/key/a+b"), validServicePaths, validHostNameBucketNameSuffixes, validDomains));
//    System.out.println(getBucketAndKeyFromUrl(new URL("http://boots.objectstorage.h-14.autoqa.qa1.eucalyptus-systems.com/key"), validServicePaths, validHostNameBucketNameSuffixes, validDomains));
//    System.out.println(getBucketAndKeyFromUrl(new URL("http://boots.walrus.h-14.autoqa.qa1.eucalyptus-systems.com/key"), validServicePaths, validHostNameBucketNameSuffixes, validDomains));
//    System.out.println(getBucketAndKeyFromUrl(new URL("http://walrus.h-14.autoqa.qa1.eucalyptus-systems.com/boots/key"), validServicePaths, validHostNameBucketNameSuffixes, validDomains));
//    System.out.println(getBucketAndKeyFromUrl(new URL("http://walrus.h-14.autoqa.qa1.eucalyptus-systems.com/boots/key"), validServicePaths, validHostNameBucketNameSuffixes, validDomains));
//    System.out.println(getBucketAndKeyFromUrl(new URL("http://walrus.h-15.autoqa.qa1.eucalyptus-systems.com/boots/key"), validServicePaths, validHostNameBucketNameSuffixes, validDomains));
//  }
}
