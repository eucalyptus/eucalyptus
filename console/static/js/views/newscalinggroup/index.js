console.log('WIZARD:start');
define([
  'wizard',
  'text!./template.html',
  './page1',
  './page2',
  './page3',
], function(Wizard, wizardTemplate, page1, page2, page3) {
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
  return ViewType;
});
