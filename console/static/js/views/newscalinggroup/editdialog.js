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

  scalingGroup = new ScalingGroup({name: 'test', min_size: 2});
  var scope = {
    scalingGroup: scalingGroup,
    errors: {}
  }
  scalingGroup.on('change', function() {
      scope.errors = scalingGroup.validate();
      console.log('CHANGE', scope.errors);
  });

  var viewBuilder = wizard.viewBuilder(wizardTemplate,scope)
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
