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
 ************************************************************************/

import java.io.*;
import org.apache.tools.ant.DemuxOutputStream;
import com.google.common.io.Files;

def moduleBasePath = "${project.baseDir}/modules";
def moduleDirs = new File(moduleBasePath).listFiles( { it.isDirectory() } as FileFilter ).collect{ it.getName() }
def modulesList = new File("${moduleBasePath}/module-inc.order");
def antTarget=project.properties.antTarget
def ant = new AntBuilder()
def modulesBuild = []
def modulesIgnore = []
def buildOrder = []
def doBuild = { module ->
  if ( new File("${moduleBasePath}/${module}/build.xml").exists() ) {
    println ( "CALL-MODULE-TARGET ${module} ${antTarget}" )
    ant.ant(dir:"modules/${module}",inheritall:'false'){
      target(name:"${antTarget}")
    }
  } else {
    println ( "SKIP-MODULE-TARGET ${module} ${antTarget}" )
  }
}

modulesList.eachLine{
  if ( it.startsWith("#") ) {
    moduleDirs.remove(it.substring(1).trim())
  } else {
    moduleDirs.remove(it)
    buildOrder += it;
  }
}
buildOrder.addAll( moduleDirs )
println "==== BUILD ORDER ===="
buildOrder.each{ print "=> ${it} "}
println "\n====================="
buildOrder.each{ doBuild(it) }
