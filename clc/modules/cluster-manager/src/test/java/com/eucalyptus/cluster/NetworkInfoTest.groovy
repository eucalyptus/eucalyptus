package com.eucalyptus.cluster

import org.junit.Test

import javax.xml.bind.JAXBContext

/**
 *
 */
class NetworkInfoTest {

  @Test
  void testXml() {
    NetworkInfo info = new NetworkInfo(
        configuration: new NIConfiguration(
            properties: [
                new NIProperty( name: 'enabledCLCIp', values: ['10.111.5.11']),
                new NIProperty( name: 'instanceDNSDomain', values: ['eucalyptus.internal']),
                new NIProperty( name: 'instanceDNSServers', values: ['10.1.1.254']),
                new NIProperty( name: 'publicIps', values: (1..32).collect{"10.111.200.${it}" as String} )
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
                            new NIProperty( name: 'privateIps', values: (33..128).collect{"1.0.0.${it}" as String} )
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
              ownerId: '856501978207',
              rules: [
                  '-P icmp -t -1:-1  -s 0.0.0.0/0',
                  '-P tcp -p 22-22  -s 0.0.0.0/0',
                  '-P tcp -p 55-55  -s 0.0.0.0/0',
              ]
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
    jc.createMarshaller().marshal( info, System.out )
    //TODO:STEVE: verify output format
  }
}
