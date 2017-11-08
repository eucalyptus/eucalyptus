/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.google.common.base.Function;

/**
 * A well formed type mapper is on an instance of {@link Function} and the annotation's value can be: 
 * - a pair of class types, {@code @TypeMapper(F.class, T.class})}, such that {@code T result = Function#apply(F)}.
 * - explict from and to declarations, {@code @TypeMapper(from = F.class, to = T.class)}, such that {@code T result = Function#apply(F)}.
 * - empty, {@code @TypeMapper}, in which case the types are intefered from the declaration of the class.
 * 
 * An off-the-cuff object-to-string example to indicate the spirit of the annotations usage:
 * 
 * <pre>
 * &#064;TypeMapper( { Object.class, String.class } )
 * private class ToString implements Function&lt;Object, String&gt; {
 *   public String apply( Object o ) {
 *     return o.toString( );
 *   }
 * };
 * </pre>
 * 
 * The same mapping could also be specified as:
 * 
 * <pre>
 * &#064;TypeMapper( from=Object.class, to=String.class )
 * </pre>
 * 
 * The default {@code @TypeMapper( From.class,To.class})} will be used if both are present.
 * A TypeMapper Function is registered in {@link TypeMappers} during {@link ServiceJarDiscovery} by {@link TypeMappers.TypeMapperDiscovery}.
 */
@Target( { ElementType.TYPE, ElementType.FIELD } )
@Retention( RetentionPolicy.RUNTIME )
public @interface TypeMapper {
  Class[] value( ) default { Object.class, Object.class };
}
