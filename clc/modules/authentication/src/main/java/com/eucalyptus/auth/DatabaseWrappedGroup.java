package com.eucalyptus.auth;

import java.security.Principal;
import java.util.Enumeration;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.group.Group;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

public class DatabaseWrappedGroup implements Group {
  private static Logger   LOG = Logger.getLogger( DatabaseWrappedGroup.class );
  private UserGroupEntity group;
  
  public DatabaseWrappedGroup( UserGroupEntity group ) {
    this.group = group;
  }
  
  @Override
  public boolean addMember( Principal user ) {
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>( "eucalyptus_general" );
    try {
      UserInfo userInfo = db.getUnique( new UserInfo( user.getName( ) ) );
      UserGroupEntity g = db.recast( UserGroupEntity.class ).getUnique( this.group );
      if ( !g.belongs( userInfo ) ) {
        g.getUsers( ).add( userInfo );
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
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>( "eucalyptus_general" );
    try {
      boolean ret = this.group.belongs( db.getUnique( new UserInfo( member.getName( ) ) ) );
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
    EntityWrapper<UserGroupEntity> db = new EntityWrapper<UserGroupEntity>( "eucalyptus_general" );
    try {
      UserGroupEntity g = db.getUnique( this.group );
      for ( UserInfo user : g.getUsers( ) ) {
        try {
          userList.add( Users.lookupUser( user.getUserName( ) ) );
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
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>( "eucalyptus_general" );
    try {
      UserInfo userInfo = db.getUnique( new UserInfo( user.getName( ) ) );
      UserGroupEntity g = db.recast( UserGroupEntity.class ).getUnique( this.group );
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
    if ( o instanceof UserGroupEntity ) {
      UserGroupEntity that = ( UserGroupEntity ) o;
      return this.getWrappedGroup( ).equals( that );
    } else if ( o instanceof DatabaseWrappedGroup ) {
      DatabaseWrappedGroup that = ( DatabaseWrappedGroup ) o;
      return this.getWrappedGroup( ).equals( that.getWrappedGroup( ) );
    } else {
      return false;
    }
  }
  
  public UserGroupEntity getWrappedGroup( ) {
    return this.group;
  }
  
}
