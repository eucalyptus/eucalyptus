/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.compute.common;

import java.util.regex.Pattern;
import javax.annotation.Nullable;
import com.eucalyptus.util.RestrictedTypes;

/**
 * Functionality from this class moved to RestrictedTypes. This class remains
 * in case we want to add any CloudMetadata specific functionality.
 */
public class CloudMetadatas extends RestrictedTypes {

  public static final Pattern ID_IMAGE         = Pattern.compile( "[ae](ki-|mi-|ri-)[0-9a-fA-F]{8}" );
  public static final Pattern ID_KERNEL_IMAGE  = Pattern.compile( "[ae]ki-[0-9a-fA-F]{8}" );
  public static final Pattern ID_MACHINE_IMAGE = Pattern.compile( "[ae]mi-[0-9a-fA-F]{8}" );
  public static final Pattern ID_RAMDISK_IMAGE = Pattern.compile( "[ae]ri-[0-9a-fA-F]{8}" );


  /**
   * Syntactic validation for image identifiers.
   *
   * @param identifier The possible identifier to check.
   * @return True if valid
   */
  public static boolean isImageIdentifier( @Nullable final String identifier ) {
    return identifier != null && ID_IMAGE.matcher( identifier ).matches( );
  }

  /**
   * Syntactic validation for kernel image identifiers.
   *
   * @param identifier The possible identifier to check.
   * @return True if valid
   */
  public static boolean isKernelImageIdentifier( @Nullable final String identifier ) {
    return identifier != null && ID_KERNEL_IMAGE.matcher( identifier ).matches( );
  }

  /**
   * Syntactic validation for machine image identifiers.
   *
   * @param identifier The possible identifier to check.
   * @return True if valid
   */
  public static boolean isMachineImageIdentifier( @Nullable final String identifier ) {
    return identifier != null && ID_MACHINE_IMAGE.matcher( identifier ).matches( );
  }

  /**
   * Syntactic validation for ramdisk image identifiers.
   *
   * @param identifier The possible identifier to check.
   * @return True if valid
   */
  public static boolean isRamdiskImageIdentifier( @Nullable final String identifier ) {
    return identifier != null && ID_RAMDISK_IMAGE.matcher( identifier ).matches( );
  }

}
