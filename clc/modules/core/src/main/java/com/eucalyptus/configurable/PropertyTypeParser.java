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

package com.eucalyptus.configurable;

import java.util.Map;
import org.apache.log4j.Logger;
import com.google.common.base.Function;
import com.google.common.collect.Maps;

public abstract class PropertyTypeParser<T> implements Function<String,T> {
  private static Logger                         LOG           = Logger.getLogger( PropertyTypeParser.class );
  private static Map<Class, PropertyTypeParser> typeParsers   = Maps.newHashMap( );
  private static PropertyTypeParser<Float>      floatParser   = new PropertyTypeParser<Float>( ) {
                                                                @Override
                                                                public Float apply( String property ) {
                                                                  return Float.parseFloat( property );
                                                                }
                                                              };
  private static PropertyTypeParser<Double>     doubleParser  = new PropertyTypeParser<Double>( ) {
                                                                @Override
                                                                public Double apply( String property ) {
                                                                  return Double.parseDouble( property );
                                                                }
                                                              };
  private static PropertyTypeParser<Integer>    integerParser = new PropertyTypeParser<Integer>( ) {
                                                                @Override
                                                                public Integer apply( String property ) {
                                                                  return Integer.parseInt( property );
                                                                }
                                                              };
  private static PropertyTypeParser<Long>       longParser    = new PropertyTypeParser<Long>( ) {
                                                                @Override
                                                                public Long apply( String property ) {
                                                                  return Long.parseLong( property );
                                                                }
                                                              };
  private static PropertyTypeParser<Boolean>    booleanParser = new PropertyTypeParser<Boolean>( ) {
                                                                @Override
                                                                public Boolean apply( String property ) {
                                                                  return Boolean.parseBoolean( property );
                                                                }
                                                              };
  private static PropertyTypeParser<String>     stringParser  = new PropertyTypeParser<String>( ) {
                                                                @Override
                                                                public String apply( String property ) {
                                                                  return property;
                                                                }
                                                              };
  static {
    typeParsers.put( Integer.class, integerParser );
    typeParsers.put( int.class, integerParser );
    typeParsers.put( Long.class, longParser );
    typeParsers.put( long.class, longParser );
    typeParsers.put( Float.class, floatParser );
    typeParsers.put( float.class, floatParser );
    typeParsers.put( Double.class, doubleParser );
    typeParsers.put( double.class, doubleParser );
    typeParsers.put( Boolean.class, booleanParser );
    typeParsers.put( boolean.class, booleanParser );
    typeParsers.put( String.class, stringParser );
  }
  
  public static void addTypeParser( Class c, PropertyTypeParser p ) {
    typeParsers.put( c, p );
  }
  
  public static PropertyTypeParser get( Class c ) {
    if ( !typeParsers.containsKey( c ) || typeParsers.get( c ) == null ) {
      RuntimeException r = new RuntimeException( "Invalid configurable type: " + c );
      LOG.fatal( r, r );
      throw r;
    } else {
      return typeParsers.get( c );
    }
  }
  
  public abstract T apply( String property );
}
