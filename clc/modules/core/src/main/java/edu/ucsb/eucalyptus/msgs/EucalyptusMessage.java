/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
