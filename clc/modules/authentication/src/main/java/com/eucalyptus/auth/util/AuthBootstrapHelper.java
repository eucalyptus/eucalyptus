package com.eucalyptus.auth.util;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.GroupExistsException;
import com.eucalyptus.auth.Groups;
import com.eucalyptus.auth.NoSuchGroupException;
import com.eucalyptus.auth.NoSuchUserException;
import com.eucalyptus.auth.UserExistsException;
import com.eucalyptus.auth.Users;

public class AuthBootstrapHelper {
  private static Logger LOG = Logger.getLogger( AuthBootstrapHelper.class );

  public static void ensureStandardGroupsExists( ) {
    try {
      Groups.ALL = Groups.lookupGroup( "all" );
    } catch ( NoSuchGroupException e ) {
      try {
        Groups.ALL = Groups.addGroup( "all" );
      } catch ( GroupExistsException e1 ) {
        LOG.error( e1, e1 );
        LOG.error( "Failed to add the 'all' group.  The system may not be able to store group information." );
      }
    }
    Groups.RESTRICTED_GROUPS.add( Groups.ALL );
    
    try {
      Groups.DEFAULT = Groups.lookupGroup( "default" );
    } catch ( NoSuchGroupException e ) {
      try {
        Groups.DEFAULT = Groups.addGroup( "default" );
      } catch ( GroupExistsException e1 ) {
        LOG.error( e1, e1 );
        LOG.error( "Failed to add the 'all' group.  The system may not be able to store group information." );
      }
    }
    Groups.RESTRICTED_GROUPS.add( Groups.DEFAULT );
  }
  
  public static void ensureAdminExists( ) {
    try {
      Users.lookupUser( "admin" );
    } catch ( NoSuchUserException e ) {
      try {
        Users.addUser( "admin", true, true );
      } catch ( UserExistsException e1 ) {} catch ( UnsupportedOperationException e1 ) {}
    }
  }
}
