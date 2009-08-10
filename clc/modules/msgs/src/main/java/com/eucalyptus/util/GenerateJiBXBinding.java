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
      if ( className.startsWith( "JiBX_" ) ) continue;
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
      System.out.println("processing "+clazz+"  field="+f.getName( ));
      processField( f );
    }
    indent--;
    append( "</mapping>" );
  }

  public static void processField( Field field ) {
    Class itsType = field.getType( );
    if ( field.getName( ).startsWith( "__" ) || field.getName( ).startsWith( "class$" ) || field.getName( ).equals( "metaClass" ) || field.getName( ).startsWith( "JiBX_" ) || itsType.getSimpleName( ).endsWith( "Channel" ) || itsType.getSimpleName( ).endsWith( "HttpResponseStatus" ) ) {
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
