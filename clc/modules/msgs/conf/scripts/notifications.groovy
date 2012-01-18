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
