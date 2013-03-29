package com.eucalyptus.cloudwatch;

import com.eucalyptus.ws.Role;
import com.eucalyptus.ws.protocol.QueryBindingInfo;

@QueryBindingInfo( statusCode = 500 )
public class InternalFailureException extends CloudWatchException {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public InternalFailureException(final String message) {
    super("InternalFailure", Role.Sender, message);
  }

}
