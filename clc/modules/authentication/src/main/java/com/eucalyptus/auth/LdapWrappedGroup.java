package com.eucalyptus.auth;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.ldap.EntryNotFoundException;
import com.eucalyptus.auth.ldap.LdapException;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.BaseAuthorization;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;

public class LdapWrappedGroup implements Group {
  private static Logger LOG = Logger.getLogger( LdapWrappedGroup.class );
  
  public static Group newInstance( Group g ) {
    if ( Groups.NAME_ALL.equals( g.getName( ) ) ) {
      return new LdapAllGroup( g );
    } else {
      return new LdapWrappedGroup( g );
    }
  }
  
  private Group group;
  
  public LdapWrappedGroup( Group group ) {
    this.group = group;
  }
  
  @Override
  public boolean addAuthorization( Authorization authorization ) {
    try {
      BaseAuthorization auth = ( BaseAuthorization ) authorization;
      GroupEntity search = new GroupEntity( this.group.getName( ) );
      search.addAuthorization( auth );
      EucaLdapHelper.addGroupAttribute( search );
      return true;
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
    return false;
  }
  
  @Override
  public ImmutableList<Authorization> getAuthorizations( ) {
    return this.group.getAuthorizations( );
  }
  
  @Override
  public ImmutableList<User> getMembers( ) {
    try {
      LdapUserGroups groups = new LdapUserGroups( );
      groups.addGroupId( this.group.getName( ) );
      List<User> users = EucaLdapHelper.getUsers( null, null, groups );
      return ImmutableList.copyOf( users );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
    return ImmutableList.copyOf( ( new ArrayList<User>( ) ) );
  }
  
  @Override
  public String getName( ) {
    return this.group.getName( );
  }
  
  @Override
  public boolean removeAuthorization( Authorization authorization ) {
    try {
      BaseAuthorization auth = ( BaseAuthorization ) authorization;
      GroupEntity search = new GroupEntity( this.group.getName( ) );
      search.addAuthorization( auth );
      EucaLdapHelper.deleteGroupAttribute( search );
      return true;
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
    return false;
  }
  
  @Override
  public boolean addMember( Principal principal ) {
    try {
      LdapUserGroups search = new LdapUserGroups( );
      search.addGroupId( this.group.getName( ) );
      EucaLdapHelper.addUserAttribute( principal.getName( ), search );
      return true;
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
    return false;
  }
  
  @Override
  public boolean isMember( Principal principal ) {
    for ( User user : this.group.getMembers( ) ) {
      if ( principal.getName( ) != null && user.getName( ) != null && principal.getName( ).equals( user.getName( ) ) ) {
        return true;
      }
    }
    return false;
  }
  
  @Override
  public Enumeration<? extends Principal> members( ) {
    try {
      LdapUserGroups groups = new LdapUserGroups( );
      groups.addGroupId( this.group.getName( ) );
      List<User> users = EucaLdapHelper.getUsers( null, null, groups );
      return Iterators.asEnumeration( users.iterator( ) );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
    return Iterators.asEnumeration( ( new ArrayList<User>( ) ).iterator( ) );
  }
  
  @Override
  public boolean removeMember( Principal principal ) {
    try {
      LdapUserGroups search = new LdapUserGroups( );
      search.addGroupId( this.group.getName( ) );
      EucaLdapHelper.deleteUserAttribute( principal.getName( ), search );
      return true;
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
    return false;
  }
  
  public String toString( ) {
    return "LdapWrappedGroup [ group = " + this.group.toString( ) + " ]";
  }
}
