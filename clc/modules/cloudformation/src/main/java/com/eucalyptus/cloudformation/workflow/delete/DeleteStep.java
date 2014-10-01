package com.eucalyptus.cloudformation.workflow.delete;

import com.eucalyptus.cloudformation.resources.ResourceAction;

/**
 * Created by ethomas on 9/28/14.
 */
public interface DeleteStep {
  public ResourceAction perform(ResourceAction resourceAction) throws Exception;
}
