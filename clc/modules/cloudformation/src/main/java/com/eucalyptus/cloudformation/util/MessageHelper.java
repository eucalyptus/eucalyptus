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
package com.eucalyptus.cloudformation.util;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.InternalFailureException;
import com.eucalyptus.util.Exceptions;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import org.apache.log4j.Logger;


/**
 * Created by ethomas on 9/28/14.
 */
public class MessageHelper {
  private static final Logger LOG = Logger.getLogger(MessageHelper.class);

  public static <T extends BaseMessage> T createMessage(Class<T> tClass, String effectiveUserId) throws CloudFormationException {
    try {
      T t = (T) tClass.newInstance();
      t.setEffectiveUserId(effectiveUserId);
      return t;
    } catch (Exception e) {
      throw new InternalFailureException(e.getMessage());
    }
  }

  public static <T extends BaseMessage> T createPrivilegedMessage(Class<T> tClass, String effectiveUserId) throws CloudFormationException {
    try {
      T t = (T) tClass.newInstance();
      t.setUserId(effectiveUserId);
      t.markPrivileged();
      return t;
    } catch (Exception e) {
      throw new InternalFailureException(e.getMessage());
    }
  }

}
