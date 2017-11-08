/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.walrus.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.eucalyptus.util.Internets;
import com.eucalyptus.walrus.exceptions.WalrusException;
import com.eucalyptus.walrus.msgs.WalrusErrorMessageType;
import com.google.common.base.Strings;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;

public class WalrusUtil {

  public static BaseMessage convertErrorMessage(ExceptionResponseType errorMessage) {
    Throwable ex = errorMessage.getException();
    String correlationId = errorMessage.getCorrelationId();
    BaseMessage errMsg = null;
    if ((errMsg = convertException(correlationId, ex)) == null) {
      errMsg = errorMessage;
    }
    return errMsg;
  }

  public static BaseMessage convertErrorMessage(EucalyptusErrorMessageType errorMessage) {
    Throwable ex = errorMessage.getException();
    String correlationId = errorMessage.getCorrelationId();
    BaseMessage errMsg = null;
    if ((errMsg = convertException(correlationId, ex)) == null) {
      errMsg = errorMessage;
    }
    return errMsg;
  }

  private static BaseMessage convertException(String correlationId, Throwable ex) {
    BaseMessage errMsg;
    if (ex instanceof WalrusException) {
      WalrusException e = (WalrusException) ex;
      errMsg =
          new WalrusErrorMessageType(e.getMessage(), e.getCode(), e.getStatus(), e.getResourceType(), e.getResource(), correlationId,
              Internets.localHostAddress(), e.getLogData());
      errMsg.setCorrelationId(correlationId);
      return errMsg;
    } else {
      return null;
    }
  }

  public static String URLdecode(String objectKey) throws UnsupportedEncodingException {
    return URLDecoder.decode(objectKey, "UTF-8");
  }

  public static String[] getTarget(String operationPath) {
    operationPath = operationPath.replaceAll("^/{2,}", "/"); // If its in the form "/////bucket/key", change it to "/bucket/key"
    if (operationPath.startsWith("/")) { // If its in the form "/bucket/key", change it to "bucket/key"
      operationPath = operationPath.substring(1);
    }
    String[] parts = operationPath.split("/", 2); // Split into a maximum of two parts [bucket, key]
    if (parts != null) {
      if (parts.length == 1 && Strings.isNullOrEmpty(parts[0])) { // Splitting empty string will lead one part, check if the part is empty
        return null;
      } else if (parts.length == 2 && Strings.isNullOrEmpty(parts[1])) { // Splitting "bucket/" will lead to two parts where the second one is empty,
                                                                         // send only bucket
        return new String[] {parts[0]};
      }
    }
    return parts;
  }
}
