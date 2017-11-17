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
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.ArrayList;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class AWSEC2DHCPOptionsProperties implements ResourceProperties {

  @Property
  private String domainName;

  @Property
  private ArrayList<String> domainNameServers = Lists.newArrayList( );

  @Property
  private ArrayList<String> netbiosNameServers = Lists.newArrayList( );

  @Property
  private Integer netbiosNodeType;

  @Property
  private ArrayList<String> ntpServers = Lists.newArrayList( );

  @Property
  private ArrayList<EC2Tag> tags = Lists.newArrayList( );

  public String getDomainName( ) {
    return domainName;
  }

  public void setDomainName( String domainName ) {
    this.domainName = domainName;
  }

  public ArrayList<String> getDomainNameServers( ) {
    return domainNameServers;
  }

  public void setDomainNameServers( ArrayList<String> domainNameServers ) {
    this.domainNameServers = domainNameServers;
  }

  public ArrayList<String> getNetbiosNameServers( ) {
    return netbiosNameServers;
  }

  public void setNetbiosNameServers( ArrayList<String> netbiosNameServers ) {
    this.netbiosNameServers = netbiosNameServers;
  }

  public Integer getNetbiosNodeType( ) {
    return netbiosNodeType;
  }

  public void setNetbiosNodeType( Integer netbiosNodeType ) {
    this.netbiosNodeType = netbiosNodeType;
  }

  public ArrayList<String> getNtpServers( ) {
    return ntpServers;
  }

  public void setNtpServers( ArrayList<String> ntpServers ) {
    this.ntpServers = ntpServers;
  }

  public ArrayList<EC2Tag> getTags( ) {
    return tags;
  }

  public void setTags( ArrayList<EC2Tag> tags ) {
    this.tags = tags;
  }

  @Override
  public String toString( ) {
  return MoreObjects.toStringHelper( this )
        .add( "domainName", domainName )
        .add( "domainNameServers", domainNameServers )
        .add( "netbiosNameServers", netbiosNameServers )
        .add( "netbiosNodeType", netbiosNodeType )
        .add( "ntpServers", ntpServers )
        .add( "tags", tags )
        .toString( );
  }
}
