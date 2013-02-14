/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.autoscaling.policies

import static org.junit.Assert.*
import org.junit.Test

import static com.eucalyptus.autoscaling.policies.AdjustmentType.*

/**
 * 
 */
class AdjustmentTypeTest {
  
  @Test
  void testChangeInCapacity() {
    assertEquals( "Change in capacity (positive)", 3, ChangeInCapacity.adjustCapacity( 1, 2, 0, 1, 10 ) )
    assertEquals( "Change in capacity (negative)", 2, ChangeInCapacity.adjustCapacity( 4, -2, 0, 1, 10 ) )
  }

  @Test
  void testExactCapacity() {
    assertEquals( "Exact capacity", 2, ExactCapacity.adjustCapacity( 1, 2, 0, 1, 10 ) )
  }

  @Test
  void testPercentChangeInCapacity() {
    assertEquals( "Percent change in capacity (positive)", 110, PercentChangeInCapacity.adjustCapacity( 100, 10, 0, 1, 1000 ) )
    assertEquals( "Percent change in capacity (positive, min)", 120, PercentChangeInCapacity.adjustCapacity( 100, 10, 20, 1, 1000 ) )
    assertEquals( "Percent change in capacity (negative)", 50, PercentChangeInCapacity.adjustCapacity( 100, -50, 0, 1, 1000 ) )
    assertEquals( "Percent change in capacity (negative, min)", 40, PercentChangeInCapacity.adjustCapacity( 100, -50, -60, 1, 1000 ) )
  }

  @Test
  void testMinMaxEnforcement() {
    assertEquals( "Maximum limit reached", 10, ChangeInCapacity.adjustCapacity( 5, 6, 0, 1, 10 ) )
    assertEquals( "Minimum limit reached", 10, ChangeInCapacity.adjustCapacity( 14, -5, 0, 10, 100 ) )
  }
}
