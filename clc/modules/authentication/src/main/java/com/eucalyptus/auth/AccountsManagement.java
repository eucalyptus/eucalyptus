package com.eucalyptus.auth;

import edu.ucsb.eucalyptus.msgs.ListUsersResponseType;
import edu.ucsb.eucalyptus.msgs.ListUsersType;

public class AccountsManagement {
  
  public ListUsersResponseType listUsers( ListUsersType request ) {
    ListUsersResponseType response = request.getReply( );
    response.setIsTruncated( false );
    return response;
  }
  
}
