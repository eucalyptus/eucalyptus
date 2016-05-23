/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.auth.euare.identity.region

import com.eucalyptus.util.Exceptions
import groovy.transform.CompileStatic
import org.junit.Test

import static org.junit.Assert.assertEquals

/**
 *
 */
@CompileStatic
class RegionConfigurationTest {

  @Test
  void testFullParse() {
    String config = """
    {
        "Regions": [
            {
                "Name": "region-1",
                "CertificateFingerprint": "EC:E7:3D:DF:97:43:00:9E:FC:F0:2C:6D:98:D2:82:EB:AA:04:75:10:E7:C2:F2:6F:31:F1:F1:CA:A1:61:DE:41",
                "IdentifierPartitions": [
                  1
                ],
                "Services": [
                    {
                      "Type": "identity",
                      "Endpoints": [
                        "https://identity.example.com:8773/services/Identity"
                      ]
                    }
                ],
                "RemoteCidrs": [
                    "1.0.0.0/32"
                ],
                "ForwardedForCidrs": [
                    "1.0.0.0/32"
                ]
            }
        ],
        "RemoteCidrs": [
            "1.0.0.0/32"
        ],
        "ForwardedForCidrs": [
            "1.0.0.0/32"
        ]
    }
    """.stripIndent()

    RegionConfiguration result = RegionConfigurations.parse( config )
    println result

    RegionConfiguration expected = new RegionConfiguration(
        regions: [
            new Region(
                name: 'region-1',
                certificateFingerprint: 'EC:E7:3D:DF:97:43:00:9E:FC:F0:2C:6D:98:D2:82:EB:AA:04:75:10:E7:C2:F2:6F:31:F1:F1:CA:A1:61:DE:41',
                identifierPartitions: [ 1 ],
                services: [
                    new Service(
                        type: 'identity',
                        endpoints: [ 'https://identity.example.com:8773/services/Identity' ]
                    )
                ],
                remoteCidrs: [ "1.0.0.0/32" ],
                forwardedForCidrs: [ "1.0.0.0/32" ]
            )
        ],
        remoteCidrs: [ "1.0.0.0/32" ],
        forwardedForCidrs: [ "1.0.0.0/32" ]
    )

    assertEquals( 'Result does not match template', expected, result)
  }

  @Test( expected = RegionConfigurationException )
  void testInvalidRegion( ) {
    String config =  """
    {
        "Regions": [
            {
                "Name": "%1s",
                "CertificateFingerprint": "EC:E7:3D:DF:97:43:00:9E:FC:F0:2C:6D:98:D2:82:EB:AA:04:75:10:E7:C2:F2:6F:31:F1:F1:CA:A1:61:DE:41",
                "IdentifierPartitions": [
                  1
                ],
                "Services": [
                    {
                      "Type": "identity",
                      "Endpoints": [
                        "https://identity.example.com:8773/services/Identity"
                      ]
                    }
                ]
            }
        ]
    }
    """

    try {
      RegionConfigurations.parse( String.format( config, 'region-1' ) )  // ensure pass with valid region name
    } catch ( Exception e ) {
      throw Exceptions.toUndeclared( e );
    }

    RegionConfigurations.parse( String.format( config, 'region 1' ) )  // should fail due to space
  }

  @Test( expected = RegionConfigurationException )
  void testInvalidServiceUrl( ) {
    String config =  """
    {
        "Regions": [
            {
                "Name": "region-1",
                "CertificateFingerprint": "EC:E7:3D:DF:97:43:00:9E:FC:F0:2C:6D:98:D2:82:EB:AA:04:75:10:E7:C2:F2:6F:31:F1:F1:CA:A1:61:DE:41",
                "IdentifierPartitions": [
                  1
                ],
                "Services": [
                    {
                      "Type": "identity",
                      "Endpoints": [
                        "%1s"
                      ]
                    }
                ]
            }
        ]
    }
    """

    try {
      RegionConfigurations.parse( String.format( config, 'https://identity.example.com:8773/services/Identity' ) )  // ensure pass with valid service endpoint
    } catch ( Exception e ) {
      throw Exceptions.toUndeclared( e );
    }

    RegionConfigurations.parse( String.format( config, 'ftp://identity.example.com:8773/services/Identity' ) )  // should fail due to invalid scheme
  }

  @Test( expected = RegionConfigurationException )
  void testInvalidRemoteCidr( ) {
    String config =  """
    {
        "Regions": [
            {
                "Name": "region-1",
                "CertificateFingerprint": "EC:E7:3D:DF:97:43:00:9E:FC:F0:2C:6D:98:D2:82:EB:AA:04:75:10:E7:C2:F2:6F:31:F1:F1:CA:A1:61:DE:41",
                "IdentifierPartitions": [
                  1
                ],
                "Services": [
                    {
                      "Type": "identity",
                      "Endpoints": [
                        "http://foo/identity"
                      ]
                    }
                ],
                "RemoteCidrs": [ "%1s" ]
            }
        ]
    }
    """

    try {
      RegionConfigurations.parse( String.format( config, '0.0.0.0/0' ) )  // ensure pass with valid remote cidr
    } catch ( Exception e ) {
      throw Exceptions.toUndeclared( e );
    }

    RegionConfigurations.parse( String.format( config, '0.0.0.0/a' ) )
  }

  @Test( expected = RegionConfigurationException )
  void testEmptyMultiElementRegions( ) {
    String config =  """
    {
          "Regions": [
            {
            },
            {
            }
        ]
    }
    """

    RegionConfigurations.parse( config )
  }

  @Test ( expected = RegionConfigurationException )
  void testEmptyElementRegions( ) {
    String config =  """
    {
          "Regions": [
            {
            }
        ]
    }
    """

    RegionConfigurations.parse(config)
  }

  @Test( expected = RegionConfigurationException )
  void testMisspelledRegions( ) {
    String config =  """
    {
        "Region": [
        {
            "Name": "foobar",
            "CertificateFingerprintDigest": "SHA-256",
            "CertificateFingerprint": "ED:8F:9A:92:45:4D:37:F3:54:E4:2E:E7:26:28:EE:04:A1:DF:AD:82:87:60:A6:C3:4A:15:CB:D7:E9:F2:99:13",
            "IdentifierPartitions": [
                1
            ],
            "Services": [
                {
                    "Type": "identity",
                    "Endpoints": [
                        "http://identity.a-41.autoqa.qa1.eucalyptus-systems.com:8773/"
                    ]
                },
                {
                    "Type": "compute",
                    "Endpoints": [
                        "http://compute.a-41.autoqa.qa1.eucalyptus-systems.com:8773/"
                    ]
                }
            ]
        }
    ]
    }
    """

    RegionConfigurations.parse( config )
  }


}
