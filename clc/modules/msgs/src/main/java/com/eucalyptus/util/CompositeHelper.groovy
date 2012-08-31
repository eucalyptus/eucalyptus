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

package com.eucalyptus.util;

import org.apache.log4j.Logger;

public class CompositeHelper<T> {
  private static Logger LOG = Logger.getLogger( CompositeHelper.class ); 
  private Class<T> destType;
  List<Class> sourceTypes;
  def vars = [:]
  public CompositeHelper( Class<T> destType, List<Class> sources ) {
    this.destType = destType;
    this.sourceTypes = sources;
    destType.metaClass.properties.findAll{ it.name!="metaClass"&&it.name!="class" }.each{ 
      vars[it.name]=it
    }
    def check=vars.clone()
    sources.each{ src -> 
      src.metaClass.properties.findAll{ it.name!="metaClass"&&it.name!="class" }.each {  f ->
        check.remove(f.name)
      }
    }
    check.each{ k,v -> LOG.debug( "WARNING: the field ${destType.class.name}.${k} will not be set since it is not defined in any of ${args}" ); }    
  }
  
  public T compose( T dest, Object... args ) {
    def props = vars.clone();
    args.each{ src ->
      src.metaClass.properties.findAll{ it.name!="metaClass"&&it.name!="class" }.each {
        if( props.containsKey( it.name ) ) {
          LOG.debug("${src.class.simpleName}.${it.name} as ${dest.class.simpleName}.${it.name}=${src[it.name]}");
          dest[it.name]=src[it.name];
          props.remove(it.name);
        } else {
          LOG.trace("WARNING: Ignoring ${src.class.name}.${it.name} as it is not in the destination type.");
        }
      }
    }
    dest.metaClass.properties.findAll{ it.name!="metaClass"&&it.name!="class" }.each { LOG.debug("${dest.class.simpleName}.${it.name} = ${dest[it.name]}"); }
    return dest;
  }
  
  public List<Object> project( T source, Object... args ) {
    args.each{ dest ->
      def props = dest.metaClass.properties.collect{ p -> p.name };
      source.metaClass.properties.findAll{ it.name!="metaClass"&&it.name!="class"&&props.contains(it.name)&&source[it.name]!=null }.each { sourceField -> 
        LOG.debug("${source.class.simpleName}.${sourceField.name} as ${dest.class.simpleName}.${sourceField.name}=${source[sourceField.name]}");
        dest[sourceField.name]=source[sourceField.name];
      }
    }
    return Arrays.asList(args);
  }
  
  public static Object update( Object source, Object dest ) {
    def props = dest.metaClass.properties.collect{ p -> p.name };
    source.metaClass.properties.findAll{ it.name!="metaClass"&&it.name!="class"&&props.contains(it.name)&&source[it.name]!=null }.each{ sourceField ->
      LOG.debug("${source.class.simpleName}.${sourceField.name} as ${dest.class.simpleName}.${sourceField.name}=${source[sourceField.name]}");
      dest[sourceField.name]=source[sourceField.name];
    }
    return dest;
  }
  
  public static Object updateNulls( Object source, Object dest ) {
    def props = dest.metaClass.properties.collect{ p -> p.name };
    source.metaClass.properties.findAll{ it.name!="metaClass"&&it.name!="class"&&props.contains(it.name) }.each{ sourceField ->
      LOG.debug("${source.class.simpleName}.${sourceField.name} as ${dest.class.simpleName}.${sourceField.name}=${source[sourceField.name]}");
      dest[sourceField.name]=source[sourceField.name];
    }
    return dest;
  }
  
}
