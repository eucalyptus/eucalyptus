/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.euare.persist;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Debugging;
import com.eucalyptus.auth.PolicyParseException;
import com.eucalyptus.auth.euare.checker.InvalidValueException;
import com.eucalyptus.auth.euare.checker.ValueChecker;
import com.eucalyptus.auth.euare.checker.ValueCheckerFactory;
import com.eucalyptus.auth.euare.persist.entities.GroupEntity;
import com.eucalyptus.auth.euare.persist.entities.GroupEntity_;
import com.eucalyptus.auth.euare.persist.entities.PolicyEntity;
import com.eucalyptus.auth.euare.persist.entities.UserEntity;
import com.eucalyptus.auth.euare.persist.entities.UserEntity_;
import com.eucalyptus.auth.euare.principal.EuareAccount;
import com.eucalyptus.auth.euare.principal.EuareGroup;
import com.eucalyptus.auth.euare.principal.EuareUser;
import com.eucalyptus.auth.policy.PolicyParser;
import com.eucalyptus.auth.policy.PolicyPolicy;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.entities.Entities;
import java.util.concurrent.ExecutionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.Tx;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;

public class DatabaseGroupProxy implements EuareGroup {
  
  private static final long serialVersionUID = 1L;

  private static final ValueChecker NAME_CHECKER = ValueCheckerFactory.createGroupNameChecker( );
  private static final ValueChecker PATH_CHECKER = ValueCheckerFactory.createPathChecker( );
  private static final ValueChecker POLICY_NAME_CHECKER = ValueCheckerFactory.createPolicyNameChecker( );

  private static Logger LOG = Logger.getLogger( DatabaseGroupProxy.class );
  
  private GroupEntity delegate;
  private transient Supplier<String> accountNumberSupplier =
      DatabaseAuthUtils.getAccountNumberSupplier( this );

  public DatabaseGroupProxy( GroupEntity delegate ) {
    this.delegate = delegate;
  }

  public DatabaseGroupProxy( final GroupEntity delegate,
                             final Supplier<String> accountNumberSupplier ) {
    this.delegate = delegate;
    this.accountNumberSupplier = accountNumberSupplier;
  }

  @Override
  public String toString( ) {
    final StringBuilder sb = new StringBuilder( );
    try {
      DatabaseAuthUtils.invokeUnique( GroupEntity.class, GroupEntity_.groupId, this.delegate.getGroupId( ), new Tx<GroupEntity>( ) {
        public void fire( GroupEntity t ) {
          sb.append( t.toString( ) );
        }
      } );
    } catch ( ExecutionException e ) {
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
      NAME_CHECKER.check( name );
    } catch ( InvalidValueException e ) {
      Debugging.logError( LOG, e, "Invalid group name " + name );
      throw new AuthException( AuthException.INVALID_NAME, e );
    }
    try {
      // try looking up the group with the same name first
      this.getAccount( ).lookupGroupByName( name );
    } catch ( AuthException ae ) {
      // not found
      try {
        DatabaseAuthUtils.invokeUnique( GroupEntity.class, GroupEntity_.groupId, this.delegate.getGroupId( ), new Tx<GroupEntity>( ) {
          public void fire( GroupEntity t ) {
            t.setName( name );
          }
        } );
      } catch ( ExecutionException e ) {
        Debugging.logError( LOG, e, "Failed to setName for " + this.delegate );
        throw new AuthException( e );
      }
      return;
    }
    // found
    throw new AuthException( AuthException.GROUP_ALREADY_EXISTS );
  }

  @Override
  public String getPath( ) {
    return this.delegate.getPath( );
  }

  @Override
  public void setPath( final String path ) throws AuthException {
    try {
      PATH_CHECKER.check( path );
    } catch ( InvalidValueException e ) {
      Debugging.logError( LOG, e, "Invalid path " + path );
      throw new AuthException( AuthException.INVALID_PATH, e );
    }    
    try {
      DatabaseAuthUtils.invokeUnique( GroupEntity.class, GroupEntity_.groupId, this.delegate.getGroupId( ), new Tx<GroupEntity>( ) {
        public void fire( GroupEntity t ) {
          t.setPath( path );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to setPath for " + this.delegate );
      throw new AuthException( e );
    }
  }

  @Override
  public Date getCreateDate() {
    return this.delegate.getCreationTimestamp( );
  }

  @Override
  public Boolean isUserGroup( ) {
    return this.delegate.isUserGroup( );
  }
  
  @Override
  public void setUserGroup( final Boolean userGroup ) throws AuthException {
    try {
      DatabaseAuthUtils.invokeUnique( GroupEntity.class, GroupEntity_.groupId, this.delegate.getGroupId( ), new Tx<GroupEntity>( ) {
        public void fire( GroupEntity t ) {
          t.setUserGroup( userGroup );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to setUserGroup for " + this.delegate );
      throw new AuthException( e );
    }
  }

  @Override
  public void addUserByName( String userName ) throws AuthException {
    try ( final TransactionResource db = Entities.transactionFor( GroupEntity.class ) ) {
      GroupEntity groupEntity = DatabaseAuthUtils.getUnique( GroupEntity.class, GroupEntity_.groupId, this.delegate.getGroupId( ) );
      UserEntity userEntity = DatabaseAuthUtils.getUniqueUser( userName, groupEntity.getAccount( ).getName( ) );
      groupEntity.getUsers( ).add( userEntity );
      userEntity.getGroups( ).add( groupEntity );
      db.commit( );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to add user " + userName + " to group " + this.delegate );
      throw new AuthException( e );
    }
  }

  @Override
  public void removeUserByName( String userName ) throws AuthException {
    try ( final TransactionResource db = Entities.transactionFor( GroupEntity.class ) ) {
      GroupEntity groupEntity = DatabaseAuthUtils.getUnique( GroupEntity.class, GroupEntity_.groupId, this.delegate.getGroupId( ) );
      UserEntity userEntity = DatabaseAuthUtils.getUniqueUser( userName, groupEntity.getAccount( ).getName( ) );
      groupEntity.getUsers( ).remove( userEntity );
      userEntity.getGroups( ).remove( groupEntity );
      db.commit( );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to remove user " + userName + " from group " + this.delegate );
      throw new AuthException( e );
    }
  }
  
  @Override
  public boolean hasUser( String userName ) throws AuthException {
    try ( final TransactionResource db = Entities.transactionFor( UserEntity.class ) ) {
      @SuppressWarnings( "unchecked" )
      List<UserEntity> users = Entities
          .criteriaQuery( UserEntity.class ).whereEqual( UserEntity_.name, userName )
          .join( UserEntity_.groups ).whereEqual( GroupEntity_.groupId, this.delegate.getGroupId( ) )
          .list( );
      db.commit( );
      return users.size( ) > 0;
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to check membership for group " + this.delegate );
      throw new AuthException( e );
    }
  }
  
  @Override
  public List<Policy> getPolicies( ) {
    final List<Policy> results = Lists.newArrayList( );
    try {
      DatabaseAuthUtils.invokeUnique( GroupEntity.class, GroupEntity_.groupId, this.delegate.getGroupId( ), new Tx<GroupEntity>( ) {
        public void fire( GroupEntity t ) {
          for ( PolicyEntity p : t.getPolicies( ) ) {
            results.add( new DatabasePolicyProxy( p ) );
          }
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to getPolicies for " + this.delegate );
    }
    return results;
  }

  @Override
  public Policy addPolicy( String name, String policy ) throws AuthException, PolicyParseException {
    return storePolicy( name, policy, /*allowUpdate*/ false );
  }

  @Override
  public Policy putPolicy( String name, String policy ) throws AuthException, PolicyParseException {
    return storePolicy( name, policy, /*allowUpdate*/ true );
  }

  private Policy storePolicy( String name, String policy, boolean allowUpdate ) throws AuthException, PolicyParseException {
    try {
      POLICY_NAME_CHECKER.check( name );
    } catch ( InvalidValueException e ) {
      Debugging.logError( LOG, e, "Invalid policy name " + name );
      throw new AuthException( AuthException.INVALID_NAME, e );
    }
    if ( DatabaseAuthUtils.policyNameinList( name, this.getPolicies( ) ) && !allowUpdate ) {
      Debugging.logError( LOG, null, "Policy name already used: " + name );
      throw new AuthException( AuthException.INVALID_NAME );
    }
    final PolicyPolicy policyPolicy = PolicyParser.getInstance().parse( policy );
    final PolicyEntity parsedPolicy = PolicyEntity.create( name, policyPolicy.getPolicyVersion( ), policy );
    try ( final TransactionResource db = Entities.transactionFor( GroupEntity.class ) ) {
      final GroupEntity groupEntity = DatabaseAuthUtils.getUnique( GroupEntity.class, GroupEntity_.groupId, this.delegate.getGroupId( ) );
      final PolicyEntity remove = DatabaseAuthUtils.removeGroupPolicy( groupEntity, name );
      if ( remove != null ) {
        Entities.delete( remove );
      }
      Entities.persist( parsedPolicy );
      parsedPolicy.setGroup( groupEntity );
      groupEntity.getPolicies( ).add( parsedPolicy );
      db.commit( );
      return new DatabasePolicyProxy( parsedPolicy );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to attach policy for " + this.delegate.getName( ) );
      throw new AuthException( "Failed to attach policy", e );
    }
  }

  @Override
  public void removePolicy( String name ) throws AuthException {
    if ( name == null ) {
      throw new AuthException( AuthException.EMPTY_POLICY_NAME );
    }
    try ( final TransactionResource db = Entities.transactionFor( GroupEntity.class ) ) {
      GroupEntity group = DatabaseAuthUtils.getUnique( GroupEntity.class, GroupEntity_.groupId, this.delegate.getGroupId( ) );
      PolicyEntity policy = DatabaseAuthUtils.removeGroupPolicy( group, name );
      if ( policy != null ) {
        Entities.delete( policy );
      }
      db.commit( );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to remove policy " + name + " in " + this.delegate );
      throw new AuthException( "Failed to remove policy", e );
    }
  }
  
  @Override
  public List<EuareUser> getUsers( ) {
    final List<EuareUser> results = Lists.newArrayList( );
    try {
      DatabaseAuthUtils.invokeUnique( GroupEntity.class, GroupEntity_.groupId, this.delegate.getGroupId( ), new Tx<GroupEntity>( ) {
        public void fire( GroupEntity t ) {
          for ( UserEntity u : t.getUsers( ) ) {
            results.add( new DatabaseUserProxy( u ) );
          }
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to getUsers for " + this.delegate );
    }
    return results;
  }

  @Override
  public String getAccountNumber( ) throws AuthException {
    return DatabaseAuthUtils.extract( accountNumberSupplier );
  }

  @Override
  public EuareAccount getAccount( ) {
    final List<DatabaseAccountProxy> results = Lists.newArrayList( );
    try {
      DatabaseAuthUtils.invokeUnique( GroupEntity.class, GroupEntity_.groupId, this.delegate.getGroupId( ), new Tx<GroupEntity>( ) {
        public void fire( GroupEntity t ) {
          Entities.initialize( t.getAccount( ) );
          results.add( new DatabaseAccountProxy( t.getAccount( ) ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to getAccount for " + this.delegate );
    }
    return results.get( 0 );
  }

  @Override
  public String getGroupId( ) {
    return this.delegate.getGroupId( );
  }

  private void readObject( ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject( );
    this.accountNumberSupplier = DatabaseAuthUtils.getAccountNumberSupplier( this );
  }
}
