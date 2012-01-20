import com.eucalyptus.component.Components

Components.list().collect{ c ->  c.services().collect{ s -> "\n" + s.toString() } }
