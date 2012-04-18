import com.eucalyptus.scripting.Groovyness
import com.eucalyptus.address.Address
import com.eucalyptus.address.Addresses
import com.eucalyptus.scripting.Groovyness
import com.eucalyptus.vm.VmInstance
import com.eucalyptus.vm.VmInstances


return Addresses.getInstance( ).listValues( ).collect { Address it ->
  Address addr = Groovyness.expandoMetaClass( it );
  String ret = addr.toString( );
  if ( addr.isAssigned( ) ) {
    try {
      VmInstance vm = Groovyness.expandoMetaClass( VmInstances.cachedLookup( addr.getInstanceId( ) ) );
      ret += " => " + vm.getInstanceId( ) + " " + vm.getState( ) + " " + vm.getPrivateAddress( );
    } catch ( Exception e ) {
      ret += " => " + e.class.simpleName;
    }
  }
  ret
};