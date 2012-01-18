import com.eucalyptus.component.ServiceConfiguration
import com.eucalyptus.component.Faults.FaultRecord
import com.eucalyptus.scripting.Groovyness
import com.eucalyptus.util.Exceptions

summary = ""
details = ""
faults.each{ FaultRecord f ->
  ServiceConfiguration s = Groovyness.expandoMetaClass(f.getServiceConfiguration( ));
  summary += """
- ${s.getFullName( )} ${f.getTransitionRecord( ).getRule( ).getFromState( )}->${f.getFinalState( )} 
  ${f.getError( ).getMessage( )}
"""
  details += """
- ${s.getFullName( )} ------------
  ${f.getTransitionRecord( )}
  ${Exceptions.string(f.getError())}
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
