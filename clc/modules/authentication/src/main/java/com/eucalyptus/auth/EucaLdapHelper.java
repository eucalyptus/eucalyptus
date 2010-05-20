package com.eucalyptus.auth;

import com.eucalyptus.auth.LdapWrappedUser;
import com.eucalyptus.auth.UserEntity;
import com.eucalyptus.auth.UserInfo;
import com.eucalyptus.auth.ldap.EntryExistsException;
import com.eucalyptus.auth.ldap.EntryNotFoundException;
import com.eucalyptus.auth.ldap.LdapAttributes;
import com.eucalyptus.auth.ldap.LdapConfiguration;
import com.eucalyptus.auth.ldap.LdapContextManager;
import com.eucalyptus.auth.ldap.LdapException;
import com.eucalyptus.auth.ldap.LdapFilter;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.google.common.collect.Lists;
import java.util.List;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import org.apache.log4j.Logger;

/**
 * LDAP access layer for eucalyptus.
 * 
 * @author wenye
 */
public class EucaLdapHelper {
  
  public static final String    FILTER_ALL_USERS                          = "(" + LdapConstants.OBJECT_CLASS + "=" + LdapConstants.EUCA_USER + ")";
  public static final String    FILTER_ALL_GROUPS                         = "(" + LdapConstants.OBJECT_CLASS + "=" + LdapConstants.EUCA_GROUP + ")";
  
  private static Logger         LOG                                       = Logger.getLogger( EucaLdapHelper.class );
  
  private static final boolean  DEBUG                                     = true;
  
  /**
   * Don't use the constants directly so that in case the attribute<-->annotation mapping changes, we don't have to change the code here.
   */
  private static final String[] USER_ENTITY_FIELD_ATTRIBUTES_CERTIFICATES = EucaLdapMapper.getFieldAttributeNames( UserEntity.class, "certificates" );
  private static final String   GROUP_ENTITY_FIELD_ATTRIBUTE_NAME         = EucaLdapMapper.getFieldAttributeNames( GroupEntity.class, "name" )[0];
  private static final String   GROUP_ENTITY_FIELD_ATTRIBUTE_TIMESTAMP    = EucaLdapMapper.getFieldAttributeNames( GroupEntity.class, "timestamp" )[0];
  
  private static final String   ATTRIBUTE_DUMMY                           = "0";
  
  public static final String   EUCA_GROUP_ID_SEPARATOR                   = ":";
  
  public static String getSearchGroupFilter( List<String> groupIds ) throws LdapException {
    LdapFilter filter = new LdapFilter( );
    filter.opBegin( LdapFilter.Op.OR );
    for ( String id : groupIds ) {
      int colon = id.indexOf( EUCA_GROUP_ID_SEPARATOR );
      if ( colon < 1 || colon > id.length( ) - 2 ) {
        throw new LdapException( "Invalid eucaGroupId string: " + id );
      }
      String name = id.substring( 0, colon );
      String timestamp = id.substring( colon + 1 );
      filter.opBegin( LdapFilter.Op.AND ).operand( LdapFilter.Type.EQUAL, GROUP_ENTITY_FIELD_ATTRIBUTE_NAME, name )
            .operand( LdapFilter.Type.EQUAL, GROUP_ENTITY_FIELD_ATTRIBUTE_TIMESTAMP, timestamp ).opEnd( );
    }
    filter.opEnd( );
    return filter.toString( );
  }
  
  public static String getEucaGroupIdString( String name, String timestamp ) {
    return name + EUCA_GROUP_ID_SEPARATOR + timestamp;
  }
  
  /*
   * dn: dc=eucalyptus,dc=com
   * objectClass: top
   * objectClass: dcObject
   * objectClass: organization
   * dc: eucalyptus
   * o: Eucalyptus Systems Inc.
   * description: Eucalyptus Systems, Inc. OpenLDAP Directory Infrastructure
   */
  public static boolean createRoot( ) {
    LdapAttributes attrs = new LdapAttributes( );
    attrs.addAttribute( LdapConstants.OBJECT_CLASS, LdapConstants.TOP, LdapConstants.DC_OBJECT, LdapConstants.ORGANIZATION ).addAttribute( LdapConstants.DC,
                                                                                                                                           "eucalyptus" )
         .addAttribute( LdapConstants.O, "Eucalyptus Systems Inc." ).addAttribute( LdapConstants.DESCRIPTION,
                                                                                   "Eucalyptus Systems, Inc. OpenLDAP Directory Infrastructure" );
    try {
      LdapContextManager contextManager = LdapContextManager.getInstance( );
      contextManager.addEntry( LdapConfiguration.ROOT_DN, attrs.getAttributes( ) );
    } catch ( EntryExistsException e ) {
      LOG.debug( "Root already exists", e );
    } catch ( LdapException e ) {
      LOG.debug( "Initializing LDAP tree root failed", e );
      return false;
    }
    return true;
  }
  
  /*
   * dn: ou=people,dc=eucalyptus,dc=com
   * objectClass: top
   * objectClass: organizationalUnit
   * ou: groups
   */
  public static boolean createGroupRoot( ) {
    LdapAttributes attrs = new LdapAttributes( );
    attrs.addAttribute( LdapConstants.OBJECT_CLASS, LdapConstants.TOP, LdapConstants.ORGANIZATIONAL_UNIT ).addAttribute( LdapConstants.OU, "groups" );
    try {
      LdapContextManager contextManager = LdapContextManager.getInstance( );
      contextManager.addEntry( LdapConfiguration.GROUP_BASE_DN, attrs.getAttributes( ) );
    } catch ( EntryExistsException e ) {
      LOG.debug( "Group tree already exists", e );
    } catch ( LdapException e ) {
      LOG.debug( "Initializing group tree failed", e );
      return false;
    }
    return true;
  }
  
  /*
   * dn: ou=people,dc=eucalyptus,dc=com
   * objectClass: top
   * objectClass: organizationalUnit
   * ou: people
   */
  public static boolean createUserRoot( ) {
    LdapAttributes attrs = new LdapAttributes( );
    attrs.addAttribute( LdapConstants.OBJECT_CLASS, LdapConstants.TOP, LdapConstants.ORGANIZATIONAL_UNIT ).addAttribute( LdapConstants.OU, "people" );
    try {
      LdapContextManager contextManager = LdapContextManager.getInstance( );
      contextManager.addEntry( LdapConfiguration.USER_BASE_DN, attrs.getAttributes( ) );
    } catch ( EntryExistsException e ) {
      LOG.debug( "User tree already exists", e );
    } catch ( LdapException e ) {
      LOG.debug( "Initializing user tree failed", e );
      return false;
    }
    return true;
  }
  
  /**
   * Add a new user by giving UserEntity and UserInfo.
   * 
   * @param user
   * @param userInfo
   * @throws EntryExistsException
   * @throws LdapException
   */
  public static void addUser( UserEntity user, UserInfo userInfo ) throws EntryExistsException, LdapException {
    Attributes attrs = EucaLdapMapper.entityToAttribute( user, userInfo );
    enforceSchemaMust( attrs, LdapConstants.EUCA_USER_OBJECT_CLASSES, LdapConstants.EUCA_USER_MUSTS );
    Debugging.logWT( LOG, ( Object ) user, ( Object ) userInfo, ( Object ) attrs );
    LdapContextManager contextManager = LdapContextManager.getInstance( );
    contextManager.addEntry( composeUserDn( user.getName( ) ), attrs );
  }
  
  /**
   * Add a new group by giving GroupEntity.
   * 
   * @param group
   * @throws EntryExistsException
   * @throws LdapException
   */
  public static void addGroup( GroupEntity group ) throws EntryExistsException, LdapException {
    Attributes attrs = EucaLdapMapper.entityToAttribute( group );
    enforceSchemaMust( attrs, LdapConstants.EUCA_GROUP_OBJECT_CLASSES, LdapConstants.EUCA_GROUP_MUSTS );
    Debugging.logWT( LOG, ( Object ) group, ( Object ) attrs );
    LdapContextManager contextManager = LdapContextManager.getInstance( );
    contextManager.addEntry( composeGroupDn( group.getName( ) ), attrs );
  }
  
  private static List<User> translateUserSearchResult( List<Attributes> results ) throws LdapException, EntryNotFoundException {
    List<User> users = Lists.newArrayList( );
    for ( Attributes attrs : results ) {
      List<Object> entities = EucaLdapMapper.attributeToEntity( attrs, UserEntity.class, UserInfo.class );
      int size = entities.size( );
      if ( size > 0 && size < 3 ) {
        UserEntity user = ( UserEntity ) entities.get( 0 );
        UserInfo info = ( size > 1 ) ? ( UserInfo ) entities.get( 1 ) : null;
        LdapWrappedUser wrapped = new LdapWrappedUser( user, info );
        users.add( wrapped );
      } else {
        LOG.debug( "Got invalid mapping result with " + size + " objects for " + attrs.toString( ) );
      }
    }
    if ( users.size( ) < 1 ) {
      throw new EntryNotFoundException( "Final search result is empty" );
    }
    return users;
  }
  
  /**
   * Get list of users by a search filter of LDAP.
   * 
   * @param filter
   * @return
   * @throws LdapException
   * @throws EntryNotFoundException
   */
  public static List<User> getUsers( String filter ) throws LdapException, EntryNotFoundException {
    LdapContextManager contextManager = LdapContextManager.getInstance( );
    List<User> result = translateUserSearchResult( contextManager.search( LdapConfiguration.USER_BASE_DN, filter, null ) );
    Debugging.logWT( LOG, ( Object ) filter, ( Object ) Debugging.getListString( result ) );
    return result;
  }
  
  /**
   * Get list of users by an example of UserEntity and UserInfo, either of them can be null.
   * 
   * @param user
   * @param info
   * @return
   * @throws LdapException
   * @throws EntryNotFoundException
   */
  public static List<User> getUsers( UserEntity user, UserInfo info ) throws EntryNotFoundException, LdapException {
    LdapContextManager contextManager = LdapContextManager.getInstance( );
    Attributes attrs = EucaLdapMapper.entityToAttribute( user, info );
    List<User> result = translateUserSearchResult( contextManager.search( LdapConfiguration.USER_BASE_DN, attrs, null ) );
    Debugging.logWT( LOG, ( Object ) user, ( Object ) info, ( Object ) Debugging.getListString( result ) );
    return result;
  }
  
  private static List<Group> translateGroupSearchResult( List<Attributes> results ) throws LdapException, EntryNotFoundException {
    List<Group> groups = Lists.newArrayList( );
    for ( Attributes attrs : results ) {
      List<Object> entities = EucaLdapMapper.attributeToEntity( attrs, GroupEntity.class );
      if ( entities.size( ) == 1 ) {
        Group group = LdapWrappedGroup.newInstance( ( GroupEntity ) entities.get( 0 ) );
        groups.add( group );
      } else {
        LOG.debug( "Got invalid mapping result with " + entities.size( ) + " objects for " + attrs.toString( ) );
      }
    }
    if ( groups.size( ) < 1 ) {
      throw new EntryNotFoundException( "Final search result is empty" );
    }
    return groups;
  }
  
  /**
   * Get a list of groups by a search filter of LDAP.
   * 
   * @param filter
   * @return
   * @throws LdapException
   * @throws EntryNotFoundException
   */
  public static List<Group> getGroups( String filter ) throws LdapException, EntryNotFoundException {
    LdapContextManager contextManager = LdapContextManager.getInstance( );
    List<Group> result = translateGroupSearchResult( contextManager.search( LdapConfiguration.GROUP_BASE_DN, filter, null ) );
    Debugging.logWT( LOG, ( Object ) filter, ( Object ) Debugging.getListString( result ) );
    return result;
  }
  
  /**
   * Get a list of groups by an example of GroupEntity.
   * 
   * @param search
   * @return
   * @throws LdapException
   * @throws EntryNotFoundException
   */
  public static List<Group> getGroups( GroupEntity search ) throws LdapException, EntryNotFoundException {
    LdapContextManager contextManager = LdapContextManager.getInstance( );
    Attributes attrs = EucaLdapMapper.entityToAttribute( search );
    List<Group> result = translateGroupSearchResult( contextManager.search( LdapConfiguration.GROUP_BASE_DN, attrs, null ) );
    Debugging.logWT( LOG, ( Object ) search, ( Object ) Debugging.getListString( result ) );
    return result;
  }
  
  public static void deleteUser( String userName ) throws LdapException, EntryNotFoundException {
    Debugging.logWT( LOG, ( Object ) userName );
    LdapContextManager contextManager = LdapContextManager.getInstance( );
    contextManager.removeEntry( composeUserDn( userName ) );
  }
  
  public static void deleteGroup( String groupName ) throws LdapException, EntryNotFoundException {
    Debugging.logWT( LOG, ( Object ) groupName );
    LdapContextManager contextManager = LdapContextManager.getInstance( );
    contextManager.removeEntry( composeGroupDn( groupName ) );
  }
  
  public static void updateUser( UserEntity user, UserInfo info ) throws LdapException, EntryNotFoundException {
    String userName = null;
    if ( user != null ) {
      userName = user.getName( );
    } else if ( info != null ) {
      userName = info.getUserName( );
    }
    if ( userName == null ) {
      throw new LdapException( "Can not update user with empty name" );
    }
    Attributes attrs = EucaLdapMapper.entityToAttribute( user, info );
    Debugging.logWT( LOG, ( Object ) user, ( Object ) info, ( Object ) attrs );
    LdapContextManager contextManager = LdapContextManager.getInstance( );
    contextManager.updateEntry( composeUserDn( userName ), attrs );
  }
  
  public static void addUserAttribute( UserEntity user ) throws LdapException, EntryNotFoundException {
    if ( user.getName( ) == null ) {
      throw new LdapException( "Can not update user with empty name" );
    }
    String dn = composeUserDn( user.getName( ) );
    // Make sure we don't add another uid attribute
    user.setName( null );
    Attributes attrs = EucaLdapMapper.entityToAttribute( user );
    Debugging.logWT( LOG, ( Object ) user, ( Object ) attrs );
    LdapContextManager contextManager = LdapContextManager.getInstance( );
    contextManager.addEntryAttribute( dn, attrs );
  }
  
  public static void deleteUserAttribute( UserEntity user ) throws LdapException, EntryNotFoundException {
    String dn = composeUserDn( user.getName( ) );
    // Make sure we don't delete the uid attribute
    user.setName( null );
    Attributes attrs = EucaLdapMapper.entityToAttribute( user );
    Debugging.logWT( LOG, ( Object ) user, ( Object ) attrs );
    LdapContextManager contextManager = LdapContextManager.getInstance( );
    contextManager.deleteEntryAttribute( dn, attrs );
  }
  
  /**
   * Special case for user certificates.
   * 
   * @param user
   * @throws LdapException
   * @throws EntryNotFoundException
   */
  public static void updateUserCertificates( UserEntity user ) throws LdapException, EntryNotFoundException {
    Attributes attrs = EucaLdapMapper.entityToAttribute( user );
    // Make sure if one of the certificate attributes (eucaCertificate and eucaRevokedCertificate) is not
    // there, set it to be null so that LDAP server will delete it
    for ( String name : USER_ENTITY_FIELD_ATTRIBUTES_CERTIFICATES ) {
      if ( attrs.get( name ) == null ) {
        attrs.put( name, null );
      }
    }
    Debugging.logWT( LOG, ( Object ) user, ( Object ) attrs );
    LdapContextManager contextManager = LdapContextManager.getInstance( );
    contextManager.updateEntry( composeUserDn( user.getName( ) ), attrs );
  }
  
  public static void addGroupAttribute( GroupEntity group ) throws LdapException, EntryNotFoundException {
    String dn = composeGroupDn( group.getName( ) );
    // Make sure we don't add another cn attribute
    group.setName( null );
    Attributes attrs = EucaLdapMapper.entityToAttribute( group );
    Debugging.logWT( LOG, ( Object ) group, ( Object ) attrs );
    LdapContextManager contextManager = LdapContextManager.getInstance( );
    contextManager.addEntryAttribute( dn, attrs );
  }
  
  public static void deleteGroupAttribute( GroupEntity group ) throws LdapException, EntryNotFoundException {
    String dn = composeGroupDn( group.getName( ) );
    // Make sure we don't delete the cn attribute
    group.setName( null );
    Attributes attrs = EucaLdapMapper.entityToAttribute( group );
    Debugging.logWT( LOG, ( Object ) group, ( Object ) attrs );
    LdapContextManager contextManager = LdapContextManager.getInstance( );
    contextManager.deleteEntryAttribute( dn, attrs );
  }
  
  /**
   * Make sure the attributes contain MUST attributes.
   * 
   * @param attrs
   * @param musts
   */
  private static void enforceSchemaMust( Attributes attrs, String[] objectClasses, String[] musts ) {
    // Add objectClass attributes
    Attribute attr = new BasicAttribute( LdapConstants.OBJECT_CLASS );
    for ( String oc : objectClasses ) {
      attr.add( oc );
    }
    attrs.put( attr );
    // Add dummy must have attributes
    for ( String must : musts ) {
      if ( attrs.get( must ) == null ) {
        attrs.put( must, ATTRIBUTE_DUMMY );
      }
    }
  }
  
  private static String composeUserDn( String userName ) {
    return LdapConstants.UID + "=" + userName + "," + LdapConfiguration.USER_BASE_DN;
  }
  
  private static String composeGroupDn( String groupName ) {
    return LdapConstants.CN + "=" + groupName + "," + LdapConfiguration.GROUP_BASE_DN;
  }
}