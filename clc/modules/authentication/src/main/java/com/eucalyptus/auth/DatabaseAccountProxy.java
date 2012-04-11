package com.eucalyptus.auth;

import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.auth.checker.InvalidValueException;
import com.eucalyptus.auth.checker.ValueChecker;
import com.eucalyptus.auth.checker.ValueCheckerFactory;
import com.eucalyptus.auth.entities.AccountEntity;
import com.eucalyptus.auth.entities.AuthorizationEntity;
import com.eucalyptus.auth.entities.CertificateEntity;
import com.eucalyptus.auth.entities.GroupEntity;
import com.eucalyptus.auth.entities.UserEntity;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.Authorization.EffectType;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.Hmacs;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.Transactions;
import java.util.concurrent.ExecutionException;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Lists;

public class DatabaseAccountProxy implements Account {

  private static final long serialVersionUID = 1L;

  private static Logger LOG = Logger.getLogger( DatabaseAccountProxy.class );

  private static final ValueChecker ACCOUNT_NAME_CHECKER = ValueCheckerFactory.createAccountNameChecker( );
  private static final ValueChecker USER_GROUP_NAME_CHECKER = ValueCheckerFactory.createUserAndGroupNameChecker( );
  private static final ValueChecker PATH_CHECKER = ValueCheckerFactory.createPathChecker( );

  private AccountEntity delegate;
  
  public DatabaseAccountProxy( AccountEntity delegate ) {
    this.delegate = delegate;
  }

  @Override
  public String getName( ) {
    return this.delegate.getName( );
  }
  
  @Override
  public String toString( ) {
    return this.delegate.toString( );
  }

  @Override
  public String getAccountNumber( ) {
    return this.delegate.getAccountNumber( );
  }

  @Override
  public void setName( final String name ) throws AuthException {
    try {
      ACCOUNT_NAME_CHECKER.check( name );
    } catch ( InvalidValueException e ) {
      Debugging.logError( LOG, e, "Invalid account name " + name );
      throw new AuthException( AuthException.INVALID_NAME, e );
    }
    try {
      // try finding the account with the same name to change to
      ( new DatabaseAuthProvider( ) ).lookupAccountByName( name );
    } catch ( AuthException ae ) {
      try {
        // not found
        DatabaseAuthUtils.invokeUnique( AccountEntity.class, "accountNumber", this.delegate.getAccountNumber( ), new Tx<AccountEntity>( ) {
          public void fire( AccountEntity t ) {
            t.setName( name );
          }
        } );
      } catch ( Exception e ) {
        Debugging.logError( LOG, e, "Failed to setName for " + this.delegate );
        throw new AuthException( e );
      }
      return;
    }
    // found
    throw new AuthException( AuthException.ACCOUNT_ALREADY_EXISTS );
  }

  @Override
  public List<User> getUsers( ) throws AuthException {
    List<User> results = Lists.newArrayList( );
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    try {
      @SuppressWarnings( "unchecked" )
      List<UserEntity> users = ( List<UserEntity> ) db
          .createCriteria( UserEntity.class ).setCacheable( true )
          .createCriteria( "groups" ).setCacheable( true ).add( Restrictions.eq( "userGroup", true ) )
          .createCriteria( "account" ).setCacheable( true ).add( Restrictions.eq( "name", this.delegate.getName( ) ) )
          .list( );
      db.commit( );
      for ( UserEntity u : users ) {
        results.add( new DatabaseUserProxy( u ) );
      }
      return results;
    } catch ( Exception e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to get users for " + this.delegate.getName( ) );
      throw new AuthException( "Failed to get users for account", e );
    }
  }

  @Override
  public List<Group> getGroups( ) throws AuthException {
    List<Group> results = Lists.newArrayList( );
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    try {
      @SuppressWarnings( "unchecked" )
      List<GroupEntity> groups = ( List<GroupEntity> ) db
          .createCriteria( GroupEntity.class ).setCacheable( true ).add( Restrictions.eq( "userGroup", false ) )
          .createCriteria( "account" ).setCacheable( true ).add( Restrictions.eq( "name", this.delegate.getName( ) ) )
          .list( );
      db.commit( );
      for ( GroupEntity g : groups ) {
        results.add( new DatabaseGroupProxy( g ) );
      }
      return results;
    } catch ( Exception e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to get groups for " + this.delegate.getName( ) );
      throw new AuthException( "Failed to get groups", e );
    }
  }
  
  @Override
  public User addUser( String userName, String path, boolean skipRegistration, boolean enabled, Map<String, String> info ) throws AuthException {
    try {
      USER_GROUP_NAME_CHECKER.check( userName );
    } catch ( InvalidValueException e ) {
      Debugging.logError( LOG, e, "Invalid user name " + userName );
      throw new AuthException( AuthException.INVALID_NAME, e );
    }
    try {
      PATH_CHECKER.check( path );
    } catch ( InvalidValueException e ) {
      Debugging.logError( LOG, e, "Invalid path " + path );
      throw new AuthException( AuthException.INVALID_PATH, e );
    }
    if ( DatabaseAuthUtils.checkUserExists( userName, this.delegate.getName( ) ) ) {
      throw new AuthException( AuthException.USER_ALREADY_EXISTS );
    }
    UserEntity newUser = new UserEntity( userName );
    newUser.setPath( path );
    newUser.setEnabled( enabled );
    newUser.setPasswordExpires( System.currentTimeMillis( ) + User.PASSWORD_LIFETIME );
    if ( skipRegistration ) {
      newUser.setRegistrationStatus( User.RegistrationStatus.CONFIRMED );
    } else {
      newUser.setRegistrationStatus( User.RegistrationStatus.REGISTERED );
    }
    if ( info != null ) {
      newUser.getInfo( ).putAll( info );
    }
    newUser.setToken( Crypto.generateSessionToken() );
    //newUser.setConfirmationCode( Crypto.generateSessionToken( userName ) );
    GroupEntity newGroup = new GroupEntity( DatabaseAuthUtils.getUserGroupName( userName ) );
    newGroup.setUserGroup( true );
    EntityWrapper<AccountEntity> db = EntityWrapper.get( AccountEntity.class );
    try {
      AccountEntity account = DatabaseAuthUtils.getUnique( db, AccountEntity.class, "name", this.delegate.getName( ) );
      newGroup = db.recast( GroupEntity.class ).merge( newGroup );
      newUser = db.recast( UserEntity.class ).merge( newUser );
      newGroup.setAccount( account );
      newGroup.getUsers( ).add( newUser );
      newUser.getGroups( ).add( newGroup );
      db.commit( );
      return new DatabaseUserProxy( newUser );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to add user: " + userName + " in " + this.delegate.getName( ) );
      db.rollback( );
      throw new AuthException( AuthException.USER_CREATE_FAILURE, e );
    }
  }
  
  private boolean userHasResourceAttached( String userName, String accountName ) throws AuthException {
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    try {
      UserEntity user = DatabaseAuthUtils.getUniqueUser( db, userName, accountName );
      GroupEntity userGroup = DatabaseAuthUtils.getUniqueGroup( db.recast( GroupEntity.class ), DatabaseAuthUtils.getUserGroupName( userName ), accountName );
      boolean result = ( user.getGroups( ).size( ) > 1
          || user.getKeys( ).size( ) > 0
          || getCurrentCertificateNumber( user.getCertificates( ) ) > 0
          || userGroup.getPolicies( ).size( ) > 0 );
      db.commit( );
      return result;
    } catch ( Exception e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to check user " + userName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_USER, e );
    }
  }
  
  private static int getCurrentCertificateNumber( List<CertificateEntity> certs ) {
    int num = 0;
    for ( CertificateEntity cert : certs ) {
      if ( !cert.isRevoked( ) ) {
        num++;
      }
    }
    return num;
  }
  
  @Override
  public void deleteUser( String userName, boolean forceDeleteAdmin, boolean recursive ) throws AuthException {
    String accountName = this.delegate.getName( );
    if ( userName == null ) {
      throw new AuthException( AuthException.EMPTY_USER_NAME );
    }
    if ( !forceDeleteAdmin && DatabaseAuthUtils.isAccountAdmin( userName ) ) {
      throw new AuthException( AuthException.DELETE_ACCOUNT_ADMIN );
    }
    if ( !recursive && userHasResourceAttached( userName, accountName ) ) {
      throw new AuthException( AuthException.USER_DELETE_CONFLICT );
    }
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    try {
      UserEntity user = DatabaseAuthUtils.getUniqueUser( db, userName, accountName );
      for ( GroupEntity ge : user.getGroups( ) ) {
        if ( ge.isUserGroup( ) ) {
          db.recast( GroupEntity.class ).delete( ge );
        } else {
          ge.getUsers( ).remove( user );
        }
      }
      db.delete( user );
      db.commit( );
    } catch ( Exception e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to delete user: " + userName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_USER, e );
    }
  }
  
  @Override
  public Group addGroup( String groupName, String path ) throws AuthException {
    try {
      USER_GROUP_NAME_CHECKER.check( groupName );
    } catch ( InvalidValueException e ) {
      Debugging.logError( LOG, e, "Invalid group name " + groupName );
      throw new AuthException( AuthException.INVALID_NAME, e );
    }
    try {
      PATH_CHECKER.check( path );
    } catch ( InvalidValueException e ) {
      Debugging.logError( LOG, e, "Invalid path " + path );
      throw new AuthException( AuthException.INVALID_PATH, e );
    }
    if ( DatabaseAuthUtils.checkGroupExists( groupName, this.delegate.getName( ) ) ) {
      throw new AuthException( AuthException.GROUP_ALREADY_EXISTS );
    }
    EntityWrapper<AccountEntity> db = EntityWrapper.get( AccountEntity.class );
    try {
      AccountEntity account = DatabaseAuthUtils.getUnique( db, AccountEntity.class, "name", this.delegate.getName( ) );
      GroupEntity group = new GroupEntity( groupName );
      group.setPath( path );
      group.setUserGroup( false );
      group.setAccount( account );
      db.recast( GroupEntity.class ).add( group );
      db.commit( );
      return new DatabaseGroupProxy( group );
    } catch ( Exception e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to add group " + groupName + " in " + this.delegate.getName( ) );
      throw new AuthException( AuthException.GROUP_CREATE_FAILURE, e );
    }
  }
  
  private boolean groupHasResourceAttached( String groupName, String accountName ) throws AuthException {
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    try {
      GroupEntity group = DatabaseAuthUtils.getUniqueGroup( db, groupName, accountName );
      boolean hasResAttached = group.getUsers( ).size( ) > 0 || group.getPolicies( ).size( ) > 0;
      db.commit( );
      return hasResAttached;
    } catch ( Exception e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to check group " + groupName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_GROUP, e );
    }
  }
  
  @Override
  public void deleteGroup( String groupName, boolean recursive ) throws AuthException {
    String accountName = this.delegate.getName( );
    if ( groupName == null ) {
      throw new AuthException( AuthException.EMPTY_GROUP_NAME );
    }
    if ( DatabaseAuthUtils.isUserGroupName( groupName ) ) {
      throw new AuthException( AuthException.USER_GROUP_DELETE );
    }
    if ( !recursive && groupHasResourceAttached( groupName, accountName ) ) {
      throw new AuthException( AuthException.GROUP_DELETE_CONFLICT );
    }
    
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    try {
      GroupEntity group = DatabaseAuthUtils.getUniqueGroup( db, groupName, accountName );
      db.delete( group );
      db.commit( );
    } catch ( Exception e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to delete group " + groupName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_GROUP, e );
    }
  }

  @Override
  public Group lookupGroupByName( String groupName ) throws AuthException {
    String accountName = this.delegate.getName( );
    if ( groupName == null ) {
      throw new AuthException( AuthException.EMPTY_GROUP_NAME );
    }
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    try {
      GroupEntity group = DatabaseAuthUtils.getUniqueGroup( db, groupName, accountName );
      db.commit( );
      return new DatabaseGroupProxy( group );
    } catch ( Exception e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to get group " + groupName + " for " + accountName );
      throw new AuthException( AuthException.NO_SUCH_GROUP, e );
    }
  }
  
  @Override
  public User lookupUserByName( String userName ) throws AuthException {
    String accountName = this.delegate.getName( );
    if ( userName == null ) {
      throw new AuthException( AuthException.EMPTY_USER_NAME );
    }
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    try {
      UserEntity user = DatabaseAuthUtils.getUniqueUser( db, userName, accountName );
      db.commit( );
      return new DatabaseUserProxy( user );
    } catch ( Exception e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to find user: " + userName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_USER, e );
    }
  }
  
  @Override
  public List<Authorization> lookupAccountGlobalAuthorizations( String resourceType ) throws AuthException {
    String accountId = this.delegate.getAccountNumber( );
    if ( resourceType == null ) {
      throw new AuthException( "Empty resource type" );
    }
    EntityWrapper<AuthorizationEntity> db = EntityWrapper.get( AuthorizationEntity.class );
    try {
      @SuppressWarnings( "unchecked" )
      List<AuthorizationEntity> authorizations = ( List<AuthorizationEntity> ) db
          .createCriteria( AuthorizationEntity.class ).setCacheable( true ).add(
              Restrictions.and(
                  Restrictions.eq( "type", resourceType ),
                  Restrictions.or( 
                      Restrictions.eq( "effect", EffectType.Allow ),
                      Restrictions.eq( "effect", EffectType.Deny ) ) ) )
          .createCriteria( "statement" ).setCacheable( true )
          .createCriteria( "policy" ).setCacheable( true )
          .createCriteria( "group" ).setCacheable( true ).add( Restrictions.eq( "name", DatabaseAuthUtils.getUserGroupName( User.ACCOUNT_ADMIN ) ) )
          .createCriteria( "account" ).setCacheable( true ).add( Restrictions.eq( "accountNumber", accountId ) )
          .list( );
      db.commit( );
      List<Authorization> results = Lists.newArrayList( );
      for ( AuthorizationEntity auth : authorizations ) {
        results.add( new DatabaseAuthorizationProxy( auth ) );
      }
      return results;
    } catch ( Exception e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to lookup global authorization for account " + accountId + ", type=" + resourceType);
      throw new AuthException( "Failed to lookup account global auth", e );
    }
  }
  
  @Override
  public List<Authorization> lookupAccountGlobalQuotas( String resourceType ) throws AuthException {
    String accountId = this.delegate.getAccountNumber( );
    if ( resourceType == null ) {
      throw new AuthException( "Empty resource type" );
    }
    EntityWrapper<AuthorizationEntity> db = EntityWrapper.get( AuthorizationEntity.class );
    try {
      @SuppressWarnings( "unchecked" )
      List<AuthorizationEntity> authorizations = ( List<AuthorizationEntity> ) db
          .createCriteria( AuthorizationEntity.class ).setCacheable( true ).add(
              Restrictions.and(
                  Restrictions.eq( "type", resourceType ),
                  Restrictions.eq( "effect", EffectType.Limit ) ) )
          .createCriteria( "statement" ).setCacheable( true )
          .createCriteria( "policy" ).setCacheable( true )
          .createCriteria( "group" ).setCacheable( true ).add( Restrictions.eq( "name", DatabaseAuthUtils.getUserGroupName( User.ACCOUNT_ADMIN ) ) )
          .createCriteria( "account" ).setCacheable( true ).add( Restrictions.eq( "accountNumber", accountId ) )
          .list( );
      db.commit( );
      List<Authorization> results = Lists.newArrayList( );
      for ( AuthorizationEntity auth : authorizations ) {
        results.add( new DatabaseAuthorizationProxy( auth ) );
      }
      return results;
    } catch ( Exception e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to lookup global quota for account " + accountId + ", type=" + resourceType);
      throw new AuthException( "Failed to lookup account global quota", e );
    }
  }
  
}
