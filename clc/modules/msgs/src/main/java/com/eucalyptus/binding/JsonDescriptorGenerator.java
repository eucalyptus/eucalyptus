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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.binding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class JsonDescriptorGenerator extends BindingGenerator {
  private static Logger                   LOG          = Logger.getLogger( JsonDescriptorGenerator.class );
  private final String                    ns           = "http://msgs.eucalyptus.com";
  private static String                   INDENT       = "";
  private PrintWriter                     out;
  private static int                      indent       = 0;
  private static Map<String, TypeBinding> typeBindings = new HashMap<String, TypeBinding>( ) {
                                                         {
                                                           put( Integer.class.getCanonicalName( ), new IntegerTypeBinding( ) );
                                                           put( Boolean.class.getCanonicalName( ), new BooleanTypeBinding( ) );
                                                           put( String.class.getCanonicalName( ), new StringTypeBinding( ) );
                                                           put( Long.class.getCanonicalName( ), new LongTypeBinding( ) );
                                                           put( Double.class.getCanonicalName( ), new DoubleTypeBinding( ) );
                                                           put( Float.class.getCanonicalName( ), new FloatTypeBinding( ) );
                                                           put( java.util.Date.class.getCanonicalName( ), new DateTimeTypeBinding( ) );
                                                           put( "boolean", new BooleanTypeBinding( ) );
                                                           put( "int", new IntegerTypeBinding( ) );
                                                           put( "long", new LongTypeBinding( ) );
                                                           put( "float", new FloatTypeBinding( ) );
                                                           put( "double", new DoubleTypeBinding( ) );
                                                         }
                                                       };
  
  private static List<String>             badClasses   = new ArrayList<String>( ) {
                                                         {
                                                           add( ".*HttpResponseStatus" );
                                                           add( ".*Closure" );
                                                           add( ".*Channel" );
                                                           add( ".*\\.JiBX_*" );
                                                         }
                                                       };
  private static List<String>             badFields    = new ArrayList<String>( ) {
                                                         {
                                                           add( "__.*" );
                                                           add( "\\w*\\$\\w*\\$*.*" );
                                                           add( "class\\$.*" );
                                                           add( "metaClass" );
                                                           add( "JiBX_.*" );
                                                         }
                                                       };
  
  public static boolean request = false;
  
  public static void write( Class parentType, String stuff ) {
    LOG.info( "Preparing JSON descriptor file: " + parentType.getCanonicalName( ) );
    File outFile = new File( "modules/msgs/src/main/resources/json/" + parentType.getSimpleName( ).replaceAll("Type\\Z","") + ".json" );
    if ( !outFile.getParentFile( ).exists( ) ) {
      outFile.getParentFile( ).mkdirs( );
    }
    if ( outFile.exists( ) ) {
      outFile.delete( );
    }
    FileWriter out = null;
    try {
      out = new FileWriter( outFile, true );
      out.write( stuff );
      out.flush( );
      out.close( );
    } catch ( FileNotFoundException ex ) {
      LOG.error( ex, ex );
      throw new RuntimeException( "Failed to create JSON descriptor file: " + outFile.getAbsolutePath( ) + " because of " + ex.getMessage( ), ex );
    } catch ( IOException ex ) {
      LOG.error( ex , ex );
      try {
        out.close();
      } catch ( Exception e ) {
	LOG.error(e, e);
      }
      throw new RuntimeException( "Failed to create JSON descriptor file: " + outFile.getAbsolutePath( ) + " because of " + ex.getMessage( ), ex );
    }
  }
  
  public JsonDescriptorGenerator( ) {}
  
  @Override
  public void processClass( Class klass ) {
    if ( BindingGenerator.MSG_TYPE.isAssignableFrom( klass ) ) {
//      System.out.println( "JSONifying " + klass.getCanonicalName( ) );
      RequestInfo.get( klass );
    }
  }
  
  @Override
  public void close( ) {
    for ( RequestInfo req : RequestInfo.getRequestInfoList( ) ) {
      String reqString = req.toString( );
      if ( req.getRequest( ) != null && reqString != null ) {
        write( req.getRequest( ), reqString.replaceAll( "\",\\s*\n(\\s*})", "\"\n$1" ).replaceAll( "},\\s*\n(\\s*])", "}\n$1" ).replaceAll( "],\\s*\n(\\s*})", "]\n$1" ) );
      }
    }
    RequestInfo.flush( );
  }
  
  public static class RequestInfo {
    private String                                name;
    private Class                                 requestType;
    private Class                                 response;
    private Class                                 parent;
    private static final Map<String, RequestInfo> requestMap = new HashMap<String, RequestInfo>( );
    
    public static List<RequestInfo> getRequestInfoList( ) {
      return new ArrayList<RequestInfo>( requestMap.values( ) );
    }
    
    public static void flush( ) {
      requestMap.clear( );
      System.gc( );
    }
    
    public static RequestInfo get( final Class type ) {
      if ( !MSG_TYPE.isAssignableFrom( type ) || type.getSimpleName( ) == null ) {
        LOG.info( "IGNORING NON-REQUEST TYPE: " + type );
        return null;
      } else if ( type.getName( ).endsWith( "ResponseType" ) ) {
        final String typeKey = type.getSimpleName( ).replaceAll( "ResponseType\\Z", "" );
        if ( !requestMap.containsKey( typeKey ) ) {
          RequestInfo newRequest = new RequestInfo( ) {
            {
              setName( typeKey );
              setResponse( type );
            }
          };
          requestMap.put( typeKey, newRequest );
          return newRequest;
        } else {
          RequestInfo existing = requestMap.get( typeKey );
          existing.setResponse( type );
          return existing;
        }
      } else if ( type.getName( ).endsWith( "Type" ) ) {
        final String typeKey = type.getSimpleName( ).replaceAll( "Type\\Z", "" );
        if ( !requestMap.containsKey( typeKey ) ) {
          RequestInfo newRequest = new RequestInfo( ) {
            {
              setName( typeKey );
              setRequest( type );
              
            }
          };
          requestMap.put( typeKey, newRequest );
          return newRequest;
        } else {
          RequestInfo existing = requestMap.get( typeKey );
          existing.setRequest( type );
          return existing;
        }
      } else {
        LOG.info( "IGNORING NON-REQUEST TYPE: " + type );
        return null;
      }
    }
    
    @Override
    public int hashCode( ) {
      final int prime = 31;
      int result = 1;
      result = prime * result + ( ( this.name == null )
        ? 0
        : this.name.hashCode( ) );
      return result;
    }
    
    @Override
    public boolean equals( Object obj ) {
      if ( this == obj ) return true;
      if ( obj == null ) return false;
      if ( getClass( ) != obj.getClass( ) ) return false;
      RequestInfo other = ( RequestInfo ) obj;
      if ( this.name == null ) {
        if ( other.name != null ) return false;
      } else if ( !this.name.equals( other.name ) ) return false;
      return true;
    }
    
    public void setParent( Class c ) {
      if ( this.parent != null ) {
        return;
      } else {
        Class targetType = c;
        do {
          this.parent = targetType;
        } while ( ( targetType = targetType.getSuperclass( ) ) != null && !BaseMessage.class.equals( targetType.getSuperclass( ) )
                  && !java.lang.Object.class.equals( targetType.getSuperclass( ) ) );
      }
    }
    
    public String getName( ) {
      return this.name;
    }
    
    public void setName( String name ) {
      this.name = name;
    }
    
    public Class getRequest( ) {
      return this.requestType;
    }
    
    public void setRequest( Class request ) {
      this.requestType = request;
    }
    
    public Class getResponse( ) {
      return this.response;
    }
    
    public void setResponse( Class response ) {
      this.response = response;
    }
    
    private String processField( Field f ) {
//      "    {\n" +
//      "        \"optional\": true,\n" +
//      "        \"type\": \"array\",\n" +
//      "        \"member-type\": \"string\",\n" +
//      "        \"name\": [\"GroupName\"],\n" +
//      "    }\n" +
      String fieldInfo = "{\n" +
                         "}\n";
      return fieldInfo;
    }
    
    @Override
    public String toString( ) {
      if ( this.parent == null ) {
        if ( this.requestType != null ) {
          this.setParent( this.requestType );
        } else if ( this.response != null ) {
          this.setParent( this.response );
        }
      }
      JsonDescriptorGenerator.indent = 0;
      RootObjectTypeBinding binding = new RootObjectTypeBinding( this );
      String out = binding.process( );
      return out;
    }
    
    /**
     * @return the parent
     */
    public Class getParent( ) {
      return this.parent;
    }
  }
  
  public static TypeBinding getTypeBinding( Field field ) {
    Class itsType = field.getType( );
    if ( JsonDescriptorGenerator.isIgnored( field ) ) {
      return new NoopTypeBinding( field );
    } else if ( List.class.isAssignableFrom( itsType ) ) {
      Class listType = getTypeArgument( field );
      if ( listType == null ) {
        System.err.printf( "IGNORE: %-70s [type=%s] NO GENERIC TYPE FOR LIST\n", field.getDeclaringClass( ).getCanonicalName( ) + "." + field.getName( ),
                           listType );
        return new NoopTypeBinding( field );
      } else if ( typeBindings.containsKey( listType.getCanonicalName( ) ) ) {
        return new CollectionTypeBinding( field.getName( ), typeBindings.get( listType.getCanonicalName( ) ) );
      } else if ( BindingGenerator.DATA_TYPE.isAssignableFrom( listType ) ) {
        return new CollectionTypeBinding( field.getName( ), new ObjectTypeBinding( field.getName( ), listType ) );
      } else {
        System.err.printf( "IGNORE: %-70s [type=%s] LIST'S GENERIC TYPE DOES NOT CONFORM TO EucalyptusData\n", field.getDeclaringClass( ).getCanonicalName( )
                                                                                                               + "." + field.getName( ),
                           listType.getCanonicalName( ) );
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
      System.err.printf( "IGNORE: %-70s [type=%s] TYPE DOES NOT CONFORM TO EucalyptusData\n",
                         field.getDeclaringClass( ).getCanonicalName( ) + "." + field.getName( ), field.getType( ).getCanonicalName( ) );
      return new NoopTypeBinding( field );
    }
  }
  
  static class RootObjectTypeBinding extends TypeBinding {
    private RequestInfo requestInfo;
    
    public RootObjectTypeBinding( RequestInfo requestInfo ) {
      super( "object" );
      this.requestInfo = requestInfo;
    }
    
    @Override
    public String getTypeName( ) {
      return this.requestInfo.getRequest( ).getCanonicalName( );
    }
    
    public String process( ) {
      if ( this.requestInfo.getRequest( ) == null || this.requestInfo.getResponse( ) == null || this.requestInfo.getRequest( ).getCanonicalName( ) == null ) {
        System.out.println( "IGNORE: anonymous class: " + this.requestInfo.getRequest( ) );
        return null;
      } else {
        JsonDescriptorGenerator.request = true;
        this.beginElem( );
        this.attr( "schema-version", "0.1" );
        this.attr( "service-version", "\"http://msgs.eucalyptus.com/\"" );
        this.attr( "name", makeJSONName( this.requestInfo.getRequest( ).getSimpleName( ).replaceAll("Type\\Z","") ) );
        this.beginList( "parameters" );
        for ( Field f : getRecursiveFields( this.requestInfo.getParent( ), this.requestInfo.getRequest( ) ) ) {
          TypeBinding tb = getTypeBinding( f );
          if ( !( tb instanceof NoopTypeBinding ) ) {
//            System.out.printf( "JSONIZE:  %-70s [type=%s:%s]\n", f.getDeclaringClass( ).getCanonicalName( ) + "." + f.getName( ), tb.getTypeName( ),
//                               f.getType( ).getCanonicalName( ) );
            this.append( tb.toString( ) );
          }
        }
        this.endList( );
        JsonDescriptorGenerator.indent = 2;
        JsonDescriptorGenerator.request = false;
        this.beginList( "response" );
        List<Field> responseFields = getRecursiveFields( this.requestInfo.getParent( ), this.requestInfo.getResponse( ) );
        try {
          responseFields.add( BindingGenerator.MSG_TYPE.getDeclaredField( "_return" ) );
          responseFields.add( BindingGenerator.MSG_TYPE.getDeclaredField( "correlationId" ) );
        } catch ( SecurityException ex ) {
          LOG.error( ex, ex );
        } catch ( NoSuchFieldException ex ) {
          LOG.error( ex, ex );
        }
        for ( Field f : responseFields ) {
          TypeBinding tb = getTypeBinding( f );
          if ( !( tb instanceof NoopTypeBinding ) ) {
//            System.out.printf( "JSONIZE:  %-70s [type=%s:%s]\n", f.getDeclaringClass( ).getCanonicalName( ) + "." + f.getName( ), tb.getTypeName( ),
//                               f.getType( ).getCanonicalName( ) );
            this.append( tb.toString( ) );
          }
        }
        this.endList( );
        this.endFile( );
        JsonDescriptorGenerator.request = true;
      }
      return this.toString( );
    }
  }
  
  private static final Field[] EMPTYFIELDS = new Field[] {};
  
  public static List<Field> getRecursiveFields( Class parentType, Class type ) {
    List<Field> fields = new ArrayList<Field>( );
    Class me = type;
    while ( parentType.isAssignableFrom( me ) ) {
      fields.addAll( Arrays.asList( me.getDeclaredFields( ) ) );
      me = me.getSuperclass( );
    }
    return fields;
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
  
  static abstract class TypeBinding {
    private StringBuilder buf = new StringBuilder( );
    private final String  typeName;
    
    public TypeBinding( ) {
      this.typeName = "UNSETOMG";
    }
    
    public TypeBinding( String typeName ) {
      this.typeName = typeName;
    }
    
    public String getTypeName( ) {
      return this.typeName;
    }
    
    private TypeBinding reindent( int delta ) {
      JsonDescriptorGenerator.indent += delta;
      INDENT = "";
      for ( int i = 0; i < indent; i++ ) {
        INDENT += "    ";
      }
      return this;
    }
    
    private TypeBinding indent( ) {
      this.reindent( +1 ).append( INDENT );
      return this;
    }
    
    private TypeBinding outdent( ) {
      this.reindent( -1 ).append( INDENT );
      return this;
    }
    
    protected TypeBinding append( Object o ) {
      this.buf.append( "" + o );
      return this;
    }
    
    protected TypeBinding eolIn( ) {
      this.append( "\n" ).indent( );
      return this;
    }
    
    protected TypeBinding eolOut( ) {
      this.append( "\n" ).outdent( );
      return this;
    }
    
    protected TypeBinding eol( ) {
      this.append( "\n" ).append( INDENT );
      return this;
    }
    
    protected TypeBinding value( String name ) {
      this.beginElem( )
          .attr( "name", makeJSONName( name.replaceFirst( "_", "" ) ) )
          .attr( "type", "\"" + this.getTypeName( ) + "\"" )
          .endElem( );
      return this;
    }
    
    protected TypeBinding beginElem( ) {
      this.append( "{" ).eolIn( );
      return this;
    }
    
    protected TypeBinding endElem( ) {
      this.outdent( ).eol( ).append( "}," );
      return this;
    }
    
    protected TypeBinding endFile( ) {
      this.outdent( ).eolOut( ).append( "}" ).eol( );
      return this;
    }
    
    protected TypeBinding beginList( String listName ) {
      this.eol( ).append( "\"" ).append( listName ).append( "\": [" ).eolIn( );
      return this;
    }
    
    protected TypeBinding endList( ) {
      this.eolOut( ).append( "]," ).eol( );
      return this;
    }
    
    protected TypeBinding attr( String name, String value ) {
      this.eol( ).append( "\"" ).append( name ).append( "\": " ).append( value ).append( "," );
      return this;
    }
    
    public String toString( ) {
      String s = this.buf.toString( );
      this.buf = new StringBuilder( this.buf.capacity( ) );
      return s;
    }
    
    protected TypeBinding collection( String name ) {
      this.beginElem( );
      if ( JsonDescriptorGenerator.request  ) {
        this.attr( "name", makeJSONName( name.substring( 0, 1 ).toUpperCase( ) + name.substring( 1 ) ) );
      } else {
        this.attr( "name", makeJSONName( name, "item" ) );
      }
      this.attr( "type", "\"array\"" )
          .attr( "member-type", "\"" + this.getTypeName( ) + "\"" )
          .attr( "optional", "true" );
      this.endElem( );
      return this;
    }
  }
  
  public static boolean isIgnored( final Field field ) {
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
  
  static class IgnoredTypeBinding extends NoopTypeBinding {
    
    public IgnoredTypeBinding( Field field ) {
      super( field );
    }
  }
  
  static class NoopTypeBinding extends TypeBinding {
    private String name;
    private Class  type;
    
    public NoopTypeBinding( Field field ) {
      super( "object" );
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
  
  public static String makeJSONName( String... names ) {
    String out = "[ ";
    for ( String s : names ) {
      if( JsonDescriptorGenerator.request ) {
        out += "\"" + s.substring( 0, 1 ).toUpperCase( ) + s.substring( 1 ) + "\" ";
      } else {
        out += "\"" + s + "\" ";
      }
    }
    return ( out + "]" ).replaceAll( "\"\\s*\"", "\", \"" );
  }
  
  static class ObjectTypeBinding extends TypeBinding {
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
      this.beginElem( );
      if ( JsonDescriptorGenerator.request ) {
        this.attr( "name", makeJSONName( name.substring( 0, 1 ).toUpperCase( ) + name.substring( 1 ) ) );
      } else {
        this.attr( "name", makeJSONName( name, "item" ) );
      }
      this.attr( "type", "\"array\"" )
          .attr( "member-type", "\"" + this.getTypeName( ) + "\"" )
          .attr( "optional", "true" )
          .beginList( "properties" );
      for ( Field f : getRecursiveFields( BindingGenerator.DATA_TYPE, this.type ) ) {
        TypeBinding tb = getTypeBinding( f );
        if ( !( tb instanceof NoopTypeBinding ) ) {
          this.append( tb.toString( ) );
        }
      }
      this.endList( );
      this.endElem( );
      return this;
    }
    
    @Override
    public String getTypeName( ) {
      return "object";//this.type.getCanonicalName( );
    }
    
    public String toString( ) {
      this.beginElem( )
          .attr( "name", makeJSONName( this.name ) )
          .attr( "type", "\"object\"" )
          .beginList( "properties" );
      for ( Field f : getRecursiveFields( BindingGenerator.DATA_TYPE, this.type ) ) {
        TypeBinding tb = getTypeBinding( f );
        if ( !( tb instanceof NoopTypeBinding ) ) {
          this.append( tb.toString( ) );
        }
      }
      this.endList( ).endElem( );
      return super.toString( );
    }
    
  }
  
  static class CollectionTypeBinding extends TypeBinding {
    private TypeBinding type;
    private String      name;
    
    public CollectionTypeBinding( String name, TypeBinding type ) {
      this.name = name;
      this.type = type;
      LOG.debug( "Found list type: " + type.getClass( ).getCanonicalName( ) );
    }
    
    @Override
    public String getTypeName( ) {
      return this.type.getTypeName( );
    }
    
    @Override
    public String toString( ) {
      LOG.debug( "Found list type: " + this.type.getTypeName( ) + " for name: " + this.name );
      String ret = this.type.collection( this.name ).buf.toString( );
      this.type.collection( this.name ).buf = new StringBuilder( );
      return ret;
    }
    
  }
  
  static class IntegerTypeBinding extends TypeBinding {
    public IntegerTypeBinding( ) {
      super( "integer" );
    }
  }
  
  static class LongTypeBinding extends TypeBinding {
    public LongTypeBinding( ) {
      super( "long" );
    }
  }
  
  static class DoubleTypeBinding extends TypeBinding {
    public DoubleTypeBinding( ) {
      super( "double" );
    }
  }
  
  static class FloatTypeBinding extends TypeBinding {
    public FloatTypeBinding( ) {
      super( "float" );
    }
  }
  
  static class StringTypeBinding extends TypeBinding {
    public StringTypeBinding( ) {
      super( "string" );
    }
  }
  
  static class BooleanTypeBinding extends TypeBinding {
    public BooleanTypeBinding( ) {
      super( "boolean" );
    }
  }
  
  static class DateTimeTypeBinding extends TypeBinding {
    public DateTimeTypeBinding( ) {
      super( "datetime" );
    }
  }
}
