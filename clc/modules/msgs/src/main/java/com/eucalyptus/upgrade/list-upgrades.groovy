/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
import com.google.common.collect.Lists;


File libDir = new File( "${System.getProperty("euca.src.dir")}/clc/target" );
List<Class> classList = Lists.newArrayList()
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
    Ats ats = Ats.from( c );
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

