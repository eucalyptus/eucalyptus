/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.ws;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.msgs.*;
import org.apache.log4j.Logger;
import org.mule.RequestContext;

import com.eucalyptus.util.EucalyptusCloudException;

public class VmControl {

  private static Logger LOG = Logger.getLogger( VmControl.class );

  public VmAllocationInfo allocate( VmAllocationInfo vmAllocInfo ) throws EucalyptusCloudException {
    return vmAllocInfo;
  }

  public DescribeInstancesResponseType DescribeInstances( DescribeInstancesType msg ) throws EucalyptusCloudException {
    DescribeInstancesResponseType reply = ( DescribeInstancesResponseType ) msg.getReply();
    try {
      reply.setReservationSet( SystemState.handle( msg.getUserId(), msg.getInstancesSet(), msg.isAdministrator() ) );
    }
    catch ( Exception e ) {
      LOG.error( e );
      LOG.debug( e,e );
      throw new EucalyptusCloudException( e.getMessage() );
    }
    return reply;
  }

  public TerminateInstancesResponseType TerminateInstances( TerminateInstancesType msg ) throws EucalyptusCloudException {
    TerminateInstancesResponseType reply = ( TerminateInstancesResponseType ) msg.getReply();
    try {
      return SystemState.handle( msg );
    }
    catch ( Exception e ) {
      LOG.error( e );
      LOG.debug( e,e );
      throw new EucalyptusCloudException( e.getMessage() );
    }
  }

  public RebootInstancesResponseType RebootInstances( RebootInstancesType msg ) throws EucalyptusCloudException {
    try {
      return SystemState.handle( msg );
    }
    catch ( Exception e ) {
      LOG.error( e );
      LOG.debug( e,e );
      throw new EucalyptusCloudException( e.getMessage() );
    }
  }

  public void GetConsoleOutput( GetConsoleOutputType request ) throws EucalyptusCloudException {
    try {
      SystemState.handle( request );
      RequestContext.getEventContext().setStopFurtherProcessing( true );
    }
    catch ( Exception e ) {
      LOG.error( e );
      LOG.debug( e, e );
      throw new EucalyptusCloudException( e.getMessage() );
    }
  }

}
