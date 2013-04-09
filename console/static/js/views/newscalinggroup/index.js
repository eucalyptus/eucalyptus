define([
  'underscore',
  'backbone',
  'wizard',
  'text!./template.html',
  './page1',
  './page2',
  './page3',
  'models/scalinggrp',
  './summary',
], function(_, Backbone, Wizard, wizardTemplate, page1, page2, page3, ScalingGroup, summary) {
  var wizard = new Wizard();

  function canFinish(position, problems) {
    // VALIDATE THE MODEL HERE AND IF THERE ARE PROBLEMS,
    // ADD THEM INTO THE PASSED ARRAY
    return position === 2;
  }

  function finish() {
      window.location = '#scaling';
  }

  var scope = {
    toggletest: new Backbone.Model({value: false}),
    scalingGroup: new ScalingGroup({})
  }
  scalescope = scope;
  var p1 = new page1({model: scope});
  var p3 = new page3({model: scope});

  var viewBuilder = wizard.viewBuilder(wizardTemplate)
          .add(p1).add(page2).add(p3).setHideDisabledButtons(true)
          .setFinishText('Create scaling group').setFinishChecker(canFinish)
          .finisher(finish)
          .summary(new summary( {model: scope} ));

  var ViewType = viewBuilder.build()
  return ViewType;
});
