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
      List<User> users = EucaLdapHelper.getUsers( searchUser, null );
      if ( users.size( ) != 1 ) {
        throw new NoSuchCertificateException( ( users.size( ) == 0 ) ? "No user with the specified certificate." : "Multiple users with the same certificate." );
      }
      return true;
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
      List<User> users = EucaLdapHelper.getUsers( searchUser, null );
      if ( users.size( ) != 1 ) {
        throw new NoSuchUserException( ( users.size( ) == 0 ) ? "No user with the specified certificate." : "Multiple users with the same certificate." );
      }
      return users.get( 0 );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
      throw new NoSuchUserException( e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
    return null;
  }
  
  @Override
  public User lookupQueryId( String queryId ) throws NoSuchUserException {
    try {
      UserEntity searchUser = new UserEntity( );
      searchUser.setQueryId( queryId );
      return EucaLdapHelper.getUsers( searchUser, null ).get( 0 );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
      throw new NoSuchUserException( e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
    return null;
  }
  
  @Override
  public User lookupUser( String userName ) throws NoSuchUserException {
    try {
      UserEntity search = new UserEntity( );
      search.setName( userName );
      return EucaLdapHelper.getUsers( search, null ).get( 0 );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
      throw new NoSuchUserException( e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
    return null;
  }
  
  @Override
  public void updateUser( String userName, Tx<User> userTx ) throws NoSuchUserException {
    try {
      if ( userTx != null ) {
        UserEntity user = new UserEntity( userName );
        userTx.fire( user );
        EucaLdapHelper.updateUser( user, null );
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
  }
  
  @Override
  public List<Group> listAllGroups( ) {
    try {
      return EucaLdapHelper.getGroups( EucaLdapHelper.FILTER_ALL_GROUPS );
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
      GroupEntity group = new GroupEntity( );
      group.setName( groupName );
      return EucaLdapHelper.getGroups( group ).get( 0 );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
      throw new NoSuchGroupException( e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
    return null;
  }
  
  @Override
  public List<Group> lookupUserGroups( User user ) {
    try {
      UserEntity search = new UserEntity( );
      search.setName( user.getName( ) );
      LdapWrappedUser foundUser = ( LdapWrappedUser ) EucaLdapHelper.getUsers( search, null ).get( 0 );
      return EucaLdapHelper.getGroups( EucaLdapHelper.getSearchGroupFilter( foundUser.getEucaGroupIds( ) ) );
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
  }
  
  @Override
  public void deleteUserInfo( String userName ) throws NoSuchUserException {
    // LDAP don't need this
  }
  
  @Override
  public UserInfo getUserInfo( UserInfo info ) throws NoSuchUserException {
    try {
      return ( ( WrappedUser ) EucaLdapHelper.getUsers( null, info ).get( 0 ) ).getUserInfo( );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
      throw new NoSuchUserException( e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
    return null;
  }
  
  @Override
  public void updateUserInfo( String name, Tx<UserInfo> infoTx ) throws NoSuchUserException {
    try {
      if ( infoTx != null ) {
        UserInfo search = new UserInfo( name );
        infoTx.fire( search );
        EucaLdapHelper.updateUser( null, search );
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
