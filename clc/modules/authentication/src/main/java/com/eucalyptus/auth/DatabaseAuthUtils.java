package com.eucalyptus.auth;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.auth.entities.AccountEntity;
import com.eucalyptus.auth.entities.GroupEntity;
import com.eucalyptus.auth.entities.PolicyEntity;
import com.eucalyptus.auth.entities.UserEntity;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.EntityWrapper;

public class DatabaseAuthUtils {

  public static boolean isAccountAdmin( String userName ) {
    return User.ACCOUNT_ADMIN.equals( userName );
  }
  
  public static String getUserGroupName( String userName ) {
    return User.USER_GROUP_PREFIX + userName;
  }
  
  public static boolean isUserGroupName( String groupName ) {
    return groupName.startsWith( User.USER_GROUP_PREFIX );
  }
  
  public static boolean isSystemAccount( String accountName ) {
    return Account.SYSTEM_ACCOUNT.equals( accountName );
  }
  
  /**
   * Must call within a transaction.
   * 
   * @param session
   * @param userName
   * @param accountName
   * @return
   */
  public static UserEntity getUniqueUser( EntityWrapper db, String userName, String accountName ) throws Exception {
    Example accountExample = Example.create( new AccountEntity( accountName ) ).enableLike( MatchMode.EXACT );
    Example groupExample = Example.create( new GroupEntity( true ) ).enableLike( MatchMode.EXACT );
    Example userExample = Example.create( new UserEntity( userName ) ).enableLike( MatchMode.EXACT );
    @SuppressWarnings( "unchecked" )
    List<UserEntity> users = ( List<UserEntity> ) db
        .createCriteria( UserEntity.class ).setCacheable( true ).add( userExample )
        .createCriteria( "groups" ).setCacheable( true ).add( groupExample )
        .createCriteria( "account" ).setCacheable( true ).add( accountExample )
        .list( );
    if ( users.size( ) != 1 ) {
      throw new AuthException( "Found " + users.size( ) + " user(s)" );
    }
    return users.get( 0 );
  }
  
  /**
   * Must call within a transaction.
   * 
   * @param session
   * @param groupName
   * @param accountName
   * @return
   * @throws Exception
   */
  public static GroupEntity getUniqueGroup( EntityWrapper db, String groupName, String accountName ) throws Exception {
    Example accountExample = Example.create( new AccountEntity( accountName ) ).enableLike( MatchMode.EXACT );
    Example groupExample = Example.create( new GroupEntity( groupName ) ).enableLike( MatchMode.EXACT );
    @SuppressWarnings( "unchecked" )
    List<GroupEntity> groups = ( List<GroupEntity> ) db
        .createCriteria( GroupEntity.class ).setCacheable( true ).add( groupExample )
        .createCriteria( "account" ).setCacheable( true ).add( accountExample )
        .list( );
    if ( groups.size( ) != 1 ) {
      throw new AuthException( "Found " + groups.size( ) + " group(s)" );
    }
    return groups.get( 0 );
  }
  
  /**
   * Must call within a transaction.
   * 
   * @param session
   * @param accountName
   * @return
   * @throws Exception
   */
  public static AccountEntity getUniqueAccount( EntityWrapper db, String accountName ) throws Exception {
    Example accountExample = Example.create( new AccountEntity( accountName ) ).enableLike( MatchMode.EXACT );
    @SuppressWarnings( "unchecked" )
    List<AccountEntity> accounts = ( List<AccountEntity> ) db
        .createCriteria( AccountEntity.class ).setCacheable( true ).add( accountExample )
        .list( );
    if ( accounts.size( ) != 1 ) {
      throw new AuthException( "Found " + accounts.size( ) + " account(s)" );
    }
    return accounts.get( 0 );
  }
  
  /**
   * Must call within a transacton.
   * 
   * @param session
   * @param accountName
   * @return
   * @throws Exception
   */
  public static PolicyEntity getUniquePolicy( EntityWrapper db, String policyName, String groupId ) throws Exception {
    Example example = Example.create( new PolicyEntity( policyName ) ).enableLike( MatchMode.EXACT );
    @SuppressWarnings( "unchecked" )
    List<PolicyEntity> policies = ( List<PolicyEntity> ) db
        .createCriteria( PolicyEntity.class ).setCacheable( true ).add( example )
        .createCriteria( "group" ).setCacheable( true ).add( Restrictions.eq( "groupId", groupId ) )
        .list( );
    if ( policies.size( ) != 1 ) {
      throw new AuthException( "Found " + policies.size( ) + " policies" );
    }
    return policies.get( 0 );
  }
  
  public static PolicyEntity removeGroupPolicy( GroupEntity group, String name ) throws Exception {
    PolicyEntity policy = null;
    for ( PolicyEntity p : group.getPolicies( ) ) {
      if ( name.equals( p.getName( ) ) ) {
        policy = p;
      }
    }
    if ( policy != null ) {
      group.getPolicies( ).remove( policy );
    }
    return policy;
  }
  
  /**
   * Check if the user name follows the IAM spec.
   * http://docs.amazonwebservices.com/IAM/latest/UserGuide/index.html?Using_Identifiers.html
   * 
   * @param userName
   * @throws AuthException
   */
  public static void checkUserName( String userName ) throws AuthException {
    if ( userName == null || "".equals( userName ) ) {
      throw new AuthException( "Empty user name" );
    }
    for ( int i = 0; i < userName.length( ); i++ ) {
      char c = userName.charAt( i );
      if ( !Character.isLetterOrDigit( c ) 
          && c != '+' && c != '=' && c != ',' && c != '.' && c != '@' && c != '-' ) {
        throw new AuthException( "Invalid character in user name: " + c );
      }
    }
  }
  
  /**
   * Check if the path follows the IAM spec.
   * http://docs.amazonwebservices.com/IAM/latest/UserGuide/index.html?Using_Identifiers.html
   * 
   * @param path
   * @throws AuthException
   */
  public static void checkPath( String path ) throws AuthException {
    if ( path != null && !path.startsWith( "/" ) ) {
      throw new AuthException( "Invalid path: " + path );
    }
  }
  
  /**
   * Check if a user exists.
   * 
   * @param userName
   * @param accountName
   * @return
   * @throws AuthException
   */
  public static boolean checkUserExists( String userName, String accountName ) throws AuthException {
    if ( userName == null || accountName == null ) {
      throw new AuthException( "Empty user name or account name" );
    }
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    try {
      Example accountExample = Example.create( new AccountEntity( accountName ) ).enableLike( MatchMode.EXACT );
      Example groupExample = Example.create( new GroupEntity( true ) ).enableLike( MatchMode.EXACT );
      Example userExample = Example.create( new UserEntity( userName ) ).enableLike( MatchMode.EXACT );
      @SuppressWarnings( "unchecked" )
      List<UserEntity> users = ( List<UserEntity> ) db
          .createCriteria( UserEntity.class ).setCacheable( true ).add( userExample )
          .createCriteria( "groups" ).setCacheable( true ).add( groupExample )
          .createCriteria( "account" ).setCacheable( true ).add( accountExample )
          .list( );
      db.commit( );
      return users.size( ) > 0;
    } catch ( Throwable e ) {
      db.rollback( );
      throw new AuthException( "Failed to find user", e );
    }
  }
  
  /**
   * Check if an account exists.
   * 
   * @param accountName
   * @return
   * @throws AuthException
   */
  public static boolean checkAccountExists( String accountName ) throws AuthException {
    if ( accountName == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    EntityWrapper<AccountEntity> db = EntityWrapper.get( AccountEntity.class );
    try {
      Example accountExample = Example.create( new AccountEntity( accountName ) ).enableLike( MatchMode.EXACT );
      @SuppressWarnings( "unchecked" )
      List<AccountEntity> accounts = ( List<AccountEntity> ) db
          .createCriteria( AccountEntity.class ).setCacheable( true ).add( accountExample )
          .list( );
      db.commit( );
      return accounts.size( ) > 0;
    } catch ( Throwable e ) {
      db.rollback( );
      throw new AuthException( "Failed to find account", e );
    }
  }
  
  /**
   * Check if a group exists.
   * 
   * @param groupName
   * @param accountName
   * @return
   * @throws AuthException
   */
  public static boolean checkGroupExists( String groupName, String accountName ) throws AuthException {
    if ( groupName == null) {
      throw new AuthException( AuthException.EMPTY_GROUP_NAME );
    }  
    if ( accountName == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    try {
      Example accountExample = Example.create( new AccountEntity( accountName ) ).enableLike( MatchMode.EXACT );
      Example groupExample = Example.create( new GroupEntity( groupName ) ).enableLike( MatchMode.EXACT );
      @SuppressWarnings( "unchecked" )
      List<GroupEntity> groups = ( List<GroupEntity> ) db
          .createCriteria( GroupEntity.class ).setCacheable( true ).add( groupExample )
          .createCriteria( "account" ).setCacheable( true ).add( accountExample )
          .list( );
      db.commit( );
      return groups.size( ) > 0;
    } catch ( Throwable e ) {
      db.rollback( );
      throw new AuthException( "Failed to find group", e );
    }
  }
  
  /**
   * Check if the acount is empty (no groups, no users).
   * 
   * @param accountName
   * @return
   * @throws AuthException
   */
  public static boolean isAccountEmpty( String accountName ) throws AuthException {
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    try {
      Example accountExample = Example.create( new AccountEntity( accountName ) ).enableLike( MatchMode.EXACT );
      @SuppressWarnings( "unchecked" )
      List<GroupEntity> groups = ( List<GroupEntity> ) db
          .createCriteria( GroupEntity.class ).setCacheable( true )
          .createCriteria( "account" ).setCacheable( true ).add( accountExample )
          .list( );
      db.commit( );
      return groups.size( ) == 0;
    } catch ( Throwable e ) {
      db.rollback( );
      throw new AuthException( "Failed to check groups for account", e );
    }
  }

}
