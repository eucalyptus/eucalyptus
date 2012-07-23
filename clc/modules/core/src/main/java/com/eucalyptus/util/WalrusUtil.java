/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.util;

import java.io.UnsupportedEncodingException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;

import edu.ucsb.eucalyptus.cloud.WalrusException;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;
import edu.ucsb.eucalyptus.msgs.WalrusErrorMessageType;


public class WalrusUtil {

  public static BaseMessage convertErrorMessage(ExceptionResponseType errorMessage) {
    Throwable ex = errorMessage.getException();
    String correlationId = errorMessage.getCorrelationId( );
    BaseMessage errMsg = null;
    if( ( errMsg = convertException( correlationId, ex ) ) == null ) {
      errMsg = errorMessage;
    }
    return errMsg;
  }
	public static BaseMessage convertErrorMessage(EucalyptusErrorMessageType errorMessage) {
		Throwable ex = errorMessage.getException();
		String correlationId = errorMessage.getCorrelationId( );
    BaseMessage errMsg = null;
		if( ( errMsg = convertException( correlationId, ex ) ) == null ) {
		  errMsg = errorMessage;
		}
		return errMsg;
	}
  private static BaseMessage convertException( String correlationId, Throwable ex ) {
    BaseMessage errMsg;
    if(ex instanceof WalrusException) {
			WalrusException e = (WalrusException) ex;
			errMsg = new WalrusErrorMessageType(e.getMessage(), e.getCode(), e.getStatus(), e.getResourceType(), e.getResource(), correlationId, Internets.localHostAddress( ), e.getLogData());
			errMsg.setCorrelationId( correlationId );
			return errMsg;
		} else {
		  return null;
		}
  }
	
	public static String URLdecode(String objectKey) throws UnsupportedEncodingException {
		return URLDecoder.decode(objectKey, "UTF-8").replace("%20", "+").replace("%2A", "*").replace("~", "%7E").replace(" ", "+");
	}

	public static String[] getTarget(String operationPath) {
		operationPath = operationPath.replaceAll("/{2,}", "/");
		if(operationPath.startsWith("/"))
			operationPath = operationPath.substring(1);
		return operationPath.split("/");
	}
}
