package com.eucalyptus.auth;

import java.util.List;
import org.apache.log4j.Logger;
import edu.ucsb.eucalyptus.msgs.ListUsersResponseType;
import edu.ucsb.eucalyptus.msgs.ListUsersType;
import edu.ucsb.eucalyptus.msgs.UserType;

public class AccountsManager {
  
  private static Logger LOG = Logger.getLogger( AccountsManager.class );
  
  public ListUsersResponseType listUsers( ListUsersType request ) {
    LOG.debug( "YE:" + "processing ListUsers" );
    ListUsersResponseType response = request.getReply( );
    List<UserType> users = response.getUsers( );
    UserType user = new UserType( );
    user.setArn( "arn:aws:iam::123456789012:user/division_abc/subdivision_xyz/engineering/Andrew" );
    user.setPath( "/division_abc/subdivision_xyz/engineering/" );
    user.setUserId( "AID2MAB8DPLSRHEXAMPLE" );
    user.setUserName( "Andrew" );
    users.add( user );
    response.setIsTruncated( false );
    return response;
  }
  
}
