package com.eucalyptus.cloudwatch;

import com.eucalyptus.ws.Role;
import com.eucalyptus.ws.protocol.QueryBindingInfo;

@QueryBindingInfo( statusCode = 404 )
public class ResourceNotFoundException extends CloudWatchException {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public ResourceNotFoundException(final String message) {
    super("ResourceNotFound", Role.Sender, message);
  }

}
