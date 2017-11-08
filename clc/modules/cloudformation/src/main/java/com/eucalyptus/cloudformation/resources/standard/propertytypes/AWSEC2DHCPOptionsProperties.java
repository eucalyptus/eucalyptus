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
