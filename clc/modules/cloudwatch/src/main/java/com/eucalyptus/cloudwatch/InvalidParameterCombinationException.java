package com.eucalyptus.cloudwatch;

import com.eucalyptus.ws.Role;
import com.eucalyptus.ws.protocol.QueryBindingInfo;

@QueryBindingInfo( statusCode = 400 )
public class InvalidParameterCombinationException extends CloudWatchException {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public InvalidParameterCombinationException(final String message) {
    super("InvalidParameterCombination", Role.Sender, message);
  }

}
