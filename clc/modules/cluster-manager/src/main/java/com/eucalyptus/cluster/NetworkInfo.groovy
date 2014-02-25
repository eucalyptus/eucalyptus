/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.cluster

import com.google.common.collect.Lists
import groovy.transform.Canonical
import groovy.transform.CompileStatic

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.bind.annotation.XmlRootElement

/**
 *
 */
@Canonical
@CompileStatic
@XmlRootElement(name = "network-data")
@XmlAccessorType( XmlAccessType.NONE )
class NetworkInfo {
  @XmlElement NIConfiguration configuration = new NIConfiguration()
  @XmlElementWrapper @XmlElement(name="instance") List<NIInstance> instances = Lists.newArrayList()
  @XmlElementWrapper @XmlElement(name="securityGroup") List<NISecurityGroup> securityGroups = Lists.newArrayList()
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NIConfiguration {
  @XmlElement(name="property") List<NIProperty> properties = Lists.newArrayList()
  @XmlElement(name="property") NISubnets subnets
  @XmlElement(name="property") NIClusters clusters
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NIProperty {
  @XmlAttribute String name
  @XmlElement(name="value") List<String> values = Lists.newArrayList()
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NISubnets {
  @XmlAttribute String name
  @XmlElement(name="subnet") List<NISubnet> subnets = Lists.newArrayList()
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NISubnet {
  @XmlAttribute String name
  @XmlElement(name="property") List<NIProperty> properties = Lists.newArrayList()
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NIClusters {
  @XmlAttribute String name
  @XmlElement(name="cluster") List<NICluster> clusters = Lists.newArrayList()
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NICluster {
  @XmlAttribute String name // partition
  @XmlElement NISubnet subnet
  @XmlElement(name="property") List<NIProperty> properties = Lists.newArrayList()
  @XmlElement(name="property") NINodes nodes
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NINodes {
  @XmlAttribute String name
  @XmlElement(name="node") List<NINode> nodes = Lists.newArrayList()
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NINode {
  @XmlAttribute String name
  @XmlElementWrapper(name="instanceIds") @XmlElement(name="value") List<String> instanceIds = Lists.newArrayList()
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NIInstance {
  @XmlAttribute String name
  @XmlElement String ownerId
  @XmlElement String macAddress
  @XmlElement String publicIp
  @XmlElement String privateIp
  @XmlElementWrapper @XmlElement(name="value") List<String> securityGroups = Lists.newArrayList()
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NISecurityGroup {
  @XmlAttribute String name
  @XmlElement String ownerId
  @XmlElementWrapper @XmlElement(name="value") List<String> rules = Lists.newArrayList()
}

