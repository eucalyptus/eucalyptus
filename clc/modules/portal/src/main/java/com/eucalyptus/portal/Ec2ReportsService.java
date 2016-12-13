/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.portal;

import com.eucalyptus.auth.AuthContextSupplier;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.portal.common.model.ViewInstanceUsageReportResponseType;
import com.eucalyptus.portal.common.model.ViewInstanceUsageReportType;
import com.eucalyptus.portal.common.model.ViewInstanceUsageResult;
import com.eucalyptus.portal.common.model.ViewReservedInstanceUtilizationReportResponseType;
import com.eucalyptus.portal.common.model.ViewReservedInstanceUtilizationReportType;
import com.eucalyptus.portal.common.model.ViewReservedInstanceUtilizationResult;
import com.eucalyptus.portal.common.policy.Ec2ReportsPolicySpec;
import com.eucalyptus.util.Exceptions;
import org.apache.log4j.Logger;

import static com.eucalyptus.util.RestrictedTypes.getIamActionByMessageType;

@SuppressWarnings( "unused" )
@ComponentNamed
public class Ec2ReportsService {
  private static final Logger logger = Logger.getLogger( Ec2ReportsService.class );

  public ViewInstanceUsageReportResponseType viewInstanceUsageReport(final ViewInstanceUsageReportType request)
          throws Ec2ReportsServiceException {
    final ViewInstanceUsageReportResponseType response = request.getReply();
    final Context context = checkAuthorized( );

    final ViewInstanceUsageResult result = new ViewInstanceUsageResult();
    result.setUsageReport("Start Time, End Time, TAG");
    response.setResult(result);
    return response;
  }

  public ViewReservedInstanceUtilizationReportResponseType viewReservedInstanceUtilizationReport(final ViewReservedInstanceUtilizationReportType request)
          throws Ec2ReportsServiceException {
    final ViewReservedInstanceUtilizationReportResponseType response = request.getReply();
    final Context context = checkAuthorized( );
    
    final ViewReservedInstanceUtilizationResult result = new ViewReservedInstanceUtilizationResult();
    result.setUtilizationReport("Start Time, End Time, TAG");
    response.setResult(result);
    return response;
  }

  private static Context checkAuthorized( ) throws Ec2ReportsServiceUnauthorizedException {
    final Context ctx = Contexts.lookup( );
    final AuthContextSupplier requestUserSupplier = ctx.getAuthContext( );
    if ( !Permissions.isAuthorized(
            Ec2ReportsPolicySpec.VENDOR_EC2REPORTS,
            "",
            "",
            null,
            getIamActionByMessageType( ),
            requestUserSupplier ) ) {
      throw new Ec2ReportsServiceUnauthorizedException(
              "UnauthorizedOperation",
              "You are not authorized to perform this operation." );
    }
    return ctx;
  }

  /**
   * Method always throws, signature allows use of "throw handleException ..."
   */
  private static Ec2ReportsServiceException handleException( final Exception e  ) throws Ec2ReportsServiceException {
    Exceptions.findAndRethrow( e, Ec2ReportsServiceException.class );

    logger.error( e, e );

    final Ec2ReportsServiceException exception = new Ec2ReportsServiceException( "InternalError", String.valueOf(e.getMessage()) );
    if ( Contexts.lookup( ).hasAdministrativePrivileges() ) {
      exception.initCause( e );
    }
    throw exception;
  }
}
