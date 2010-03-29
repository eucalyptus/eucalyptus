package com.eucalyptus.auth;

import java.util.List;

public class Groups {
  public static List<Group> getGroups( User user ) {
    return CredentialProviders.getUserProvider( ).getUserGroups( user );
  }
}
