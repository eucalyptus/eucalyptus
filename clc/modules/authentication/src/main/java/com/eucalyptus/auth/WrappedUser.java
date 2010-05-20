package com.eucalyptus.auth;

import java.util.List;
import com.eucalyptus.auth.principal.Group;

public interface WrappedUser {
  public UserInfo getUserInfo( ) throws NoSuchUserException;
}