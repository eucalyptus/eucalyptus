/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.util;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import com.google.common.collect.Lists;

public class GenerateJiBXBinding {

  private static String bindingFile = "";
  private static int    indent      = 0;

  public static void main( String[] args ) throws Exception {
    List<String> pathList = Lists.newArrayList( new File( "build/edu/ucsb/eucalyptus/msgs/" ).list( ) );
    List<Class> classList = Lists.newArrayList( );
    for ( String className : pathList ) {
      if ( className.startsWith( "JiBX_" ) || className.endsWith( "Category" ) ) continue;
      classList.add( Class.forName( "edu.ucsb.eucalyptus.msgs." + className.replaceAll( ".class", "" ) ) );
    }
    GenerateJiBXBinding.binding( "http://msgs.eucalyptus.ucsb.edu", classList );
    File out = new File( "src/main/resources/msgs-binding.xml" );
    out.delete( );
    PrintWriter os = new PrintWriter( out );
    os.write( bindingFile );
    os.flush( );
    os.close( );
  }

  public static void binding( String ns, List<Class> classList ) {
    String bindingName = ns.replaceAll( "(http://)|(/$)", "" ).replaceAll( "[./-]", "_" );
    append( "<binding xmlns:euca=\"" + ns + "\" name=\"" + bindingName + "\">" );
    indent++;
    append( "<namespace uri=\"" + ns + "\" default=\"elements\" prefix=\"euca\"/>" );
    for ( Class clazz : classList ) {
      processClass( clazz );
    }
    indent--;
    append( "</binding>" );
  }

  public static void processClass( Class clazz ) {
    if ( clazz.getSuperclass( ).getSimpleName( ).equals( "Object" ) ) {
      baseMapping( clazz, clazz.getSimpleName( ), clazz.getName( ) );
    } else if ( clazz.getSuperclass( ).getSimpleName( ).equals( "EucalyptusData" ) ) {
      childMapping( clazz, clazz.getSimpleName( ).replaceAll( "Type", "" ), clazz.getName( ), clazz.getSuperclass( ).getName( ), true );
    } else {
      childMapping( clazz, clazz.getSimpleName( ).replaceAll( "Type", "" ), clazz.getName( ), clazz.getSuperclass( ).getName( ), false );
    }
  }

  private static void childMapping( Class clazz, String name, String className, String extendsName, boolean isAbstract ) {
    append( "<mapping " + ( isAbstract ? "abstract=\"true\"" : "name=\"" + name + "\"" ) + " extends=\"" + extendsName + "\" class=\"" + className + "\" >" );
    indent++;
    append( "<structure map-as=\"" + extendsName + "\"/>" );
    for ( Field f : clazz.getDeclaredFields( ) ) {
      processField( f );
    }
    indent--;
    append( "</mapping>" );
  }

  private static void baseMapping( Class clazz, String simpleName, String name ) {
    append( "<mapping abstract=\"true\" class=\"" + name + "\">" );
    indent++;
    for ( Field f : clazz.getDeclaredFields( ) ) {
      processField( f );
    }
    indent--;
    append( "</mapping>" );
  }

  public static void processField( Field field ) {
    Class itsType = field.getType( );
    if ( field.getName( ).startsWith( "__" ) 
        || field.getName( ).startsWith( "$" ) 
        || field.getName( ).startsWith( "class$" ) 
        || field.getName( ).equals( "metaClass" ) 
        || field.getName( ).startsWith( "JiBX_" ) 
        || itsType.getSimpleName( ).endsWith( "Channel" ) 
        || itsType.getSimpleName( ).endsWith( "HttpResponseStatus" ) ) {
      return;
    } else if ( itsType.getSuperclass( ) != null && "EucalyptusData".equals( itsType.getSuperclass( ).getSimpleName( ) ) ) {
      typeBind( field.getName( ), itsType.getName( ) );
    } else if ( field.getType( ).equals( java.util.ArrayList.class ) ) {
      String typeArg = getTypeArgument( field );
      if ( "java.lang.String".equals( typeArg ) ) {
        stringCollection( field.getName( ) );
      } else if ( typeArg != null ) {
        typedCollection( field.getName( ), typeArg );
      }
    } else {
      valueBind( field.getName( ) );
    }
  }

  private static void typedCollection( String name, String typeArgument ) {
    append( "<structure name=\"" + name + "\" usage=\"optional\">" );
    indent++;
    append( "<collection factory=\"org.jibx.runtime.Utility.arrayListFactory\" field=\"" + name + "\" usage=\"required\">" );
    indent++;
    append( "<structure name=\"item\" map-as=\"" + typeArgument + "\"/>" );
    indent--;
    append( "</collection>" );
    indent--;
    append( "</structure>" );
  }

  @SuppressWarnings( "unchecked" )
  public static String getTypeArgument( Field f ) {
    Type t = f.getGenericType( );
    if ( t != null && t instanceof ParameterizedType ) {
      Type tv = ( ( ParameterizedType ) t ).getActualTypeArguments( )[0];
      if ( tv instanceof Class ) { return ( ( Class ) tv ).getCanonicalName( ); }
    }
    return null;
  }

  private static void typeBind( String name, String className ) {
    append( "<structure name=\"" + name + "\" field=\"" + name + "\" map-as=\"" + className + "\" usage=\"optional\"/>" );
  }

  private static void stringCollection( String name ) {
    append( "<structure name=\"" + name + "\" usage=\"optional\">" );
    indent++;
    append( "<collection factory=\"org.jibx.runtime.Utility.arrayListFactory\" field=\"" + name + "\" item-type=\"java.lang.String\" usage=\"required\">" );
    indent++;
    append( "<structure name=\"item\">" );
    indent++;
    append( "<value name=\"entry\"/>" );
    indent--;
    append( "</structure>" );
    indent--;
    append( "</collection>" );
    indent--;
    append( "</structure>" );
  }

  private static void valueBind( String name ) {
    append( "<value style=\"element\" name=\"" + name + "\" field=\"" + name + "\" usage=\"optional\"/>" );
  }

  public static String addIndent( ) {
    String indentStr = "";
    for ( int i = 0; i < indent; i++ ) {
      indentStr += "  ";
    }
    return indentStr;
  }

  public static void append( String addMe ) {
    bindingFile += addIndent( ) + addMe + "\n";
  }

}
