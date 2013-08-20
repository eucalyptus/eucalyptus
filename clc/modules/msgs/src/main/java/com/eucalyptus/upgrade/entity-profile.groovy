/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 ************************************************************************/

import com.eucalyptus.system.Ats
import org.hibernate.annotations.Parent

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.jar.JarEntry
import java.util.jar.JarFile
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Embeddable
import javax.persistence.Embedded
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.MappedSuperclass
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.Table
import javax.persistence.Transient

File libDir = new File( "${System.getProperty("euca.src.dir")}/clc/target" );
classList = []
libDir.listFiles( ).each { File f ->
  System.err.println "Reading ${f.getAbsolutePath()}"
  try {
    JarFile jar = new JarFile( f );
    List<JarEntry> jarList = Collections.list( jar.entries( ) );
    jarList.each { JarEntry j ->
      try {
        if ( j.getName( ).matches( ".*\\.class.{0,1}" ) ) {
          String classGuess = j.getName( ).replaceAll( "/", "." ).replaceAll( "\\.class.{0,1}", "" );
          try {
            classList.add( ClassLoader.systemClassLoader.loadClass(classGuess) );
          } catch ( final ClassNotFoundException e ) {
            println e.getMessage()
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
classList.each { Class c ->
  try {
    ats = Ats.from( c );
    if ( ats.has( javax.persistence.Entity.class ) 
      || ats.has( javax.persistence.Embeddable.class ) 
      || ats.has( MappedSuperclass.class ) ) {
      Table table = ats.get( Table.class );
      String tableName = table == null ? c.getName() : table.name();
      if( table == null ) {
        table = Ats.inClassHierarchy( c ).get( Table.class );
        if ( table != null ) {
          tableName = table.name( );
        } else if ( ats.has( Embeddable.class ) ) {
          tableName = "embedded";
        } else if ( ats.has( MappedSuperclass.class ) ) {
          tableName = "superclass";
        } else {
          System.err.println "Missing @Table: ${c.getCanonicalName( )}"
        }
      }
      candidateFields = c.declaredFields.findAll{ Field f ->
        !Modifier.isStatic( f.getModifiers( ) ) &&
            !Ats.from( f ).has( Transient.class ) &&
            !"metaClass".equals( f.getName( ) )
      }
      candidateFields.each{ Field f ->
        if ( Ats.from( f ).has( ManyToOne.class ) ) {
          println "${tableName} ${c.getCanonicalName( )} ${f.getName()} n-1-RELATION";
        } else if ( Ats.from( f ).has( ManyToMany.class ) ) {
          println "${tableName} ${c.getCanonicalName( )} ${f.getName()} n-n-RELATION";
        } else if ( Ats.from( f ).has( OneToMany.class ) ) {
          println "${tableName} ${c.getCanonicalName( )} ${f.getName()} 1-n-RELATION";
        } else if ( Ats.from( f ).has( OneToOne.class ) ) {
          println "${tableName} ${c.getCanonicalName( )} ${f.getName()} 1-1-RELATION";
        } else if ( Ats.from( f ).has( ElementCollection.class ) ) {
          println "${tableName} ${c.getCanonicalName( )} ${f.getName()} ELEMENTCOLLECTION";
        } else if ( Ats.from( f ).has( Embedded.class ) ) {
          println "${tableName} ${c.getCanonicalName( )} ${f.getName()} EMBEDDED";
        } else if ( Ats.from( f ).has( Parent.class ) ) {
          println "${tableName} ${c.getCanonicalName( )} ${f.getName()} PARENT";
        } else if ( Ats.from( f ).has( Column.class ) ) {
          println "${tableName} ${c.getCanonicalName( )} ${Ats.from( f ).get( Column.class ).name( )}";
        } else if ( Collection.class.isAssignableFrom(f.getType( )) ) {
          System.err.println "Missing relation information: ${c.getCanonicalName( )}.${f.getName( )}";
        } else {
          System.err.println "Missing @Column: ${c.getCanonicalName( )}.${f.getName( )}";
          println "${tableName} ${c.getCanonicalName( )} ${f.getName( )}";
        }
      }
    }
  } catch ( Throwable t ) {
    println "${c} : ${t.getMessage( )}";
  }
}

