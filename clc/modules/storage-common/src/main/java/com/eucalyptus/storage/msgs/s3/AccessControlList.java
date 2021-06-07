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
package com.eucalyptus.storage.msgs.s3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class AccessControlList extends EucalyptusData {

  private ArrayList<Grant> grants = new ArrayList<Grant>( );

  @SuppressWarnings( "unchecked" )
  @Override
  public boolean equals( Object o ) {
    if ( o == null || !( o instanceof AccessControlList ) ) {
      return false;
    }


    AccessControlList other = (AccessControlList) o;
    //Do an unordered comparison, sort clones of each first
    List<Grant> myGrants = (List<Grant>) this.grants.clone( );
    Collections.sort( myGrants );
    List<Grant> otherGrants = (List<Grant>) other.grants.clone( );
    Collections.sort( otherGrants );
    return myGrants.equals( otherGrants );
  }

  @SuppressWarnings( "unchecked" )
  @Override
  public int hashCode( ) {
    List<Grant> myGrants = (List<Grant>) this.grants.clone( );
    Collections.sort( myGrants );
    return Objects.hash( myGrants );
  }

  public ArrayList<Grant> getGrants( ) {
    return grants;
  }

  public void setGrants( ArrayList<Grant> grants ) {
    this.grants = grants;
  }
}
