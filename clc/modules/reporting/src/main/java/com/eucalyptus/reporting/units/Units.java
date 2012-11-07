/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.reporting.units;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.google.common.base.CaseFormat;

@ConfigurableClass(root = "reporting", description = "Parameters controlling reporting units")
public class Units
{
  @ConfigurableField( initial = "DAYS", description = "Default time unit" )
  public static String DEFAULT_TIME_UNIT = "DAYS";
  @ConfigurableField( initial = "GB", description = "Default size unit" )
  public static String DEFAULT_SIZE_UNIT = "GB";
  @ConfigurableField( initial = "DAYS", description = "Default size-time time unit (GB-days, etc)" )
  public static String DEFAULT_SIZE_TIME_TIME_UNIT = "DAYS";
  @ConfigurableField( initial = "GB", description = "Default size-time size unit (GB-days, etc)" )
  public static String DEFAULT_SIZE_TIME_SIZE_UNIT = "GB";

  public static Units getDefaultDisplayUnits() {
    return new Units(
        TimeUnit.fromString( DEFAULT_TIME_UNIT, TimeUnit.DAYS ),
        SizeUnit.fromString( DEFAULT_SIZE_UNIT, SizeUnit.GB ),
        TimeUnit.fromString( DEFAULT_SIZE_TIME_TIME_UNIT, TimeUnit.DAYS ),
        SizeUnit.fromString( DEFAULT_SIZE_TIME_SIZE_UNIT, SizeUnit.GB ) );
  }

  private final TimeUnit timeUnit;
  private final SizeUnit sizeUnit;
  private final TimeUnit sizeTimeTimeUnit;
  private final SizeUnit sizeTimeSizeUnit;

  /**
   * Default no-arg ctor is required for euca to set dynamic properties
   * above. Please don't use this; it may go away.
   */
  public Units() {
    this( getDefaultDisplayUnits() );
  }

  public Units( final Units units ) {
    this(
        units.getTimeUnit(),
        units.getSizeUnit(),
        units.getSizeTimeTimeUnit(),
        units.getSizeTimeSizeUnit() );
  }

  public Units( final TimeUnit timeUnit,
                final SizeUnit sizeUnit,
                final TimeUnit sizeTimeTimeUnit,
                final SizeUnit sizeTimeSizeUnit ) {
    this.timeUnit = timeUnit;
    this.sizeUnit = sizeUnit;
    this.sizeTimeTimeUnit = sizeTimeTimeUnit;
    this.sizeTimeSizeUnit = sizeTimeSizeUnit;
  }

  public TimeUnit getTimeUnit() {
    return timeUnit;
  }

  public SizeUnit getSizeUnit() {
    return sizeUnit;
  }

  public TimeUnit getSizeTimeTimeUnit() {
    return sizeTimeTimeUnit;
  }

  public SizeUnit getSizeTimeSizeUnit() {
    return sizeTimeSizeUnit;
  }

  public String toString() {
    return String.format( "[timeUnit:%s,sizeUnit:%s,sizeTimeTimeUnit:%s,"
        + "sizeTimeSizeUnit:%s]", timeUnit, sizeUnit, sizeTimeTimeUnit,
        sizeTimeSizeUnit );
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime
        * result
        + ((sizeTimeSizeUnit == null) ? 0 : sizeTimeSizeUnit.hashCode());
    result = prime
        * result
        + ((sizeTimeTimeUnit == null) ? 0 : sizeTimeTimeUnit.hashCode());
    result = prime * result
        + ((sizeUnit == null) ? 0 : sizeUnit.hashCode());
    result = prime * result
        + ((timeUnit == null) ? 0 : timeUnit.hashCode());
    return result;
  }

  @Override
  public boolean equals( Object obj ) {
    if ( this == obj )
      return true;
    if ( obj == null )
      return false;
    if ( getClass() != obj.getClass() )
      return false;
    Units other = (Units) obj;
    if ( sizeTimeSizeUnit != other.sizeTimeSizeUnit )
      return false;
    if ( sizeTimeTimeUnit != other.sizeTimeTimeUnit )
      return false;
    if ( sizeUnit != other.sizeUnit )
      return false;
    if ( timeUnit != other.timeUnit )
      return false;
    return true;
  }

  public String labelForSizeTime() {
    return getSizeTimeSizeUnit() + "-" +
        capitalize( getSizeTimeTimeUnit() );
  }

  public String labelForSize() {
    return getSizeUnit().toString();
  }

  public String labelForTime() {
    return capitalize( getTimeUnit() );
  }

  private String capitalize( final Object item ) {
    return CaseFormat.LOWER_CAMEL.to( CaseFormat.UPPER_CAMEL, item.toString() );
  }
}
