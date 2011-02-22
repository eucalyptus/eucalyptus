package com.eucalyptus.auth.policy.ern;

/**
 * A wildcard resource: "*"
 * 
 * @author wenye
 *
 */
public class WildcardResourceName extends Ern {
  
  @Override
  public String toString( ) {
    return "*";
  }
  
  @Override
  public String getResourceType( ) {
    return "*";
  }
  
  @Override
  public String getResourceName( ) {
    return "*";
  }
  
}
