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
import edu.emory.mathcs.backport.java.util.Collections;

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
    BRANCH( "branch-nick" );
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
        final List<URL> propFiles = Collections.list( ClassLoader.getSystemResources( "euca-version.properties" ) );
        for ( final URL u : propFiles ) {
          final Properties temp = new Properties( );
          final InputStream in = Resources.newInputStreamSupplier( u ).getInput( );
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
}
