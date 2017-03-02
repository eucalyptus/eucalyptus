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
package com.eucalyptus.cassandra.config

import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Test

/**
 *
 */
class UtilTest {

  @Test
  void testTemplate( ) {
    String name = 'test-name'
    String bindAddr = '1.2.3.4'
    String dir = '/var/lib/eucalyptus/cassandra'
    String generated = Util.generateCassandraYaml( name, bindAddr, dir )
    println generated
    Assert.assertThat( 'All placeholders replaced', generated, Matchers.not( Matchers.containsString('$') ) )
  }
}
