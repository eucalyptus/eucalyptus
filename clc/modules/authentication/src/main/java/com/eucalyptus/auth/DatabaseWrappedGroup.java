package com.eucalyptus.auth;

import java.security.Principal;
import java.util.Enumeration;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.BaseAuthorization;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.records.EventClass;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.FinalReturn;
import com.eucalyptus.util.TransactionException;
import com.eucalyptus.util.Transactions;
import com.eucalyptus.util.Tx;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

public class DatabaseWrappedGroup implements Group {
  private static Logger LOG = Logger.getLogger( DatabaseWrappedGroup.class );

  public static Group newInstance( Group g ) {
    if( Groups.NAME_ALL.equals( g.getName( ) ) ) {
      return new AllGroup( g );
    } else {
      return new DatabaseWrappedGroup( g );
    }
  }

  private GroupEntity   searchGroup;
  private Group group;
  
  protected DatabaseWrappedGroup( Group group ) {
    this.searchGroup = new GroupEntity( group.getName( ) );
    this.group = group;
  }
  
  @Override
  public boolean addMember( Principal principal ) {
    EntityWrapper<UserEntity> db = Authentication.getEntityWrapper( );
    try {
      UserEntity user = db.getUnique( new UserEntity( principal.getName( ) ) );
      GroupEntity g = db.recast( GroupEntity.class ).getUnique( this.searchGroup );
      if ( !g.isMember( user ) ) {
        g.addMember( user );
        db.commit( );
        EventRecord.here( Groups.class, EventClass.GROUP, EventType.GROUP_MEMBER_ADDED, this.getName( ), user.getName( ) ).info();
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
      boolean ret = this.group.isMember( db.getUnique( new UserEntity( member.getName( ) ) ) );
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
          for( User user : t.getMembers( ) ) {
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
      GroupEntity g = db.recast( GroupEntity.class ).getUnique( this.searchGroup );
      if ( g.isMember( userInfo ) ) {
        g.removeMember( userInfo );
        db.commit( );
        EventRecord.here( Groups.class, EventClass.GROUP, EventType.GROUP_MEMBER_REMOVED, this.getName( ), userInfo.getName( ) ).info();
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
      return this.group.equals( that );
    } else if ( o instanceof DatabaseWrappedGroup ) {
      DatabaseWrappedGroup that = ( DatabaseWrappedGroup ) o;
      return this.group.equals( that.group );
    } else {
      return false;
    }
  }
  
  @Override
  public boolean addAuthorization( final Authorization authorization ) {
    if ( authorization instanceof BaseAuthorization ) {
      BaseAuthorization auth = ( BaseAuthorization ) authorization;
      EntityWrapper<BaseAuthorization> db = EntityWrapper.get( BaseAuthorization.class );
      boolean ret = false;
      try {
        db.add( auth );
        GroupEntity g = db.recast( GroupEntity.class ).getUnique( searchGroup );
        ret = g.addAuthorization( auth );
        db.recast( GroupEntity.class ).merge( g );
        this.group = g;
        db.commit( );
        EventRecord.here( Groups.class, EventClass.GROUP, EventType.GROUP_AUTH_GRANTED, this.getName( ), auth.getDisplayName( ), auth.getValue( ) ).info();
      } catch ( Throwable e ) {
        ret = false;
        LOG.debug( e, e );
        db.rollback( );
      } 
      return ret;
    } else {
      throw new RuntimeException( "Authorizations must extend from BaseAuthorization, passed: " + authorization.getClass( ).getCanonicalName( ) );
    }
  }
  
  @Override
  public ImmutableList<Authorization> getAuthorizations( ) {
    final List<Authorization> auths = Lists.newArrayList( );
    try {
      Transactions.one( this.searchGroup, new Tx<GroupEntity>( ) {
        @Override
        public void fire( GroupEntity t ) throws Throwable {
          for( Authorization a : t.getAuthorizations( ) ) {
            auths.add( a );
          }
        }
      } );
    } catch ( TransactionException e ) {
      LOG.debug( e, e );
    }
    return ImmutableList.copyOf( auths );
  }
  
  @Override
  public ImmutableList<User> getMembers( ) {
    final List<User> users = Lists.newArrayList( );
    try {
      Transactions.one( this.searchGroup, new Tx<GroupEntity>( ) {
        @Override
        public void fire( GroupEntity t ) throws Throwable {
          users.addAll( t.getMembers( ) );
        }
      } );
    } catch ( TransactionException e ) {
      LOG.debug( e, e );
    }
    return ImmutableList.copyOf( users );
  }
  
  @Override
  public boolean removeAuthorization( final Authorization auth ) {
    final FinalReturn<Boolean> ret = FinalReturn.newInstance( );  
    try {
      Transactions.one( this.searchGroup, new Tx<GroupEntity>( ) {
        @Override
        public void fire( GroupEntity t ) throws Throwable {
           ret.set( t.removeAuthorization( auth ) );
           EventRecord.here( Groups.class, EventClass.GROUP, EventType.GROUP_AUTH_REVOKED, t.getName( ), auth.getDisplayName( ), auth.getValue( ) ).info();
        }
      } );
    } catch ( TransactionException e ) {
      LOG.debug( e, e );
    }
    return ret.get( );
  }
  
}
