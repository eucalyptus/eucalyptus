package com.eucalyptus.util.fsm;

import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Component;
import com.eucalyptus.util.HasName;
import com.google.common.base.Predicate;

public class Transitions {
  private static Logger                        LOG  = Logger.getLogger( Transitions.class );
                                                    
  public static <P extends HasName<P>> TransitionListener<P> createListener( final Predicate<P> p ) {
    return new TransitionListener<P>() {

      @Override
      public boolean before( P parent ) {
        return true;
      }

      @Override
      public void leave( P parent ) {
        try {
          p.apply( parent );
        } catch ( Throwable ex ) {
          LOG.error( ex , ex );
        }
      }

      @Override
      public void enter( P parent ) {}

      @Override
      public void after( P parent ) {}};
  }
}
