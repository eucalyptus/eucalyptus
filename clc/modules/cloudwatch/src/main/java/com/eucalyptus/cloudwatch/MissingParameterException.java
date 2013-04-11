package com.eucalyptus.cloudwatch;

import com.eucalyptus.ws.Role;
import com.eucalyptus.ws.protocol.QueryBindingInfo;

@QueryBindingInfo( statusCode = 400 )
public class MissingParameterException extends CloudWatchException {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public MissingParameterException(final String message) {
    super("MissingParameter", Role.Sender, message);
  }

}
