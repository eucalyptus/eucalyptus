/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudformation.template;

import com.eucalyptus.cloudformation.ValidationErrorException;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by ethomas on 11/1/16.
 */
public class FnSubHelper {
  public static Collection<String> extractVariables(String value) throws ValidationErrorException {
    List<String> variables = Lists.newArrayList();
    recursivelyExtractVariablesToCollection(value, variables);
    return variables;
  }

  private static void recursivelyExtractVariablesToCollection(String s, Collection<String> variables) throws ValidationErrorException {
    if (s != null) {
      int beginPos = s.indexOf("${");
      // only proceed if we can find a ${ not at the end of the string
      if (beginPos != -1 && s.length() != beginPos + 2) {
        if (s.charAt(beginPos + 2) == '!') { // literal
          recursivelyExtractVariablesToCollection(s.substring(beginPos + 3), variables);
        } else {
          int endPos = s.indexOf("}", beginPos + 2);
          if (endPos != -1) {
            String variable = s.substring(beginPos + 2, endPos);
            Pattern pattern = Pattern.compile("[A-Za-z0-9_\\:\\.]+");
            if (!pattern.matcher(variable).matches()) {
              throw new ValidationErrorException("Template error: variable names in Fn::Sub syntax must contain only " +
                "alphanumeric characters, underscores, periods, and colons");
            }
            variables.add(variable);
            recursivelyExtractVariablesToCollection(s.substring(endPos + 1), variables);
          }
        }
      }
    }
  }

  public static String replaceVariables(String value, Map<String, String> variableMapping) throws ValidationErrorException {
    StringBuilder builder = new StringBuilder();
    recursivelyReplaceVariablesWithStringBuilder(value, variableMapping, builder);
    return builder.toString();
  }

  private static void recursivelyReplaceVariablesWithStringBuilder(String value, Map<String, String> variableMapping, StringBuilder builder) throws ValidationErrorException {
    if (value != null) {
      int beginPos = value.indexOf("${");
      // only proceed if we can find a ${ not at the end of the string
      if (beginPos != -1 && value.length() != beginPos + 2) {
        if (value.charAt(beginPos + 2) == '!') { // literal
          builder.append(value.substring(0, beginPos+2)); // add everything up to here
          recursivelyReplaceVariablesWithStringBuilder(value.substring(beginPos + 3), variableMapping, builder);
        } else {
          int endPos = value.indexOf("}", beginPos + 2);
          if (endPos != -1) {
            String variable = value.substring(beginPos + 2, endPos);
            Pattern pattern = Pattern.compile("[A-Za-z0-9_\\:\\.]+");
            if (!pattern.matcher(variable).matches()) {
              throw new ValidationErrorException("Template error: variable names in Fn::Sub syntax must contain only " +
                "alphanumeric characters, underscores, periods, and colons");
            }
            if (!variableMapping.containsKey(variable)) {
              throw new ValidationErrorException("Template error: unmapped variable " + variable + " in string for Fn::Sub");
            }
            builder.append(value.substring(0, beginPos));
            builder.append(variableMapping.get(variable));
            recursivelyReplaceVariablesWithStringBuilder(value.substring(endPos + 1), variableMapping, builder);
          } else {
            // might have been a variable but no closing }.  Use whole thing
            builder.append(value);
          }
        }
      } else {
        // no variables left in string, use the whole thing
        builder.append(value);
      }
    }
  }

}
