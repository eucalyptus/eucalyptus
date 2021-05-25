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
package com.eucalyptus.cloudformation.util;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.InternalFailureException;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import java.util.function.Function;
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

  public static Function<BaseMessage,BaseMessage> userIdentity(final String effectiveUserId) {
    return message -> {
      message.setEffectiveUserId(effectiveUserId);
      return message;
    };
  }

  public static Function<BaseMessage,BaseMessage> privilegedUserIdentity(final String effectiveUserId) {
    return message -> {
      message.setEffectiveUserId(effectiveUserId);
      message.markPrivileged();
      return message;
    };
  }
}
