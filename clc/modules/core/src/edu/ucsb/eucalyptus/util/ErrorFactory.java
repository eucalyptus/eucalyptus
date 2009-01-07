/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.util;

import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import org.apache.log4j.Logger;

/**
 * User: decker
 * Date: May 11, 2008
 * Time: 1:16:52 PM
 */
public class ErrorFactory {

  private static Logger LOG = Logger.getLogger( ErrorFactory.class );

  public static EucalyptusCloudException makeFault(Exception e )
  {
    return makeFault( e.getMessage() );
  }

  public static EucalyptusCloudException makeFault( String feature, String state, String depends )
  {
    EucalyptusCloudException blah = new EucalyptusCloudException( feature +"\nSTATE: " + state + "\nDEPENDS: " + depends );
    return blah;
  }
  public static EucalyptusCloudException makeFault( String errorMessage, String hint )
  {
    EucalyptusCloudException blah = new EucalyptusCloudException( errorMessage +"\nHINT: " + hint );
    return blah;
  }
  public static EucalyptusCloudException makeFault( String errorMessage )
  {
    EucalyptusCloudException blah = new EucalyptusCloudException( errorMessage );
    return blah;
  }
}
