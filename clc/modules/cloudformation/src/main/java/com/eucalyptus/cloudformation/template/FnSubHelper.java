/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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
