/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudformation.resources;

import com.eucalyptus.crypto.Crypto;

/**
 * Created by ethomas on 9/28/14.
 */
public class ResourceActionHelper {
  static final String getDefaultPhysicalResourceId(String stackName, String logicalResourceId, int maxLength) {
    // in case we need to truncate this we need to truncate prefix and middle
    String prefix = (stackName != null) ? stackName : "UNKNOWN";
    String middle = (logicalResourceId != null) ? logicalResourceId : "UNKNOWN";
    String suffix = Crypto.generateAlphanumericId(13, "");
    String finalString = prefix + "-" + middle + "-" + suffix;
    if (finalString.length() > maxLength) {
      int prefixMiddleAndDashesLength = maxLength - suffix.length();
      int prefixAndDashLength = prefixMiddleAndDashesLength / 2;
      int middleAndDashLength = prefixMiddleAndDashesLength - prefixAndDashLength;
      int prefixLength = prefixAndDashLength - 1;
      int middleLength = middleAndDashLength - 1;
      if (prefix.length() < prefixLength) {
        int difference = prefixLength - prefix.length();
        prefixLength -= difference;
        middleLength += difference;
      } else if (middle.length() < middleLength) {
        int difference = middleLength - middle.length();
        middleLength -= difference;
        prefixLength += difference;
      }
      if (prefix.charAt(prefixLength - 1) == '-' && middleLength < middle.length()) {
        prefixLength -= 1;
        middleLength += 1;
      } else if (middle.charAt(middleLength - 1) == '-' && prefixLength < prefix.length()) {
        middleLength -= 1;
        prefixLength += 1;
      }
      return prefix.substring(0, prefixLength) + "-" + middle.substring(0, middleLength) + "-" + suffix;
    }
    return finalString;
  }

}
