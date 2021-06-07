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

public class DhcpOptionsType extends EucalyptusData implements ResourceTagged {

  private String dhcpOptionsId;
  private DhcpConfigurationItemSetType dhcpConfigurationSet;
  private ResourceTagSetType tagSet;

  public DhcpOptionsType( ) {
  }

  public DhcpOptionsType( String dhcpOptionsId, Collection<DhcpConfigurationItemType> configuration ) {
    this.dhcpOptionsId = dhcpOptionsId;
    this.dhcpConfigurationSet = new DhcpConfigurationItemSetType( configuration );
  }

  public static CompatFunction<DhcpOptionsType, String> id( ) {
    return new CompatFunction<DhcpOptionsType, String>( ) {
      @Override
      public String apply( final DhcpOptionsType clusterInfoType ) {
        return clusterInfoType.getDhcpOptionsId( );
      }
    };
  }

  public String getDhcpOptionsId( ) {
    return dhcpOptionsId;
  }

  public void setDhcpOptionsId( String dhcpOptionsId ) {
    this.dhcpOptionsId = dhcpOptionsId;
  }

  public DhcpConfigurationItemSetType getDhcpConfigurationSet( ) {
    return dhcpConfigurationSet;
  }

  public void setDhcpConfigurationSet( DhcpConfigurationItemSetType dhcpConfigurationSet ) {
    this.dhcpConfigurationSet = dhcpConfigurationSet;
  }

  public ResourceTagSetType getTagSet( ) {
    return tagSet;
  }

  public void setTagSet( ResourceTagSetType tagSet ) {
    this.tagSet = tagSet;
  }
}
