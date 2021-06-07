/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRange;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class GetTemplateResult extends EucalyptusData {

  private StageList stagesAvailable;

  @FieldRange(min = 1)
  private String templateBody;

  public StageList getStagesAvailable() {
    return stagesAvailable;
  }

  public void setStagesAvailable(final StageList stagesAvailable) {
    this.stagesAvailable = stagesAvailable;
  }

  public String getTemplateBody() {
    return templateBody;
  }

  public void setTemplateBody(final String templateBody) {
    this.templateBody = templateBody;
  }

}
