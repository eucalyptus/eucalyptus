/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
