package com.eucalyptus.auth;

import java.util.List;
import com.google.common.collect.Lists;

/**
 * Represents the eucaGroupId attribute in LDAP entry for a user. Note that this is not the accurate group membership information for a user. When we delete a
 * group, we just deleted the group entry from group subtree. The user will have the deleted group ID in its entry. But this is fine. Every time we load a
 * user's group membership, we will use this to do a group tree lookup to get the groups. So the obsolete group IDs won't cause any trouble. The only issue is
 * that it leaves some garbage in a user's entry. Given group deletion is rare in reality. This does not seem to be a big problem. If it is, we can always purge
 * a user's group IDs when we lookup user groups. TODO (wenye): purge user's eucaGroupId attribute if necessary.
 * 
 * @author wenye
 */
public class LdapUserGroups {
  @Ldap( names = { LdapConstants.EUCA_GROUP_ID }, converter = EucaLdapMapping.MEMBERSHIP )
  List<String> eucaGroupIds = Lists.newArrayList( );
  
  public LdapUserGroups( ) {}
  
  public void addGroupId( String groupId ) {
    this.eucaGroupIds.add( groupId );
  }
  
  public List<String> getEucaGroupIds( ) {
    return this.eucaGroupIds;
  }
  
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "LdapUserGroups [ " );
    sb.append( "eucaGroupIds = " );
    for ( String id : eucaGroupIds ) {
      sb.append( id ).append( ", ");
    }
    sb.append( "]" );
    return sb.toString( );
  }
}
