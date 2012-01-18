import com.eucalyptus.component.Components

Components.list().collect{ c -> "\n" + c.services().collect{ s -> s.toString() } }
