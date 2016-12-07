/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.simpleworkflow.common.client

import com.amazonaws.ClientConfiguration
import static org.junit.Assert.*
import org.junit.Test

/**
 *
 */
class ConfigTest {

  @Test
  void testBuildConfig() {
    final ClientConfiguration clientConfiguration =
        Config.buildConfiguration( '{"ConnectionTimeout": 731, "MaxConnections": 9431}' )
    assertEquals( 'ConnectionTimeout', 731, clientConfiguration.connectionTimeout )
    assertEquals( 'MaxConnections', 9431, clientConfiguration.maxConnections )
  }
}
