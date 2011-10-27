import com.eucalyptus.util.fsm.TransitionHandler;

import com.eucalyptus.component.Component
import com.eucalyptus.component.ServiceConfiguration
import com.eucalyptus.component.ServiceTransitions
import com.eucalyptus.component.Topology
import com.eucalyptus.component.id.Walrus
import com.eucalyptus.util.fsm.StateMachine
import com.eucalyptus.util.fsm.TransitionHandler
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ImmutableList
import com.google.common.collect.Multimap

//ServiceConfiguration config = Topology.lookup( Walrus.class );
//StateMachine fsm = config.getStateMachine( );
//ImmutableList<TransitionHandler> transitions = fsm.getTransitions( );
//Multimap<Component.State,Component.State> s2s = ArrayListMultimap.create( );
//transitions.collect{ TransitionHandler it -> it.g }

getPath = { statePath ->
  Component.State.values().collect{ state ->
    [ ("\n" + state): statePath(state).collect{ "-> " + it.name()} ]
  }
}
result = [:]
statePath = { fromState ->
  ServiceTransitions.pathToEnabled(fromState)
}
result[Component.State.ENABLED] = getPath(statePath);
statePath = { fromState ->
  ServiceTransitions.pathToDisabled(fromState)
}
result[Component.State.DISABLED] = getPath(statePath);
statePath = { fromState ->
  ServiceTransitions.pathToStarted(fromState)
}
result[Component.State.NOTREADY] = getPath(statePath);
statePath = { fromState ->
  ServiceTransitions.pathToLoaded(fromState)
}
result[Component.State.LOADED] = getPath(statePath);
statePath = { fromState ->
  ServiceTransitions.pathToInitialized(fromState)
}
result[Component.State.INITIALIZED] = getPath(statePath);
statePath = { fromState ->
  ServiceTransitions.pathToPrimordial(fromState)
}
result[Component.State.PRIMORDIAL] = getPath(statePath);
statePath = { fromState ->
  ServiceTransitions.pathToBroken(fromState)
}
result[Component.State.BROKEN] = getPath(statePath);
result.collect{ "\n\n" + it.getKey() + "\n" + it.getValue() }