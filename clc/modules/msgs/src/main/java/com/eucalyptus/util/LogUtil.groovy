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

package com.eucalyptus.util;
import org.apache.log4j.Logger;

public class LogUtil {
  private static Logger LOG = Logger.getLogger( LogUtil.class );
  private static String LONG_BAR = "=============================================================================================================================================================================================================";
  private static String MINI_BAR = "-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------";
  public static String FAIL = "FAIL"
  public static LogUtil singleton = new LogUtil();

  public static String header( String message ) {
    return String.format( "%80.80s\n%s\n%1\$80.80s", LONG_BAR, message );
  }
  
  public static String errorheader( String message, Exception e ) {
    String msg = String.format( "%80.80s\n%s\n%1\$80.80s", MINI_BAR, message ).replaceAll("-","*");
  }
  public static String subheader( String message ) {
    return String.format( "%80.80s\n%s\n%1\$80.80s", MINI_BAR, message );
  }

  public static String dumpObject( Object o ) {
    try {
      return o.dump().replaceAll("<","[").replaceAll(">","]").replaceAll("[\\w\\.]+\\.(\\w+)@\\w*", { Object[] it -> it[1] }).replaceAll("class:class [\\w\\.]+\\.(\\w+),", { Object[] it -> it[1] });
    } catch( Exception e ) {
      return ""+o;
    }
  }
  
  public static String lineObject( Object o ) {
    return String.format("%-200.200s",o.dump().replaceFirst("<\\w*.\\w*@","<"));
  }
  
  public static LogUtil log( Object message ) {
    LOG.info( message );
    return singleton;
  }
  public static LogUtil logHeader( Object message ) {
    LOG.info( LogUtil.subheader( message ) );
    return singleton;
  }
}
