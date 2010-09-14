package com.eucalyptus.util.fsm;

import com.eucalyptus.util.HasName;
import com.eucalyptus.util.async.Callback.Completion;

public class SimpleTransitionListener<P extends HasName<P>> implements TransitionListener<P> {
  /**
   * @see com.eucalyptus.util.fsm.TransitionListener#leave(com.eucalyptus.util.HasName, com.eucalyptus.util.async.Callback.Completion)
   * @param parent
   * @param transitionCallback
   */
  @Override
  public void leave( P parent, Completion transitionCallback ) {}

  /**
   * @see com.eucalyptus.util.fsm.TransitionListener#before(com.eucalyptus.util.HasName)
   * @param parent
   * @return
   */
  @Override
  public boolean before( P parent ) {
    return true;
  }

  /**
   * @see com.eucalyptus.util.fsm.TransitionListener#enter(com.eucalyptus.util.HasName)
   * @param parent
   */
  @Override
  public void enter( P parent ) {}

  /**
   * @see com.eucalyptus.util.fsm.TransitionListener#after(com.eucalyptus.util.HasName)
   * @param parent
   */
  @Override
  public void after( P parent ) {}

}
