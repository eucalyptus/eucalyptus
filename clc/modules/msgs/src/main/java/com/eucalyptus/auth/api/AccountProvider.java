package com.eucalyptus.auth.api;

import java.security.cert.X509Certificate;
import java.util.List;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;

public interface AccountProvider {

  public Account lookupAccountByName( String accountName ) throws AuthException;
  public Account lookupAccountById( String accountId ) throws AuthException;
  
  public Account addAccount( String accountName ) throws AuthException;
  public void deleteAccount( String accountName, boolean forceDeleteSystem, boolean recursive ) throws AuthException;
  public List<Account> listAllAccounts( ) throws AuthException;
  
  public List<User> listAllUsers( ) throws AuthException;
    
  public boolean shareSameAccount( String userId1, String userId2 );
  
  public User lookupUserById( String userId ) throws AuthException;
  public User lookupUserByAccessKeyId( String keyId ) throws AuthException;
  public User lookupUserByCertificate( X509Certificate cert ) throws AuthException;
  public User lookupUserByConfirmationCode( String code ) throws AuthException;
  
  public Group lookupGroupById( String groupId ) throws AuthException;
  
  public Certificate lookupCertificate( X509Certificate cert ) throws AuthException;
  
  public AccessKey lookupAccessKeyById( String keyId ) throws AuthException;
  @Deprecated
  public User lookupUserByName( String userName ) throws AuthException;
  
}
