package com.eucalyptus.auth;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.BaseAuthorization;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.TransactionException;
import com.eucalyptus.util.Transactions;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

public class DatabaseWrappedGroup implements Group {
  private static Logger LOG = Logger.getLogger( DatabaseWrappedGroup.class );
  
  private GroupEntity   searchGroup;
  private GroupEntity   group;
  
  public DatabaseWrappedGroup( GroupEntity group ) {
    this.searchGroup = new GroupEntity( group.getName( ) );
    this.group = group;
  }
  
  @Override
  public boolean addMember( Principal principal ) {
    EntityWrapper<UserEntity> db = Authentication.getEntityWrapper( );
    try {
      UserEntity user = db.getUnique( new UserEntity( principal.getName( ) ) );
      GroupEntity g = db.recast( GroupEntity.class ).getUnique( this.group );
      if ( !g.belongs( user ) ) {
        g.getUsers( ).add( user );
        db.commit( );
        return true;
      } else {
        db.rollback( );
        return false;
      }
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      db.rollback( );
      return false;
    }
  }
  
  @Override
  public boolean isMember( Principal member ) {
    EntityWrapper<UserEntity> db = Authentication.getEntityWrapper( );
    try {
      boolean ret = this.group.belongs( db.getUnique( new UserEntity( member.getName( ) ) ) );
      db.commit( );
      return ret;
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      db.rollback( );
      return false;
    }
  }
  
  @Override
  public Enumeration<? extends Principal> members( ) {
    final List<User> userList = Lists.newArrayList( );
    try {
      Transactions.one( this.searchGroup, new Tx<GroupEntity>( ) {
        @Override
        public void fire( GroupEntity t ) throws Throwable {
          for( UserEntity user : t.getUsers( ) ) {
            try {
              userList.add( Users.lookupUser( user.getName( ) ) );
            } catch ( NoSuchUserException e ) {
              LOG.debug( e, e );
            }
          }
        }
      } );
    } catch ( TransactionException e1 ) {
      LOG.debug( e1, e1 );
    }
    return Iterators.asEnumeration( userList.iterator( ) );
  }
  
  @Override
  public boolean removeMember( Principal user ) {
    EntityWrapper<UserEntity> db = Authentication.getEntityWrapper( );
    try {
      UserEntity userInfo = db.getUnique( new UserEntity( user.getName( ) ) );
      GroupEntity g = db.recast( GroupEntity.class ).getUnique( this.group );
      if ( g.belongs( userInfo ) ) {
        g.getUsers( ).remove( userInfo );
        db.commit( );
        return true;
      } else {
        db.rollback( );
        return false;
      }
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      db.rollback( );
      return false;
    }
  }
  
  @Override
  public String getName( ) {
    return this.group.getName( );
  }
  
  @Override
  public boolean equals( Object o ) {
    if ( this == o ) return true;
    if ( o instanceof GroupEntity ) {
      GroupEntity that = ( GroupEntity ) o;
      return this.getWrappedGroup( ).equals( that );
    } else if ( o instanceof DatabaseWrappedGroup ) {
      DatabaseWrappedGroup that = ( DatabaseWrappedGroup ) o;
      return this.getWrappedGroup( ).equals( that.getWrappedGroup( ) );
    } else {
      return false;
    }
  }
  
  private GroupEntity getWrappedGroup( ) {
    return this.group;
  }
  
  @Override
  public void addAuthorization( final Authorization authorization ) {
    if ( authorization instanceof BaseAuthorization ) {
      BaseAuthorization auth = ( BaseAuthorization ) authorization;
      EntityWrapper<BaseAuthorization> db = EntityWrapper.get( BaseAuthorization.class );
      try {
        db.add( auth );
        GroupEntity g = db.recast( GroupEntity.class ).getUnique( searchGroup );
        g.getAuthorizations( ).add( auth );
        db.recast( GroupEntity.class ).merge( g );
        this.group = g;
        db.commit( );
      } catch ( Throwable e ) {
        LOG.debug( e, e );
        db.rollback( );
      } 
    } else {
      throw new RuntimeException( "Authorizations must extend from BaseAuthorization, passed: " + authorization.getClass( ).getCanonicalName( ) );
    }
  }
  
  @Override
  public List<Authorization> getAuthorizations( ) {
    final List<Authorization> auths = Lists.newArrayList( );
    try {
      Transactions.one( this.searchGroup, new Tx<GroupEntity>( ) {
        @Override
        public void fire( GroupEntity t ) throws Throwable {
          for( BaseAuthorization a : t.getAuthorizations( ) ) {
            auths.add( a );
          }
        }
      } );
    } catch ( TransactionException e ) {
      LOG.debug( e, e );
    }
    return auths;
  }
  
  @Override
  public List<User> getUsers( ) {
    return new ArrayList<User>( this.group.getUsers( ) );
  }
  
  @Override
  public void removeAuthorization( final Authorization auth ) {
    try {
      Transactions.one( this.searchGroup, new Tx<GroupEntity>( ) {
        @Override
        public void fire( GroupEntity t ) throws Throwable {
          t.getAuthorizations( ).remove( auth );
        }
      } );
    } catch ( TransactionException e ) {
      LOG.debug( e, e );
    }
  }
  
}
