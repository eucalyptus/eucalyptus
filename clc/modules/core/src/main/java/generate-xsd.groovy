/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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

import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.jar.JarEntry
import java.util.jar.JarFile
import javax.xml.transform.OutputKeys
import javax.xml.transform.Source
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.sax.SAXTransformerFactory
import javax.xml.transform.stream.StreamResult
import org.xml.sax.InputSource
import com.eucalyptus.binding.HttpParameterMapping
import com.eucalyptus.component.ComponentId
import com.eucalyptus.component.annotation.ComponentMessage
import com.eucalyptus.system.Ats
import com.eucalyptus.util.Classes
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Sets
import com.google.common.io.Files
import com.google.common.primitives.Primitives
import edu.ucsb.eucalyptus.msgs.BaseMessage

boolean lameDebugFlag = System.getenv( ).containsKey( "DEBUG" ) ? true : false;
boolean lameVerboseFlag = System.getenv( ).containsKey( "VERBOSE" ) || lameDebugFlag ? true : false;
def log = { Object o, boolean eol = true ->
  if ( lameVerboseFlag && !eol ) {
    print o
  } else if ( lameVerboseFlag && eol ) {
    println o
  }
}

def usage = {
  System.out.println "Usage: ./devel/groovy.sh ./clc/modules/msgs/src/main/java/generate-xsd.groovy [jar directory]"
  System.out.println ""
  System.out.println " jar directory - Optional path to where the eucalyptus-*.jar files are."
  System.out.println "                 Defaults to System.getProperty(\"euca.src.dir\")}/clc/target"
  System.out.println ""
  System.out.println " Set either DEBUG or VERBOSE environment variables for more output."
  System.out.println ""
  System.exit(1)
}

if ( args.length > 1 ) {
  usage()
}

String dirName = args.length == 1 ? args[0] : "${System.getProperty("euca.src.dir")}/clc/target";
File libDir = new File( dirName );
dirName = libDir.getCanonicalPath( );
log "Using ${dirName}"
classList = []
libDir.listFiles( ).findAll{ it.getName().endsWith("jar") }.each { File f ->
  try {
    JarFile jar = new JarFile( f );
    log "Reading ${f.getCanonicalPath()}"
    List<JarEntry> jarList = Collections.list( jar.entries( ) );
    jarList.each { JarEntry j ->
      try {
        if ( j.getName( ).matches( ".*\\.class.{0,1}" ) ) {
          String classGuess = j.getName( ).replaceAll( "/", "." ).replaceAll( "\\.class.{0,1}", "" );
          try {
            classList.add( ClassLoader.systemClassLoader.loadClass(classGuess) );
          } catch ( final ClassNotFoundException e ) {
            log e.getMessage()
          }
        }
      } catch ( RuntimeException ex ) {
        ex.printStackTrace( );
        jar.close( );
        throw ex;
      }
    }
    jar.close( );
  } catch ( final Throwable e ) {
  }
}
if ( classList.isEmpty( ) ) {
  System.err.println "ERROR: Failed to find any classes in the given jar directory: ${dirName}"
  usage()
}

Multimap<Class<? extends ComponentId>,Class> messageTypes = HashMultimap.create();
Multimap<Class<? extends ComponentId>,Class> complexTypes = HashMultimap.create();
Multimap<Class<? extends ComponentId>,Class> collectionTypes = HashMultimap.create();
Set<Class> failedTypes = Sets.newHashSet( );
def componentFile = { Class<? extends ComponentId> compId -> new File("${compId.getSimpleName()}.wsdl") }
def writeComponentFile = { Class<? extends ComponentId> compId, String line ->
  if ( lameDebugFlag ) {
    log line
  }
  (componentFile(compId)).append( "${line}\n" );
}

xsTypeMap = [
  'java.lang.String':'string',
  'java.lang.Long':'long',
  'long':'long',
  'java.lang.Integer':'int',
  'int':'int',
  'java.lang.Boolean':'boolean',
  'boolean':'boolean',
  'java.lang.Double':'float',
  'double':'float',
  'java.lang.Float':'float',
  'float':'float',
]

def filterFieldNames = { Field f ->
  ( !f.getName( ).startsWith( "_" )
      && !f.getName( ).startsWith( "\$" )
      && !f.getName( ).equals( "metaClass" )
      ) ? true : false;
}
def filterPrimitiveType = { Type t -> ( t?.isPrimitive( ) || Primitives.isWrapperType( t ) || String.class.equals( t ) ) }
def filterPrimitives = { Field f -> filterFieldNames( f ) && filterPrimitiveType( f?.getType( ) ) }
def filterCollections = { Field f -> filterFieldNames( f ) && Collection.class.isAssignableFrom( f?.getType( ) ) }
def filterComplex = { Field f ->
  filterFieldNames( f ) && !filterPrimitives( f ) && !filterCollections( f ) && !Class.class.isAssignableFrom( f?.getType() )
}

def filterMessageTypes = { Class c ->
  BaseMessage.class.isAssignableFrom( c ) && !BaseMessage.class.equals( c ) && !c.toString( ).contains("\$")
}

def messageHasAnnotation = { Class c ->
  ats = Ats.inClassHierarchy( c );
  if ( !ats.has( ComponentMessage.class ) ) {
    failedTypes.add( c );
    false
  } else {
    true
  }
}

def transformToGenericType = { Field f ->
  Type t = f.getGenericType( );
  if ( t != null && t instanceof ParameterizedType ) {
    Type tv = ( ( ParameterizedType ) t ).getActualTypeArguments( )[0];
    if ( tv instanceof Class ) {
      return ( ( Class ) tv );
    }
  }
  return Object.class;
}

def processComplexType 
processComplexType = { Class compId, Class c ->
  println "${compId.simpleName}: ${c.simpleName}"
  complexTypes.put( compId, c );
  //TODO:GRZE fix the recursion issue here...
  c.getDeclaredFields( ).findAll( filterComplex ).findAll{ !it.getType().equals( c ) }.each { Field f ->
    processComplexType( compId, ( Class ) f.getType( ) );
  }
}

def processMessageType = { Class<? extends ComponentId> compId, Class c ->
  messageTypes.put( compId, c );
  c.getDeclaredFields( ).findAll( filterComplex ).each { Field f ->
    processComplexType( compId, f.getType( ) );
  }
  c.getDeclaredFields( ).findAll( filterCollections ).each { Field f ->
    Class fc = transformToGenericType( f );
    if ( fc != null && !filterPrimitiveType( fc ) ) {
      collectionTypes.put( compId, fc );
      complexTypes.put( compId, fc );
    }
  }
}

//find all the message types
classList.findAll( filterMessageTypes ).findAll( messageHasAnnotation ).each { Class c ->
  ats = Ats.inClassHierarchy( c );
  ComponentMessage comp = ats.get( ComponentMessage.class );
  Class<? extends ComponentId> compId = comp.value( );
  processMessageType( compId, c );
}

failedTypes.each { Class c ->
  //handle the unannotated {Walrus,Storage,etc.}ResponseType hierarchies by guessing their counterpart in the properly annotated hierarchy
  if ( !Ats.inClassHierarchy( c ).has( ComponentMessage.class ) && c.getCanonicalName( ).endsWith( "ResponseType" ) ) {
    try {
      Class request = Class.forName( c.getCanonicalName( ).replaceAll("ResponseType\$","Type") );
      ats = Ats.inClassHierarchy( request );
      ComponentMessage comp = ats.get( ComponentMessage.class );
      Class<? extends ComponentId> compId = comp.value( );
      processMessageType( compId, c );
    } catch ( Exception e ) {}
  }
}


messageTypes.keySet( ).each { Class<? extends ComponentId> compId ->
  if ( componentFile(compId).exists() ) {
    File f = componentFile(compId);
    if( f.exists( ) ) {
      f.delete();
    }
    f.write( """<?xml version="1.0" encoding="utf-8"?>\n""" )
  }
}


String wsdlHeader = """<definitions targetNamespace="http://msgs.eucalyptus.com/" xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:tns="http://msgs.eucalyptus.com/" xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/" xmlns="http://schemas.xmlsoap.org/wsdl/">
"""

def wsdlFooter = { Class<? extends ComponentId> compId ->
  """  <service name="${compId.getSimpleName()}">
    <port name="${compId.getSimpleName()}Port" binding="tns:${compId.getSimpleName()}Binding">
      <soap:address location="http://msgs.eucalyptus.com/" />
    </port>
  </service>
</definitions>""";
}

String schemaHeader = """  <types>
    <xs:schema xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:tns="http://msgs.eucalyptus.com/"
      targetNamespace="http://msgs.eucalyptus.com/" elementFormDefault="qualified">
"""

String schemaFooter = """    </xs:schema>
  </types>
"""

def renderSimpleField = { Field f -> """<xs:element name="${f.getName()}" type="xs:${xsTypeMap.get(f.getType().getCanonicalName())}" />""" }

def renderComplexField = { Field f -> """<xs:element name="${f.getName()}" type="tns:${f.getType().getSimpleName()}Type" />""" }

def renderCollectionField = { Field f -> """<xs:element name="${f.getName()}" type="tns:${transformToGenericType(f).getSimpleName()}SetType" minOccurs="0" />""" }

def renderCollectionWrapper = { Class c ->  """      <xs:complexType name="${c.getSimpleName()}SetType">
        <xs:sequence>
          <xs:element name="item" type="tns:${c.getSimpleName()}ItemType" minOccurs="0" maxOccurs="unbounded" />
        </xs:sequence>
      </xs:complexType>
""" }

def transformFieldToQuery = { Field f -> f.getName( ).substring( 0, 1 ).toUpperCase( ).concat( f.getName( ).substring( 1 ) ) }

def renderFieldReference = { Class c, Field f ->
  log("renderMessageType: ${c.getSimpleName()}.${f.getName()}",false)
  fats = Ats.from( f );
  PREFIX="        "
  comment = ""
  if ( fats.has( HttpParameterMapping.class ) ) {
    HttpParameterMapping mapping = fats.get( HttpParameterMapping.class );
    comment += "\n${PREFIX}<!-- Query: ${mapping.parameter( )} -->\n"
  } else {
    comment += "\n${PREFIX}<!-- Query: ${transformFieldToQuery(f)} -->\n"
  }
  if ( f == null ) {
    return ""
  } else if ( filterPrimitives( f ) ) {
    log " [primitive] ${f.getType()}"
    return "${comment}${PREFIX}${renderSimpleField(f)}"
  } else if ( filterCollections( f ) ) {
    log " [collection] ${transformToGenericType(f)}"
    return "${comment}${PREFIX}${renderCollectionField(f)}"
  } else if ( filterComplex( f ) ) {
    log " [complex] ${f.getType()}"
    return  "${comment}${PREFIX}${renderComplexField(f)}"
  }
}



def renderComplexType = { Class c ->
  String header = """      <xs:complexType name="${c.getSimpleName()}Type">
        <xs:sequence>"""
  String footer = """
        </xs:sequence>
      </xs:complexType>
"""
  Classes.ancestors( c ).each { Class a ->
    a.getDeclaredFields().findAll(filterFieldNames).each { Field f ->
      header += renderFieldReference( c, f )
    }
  }
  header + footer
}

def renderMessageType = { Class c ->
  String shortName = "${c.getSimpleName().replaceAll('Type$','')}";
  String header = """      <xs:element name="${shortName}" type="tns:${c.getSimpleName()}" />
      <xs:complexType name="${c.getSimpleName()}">
        <xs:sequence>"""
  String footer = """
        </xs:sequence>
      </xs:complexType>
"""
  Classes.ancestors( c ).each { Class a ->
    if ( filterMessageTypes( a ) ) {
      a.getDeclaredFields().findAll(filterFieldNames).each { Field f ->
        header += renderFieldReference( c, f )
      }
    }
  }
  header + footer
}

def renderMessagePart = { Class c ->
  if ( !c.getSimpleName( ).endsWith( "ResponseType" ) ) {
    """  <message name="${c.getSimpleName()}RequestMsg">
      <part name="${c.getSimpleName()}RequestMsgReq" element="tns:${c.getSimpleName()}" />
    </message>
    <message name="${c.getSimpleName()}ResponseMsg">
      <part name="${c.getSimpleName()}ResponseMsgResp" element="tns:${c.getSimpleName()}Response" />
    </message>
  """
  } else {
    ""
  }
}

def renderPortTypeHeader = { Class<? extends ComponentId> compId ->
  """  <portType name="${compId.getSimpleName()}PortType">"""
}

String portTypeFooter = "</portType>"

def renderPortTypeOperation = { Class c ->
  if ( !c.getSimpleName( ).endsWith( "ResponseType" ) ) {
    """    <operation name="${c.getSimpleName()}">
      <input message="tns:${c.getSimpleName()}RequestMsg" />
      <output message="tns:${c.getSimpleName()}ResponseMsg" />
    </operation>
"""
  } else {
    ""
  }
}

def renderBindingHeader = { Class<? extends ComponentId> compId ->
  """  <binding name="${compId.getSimpleName()}Binding" type="tns:${compId.getSimpleName()}PortType">
    <soap:binding style="document" transport="http://schemas.xmlsoap.org/soap/http" />
"""
}

def renderBindingOperation = { Class c -> """ 
    <operation name="${c.getSimpleName()}">
      <soap:operation soapAction="${c.getSimpleName()}" />
      <input>
        <soap:body use="literal" />
      </input>
      <output>
        <soap:body use="literal" />
      </output>
    </operation>
""" }
String bindingFooter = "  </binding>"

def generateSchema = { Class<? extends ComponentId> compId ->
  writeComponentFile( compId, schemaHeader );
  complexTypes.get( compId ).each { Class complexType ->
    log "generateSchema: ComplexType ${compId.getSimpleName()} ${complexType}"
    writeComponentFile( compId, renderComplexType( complexType ) );
  }
  collectionTypes.get( compId ).each { Class collectionType ->
    log "generateSchema: Collection  ${compId.getSimpleName()} ${collectionType}"
    writeComponentFile( compId, renderCollectionWrapper( collectionType ) );
  }
  messageTypes.get( compId ).each { Class msgType ->
    log "generateSchema: Message     ${compId.getSimpleName()} ${msgType}"
    writeComponentFile( compId, renderMessageType( msgType ) );
  }
  writeComponentFile( compId, schemaFooter );
}

def generateMessagePartsSection = { Class<? extends ComponentId> compId ->
  //Message Parts
  messageTypes.get( compId ).each { Class msgType ->
    writeComponentFile( compId, renderMessagePart( msgType ) );
  }
}

def generatePortTypeSection = { Class<? extends ComponentId> compId ->
  //Port Type Operations
  writeComponentFile( compId, renderPortTypeHeader( compId ) );
  messageTypes.get( compId ).each { Class msgType ->
    writeComponentFile( compId, renderPortTypeOperation( msgType ) );
  }
  writeComponentFile( compId, portTypeFooter );
}

def generateBindingSection = { Class<? extends ComponentId> compId ->
  //Binding section
  writeComponentFile( compId, renderBindingHeader( compId ) );
  messageTypes.get( compId ).each { Class msgType ->
    writeComponentFile( compId, renderBindingOperation( msgType ) );
  }
  writeComponentFile( compId, bindingFooter );
}

//log generateSchema( ConfigurationService.class )
messageTypes.keySet( ).each { Class<? extends ComponentId> compId ->
  writeComponentFile( compId, wsdlHeader );
  generateSchema( compId );
  generateMessagePartsSection( compId );
  generatePortTypeSection( compId );
  generateBindingSection( compId );
  writeComponentFile( compId, wsdlFooter( compId ) );
}
failedTypes.findAll{ !messageTypes.containsValue( it ) }.each { Class c ->
  log "ERROR: Failed to find @ComponentMessage in class hierarchy for: ${c}";
}

messageTypes.keySet( ).each { Class<? extends ComponentId> compId ->
  File compFile = componentFile(compId);
  File tmpFile = File.createTempFile(compFile.getName(),null);
  //  tmpFile.deleteOnExit( );
  log "Moving ${compFile.getCanonicalPath()} to ${tmpFile.getCanonicalPath()}"
  Files.move(compFile,tmpFile);
  
  componentFile(compId).withPrintWriter{ PrintWriter w ->
    log "Rewriting ${compFile.getCanonicalPath()}"
    w.write(tmpFile.text.replaceAll("(?m)^[ \t]*\r?\n", ""));
  }
}
