package com.eucalyptus.auth;

import java.util.List;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.auth.entities.AccountEntity;
import com.eucalyptus.auth.entities.AuthorizationEntity;
import com.eucalyptus.auth.entities.ConditionEntity;
import com.eucalyptus.auth.entities.GroupEntity;
import com.eucalyptus.auth.entities.PolicyEntity;
import com.eucalyptus.auth.entities.StatementEntity;
import com.eucalyptus.auth.entities.UserEntity;
import com.eucalyptus.auth.policy.PolicyParser;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.TransactionException;
import com.eucalyptus.util.Transactions;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Lists;

public class DatabaseGroupProxy implements Group {
  
  private static final long serialVersionUID = 1L;

  private static Logger LOG = Logger.getLogger( DatabaseGroupProxy.class );
  
  private GroupEntity delegate;
  
  public DatabaseGroupProxy( GroupEntity delegate ) {
    this.delegate = delegate;
  }
  
  @Override
  public String toString( ) {
    final StringBuilder sb = new StringBuilder( );
    try {
      Transactions.one( GroupEntity.newInstanceWithGroupId( this.delegate.getGroupId() ), new Tx<GroupEntity>( ) {
        public void fire( GroupEntity t ) throws Throwable {
          sb.append( t.toString( ) );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to toString for " + this.delegate );
    }
    return sb.toString( );
  }

  @Override
  public String getName( ) {
    return this.delegate.getName( );
  }

  @Override
  public void setName( final String name ) throws AuthException {
    try {
      Transactions.one( GroupEntity.newInstanceWithGroupId( this.delegate.getGroupId() ), new Tx<GroupEntity>( ) {
        public void fire( GroupEntity t ) throws Throwable {
          t.setName( name );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to setName for " + this.delegate );
      throw new AuthException( e );
    }
  }

  @Override
  public String getPath( ) {
    return this.delegate.getPath( );
  }

  @Override
  public void setPath( final String path ) throws AuthException {
    try {
      Transactions.one( GroupEntity.newInstanceWithGroupId( this.delegate.getGroupId() ), new Tx<GroupEntity>( ) {
        public void fire( GroupEntity t ) throws Throwable {
          t.setPath( path );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to setPath for " + this.delegate );
      throw new AuthException( e );
    }
  }
  
  @Override
  public Boolean isUserGroup( ) {
    return this.delegate.isUserGroup( );
  }
  
  @Override
  public void setUserGroup( final Boolean userGroup ) throws AuthException {
    try {
      Transactions.one( GroupEntity.newInstanceWithGroupId( this.delegate.getGroupId() ), new Tx<GroupEntity>( ) {
        public void fire( GroupEntity t ) throws Throwable {
          t.setUserGroup( userGroup );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to setUserGroup for " + this.delegate );
      throw new AuthException( e );
    }
  }

  @Override
  public void addUserByName( String userName ) throws AuthException {
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    try {
      GroupEntity groupEntity = db.getUnique( GroupEntity.newInstanceWithGroupId( this.delegate.getGroupId( ) ) );
      UserEntity userEntity = DatabaseAuthUtils.getUniqueUser( db, userName, groupEntity.getAccount( ).getName( ) );
      groupEntity.getUsers( ).add( userEntity );
      //userEntity.addGroup( groupEntity );
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to add user " + userName + " to group " + this.delegate );
      throw new AuthException( e );
    }
  }

  @Override
  public void removeUserByName( String userName ) throws AuthException {
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    try {
      GroupEntity groupEntity = db.getUnique( GroupEntity.newInstanceWithGroupId( this.delegate.getGroupId( ) ) );
      UserEntity userEntity = DatabaseAuthUtils.getUniqueUser( db, userName, groupEntity.getAccount( ).getName( ) );
      groupEntity.getUsers( ).remove( userEntity );
      //userEntity.getGroups( ).remove( groupEntity );
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to remove user " + userName + " from group " + this.delegate );
      throw new AuthException( e );
    }
  }
  
  @Override
  public boolean hasUser( String userName ) throws AuthException {
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    try {
      Example userExample = Example.create( new UserEntity( userName ) ).enableLike( MatchMode.EXACT );
      @SuppressWarnings( "unchecked" )
      List<UserEntity> users = ( List<UserEntity> ) db
          .createCriteria( UserEntity.class ).setCacheable( true ).add( userExample )
          .createCriteria( "groups" ).setCacheable( true ).add( Restrictions.idEq( this.delegate.getGroupId( ) ) )
          .list( );
      db.commit( );
      return users.size( ) > 0;
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to check membership for group " + this.delegate );
      throw new AuthException( e );
    }
  }
  
  @Override
  public List<Policy> getPolicies( ) {
    final List<Policy> results = Lists.newArrayList( );
    try {
      Transactions.one( GroupEntity.newInstanceWithGroupId( this.delegate.getGroupId() ), new Tx<GroupEntity>( ) {
        public void fire( GroupEntity t ) throws Throwable {
          for ( PolicyEntity p : t.getPolicies( ) ) {
            results.add( new DatabasePolicyProxy( p ) );
          }
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to getUsers for " + this.delegate );
    }
    return results;
  }

  @Override
  public Policy addPolicy( String name, String policy ) throws AuthException, PolicyParseException {
    PolicyEntity parsedPolicy = PolicyParser.getInstance( ).parse( policy );
    parsedPolicy.setName( name );
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    try {
      GroupEntity groupEntity = db.getUnique( GroupEntity.newInstanceWithGroupId( this.delegate.getGroupId( ) ) );
      db.recast( PolicyEntity.class ).add( parsedPolicy );
      parsedPolicy.setGroup( groupEntity );
      for ( StatementEntity statement : parsedPolicy.getStatements( ) ) {
        db.recast( StatementEntity.class ).add( statement );
        statement.setPolicy( parsedPolicy );
        for ( AuthorizationEntity auth : statement.getAuthorizations( ) ) {
          db.recast( AuthorizationEntity.class ).add( auth );
          auth.setStatement( statement );
        }
        for ( ConditionEntity cond : statement.getConditions( ) ) {
          db.recast( ConditionEntity.class ).add( cond );
          cond.setStatement( statement );
        }
      }
      db.commit( );
      return new DatabasePolicyProxy( parsedPolicy );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to attach policy for " + this.delegate.getName( ) );
      throw new AuthException( "Failed to attach policy", e );
    }
  }

  @Override
  public void removePolicy( String name ) throws AuthException {
    if ( name == null ) {
      throw new AuthException( "Empty policy name" );
    }
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    try {
      GroupEntity group = db.getUnique( GroupEntity.newInstanceWithGroupId( this.delegate.getGroupId() ) );
      PolicyEntity policy = DatabaseAuthUtils.removeGroupPolicy( group, name );
      if ( policy != null ) {
        db.recast( PolicyEntity.class ).delete( policy );
      }
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to remove policy " + name + " in " + this.delegate );
      throw new AuthException( "Failed to remove policy", e );
    }
  }
  
  @Override
  public List<User> getUsers( ) {
    final List<User> results = Lists.newArrayList( );
    try {
      Transactions.one( GroupEntity.newInstanceWithGroupId( this.delegate.getGroupId() ), new Tx<GroupEntity>( ) {
        public void fire( GroupEntity t ) throws Throwable {
          for ( UserEntity u : t.getUsers( ) ) {
            results.add( new DatabaseUserProxy( u ) );
          }
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to getUsers for " + this.delegate );
    }
    return results;
  }

  @Override
  public Account getAccount( ) {
    final List<DatabaseAccountProxy> results = Lists.newArrayList( );
    try {
      Transactions.one( GroupEntity.newInstanceWithGroupId( this.delegate.getGroupId( ) ), new Tx<GroupEntity>( ) {
        public void fire( GroupEntity t ) throws Throwable {
          results.add( new DatabaseAccountProxy( ( AccountEntity) t.getAccount( ) ) );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to getAccount for " + this.delegate );
    }
    return results.get( 0 );
  }

  @Override
  public String getGroupId( ) {
    return this.delegate.getGroupId( );
  }
  
}
