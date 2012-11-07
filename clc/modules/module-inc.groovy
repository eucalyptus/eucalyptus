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

import java.io.*;
import org.apache.tools.ant.DemuxOutputStream;
import com.google.common.io.Files;

/*
 * Implements the following semantics:
 * The ordering in a module-inc.order or submodule-inc.order file
 * only guarantees 'builds-after' semantics.
 * If entry Y is listed after entry X in module-inc.order or
 * in a submodule-inc.order file then entry Y will be built after
 * the module X.
 * 
 * module-inc.order is processed first and after each entry is added to
 * the build order list it is checked for a submodule-inc.order file in
 * the module directory and that file is then processed.
 * 
 * It is possible that if duplicate entries exist in the set of 
 * entries in all submodule-inc.order and module-inc.order files then
 * a build-after relation could be broken, this is up to the user to
 * resolve.
 * 
 */

moduleBasePath = "${project.baseDir}/modules";
moduleDirs = new File(moduleBasePath).listFiles( { it.isDirectory() } as FileFilter ).collect{ it.getName() }
modulesList = new File("${moduleBasePath}/module-inc.order");
def antTarget=project.properties.antTarget
def ant = new AntBuilder()
def modulesBuild = []
def modulesIgnore = []
buildOrder = []
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

//Force allows for cases where the user is explicitly overwritting any previous ordering
// that may have occured by coincidence. Force should = true when processing lines from
// an ordering file
def processModuleEntry(String moduleEntry, boolean force) {
	if(moduleEntry.startsWith("#")) {
		moduleDirs.remove(moduleEntry.substring(1).trim())
		buildOrder.remove(moduleEntry.substring(1).trim())
	} else {
		if(moduleDirs.contains(moduleEntry) || force) {
			moduleDirs.remove(moduleEntry)
			buildOrder.remove(moduleEntry)
			buildOrder += moduleEntry;

			def submod = new File("${moduleBasePath}/${moduleEntry}/submodule-inc.order")
			if(submod.exists()) {
				submod.eachLine{
					//Force changes since this is explicitly ordered by user
					processModuleEntry( it , true)
				}
			}
		}
	}
}

//Process the main module-inc.order list, force changes since these are explicit
modulesList.eachLine{
	processModuleEntry( it , true)
}

def remainingList = []
remainingList.addAll(moduleDirs)

//Changes are not forced since these are not explicit by the user
remainingList.each{
	processModuleEntry( it , false)
}

buildOrder.addAll( moduleDirs )
println "==== BUILD ORDER ===="
buildOrder.each{ print "=> ${it} "}
println "\n====================="
buildOrder.each{ doBuild(it) }
