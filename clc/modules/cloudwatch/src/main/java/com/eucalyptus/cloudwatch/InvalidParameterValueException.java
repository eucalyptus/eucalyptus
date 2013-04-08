package com.eucalyptus.cloudwatch;

import com.eucalyptus.ws.Role;
import com.eucalyptus.ws.protocol.QueryBindingInfo;

@QueryBindingInfo( statusCode = 400 )
public class InvalidParameterValueException extends CloudWatchException {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public InvalidParameterValueException(final String message) {
    super("InvalidParameterValue", Role.Sender, message);
  }

}
