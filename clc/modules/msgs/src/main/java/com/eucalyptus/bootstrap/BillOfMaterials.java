/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Logger;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import java.util.Collections;

/**
 * Purpose: Show version information about the tree used to build the running software.
 * 
 * <b>Generating bill of materials</b>
 * 
 * <pre>
 * bzr version-info \
 *    --include-file-revisions  
 *    --rio | \
 * awk '
 *    BEGIN{path=0} 
 *    $1 == "file-revisions:" {path=1} 
 *    $1 == "path:" && path == 1 {p=$2;getline;print p,$2} 
 *    path == 0 {print}'
 * </pre>
 * 
 * Usage: bzr version-info [LOCATION]
 * 
 * Options:
 * --all Include all possible information.
 * -v, --verbose Display more information.
 * --include-history Include the revision-history.
 * --check-clean Check if tree is clean.
 * -q, --quiet Only display errors and warnings.
 * --include-file-revisions
 * Include the last revision for each file.
 * --template=ARG Template for the output.
 * --usage Show usage message and options.
 * -h, --help Show help message.
 * 
 * format:
 * --format=ARG Select the output format.
 * --custom Version info in Custom template-based format.
 * --python Version info in Python format.
 * --rio Version info in RIO (simple text) format (default).
 * 
 * Description:
 * You can use this command to add information about version into
 * source code of an application. The output can be in one of the
 * supported formats or in a custom format based on a template.
 * 
 * For example:
 * 
 * bzr version-info --custom \
 * --template="#define VERSION_INFO \"Project 1.2.3 (r{revno})\"\n"
 * 
 * will produce a C header file with formatted string containing the
 * current revision number. Other supported variables in templates are:
 * 
 * {date} - date of the last revision
 * {build_date} - current date
 * {revno} - revision number
 * {revision_id} - revision id
 * {branch_nick} - branch nickname
 * {clean} - 0 if the source tree contains uncommitted changes,
 * otherwise 1
 */
public class BillOfMaterials {
  private static Logger LOG = Logger.getLogger( BillOfMaterials.class );
  
  enum RequiredFields {
    VERSION( "" ) {
      
      @Override
      public String getValue( ) {
        return System.getProperty( "euca.version", "ERROR: no version information available." );
      }
    },
    REVISIONID( "revision-id" ),
    DATE( "date" ),
    BUILD( "build-date" ),
    REVNO( "revno" ),
    BRANCH( "branch-nick" ),
    EXTRA_VERSION( "extra-version" );
    private final String key;
    
    private RequiredFields( final String key ) {
      this.key = key;
    }
    
    public String getValue( ) {
      if ( !BillOfMaterials.loadProps( ).containsKey( this.key ) ) {
        return VERSION.getValue( );
      } else {
        return BillOfMaterials.loadProps( ).get( this.key );
      }
    }
  }
  
  /**
   * Simple defaults to be used in lieu of explicit properties when they fail to load. This is
   * <b>definitely not</b> a candidate for autoconf generation.
   */
  private static Map<String, String> loadedProps;
  
  @SuppressWarnings( "unchecked" )
  static synchronized Map<String, String> loadProps( ) {
    if ( ( loadedProps == null ) || loadedProps.isEmpty( ) ) {
      loadedProps = Maps.newHashMap( );
      try {
        final List<URL> propFiles = Collections.list( ClassLoader.getSystemResources( "version.properties" ) );
        for ( final URL u : propFiles ) {
          final Properties temp = new Properties( );
          final InputStream in = Resources.asByteSource( u ).openStream( );
          try {
            temp.load( in );
          } finally {
            in.close( );
          }
          loadedProps.putAll( Maps.fromProperties( temp ) );
        }
      } catch ( final IOException ex ) {
        LOG.error( ex, ex );
      }
      return loadedProps;
    } else {
      return loadedProps;
    }
  }
  
  public static String getRevisionDate( ) {
    return RequiredFields.DATE.getValue( );
  }
  
  public static String getBuildDate( ) {
    return RequiredFields.BUILD.getValue( );
  }
  
  public String getRevisionNumber( ) {
    return RequiredFields.REVNO.getValue( );
  }
  
  public String getRevisionId( ) {
    return RequiredFields.REVISIONID.getValue( );
  }
  
  public String getRevisionId( final String fileName ) {
    return RequiredFields.REVISIONID.getValue( );
  }
  
  public String getBranchNick( ) {
    return RequiredFields.BRANCH.getValue( );
  }
 
  public static String getVersion( ) {
    return RequiredFields.VERSION.getValue( );
  }

  public static String getExtraVersion( ) {
    return RequiredFields.EXTRA_VERSION.getValue( );
  }
}
