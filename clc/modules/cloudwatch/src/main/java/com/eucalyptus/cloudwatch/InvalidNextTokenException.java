package com.eucalyptus.cloudwatch;

import com.eucalyptus.ws.Role;
import com.eucalyptus.ws.protocol.QueryBindingInfo;

@QueryBindingInfo( statusCode = 400 )
public class InvalidNextTokenException extends CloudWatchException {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public InvalidNextTokenException(final String message) {
    super("InvalidNextToken", Role.Sender, message);
  }

}
