/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 *
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */

package edu.ucsb.eucalyptus.cloud.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table( name = "network_rule_peer_network" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class NetworkPeer {

  @Id
  @GeneratedValue
  @Column( name = "network_rule_peer_network_id" )
  private Long id = -1l;
  @Column( name = "network_rule_peer_network_user_query_key" )
  private String userQueryKey;
  @Column( name = "network_rule_peer_network_user_group" )
  private String groupName;

  public NetworkPeer() {}

  public NetworkPeer( final String userQueryKey, final String groupName )
  {
    this.userQueryKey = userQueryKey;
    this.groupName = groupName;
  }

  public Long getId()
  {
    return id;
  }

  public String getUserQueryKey()
  {
    return userQueryKey;
  }

  public void setUserQueryKey( final String userQueryKey )
  {
    this.userQueryKey = userQueryKey;
  }

  public String getGroupName()
  {
    return groupName;
  }

  public void setGroupName( final String groupName )
  {
    this.groupName = groupName;
  }

  public boolean equals( final Object o )
  {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    NetworkPeer that = ( NetworkPeer ) o;

    if ( !groupName.equals( that.groupName ) ) return false;
    if ( !userQueryKey.equals( that.userQueryKey ) ) return false;

    return true;
  }

  public int hashCode()
  {
    int result;
    result = userQueryKey.hashCode();
    result = 31 * result + groupName.hashCode();
    return result;
  }

  public List<NetworkRule> getAsNetworkRules()
  {
    List<NetworkRule> ruleList = new ArrayList<NetworkRule>();
    ruleList.add( new NetworkRule( "tcp", 0, 65535, new NetworkPeer( this.getUserQueryKey(), this.getGroupName() ) ) );
    ruleList.add( new NetworkRule( "udp", 0, 65535, new NetworkPeer( this.getUserQueryKey(), this.getGroupName() ) ) );
    ruleList.add( new NetworkRule( "icmp", -1, -1, new NetworkPeer( this.getUserQueryKey(), this.getGroupName() ) ) );
    return ruleList;
  }
}
