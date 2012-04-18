package com.eucalyptus.auth.principal;

/**
 * 
 * @author wenye
 *
 */
public abstract interface HasId {

  /**
   * TODO:YE: in certain cases we /cannot/ rely on the database's ID field for use in the AWS API.
   * @see {@link com.eucalyptus.auth.Accounts}
   */
  public String getId( );
  
}
