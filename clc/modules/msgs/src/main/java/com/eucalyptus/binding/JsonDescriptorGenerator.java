/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
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
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.binding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import com.eucalyptus.binding.JsonDescriptorGenerator.BooleanTypeBinding;
import com.eucalyptus.binding.JsonDescriptorGenerator.CollectionTypeBinding;
import com.eucalyptus.binding.JsonDescriptorGenerator.Elem;
import com.eucalyptus.binding.JsonDescriptorGenerator.ElemItem;
import com.eucalyptus.binding.JsonDescriptorGenerator.IntegerTypeBinding;
import com.eucalyptus.binding.JsonDescriptorGenerator.LongTypeBinding;
import com.eucalyptus.binding.JsonDescriptorGenerator.NoopTypeBinding;
import com.eucalyptus.binding.JsonDescriptorGenerator.ObjectTypeBinding;
import com.eucalyptus.binding.JsonDescriptorGenerator.RootObjectTypeBinding;
import com.eucalyptus.binding.JsonDescriptorGenerator.StringTypeBinding;
import com.eucalyptus.binding.JsonDescriptorGenerator.TypeBinding;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class JsonDescriptorGenerator extends BindingGenerator {
  private static Logger            LOG          = Logger.getLogger( JsonDescriptorGenerator.class );
  private final String             ns           = "http://msgs.eucalyptus.com";
  private static String            INDENT       = "";
  private final File               outFile;
  private PrintWriter              out;
  private String                   bindingName;
  private int                      indent       = 0;
  private Map<String, TypeBinding> typeBindings = new HashMap<String, TypeBinding>( ) {
                                                  {
                                                    put( Integer.class.getCanonicalName( ), new IntegerTypeBinding( ) );
                                                    put( Boolean.class.getCanonicalName( ), new BooleanTypeBinding( ) );
                                                    put( String.class.getCanonicalName( ), new StringTypeBinding( ) );
                                                    put( Long.class.getCanonicalName( ), new LongTypeBinding( ) );
                                                    put( "boolean", new BooleanTypeBinding( ) );
                                                    put( "int", new IntegerTypeBinding( ) );
                                                    put( "long", new LongTypeBinding( ) );
                                                    put( java.util.Date.class.getCanonicalName( ), new StringTypeBinding( ) );
                                                  }
                                                };
  
  private static List<String>      badClasses   = new ArrayList<String>( ) {
                                                  {
                                                    add( ".*HttpResponseStatus" );
                                                    add( ".*Closure" );
                                                    add( ".*Channel" );
                                                    add( ".*\\.JiBX_*" );
                                                  }
                                                };
  private static List<String>      badFields    = new ArrayList<String>( ) {
                                                  {
                                                    add( "__.*" );
                                                    add( "\\w*\\$\\w*\\$*.*" );
                                                    add( "class\\$.*" );
                                                    add( "metaClass" );
                                                    add( "JiBX_.*" );
                                                  }
                                                };
  
  public JsonDescriptorGenerator( ) {
    this.outFile = new File( "modules/msgs/src/main/resources/msgs-binding.xml" );
    if ( outFile.exists( ) ) {
      outFile.delete( );
    }
    try {
      this.out = new PrintWriter( outFile );
    } catch ( FileNotFoundException e ) {
      e.printStackTrace( System.err );
      System.exit( -1 );
    }
    this.bindingName = this.ns.replaceAll( "(http://)|(/$)", "" ).replaceAll( "[./-]", "_" );
    this.out.write( "<binding xmlns:euca=\"" + ns + "\" name=\"" + bindingName + "\">\n" );
    this.out.write( "  <namespace uri=\"" + ns + "\" default=\"elements\" prefix=\"euca\"/>\n" );
    this.out.flush( );
  }
  
  public ElemItem peek( ) {
    return this.elemStack.peek( );
  }
  
  @Override
  public void processClass( Class klass ) {
    if ( BindingGenerator.DATA_TYPE.isAssignableFrom( klass ) || BindingGenerator.MSG_TYPE.isAssignableFrom( klass ) ) {
      String mapping = new RootObjectTypeBinding( klass ).process( );
      out.write( mapping );
      out.flush( );
    }
  }
  
  @Override
  public void close( ) {
    this.out.write( "</binding>" );
    this.out.flush( );
    this.out.close( );
//    try {
//      XMLSerializer xmlSerializer = new XMLSerializer();  
//      JSON json = xmlSerializer.readFromFile( this.outFile );  
//      System.out.println( json.toString(2) );
//    } catch ( Throwable ex ) {
//      LOG.error( ex , ex );
//    }    
  }
  
  public TypeBinding getTypeBinding( Field field ) {
    Class itsType = field.getType( );
    if ( this.isIgnored( field ) ) {
      return new NoopTypeBinding( field );
    } else if ( List.class.isAssignableFrom( itsType ) ) {
      Class listType = getTypeArgument( field );
      if ( listType == null ) {
        System.err.printf( "IGNORE: %-70s [type=%s] NO GENERIC TYPE FOR LIST\n", field.getDeclaringClass( ).getCanonicalName( ) + "."+ field.getName( ), listType );
        return new NoopTypeBinding( field );        
      } else if ( typeBindings.containsKey( listType.getCanonicalName( ) ) ) {
        return new CollectionTypeBinding( field.getName( ), typeBindings.get( listType.getCanonicalName( ) ) );
      } else if ( BindingGenerator.DATA_TYPE.isAssignableFrom( listType ) ) {
        return new CollectionTypeBinding( field.getName( ), new ObjectTypeBinding( field.getName( ), listType ) );
      } else {
        System.err.printf( "IGNORE: %-70s [type=%s] LIST'S GENERIC TYPE DOES NOT CONFORM TO EucalyptusData\n", field.getDeclaringClass( ).getCanonicalName( ) + "."+ field.getName( ), listType.getCanonicalName( ) );
        return new NoopTypeBinding( field );        
      }
    } else if ( typeBindings.containsKey( itsType.getCanonicalName( ) ) ) {
      TypeBinding t = typeBindings.get( itsType.getCanonicalName( ) );
      try {
        t = typeBindings.get( itsType.getCanonicalName( ) ).getClass( ).newInstance( );
      } catch ( Exception e ) {}
      return t.value( field.getName( ) );
    } else if ( BindingGenerator.DATA_TYPE.isAssignableFrom( field.getType( ) ) ) {
      return new ObjectTypeBinding( field );
    } else {
      System.err.printf( "IGNORE: %-70s [type=%s] TYPE DOES NOT CONFORM TO EucalyptusData\n", field.getDeclaringClass( ).getCanonicalName( ) + "."+ field.getName( ), field.getType( ).getCanonicalName( ) );
      return new NoopTypeBinding( field );
    }
  }
  
  class RootObjectTypeBinding extends TypeBinding {
    private Class   type;
    private boolean abs;
    
    public RootObjectTypeBinding( Class type ) {
      JsonDescriptorGenerator.this.indent = 2;
      this.type = type;
      if ( Object.class.equals( type.getSuperclass( ) ) ) {
        this.abs = true;
      } else if ( type.getSuperclass( ).getSimpleName( ).equals( "EucalyptusData" ) ) {
        this.abs = true;
      } else {
        this.abs = false;
      }
    }
    
    @Override
    public String getTypeName( ) {
      return type.getCanonicalName( );
    }
    
    public String process( ) {
      if( this.type.getCanonicalName( ) == null ) {
        new RuntimeException( "Ignoring anonymous class: " + this.type ).printStackTrace( );
      } else {
        this.elem( Elem.mapping );
        if ( this.abs ) {
          this.attr( "abstract", "true" );
        } else {
          this.attr( "name", this.type.getSimpleName( ) ).attr( "extends", this.type.getSuperclass( ).getCanonicalName( ) );
        }
        this.attr( "class", this.type.getCanonicalName( ) );
        if( BindingGenerator.MSG_TYPE.isAssignableFrom( this.type.getSuperclass( ) ) || BindingGenerator.DATA_TYPE.isAssignableFrom( this.type.getSuperclass( ) ) ) {
          this.elem( Elem.structure ).attr( "map-as", this.type.getSuperclass().getCanonicalName( ) ).end( );
        }
        for ( Field f : type.getDeclaredFields( ) ) {
          TypeBinding tb = getTypeBinding( f );
          if ( !( tb instanceof NoopTypeBinding ) ) {
            System.out.printf( "BOUND:  %-70s [type=%s:%s]\n", f.getDeclaringClass( ).getCanonicalName( ) +"."+ f.getName( ), tb.getTypeName( ), f.getType( ).getCanonicalName( ) );          
            this.append( tb.toString( ) );
          }
        }
        this.end( );
      }
      return this.toString( );
    }
  }
  
  @SuppressWarnings( "unchecked" )
  public static Class getTypeArgument( Field f ) {
    Type t = f.getGenericType( );
    if ( t != null && t instanceof ParameterizedType ) {
      Type tv = ( ( ParameterizedType ) t ).getActualTypeArguments( )[0];
      if ( tv instanceof Class ) {
        return ( ( Class ) tv );
      }
    }
    return null;
  }
  
  abstract class TypeBinding {
    private StringBuilder buf = new StringBuilder( );
    
    public abstract String getTypeName( );
    
    private TypeBinding reindent( int delta ) {
      JsonDescriptorGenerator.this.indent += delta;
      INDENT = "";
      for ( int i = 0; i < indent; i++ ) {
        INDENT += "  ";
      }
      return this;
    }
    
    private TypeBinding indent( String addMe ) {
      this.reindent( +1 ).append( INDENT ).append( addMe );
      return this;
    }
    
    private TypeBinding outdent( String addMe ) {
      this.reindent( -1 ).append( INDENT ).append( addMe );
      return this;
    }
    
    protected TypeBinding append( Object o ) {
      this.buf.append( ""+o );
      return this;
    }
    
    protected TypeBinding eolIn( ) {
      this.append( "\n" ).indent( INDENT );
      return this;
    }
    
    protected TypeBinding eolOut( ) {
      this.append( "\n" ).outdent( INDENT );
      return this;
    }
    
    protected TypeBinding eol( ) {
      this.append( "\n" ).append( INDENT );
      return this;
    }
    
    protected TypeBinding value( String name ) {
      this.elem( Elem.value ).attr( "name", name ).attr( "field", name ).attr( "usage", "optional" ).attr( "style", "element" ).end( );
      return this;
    }
    
    private TypeBinding begin( ) {
      ElemItem top = JsonDescriptorGenerator.this.elemStack.peek( );
      if ( top != null && top.children ) {
        this.eol( );
      } else if ( top != null && !top.children ) {
        this.append( ">" ).eolIn( );
        top.children = true;
      } else {
        this.eolIn( );
      }
      return this;
    }
    
    protected TypeBinding elem( Elem name ) {
      this.begin( ).append( "<" ).append( name.toString( ) ).append( " " );
      JsonDescriptorGenerator.this.elemStack.push( new ElemItem( name, JsonDescriptorGenerator.this.indent, false ) );
      return this;
    }
    
    protected TypeBinding end( ) {
      ElemItem top = JsonDescriptorGenerator.this.elemStack.pop( );
      if ( top != null && top.children ) {
        this.eolOut( ).append( "</" ).append( top.name.toString( ) ).append( ">" );
      } else if ( top != null && !top.children ) {
        this.append( "/>" );
      } else {
        this.append( "/>" );
      }
      return this;
    }
    
    protected TypeBinding attr( String name, String value ) {
      this.append( name ).append( "=\"" ).append( value ).append( "\" " );
      return this;
    }
    
    public String toString( ) {
      String s = buf.toString( );
      buf = new StringBuilder( buf.capacity( ) );
      return s;
    }
    
    protected TypeBinding collection( String name ) {
      this.elem( Elem.structure ).attr( "name", name ).attr( "usage", "optional" );
      this.elem( Elem.collection ).attr( "factory", "com.eucalyptus.binding.Binding.listFactory" ).attr( "field", name )
          .attr( "item-type", this.getTypeName( ) ).attr( "usage", "required" );
      this.elem( Elem.structure ).attr( "name", "item" );
      this.elem( Elem.value ).attr( "name", "entry" ).end( ).end( ).end( ).end( );
      return this;
    }
    
  }
  
  public boolean isIgnored( final Field field ) {
    final int mods = field.getModifiers( );
    final String name = field.getName( );
    final String type = field.getType( ).getSimpleName( );
    if ( Modifier.isFinal( mods ) ) {
      LOG.debug( "Ignoring field with bad type: " + field.getDeclaringClass( ).getCanonicalName( ) + "." + name + " of type " + type
                 + " due to: final modifier" );
    } else if ( Modifier.isStatic( mods ) ) {
      LOG.debug( "Ignoring field with bad type: " + field.getDeclaringClass( ).getCanonicalName( ) + "." + name + " of type " + type
                 + " due to: static modifier" );
    }
    boolean ret = Iterables.any( badClasses, new Predicate<String>( ) {
      @Override
      public boolean apply( String arg0 ) {
        if ( type.matches( arg0 ) ) {
          LOG.debug( "Ignoring field with bad type: " + field.getDeclaringClass( ).getCanonicalName( ) + "." + name + " of type " + type + " due to: " + arg0 );
          return true;
        } else {
          return false;
        }
      }
    } );
    ret |= Iterables.any( badFields, new Predicate<String>( ) {
      @Override
      public boolean apply( String arg0 ) {
        if ( name.matches( arg0 ) ) {
          LOG.debug( "Ignoring field with bad name: " + field.getDeclaringClass( ).getCanonicalName( ) + "." + name + " of type " + type + " due to: " + arg0 );
          return true;
        } else {
          return false;
        }
      }
    } );
    
    return ret;
  }
  
  private class ElemItem {
    Elem    name;
    int     indent;
    boolean children;
    
    public ElemItem( Elem name, int indent, boolean children ) {
      this.name = name;
      this.indent = indent;
      this.children = children;
    }
    
    @Override
    public String toString( ) {
      return String.format( "ElemItem [name=%s, indent=%s, children=%s]", this.name, this.indent, Boolean.valueOf( children ) );
    }
    
  }
  
  private Deque<ElemItem> elemStack = new LinkedList<ElemItem>( );
  
  enum Elem {
    structure, collection, value, mapping, binding
  }
  class IgnoredTypeBinding extends NoopTypeBinding {

    public IgnoredTypeBinding( Field field ) {
      super( field );
    }
  }  
  class NoopTypeBinding extends TypeBinding {
    private String name;
    private Class  type;
    
    public NoopTypeBinding( Field field ) {
      this.name = field.getName( );
      this.type = field.getType( );
    }
    
    @Override
    public String toString( ) {
      return "";
    }
    
    @Override
    public String getTypeName( ) {
      return "NOOP";
    }
    
  }
  
  class ObjectTypeBinding extends TypeBinding {
    private String name;
    private Class  type;
    
    public ObjectTypeBinding( String name, Class type ) {
      this.name = name;
      this.type = type;
    }
    
    public ObjectTypeBinding( Field field ) {
      this.name = field.getName( );
      this.type = field.getType( );
    }
    
    @Override
    protected TypeBinding collection( String name ) {
      this.elem( Elem.structure ).attr( "name", name ).attr( "usage", "optional" );
      this.elem( Elem.collection ).attr( "factory", "com.eucalyptus.binding.Binding.listFactory" ).attr( "field", name ).attr( "usage", "required" );
      this.elem( Elem.structure ).attr( "name", "item" ).attr( "map-as", type.getCanonicalName( ) );
      this.end( ).end( ).end( );
      return this;
    }
    
    @Override
    public String getTypeName( ) {
      return this.type.getCanonicalName( );
    }
    
    public String toString( ) {
      this.elem( Elem.structure ).attr( "name", this.name ).attr( "field", this.name ).attr( "map-as", this.type.getCanonicalName( ) ).attr( "usage",
                                                                                                                                             "optional" ).end( );
      return super.toString( );
    }
    
  }
  
  class CollectionTypeBinding extends TypeBinding {
    private TypeBinding type;
    private String      name;
    
    public CollectionTypeBinding( String name, TypeBinding type ) {
      this.name = name;
      this.type = type;
      LOG.debug( "Found list type: " + type.getClass( ).getCanonicalName( ) );
    }
    
    @Override
    public String getTypeName( ) {
      return type.getTypeName( );
    }
    
    @Override
    public String toString( ) {
      LOG.debug( "Found list type: " + this.type.getTypeName( ) + " for name: " + name );
      String ret = this.type.collection( this.name ).buf.toString( );
      this.type.collection( this.name ).buf = new StringBuilder( );
      return ret;
    }
    
  }
  
  class IntegerTypeBinding extends TypeBinding {
    @Override
    public String getTypeName( ) {
      return Integer.class.getCanonicalName( );
    }
  }
  
  class LongTypeBinding extends TypeBinding {
    @Override
    public String getTypeName( ) {
      return Long.class.getCanonicalName( );
    }
  }
  
  class StringTypeBinding extends TypeBinding {
    @Override
    public String getTypeName( ) {
      return String.class.getCanonicalName( );
    }
  }
  
  class BooleanTypeBinding extends TypeBinding {
    @Override
    public String getTypeName( ) {
      return Boolean.class.getCanonicalName( );
    }
  }
  
}
