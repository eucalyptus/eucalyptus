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
                "Certificate": "-----BEGIN CERTIFICATE-----\\nMIIDQDCCAiigAwIBAgIGEa2aEc4QMA0GCSqGSIb3DQEBDQUAMEcxCzAJBgNVBAYT\\nAlVTMQ4wDAYDVQQKEwVDbG91ZDETMBEGA1UECxMKRXVjYWx5cHR1czETMBEGA1UE\\nAxMKZXVjYWx5cHR1czAeFw0xNTAzMDIwMTIyMjFaFw0yMDAzMDIwMTIyMjFaMEcx\\nCzAJBgNVBAYTAlVTMQ4wDAYDVQQKEwVDbG91ZDETMBEGA1UECxMKRXVjYWx5cHR1\\nczETMBEGA1UEAxMKZXVjYWx5cHR1czCCASIwDQYJKoZIhvcNAQEBBQADggEPADCC\\nAQoCggEBAILQlVZkR+JoJxXqo0KRrkCYwhzTmDQceqe5gwwfq9hY/ziN+rNIcwdK\\nfJD63tbQASaxjrIs8o/yB73c9wbOCN9wBZFMK9mY+K5gb0OxGPkNusnduBQAneVq\\nMf3syjH9ptNWVkzw6P7nPE5b0snYaQXAUe+MmKLYd5o+efz2ls6+IHi22YBz++wm\\nffbaT8R+NKL/ZNlZdL9YP6xhTNMFNMUCibgqmDipLo78U5J/t4oXAj4oJbR+evig\\ntt4x0ZRsVgrQIzQ0hgKH9ardGMBaMWw/3H5ho9fMPuZGzt9UXifZpuOW9aNUUGQt\\nEV1qXG/rVZ/CjGI0TFjzRHDKuacEPRsCAwEAAaMyMDAwDwYDVR0TAQH/BAUwAwEB\\n/zAdBgNVHQ4EFgQUg/yqP65X2nnIeWTqxDCkTqJ2T2MwDQYJKoZIhvcNAQENBQAD\\nggEBAHHJG5iiXy4p2bFtRIS5LEy9mp3OMyxLslHnG0x8yPKH4pGheANk1XRNZyqA\\ne+HUOkX9BpWeWlHOLFiJ1/YERBZvG1TDKf0RuZjTiTvG99njGyTr3cK18y9CSM+0\\nrPP2m45OpBae5p7gPG6Az/LcipEapytMAVkn6v1Ip3BENxD/r/IVnVSEts9GDDbn\\n3Z6g7X98CoG152wT0w7Nw/bl/chBypTzvKkc9cY2U/Eqobci1BbekOk3HOdneaDm\\n4lSHPaLsdCoMDv44lujO09V5zt21fXQF3FeWFv8y0xkg23DHxZr/XysUf7gGEyrd\\n5DQlQ0lUi9rQMcxa2eDYGjQNtLQ=\\n-----END CERTIFICATE-----",
                "IdentifierPartitions": [
                  1
                ],
                "Services": [
                    {
                      "Type": "identity",
                      "Endpoints": [
                        "http://identity.example.com:8773/services/Identity"
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
                certificate: '''\
    -----BEGIN CERTIFICATE-----
    MIIDQDCCAiigAwIBAgIGEa2aEc4QMA0GCSqGSIb3DQEBDQUAMEcxCzAJBgNVBAYT
    AlVTMQ4wDAYDVQQKEwVDbG91ZDETMBEGA1UECxMKRXVjYWx5cHR1czETMBEGA1UE
    AxMKZXVjYWx5cHR1czAeFw0xNTAzMDIwMTIyMjFaFw0yMDAzMDIwMTIyMjFaMEcx
    CzAJBgNVBAYTAlVTMQ4wDAYDVQQKEwVDbG91ZDETMBEGA1UECxMKRXVjYWx5cHR1
    czETMBEGA1UEAxMKZXVjYWx5cHR1czCCASIwDQYJKoZIhvcNAQEBBQADggEPADCC
    AQoCggEBAILQlVZkR+JoJxXqo0KRrkCYwhzTmDQceqe5gwwfq9hY/ziN+rNIcwdK
    fJD63tbQASaxjrIs8o/yB73c9wbOCN9wBZFMK9mY+K5gb0OxGPkNusnduBQAneVq
    Mf3syjH9ptNWVkzw6P7nPE5b0snYaQXAUe+MmKLYd5o+efz2ls6+IHi22YBz++wm
    ffbaT8R+NKL/ZNlZdL9YP6xhTNMFNMUCibgqmDipLo78U5J/t4oXAj4oJbR+evig
    tt4x0ZRsVgrQIzQ0hgKH9ardGMBaMWw/3H5ho9fMPuZGzt9UXifZpuOW9aNUUGQt
    EV1qXG/rVZ/CjGI0TFjzRHDKuacEPRsCAwEAAaMyMDAwDwYDVR0TAQH/BAUwAwEB
    /zAdBgNVHQ4EFgQUg/yqP65X2nnIeWTqxDCkTqJ2T2MwDQYJKoZIhvcNAQENBQAD
    ggEBAHHJG5iiXy4p2bFtRIS5LEy9mp3OMyxLslHnG0x8yPKH4pGheANk1XRNZyqA
    e+HUOkX9BpWeWlHOLFiJ1/YERBZvG1TDKf0RuZjTiTvG99njGyTr3cK18y9CSM+0
    rPP2m45OpBae5p7gPG6Az/LcipEapytMAVkn6v1Ip3BENxD/r/IVnVSEts9GDDbn
    3Z6g7X98CoG152wT0w7Nw/bl/chBypTzvKkc9cY2U/Eqobci1BbekOk3HOdneaDm
    4lSHPaLsdCoMDv44lujO09V5zt21fXQF3FeWFv8y0xkg23DHxZr/XysUf7gGEyrd
    5DQlQ0lUi9rQMcxa2eDYGjQNtLQ=
    -----END CERTIFICATE-----'''.stripIndent( ),
                identifierPartitions: [ 1 ],
                services: [
                    new Service(
                        type: 'identity',
                        endpoints: [ 'http://identity.example.com:8773/services/Identity' ]
                    )
                ]
            )
        ]
    )

    assertEquals( 'Result does not match template', expected, result)
  }
}
