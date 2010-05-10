package com.eucalyptus.auth.api;

import com.eucalyptus.auth.NoSuchUserException;
import com.eucalyptus.auth.UserExistsException;
import com.eucalyptus.auth.UserInfo;
import com.eucalyptus.util.Tx;

public interface UserInfoProvider {
  public UserInfo getUserInfo( UserInfo user ) throws NoSuchUserException;
  
  public void addUserInfo( UserInfo user ) throws UserExistsException;
  
  public void deleteUserInfo( String userName ) throws NoSuchUserException;
  
  public void updateUserInfo( String name, Tx<UserInfo> infoTx ) throws NoSuchUserException;
}
