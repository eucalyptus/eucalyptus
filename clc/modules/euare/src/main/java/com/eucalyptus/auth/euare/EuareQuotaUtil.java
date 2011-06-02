package com.eucalyptus.auth.euare;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;

public class EuareQuotaUtil {

  public static long countUserByAccount( String accountId ) throws AuthException {
    return Accounts.lookupAccountById( accountId ).getUsers( ).size( );
  }

  public static long countGroupByAccount( String accountId ) throws AuthException {
    return Accounts.lookupAccountById( accountId ).getGroups( ).size( );
  }

}
