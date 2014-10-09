package com.eucalyptus.cloudformation.workflow.steps;

import com.google.common.base.Function;

import javax.annotation.Nullable;

/**
 * Created by ethomas on 10/2/14.
 */
public enum StepTransform implements Function<Nameable, String> {
  INSTANCE {
    @Nullable
    @Override
    public String apply(@Nullable Nameable nameable) {
      return nameable.name();
    }
  }

}
