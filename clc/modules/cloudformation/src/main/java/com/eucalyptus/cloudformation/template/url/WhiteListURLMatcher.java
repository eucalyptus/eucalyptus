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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Created by ethomas on 9/18/14.
 */
public class WhiteListURLMatcher {

  private static final String[] SUPPORTED_PROTOCOLS = new String[] {"http", "https"};
  public static boolean urlIsAllowed(URL url, String whiteList)  {
    if (whiteList == null) return false;
    boolean unsupportedProtocol = true;
    for (String protocol: SUPPORTED_PROTOCOLS) {
      if (protocol.equalsIgnoreCase(url.getProtocol())) {
        unsupportedProtocol = false;
        break;
      }
    }
    if (unsupportedProtocol) return false;
    String[] whiteListPatterns = whiteList.split("\\,");
    boolean supportedPattern = false;
    if (whiteListPatterns != null) {
      for (String whiteListPattern: whiteListPatterns) {
        whiteListPattern = whiteListPattern.trim();
        // check if the pattern includes a protocol
        String includedProtocol = null;
        for (String protocol: SUPPORTED_PROTOCOLS) {
          if (whiteListPattern.toLowerCase().startsWith(protocol.toLowerCase() + "://")) {
            includedProtocol = protocol;
            whiteListPattern = whiteListPattern.substring(protocol.toLowerCase().length() + "://".length());
            break;
          }
        }
        if (matchesString(whiteListPattern.toLowerCase(), url.getHost().toLowerCase()) &&
          (includedProtocol == null || includedProtocol.equalsIgnoreCase(url.getProtocol()))) {
          supportedPattern = true;
          break;
        }
      }
    }
    return supportedPattern;
  }

  private static boolean matchesString(String pattern, String target) {
    // We want to check just against ? and * in the pattern, but need to be careful if the string contains other reg-exp characters
    StringTokenizer stok = new StringTokenizer(pattern, "*?", true); // tokenize on * and ? and leave those tokens in there
    StringBuilder newPattern = new StringBuilder();
    while (stok.hasMoreTokens()) {
      String currentToken = stok.nextToken();
      if ("*".equals(currentToken)) {
        newPattern.append(".*");
      } else if ("?".equals(currentToken)) {
        newPattern.append(".?");
      } else {
        newPattern.append(Pattern.quote(currentToken)); // literal values for everything else
      }
    }
    return target.matches(newPattern.toString());
  }

}
