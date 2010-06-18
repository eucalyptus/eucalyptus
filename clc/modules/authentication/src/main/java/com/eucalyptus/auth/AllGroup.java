package com.eucalyptus.auth;

import java.security.Principal;
import java.util.Enumeration;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;

public class AllGroup extends DatabaseWrappedGroup {
  
  public AllGroup( Group g ) {
    super( g );
    if( !"all".equals( g.getName( ) ) ) {
      throw new RuntimeException( "EID: This is exclusively for the 'all' group and can't be used for: " + g.getName( ) );
    }
  }

  @Override
  public ImmutableList<User> getMembers( ) {
    return ImmutableList.copyOf( Users.listAllUsers( ) );
  }

  @Override
  public boolean addMember( Principal principal ) {
    return true;
  }

  @Override
  public boolean isMember( Principal member ) {
    return true;
  }

  @Override
  public Enumeration<? extends Principal> members( ) {
    return Iterators.asEnumeration( Users.listAllUsers( ).iterator( ) );
  }

  @Override
  public boolean removeMember( Principal user ) {
    return true;
  }

}
