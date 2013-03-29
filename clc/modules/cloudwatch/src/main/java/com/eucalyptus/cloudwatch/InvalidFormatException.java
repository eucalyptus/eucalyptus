package com.eucalyptus.cloudwatch;

import com.eucalyptus.ws.Role;
import com.eucalyptus.ws.protocol.QueryBindingInfo;

@QueryBindingInfo( statusCode = 400 )
public class InvalidFormatException extends CloudWatchException {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public InvalidFormatException(final String message) {
    super("InvalidFormat", Role.Sender, message);
  }

}
