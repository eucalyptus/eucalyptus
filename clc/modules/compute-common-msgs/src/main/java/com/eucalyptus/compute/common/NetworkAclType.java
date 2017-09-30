/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.compute.common;

import java.util.Collection;
import com.eucalyptus.util.CompatFunction;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class NetworkAclType extends EucalyptusData implements VpcTagged {

  private String networkAclId;
  private String vpcId;
  private Boolean _default;
  private NetworkAclEntrySetType entrySet;
  private NetworkAclAssociationSetType associationSet;
  private ResourceTagSetType tagSet;

  public NetworkAclType( ) {
  }

  public NetworkAclType( final String networkAclId, final String vpcId, final Boolean _default, final Collection<NetworkAclEntryType> entries, final Collection<NetworkAclAssociationType> associations ) {
    this.networkAclId = networkAclId;
    this.vpcId = vpcId;
    this._default = _default;
    this.entrySet = new NetworkAclEntrySetType( entries );
    this.associationSet = new NetworkAclAssociationSetType( associations );
  }

  public static CompatFunction<NetworkAclType, String> id( ) {
    return new CompatFunction<NetworkAclType, String>( ) {
      @Override
      public String apply( final NetworkAclType networkAclType ) {
        return networkAclType.getNetworkAclId( );
      }
    };
  }

  public String getNetworkAclId( ) {
    return networkAclId;
  }

  public void setNetworkAclId( String networkAclId ) {
    this.networkAclId = networkAclId;
  }

  public String getVpcId( ) {
    return vpcId;
  }

  public void setVpcId( String vpcId ) {
    this.vpcId = vpcId;
  }

  public Boolean get_default( ) {
    return _default;
  }

  public void set_default( Boolean _default ) {
    this._default = _default;
  }

  public NetworkAclEntrySetType getEntrySet( ) {
    return entrySet;
  }

  public void setEntrySet( NetworkAclEntrySetType entrySet ) {
    this.entrySet = entrySet;
  }

  public NetworkAclAssociationSetType getAssociationSet( ) {
    return associationSet;
  }

  public void setAssociationSet( NetworkAclAssociationSetType associationSet ) {
    this.associationSet = associationSet;
  }

  public ResourceTagSetType getTagSet( ) {
    return tagSet;
  }

  public void setTagSet( ResourceTagSetType tagSet ) {
    this.tagSet = tagSet;
  }
}
