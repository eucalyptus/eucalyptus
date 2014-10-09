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
