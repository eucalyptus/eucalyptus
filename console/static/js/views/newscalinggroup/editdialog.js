define([
   'views/dialogs/eucadialogview',
  'wizard',
   'text!./nosidebar.html',
  './page1',
  './page2',
  './page3',
  'models/scalinggrp',
], function(EucaDialogView, Wizard, wizardTemplate, page1, page2, page3, ScalingGroup) {
  var wizard = new Wizard();

  function canFinish(position, problems) {
    // VALIDATE THE MODEL HERE AND IF THERE ARE PROBLEMS,
    // ADD THEM INTO THE PASSED ARRAY
    return position === 2;
  }

  function finish() {
    alert("Congratulations!  You finished a pointless wizard!")
  }

  var viewBuilder = wizard.viewBuilder(wizardTemplate)
          .add(page1).add(page2).add(page3).setHideDisabledButtons(true)
          .setFinishText('Create scaling group').setFinishChecker(canFinish)
          .finisher(finish);

  var ViewType = viewBuilder.build()

    return EucaDialogView.extend({
        scope : {
        },
        initialize : function() {
            var v = new ViewType();
            v.render();
            this.$el = v.$el;
            this._do_init();
        },
	});
});
