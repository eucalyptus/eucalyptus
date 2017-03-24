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
package com.eucalyptus.testing;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.function.LongSupplier;
import com.eucalyptus.util.Assert;

/**
 *
 */
public class SuppliedClock extends Clock {

  private final ZoneId zoneId;
  private final LongSupplier supplier;

  public SuppliedClock( final LongSupplier supplier ) {
    this( ZoneId.systemDefault( ), supplier );
  }

  public SuppliedClock( final ZoneId zoneId, final LongSupplier supplier ) {
    this.zoneId = Assert.notNull( zoneId, "zoneId" );
    this.supplier = Assert.notNull( supplier, "supplier" );
  }

  public static SuppliedClock of( final LongSupplier supplier ) {
    return new SuppliedClock( supplier );
  }

  @Override
  public long millis( ) {
    return supplier.getAsLong( );
  }

  @Override
  public ZoneId getZone( ) {
    return zoneId;
  }

  @Override
  public Clock withZone( final ZoneId zone ) {
    return new SuppliedClock( zone, supplier );
  }

  @Override
  public Instant instant( ) {
    return Instant.ofEpochMilli( millis( ) );
  }
}
