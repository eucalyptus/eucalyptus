/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.cluster

import org.junit.Test
import static org.junit.Assert.*

import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller

class NetworkInfoTest {

  @Test
  void testXml() {
    NetworkInfo info = new NetworkInfo(
        configuration: new NIConfiguration(
            properties: [
                new NIProperty( name: 'enabledCLCIp', values: ['10.111.5.11']),
                new NIProperty( name: 'instanceDNSDomain', values: ['eucalyptus.internal']),
                new NIProperty( name: 'instanceDNSServers', values: ['10.1.1.254']),
                new NIProperty( name: 'publicIps', values: (1..10).collect{"10.111.200.${it}" as String} )
            ],
            subnets: new NISubnets(
                name: "subnets",
                subnets: [
                    new NISubnet(
                        name: "1.0.0.0",
                        properties: [
                            new NIProperty( name: 'subnet', values: ['1.0.0.0']),
                            new NIProperty( name: 'netmask', values: ['255.255.0.0']),
                            new NIProperty( name: 'gateway', values: ['1.0.0.1'])
                        ]
                    ),
                    new NISubnet(
                        name: "2.0.0.0",
                        properties: [
                            new NIProperty( name: 'subnet', values: ['2.0.0.0']),
                            new NIProperty( name: 'netmask', values: ['255.255.0.0']),
                            new NIProperty( name: 'gateway', values: ['2.0.0.1'])
                        ]
                    )
                ]
            ),
            clusters: new NIClusters(
                name: 'clusters',
                clusters: [
                    new NICluster(
                        name: 'edgecluster0',
                        properties: [
                            new NIProperty( name: 'enabledCCIp', values: ['10.111.5.16']),
                            new NIProperty( name: 'macPrefix', values: ['d0:0d']),
                            new NIProperty( name: 'privateIps', values: (11..20).collect{"1.0.0.${it}" as String} )
                        ],
                        nodes: new NINodes(
                            name: 'nodes',
                            nodes: [
                                new NINode(
                                    name: '10.111.5.70',
                                    instanceIds: [ 'i-AB6B409C' ]
                                )
                            ]
                        ),
                        subnet: new NISubnet(
                            name: "1.0.0.0",
                            properties: [
                                new NIProperty( name: 'subnet', values: ['1.0.0.0']),
                                new NIProperty( name: 'netmask', values: ['255.255.0.0']),
                                new NIProperty( name: 'gateway', values: ['1.0.0.1'])
                            ]
                        )
                    )
                ]
            )
        ),
        securityGroups: [
          new NISecurityGroup(
              name: 'c4d8e62e-ea5d-49fb-b1e9-b908e9becf8',
              ownerId: '856501978207'
          )
        ],
        instances: [
          new NIInstance(
              name: 'i-AB6B409C',
              ownerId: '856501978207',
              macAddress: 'd0:0d:01:00:00:21',
              publicIp: '10.111.200.1',
              privateIp: '1.0.0.33',
              securityGroups: [
                  'c4d8e62e-ea5d-49fb-b1e9-b908e9becf8'
              ]
          )
        ]

    )

    JAXBContext jc = JAXBContext.newInstance( "com.eucalyptus.cluster" );
    Marshaller marshaller = jc.createMarshaller( )
    marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true )
    StringWriter writer = new StringWriter();
    marshaller.marshal( info, writer )
    String outputXml = writer.toString( ).trim()
    println( outputXml )

    String expectedXml =  """
      <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
      <network-data>
          <configuration>
              <property name="enabledCLCIp">
                  <value>10.111.5.11</value>
              </property>
              <property name="instanceDNSDomain">
                  <value>eucalyptus.internal</value>
              </property>
              <property name="instanceDNSServers">
                  <value>10.1.1.254</value>
              </property>
              <property name="publicIps">
                  <value>10.111.200.1</value>
                  <value>10.111.200.2</value>
                  <value>10.111.200.3</value>
                  <value>10.111.200.4</value>
                  <value>10.111.200.5</value>
                  <value>10.111.200.6</value>
                  <value>10.111.200.7</value>
                  <value>10.111.200.8</value>
                  <value>10.111.200.9</value>
                  <value>10.111.200.10</value>
              </property>
              <property name="subnets">
                  <subnet name="1.0.0.0">
                      <property name="subnet">
                          <value>1.0.0.0</value>
                      </property>
                      <property name="netmask">
                          <value>255.255.0.0</value>
                      </property>
                      <property name="gateway">
                          <value>1.0.0.1</value>
                      </property>
                  </subnet>
                  <subnet name="2.0.0.0">
                      <property name="subnet">
                          <value>2.0.0.0</value>
                      </property>
                      <property name="netmask">
                          <value>255.255.0.0</value>
                      </property>
                      <property name="gateway">
                          <value>2.0.0.1</value>
                      </property>
                  </subnet>
              </property>
              <property name="clusters">
                  <cluster name="edgecluster0">
                      <subnet name="1.0.0.0">
                          <property name="subnet">
                              <value>1.0.0.0</value>
                          </property>
                          <property name="netmask">
                              <value>255.255.0.0</value>
                          </property>
                          <property name="gateway">
                              <value>1.0.0.1</value>
                          </property>
                      </subnet>
                      <property name="enabledCCIp">
                          <value>10.111.5.16</value>
                      </property>
                      <property name="macPrefix">
                          <value>d0:0d</value>
                      </property>
                      <property name="privateIps">
                          <value>1.0.0.11</value>
                          <value>1.0.0.12</value>
                          <value>1.0.0.13</value>
                          <value>1.0.0.14</value>
                          <value>1.0.0.15</value>
                          <value>1.0.0.16</value>
                          <value>1.0.0.17</value>
                          <value>1.0.0.18</value>
                          <value>1.0.0.19</value>
                          <value>1.0.0.20</value>
                      </property>
                      <property name="nodes">
                          <node name="10.111.5.70">
                              <instanceIds>
                                  <value>i-AB6B409C</value>
                              </instanceIds>
                          </node>
                      </property>
                  </cluster>
              </property>
          </configuration>
          <vpcs/>
          <instances>
              <instance name="i-AB6B409C">
                  <ownerId>856501978207</ownerId>
                  <macAddress>d0:0d:01:00:00:21</macAddress>
                  <publicIp>10.111.200.1</publicIp>
                  <privateIp>1.0.0.33</privateIp>
                  <networkInterfaces/>
                  <securityGroups>
                      <value>c4d8e62e-ea5d-49fb-b1e9-b908e9becf8</value>
                  </securityGroups>
              </instance>
          </instances>
          <dhcpOptionSets/>
          <internetGateways/>
          <securityGroups>
              <securityGroup name="c4d8e62e-ea5d-49fb-b1e9-b908e9becf8">
                  <ownerId>856501978207</ownerId>
                  <ingressRules/>
                  <egressRules/>
              </securityGroup>
          </securityGroups>
      </network-data>
      """.stripIndent().trim()

      assertEquals( "Message output", expectedXml, outputXml )
  }
}
