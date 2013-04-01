define([
  'wizard',
  'text!./template.html',
  './image',
  './type',
  './security',
  './advanced',
  './summary',
  'models/launchconfig',
  './imagemodel'
], function(Wizard, wizardTemplate, page1, page2, page3, page4, summary, launchconfigModel, imageModel) {
  var wizard = new Wizard();

  function canFinish(position, problems) {
    // VALIDATE THE MODEL HERE AND IF THERE ARE PROBLEMS,
    // ADD THEM INTO THE PASSED ARRAY
    return position === 2;
  }

  function finish() {
    alert("Congratulations!  You finished a pointless wizard!")
  }

  var launchConfigModel = new launchconfigModel();
  var imageModel = new imageModel();

  var viewBuilder = wizard.viewBuilder(wizardTemplate)
          .add(new page1({model: imageModel}))
          .add(new page2({model: launchConfigModel}))
          .add(new page3({model: launchConfigModel}))
          .add(new page4({model: launchConfigModel}))
          .setHideDisabledButtons(true)
          .setFinishText('Launch Instance(s)').setFinishChecker(canFinish)
          .finisher(finish)
          .summary(new summary( {model: launchConfigModel, imageModel: imageModel} ));
//  var ViewType = wizard.makeView(options, wizardTemplate);
  var ViewType = viewBuilder.build()
  return ViewType;
});
