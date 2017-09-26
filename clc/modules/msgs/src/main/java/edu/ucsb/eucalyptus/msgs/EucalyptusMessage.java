/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package edu.ucsb.eucalyptus.msgs;

import java.io.Serializable;
import com.eucalyptus.component.annotation.ComponentMessage;
import com.eucalyptus.component.id.Eucalyptus;

/**
 * GRZE:WARN: anything inheriting from this is (and /should be/) treated as in the 'ec2' vendor namespace as far as the IAM implementation is concerned.
 * There is no reason to annotate /any/ message which inherits from this class:
 * - to get vendor namespace use ComponentId.getVendorName()
 * - to get action use
 */
@ComponentMessage( Eucalyptus.class )
public class EucalyptusMessage extends BaseMessage implements Cloneable, Serializable {

  public EucalyptusMessage( ) {
    super( );
  }

  public EucalyptusMessage( EucalyptusMessage msg ) {
    this( );
    regarding( msg );
    regardingUserRequest( msg );
    this.setUserId( msg.getUserId( ) );
    this.setEffectiveUserId( msg.getEffectiveUserId( ) );
    this.setCorrelationId( msg.getCorrelationId( ) );
  }

  public EucalyptusMessage( final String userId ) {
    this( );
    this.setUserId( userId );
    this.setEffectiveUserId( userId );
  }
}
