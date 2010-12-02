package com.eucalyptus.auth.principal;

import java.security.Principal;
import java.util.Enumeration;
import java.util.List;
import com.eucalyptus.auth.AuthException;

/**
 * Dummy class for image user group. Only "all" is supported in AWS.
 * 
 * @author wenye
 *
 */
public class ImageUserGroup implements Group {
  
  public static final ImageUserGroup ALL = new ImageUserGroup( "all" );
  
  private String name;
  
  public ImageUserGroup( String name ) {
    this.name = name;
  }
  
  @Override
  public boolean addMember( Principal arg0 ) {
    // TODO Auto-generated method stub
    return false;
  }
  
  @Override
  public boolean isMember( Principal arg0 ) {
    // TODO Auto-generated method stub
    return false;
  }
  
  @Override
  public Enumeration<? extends Principal> members( ) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public boolean removeMember( Principal arg0 ) {
    // TODO Auto-generated method stub
    return false;
  }
  
  @Override
  public String getName( ) {
    return this.name;
  }
  
  @Override
  public void setName( String name ) throws AuthException {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public String getPath( ) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public Boolean isUserGroup( ) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public Account getAccount( ) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<? extends User> getUsers( ) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getGroupId( ) {
    // TODO Auto-generated method stub
    return null;
  }
  
}
