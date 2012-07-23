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

import com.eucalyptus.component.ServiceConfiguration
import com.eucalyptus.component.Faults.FaultRecord
import com.eucalyptus.scripting.Groovyness
import com.eucalyptus.util.Exceptions
import com.google.common.base.Throwables

summary = ""
details = ""
faults.each{ FaultRecord f ->
  ServiceConfiguration s = Groovyness.expandoMetaClass(f.getServiceConfiguration( ));
  summary += """
- ${s.getFullName( )} ${f.getTransitionRecord( ).getRule( ).getFromState( )}->${f.getFinalState( )} ${f.getError( ).getTimestamp( )}
  ${Throwables.getRootCause(f.getError( )).getMessage( )}
"""
  details += """
- ${s.getFullName( )} ------------
  ${f.getTransitionRecord( )}
  ${Exceptions.causeString(f.getError())}
"""
}
content = """
Impacted Services Summary
=========================
${summary}

Details
=======
${details}

""".toString()
