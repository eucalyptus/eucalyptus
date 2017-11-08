/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
    String suffix = Crypto.generateAlphanumericId(13);
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
