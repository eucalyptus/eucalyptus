/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE.
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
