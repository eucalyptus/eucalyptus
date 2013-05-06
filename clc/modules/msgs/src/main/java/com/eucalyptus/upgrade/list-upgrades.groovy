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
import java.io.File;
import java.lang.reflect.Field
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.persistence.Column
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne
import javax.persistence.Table
import javax.persistence.Transient
import org.hibernate.annotations.Parent
import com.eucalyptus.system.Ats
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.upgrade.Upgrades.PostUpgrade;
import com.eucalyptus.upgrade.Upgrades.PreUpgrade;
import com.eucalyptus.util.Classes;
import com.google.common.base.Predicate;


File libDir = new File( "${System.getProperty("euca.src.dir")}/clc/target" );
classList = []
System.err.print "Reading ${libDir.getAbsolutePath()}"
libDir.listFiles( ).each { File f ->
  System.err.print "."
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
System.err.print "\nRead from ${libDir.listFiles( ).length} jars [done]\n"
def upgradePre = [] as SortedSet
def upgradePost = [] as SortedSet
def upgradeEntity = [] as SortedSet

def testClass = { Class c, Class cast ->
  try {
    cast.cast( Classes.newInstance( c ) );
    return ""
  } catch ( ClassCastException ex ) {
    return ex.getMessage( );
  }
}
classList.each { Class c ->
  try {
    ats = Ats.from( c );
    if ( ats.has( PreUpgrade.class ) ) {
      PreUpgrade pre = ats.get( PreUpgrade.class )
      upgradePre.add( "@PreUpgrade    ${pre.since()}\t${pre.value( ).name}\t${c} ${testClass(c,Callable.class)}" )
    }
    if ( ats.has( PostUpgrade.class ) ) {
      PostUpgrade post = ats.get( PostUpgrade.class )
      upgradePost.add( "@PostUpgrade   ${post.since()}\t${post.value( ).name}\t${c} ${testClass(c,Callable.class)}" )
    }
    if ( ats.has( EntityUpgrade.class ) ) {
      EntityUpgrade entity = ats.get( EntityUpgrade.class )
      upgradeEntity.add( "@EntityUpgrade ${entity.since()}\t${entity.value( ).name}\t${c} ${testClass(c,Predicate.class)}" )
      entity.entities( ).each{ upgradeEntity.add( "@EntityUpgrade ${entity.since()}\t${entity.value( ).name}\t${c} ${it} " ) }
    }
  } catch ( Throwable t ) {
    println "${c} : ${t.getMessage( )}";
    t.printStackTrace( )
  }
}
upgradePre.each{ println it }
upgradePost.each{ println it }
upgradeEntity.each{ println it }

