package com.eucalyptus.auth;

import java.security.Principal;
import java.util.Enumeration;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

public class DatabaseWrappedGroup implements Group {
  private static Logger   LOG = Logger.getLogger( DatabaseWrappedGroup.class );
  private GroupEntity group;
  
  public DatabaseWrappedGroup( GroupEntity group ) {
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
    List<User> userList = Lists.newArrayList( );
    EntityWrapper<GroupEntity> db = Authentication.getEntityWrapper( );
    try {
      GroupEntity g = db.getUnique( this.group );
      for ( UserEntity user : g.getUsers( ) ) {
        try {
          userList.add( Users.lookupUser( user.getName( ) ) );
        } catch ( NoSuchUserException e ) {
          LOG.debug( e, e );
        }
      }
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      db.rollback( );
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
  
  public GroupEntity getWrappedGroup( ) {
    return this.group;
  }
  
}
