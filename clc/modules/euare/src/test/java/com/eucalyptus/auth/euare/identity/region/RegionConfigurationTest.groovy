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
                ]
            }
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
                ]
            )
        ]
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
}
