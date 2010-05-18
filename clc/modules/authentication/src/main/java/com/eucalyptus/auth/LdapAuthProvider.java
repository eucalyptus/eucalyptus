package com.eucalyptus.auth;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.api.GroupProvider;
import com.eucalyptus.auth.api.NoSuchCertificateException;
import com.eucalyptus.auth.api.UserInfoProvider;
import com.eucalyptus.auth.api.UserProvider;
import com.eucalyptus.auth.crypto.Crypto;
import com.eucalyptus.auth.crypto.Hmacs;
import com.eucalyptus.auth.ldap.EntryExistsException;
import com.eucalyptus.auth.ldap.EntryNotFoundException;
import com.eucalyptus.auth.EucaLdapHelper;
import com.eucalyptus.auth.ldap.LdapException;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.util.Tx;

public class LdapAuthProvider implements UserProvider, GroupProvider, UserInfoProvider {
  
  private static Logger LOG = Logger.getLogger( LdapAuthProvider.class );
  
  LdapAuthProvider( ) {}
  
  @Override
  public User addUser( String userName, Boolean admin, Boolean enabled ) throws UserExistsException {
    UserEntity newUser = new UserEntity( userName );
    newUser.setQueryId( Hmacs.generateQueryId( userName ) );
    newUser.setSecretKey( Hmacs.generateSecretKey( userName ) );
    newUser.setAdministrator( admin );
    newUser.setEnabled( enabled );
    newUser.setPassword( Crypto.generateHashedPassword( userName ) );
    newUser.setToken( Crypto.generateSessionToken( userName ) );
    String confirmCode = Crypto.generateSessionToken( userName );
    UserInfo newUserInfo = new UserInfo( userName, confirmCode );
    try {
      EucaLdapHelper.addUser( newUser, newUserInfo );
    } catch ( EntryExistsException e ) {
      LOG.error( e, e );
      throw new UserExistsException( e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
    User proxy = new LdapWrappedUser( newUser, newUserInfo );
    Groups.DEFAULT.addMember( proxy );
    return proxy;
  }
  
  @Override
  public boolean checkRevokedCertificate( X509Certificate cert ) throws NoSuchCertificateException {
    try {    
      UserEntity searchUser = new UserEntity( );
      searchUser.setEnabled( true );
      searchUser.setX509Certificate( cert );
      searchUser.revokeX509Certificate( );
      User found;
      if ( ( found = LdapCache.getInstance( ).getUserByRevokedCert( searchUser.getCertificates( ).get( 0 ).getPemCertificate( ) ) ) == null ) {
        found = EucaLdapHelper.getUsers( searchUser, null ).get( 0 );
        LdapCache.getInstance( ).addUser( found );
      }
      if ( found.isEnabled( ) ) {
        return true;
      }
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
      throw new NoSuchCertificateException( "No user with the specified certificate.", e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
    return false;
  }
  
  @Override
  public void deleteUser( String userName ) throws NoSuchUserException, UnsupportedOperationException {
    try {
      EucaLdapHelper.deleteUser( userName );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
      throw new NoSuchUserException( userName, e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
    LdapCache.getInstance( ).removeUser( userName );
  }
  
  @Override
  public List<User> listAllUsers( ) {
    try {
      return EucaLdapHelper.getUsers( EucaLdapHelper.FILTER_ALL_USERS );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
    return new ArrayList<User>( );
  }
  
  @Override
  public List<User> listEnabledUsers( ) {
    try {
      UserEntity user = new UserEntity( );
      user.setEnabled( true );
      return EucaLdapHelper.getUsers( user, null );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
    return new ArrayList<User>( );
  }
  
  @Override
  public User lookupCertificate( X509Certificate cert ) throws NoSuchUserException {
    try {
      UserEntity searchUser = new UserEntity( );
      searchUser.setEnabled( true );
      searchUser.setX509Certificate( cert );
      User found;
      if ( ( found = LdapCache.getInstance( ).getUserByCert( searchUser.getCertificates( ).get( 0 ).getPemCertificate( ) ) ) == null ) {
        found = EucaLdapHelper.getUsers( searchUser, null ).get( 0 );
        LdapCache.getInstance( ).addUser( found );
      }
      if ( found.isEnabled( ) ) {
        return found;
      }
      throw new EntryNotFoundException( "Found user is not enabled" );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
      throw new NoSuchUserException( e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
      throw new NoSuchUserException( e );
    }
  }
  
  @Override
  public User lookupQueryId( String queryId ) throws NoSuchUserException {
    try {
      User found;
      if ( ( found = LdapCache.getInstance( ).getUserByQueryId( queryId ) ) == null ) {
        UserEntity searchUser = new UserEntity( );
        searchUser.setQueryId( queryId );
        found = EucaLdapHelper.getUsers( searchUser, null ).get( 0 );
        LdapCache.getInstance( ).addUser( found );
      } 
      return found;
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
      throw new NoSuchUserException( e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
      throw new NoSuchUserException( e );
    }
  }
  
  @Override
  public User lookupUser( String userName ) throws NoSuchUserException {
    try {
      User found;
      if ( ( found = LdapCache.getInstance( ).getUserByName( userName ) ) == null ) {
        UserEntity search = new UserEntity( );
        search.setName( userName );
        found = EucaLdapHelper.getUsers( search, null ).get( 0 );
        LdapCache.getInstance( ).addUser( found );
      }
      return found;
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
      throw new NoSuchUserException( e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
      throw new NoSuchUserException( e );
    }
  }
  
  @Override
  public void updateUser( String userName, Tx<User> userTx ) throws NoSuchUserException {
    try {
      if ( userTx != null ) {
        UserEntity user = new UserEntity( userName );
        userTx.fire( user );
        EucaLdapHelper.updateUser( user, null );
        LdapCache.getInstance( ).removeUser( userName );
      }
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
      throw new NoSuchUserException( e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    } catch ( Throwable e ) {
      LOG.error( e, e );
    }
  }
  
  @Override
  public Group addGroup( String groupName ) throws GroupExistsException {
    GroupEntity newGroup = new GroupEntity( groupName, Long.toString( Calendar.getInstance( ).getTimeInMillis( ) ) );
    try {     
      EucaLdapHelper.addGroup( newGroup );
    } catch ( EntryExistsException e ) {
      LOG.error( e, e );
      throw new GroupExistsException( e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
    // Invalid group cache
    LdapCache.getInstance( ).clearGroups( );
    return LdapWrappedGroup.newInstance( newGroup );
  }
  
  /**
   * We only delete the group from group subtree but leave the group ID in user entry.
   * See LdapUserGroups.java for details.
   * TODO (wenye): purge user's eucaGroupId attribute if necessary.
   */
  @Override
  public void deleteGroup( String groupName ) throws NoSuchGroupException {
    try {
      EucaLdapHelper.deleteGroup( groupName );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
      throw new NoSuchGroupException( groupName, e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
    // Invalid group cache
    LdapCache.getInstance( ).clearGroups( );
  }
  
  @Override
  public List<Group> listAllGroups( ) {
    try {
      if ( LdapCache.getInstance( ).isGroupCacheInvalidated( ) ) {
        LdapCache.getInstance( ).reloadGroups( EucaLdapHelper.getGroups( EucaLdapHelper.FILTER_ALL_GROUPS ) );
      }
      return LdapCache.getInstance( ).getGroups( null );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
    return new ArrayList<Group>( );
  }
  
  @Override
  public Group lookupGroup( String groupName ) throws NoSuchGroupException {
    try {
      if ( LdapCache.getInstance( ).isGroupCacheInvalidated( ) ) {
        LdapCache.getInstance( ).reloadGroups( EucaLdapHelper.getGroups( EucaLdapHelper.FILTER_ALL_GROUPS ) );
      }
      Group result = LdapCache.getInstance( ).getGroup( groupName );
      if ( result == null ) {
        throw new EntryNotFoundException( "Can not found group " + groupName );
      }
      return result;
      //GroupEntity group = new GroupEntity( );
      //group.setName( groupName );
      //return EucaLdapHelper.getGroups( group ).get( 0 );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
      throw new NoSuchGroupException( e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
      throw new NoSuchGroupException( e );
    }
  }
  
  @Override
  public List<Group> lookupUserGroups( User user ) {
    try {
      LdapWrappedUser foundUser = ( LdapWrappedUser ) lookupUser( user.getName( ) );
      if ( LdapCache.getInstance( ).isGroupCacheInvalidated( ) ) {
        LdapCache.getInstance( ).reloadGroups( EucaLdapHelper.getGroups( EucaLdapHelper.FILTER_ALL_GROUPS ) );
      }
      return LdapCache.getInstance( ).getGroups( EucaLdapHelper.getNamesFromEucaGroupIds( foundUser.getEucaGroupIds( ) ) );
      //UserEntity search = new UserEntity( );
      //search.setName( user.getName( ) );
      //LdapWrappedUser foundUser = ( LdapWrappedUser ) EucaLdapHelper.getUsers( search, null ).get( 0 );
      //return EucaLdapHelper.getGroups( EucaLdapHelper.getSearchGroupFilter( foundUser.getEucaGroupIds( ) ) );
    } catch ( NoSuchUserException e ) {
      LOG.error( e, e );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
    return new ArrayList<Group>( );
  }
  
  @Override
  public void addUserInfo( UserInfo user ) throws UserExistsException {
    try {
      EucaLdapHelper.updateUser( null, user );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
    LdapCache.getInstance( ).removeUser( user.getUserName( ) );
  }
  
  @Override
  public void deleteUserInfo( String userName ) throws NoSuchUserException {
    // LDAP don't need this
  }
  
  @Override
  public UserInfo getUserInfo( UserInfo info ) throws NoSuchUserException {
    try {
      User found;
      if ( ( found = LdapCache.getInstance( ).getUserByName( info.getUserName( ) ) ) == null ) {
        found = EucaLdapHelper.getUsers( null, info ).get( 0 );
        LdapCache.getInstance( ).addUser( found );
      }
      return ( ( WrappedUser ) found ).getUserInfo( );    
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
      throw new NoSuchUserException( e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
      throw new NoSuchUserException( e );
    }
  }
  
  @Override
  public void updateUserInfo( String name, Tx<UserInfo> infoTx ) throws NoSuchUserException {
    try {
      if ( infoTx != null ) {
        UserInfo search = new UserInfo( name );
        infoTx.fire( search );
        EucaLdapHelper.updateUser( null, search );
        LdapCache.getInstance( ).removeUser( name );
      }
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
      throw new NoSuchUserException( e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    } catch ( Throwable e ) {
      LOG.error( e, e );
    }
  }
}
