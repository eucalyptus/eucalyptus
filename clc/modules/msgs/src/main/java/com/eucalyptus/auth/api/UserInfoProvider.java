package com.eucalyptus.auth.api;

import com.eucalyptus.auth.NoSuchUserException;
import com.eucalyptus.auth.UserExistsException;
import com.eucalyptus.auth.UserInfo;

public interface UserInfoProvider {
  public UserInfo getUserInfo(String name) throws NoSuchUserException;
  public void addUserInfo(UserInfo user) throws UserExistsException;
  public void updateUserInfo(UserInfo user) throws NoSuchUserException;
  public void deleteUserInfo(UserInfo user) throws NoSuchUserException;
}
