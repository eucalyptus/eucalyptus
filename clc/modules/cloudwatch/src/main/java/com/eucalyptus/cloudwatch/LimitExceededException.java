package com.eucalyptus.cloudwatch;

import com.eucalyptus.ws.Role;
import com.eucalyptus.ws.protocol.QueryBindingInfo;

@QueryBindingInfo( statusCode = 400 )
public class LimitExceededException extends CloudWatchException {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public LimitExceededException(final String message) {
    super("LimitExceeded", Role.Sender, message);
  }

}
