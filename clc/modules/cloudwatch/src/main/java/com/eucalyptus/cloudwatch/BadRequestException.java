package com.eucalyptus.cloudwatch;

import com.eucalyptus.ws.Role;
import com.eucalyptus.ws.protocol.QueryBindingInfo;

@QueryBindingInfo( statusCode = 400 )
public class BadRequestException extends CloudWatchException {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public BadRequestException(final String message) {
    super("400 Bad Request", Role.Sender, message);
  }

}
