define([
  'wizard',
  'text!./template.html',
  './image',
  './type',
  './security',
  './advanced',
  './summary',
  'models/launchconfig',
  './imagemodel',
  './typemodel',
  './securitymodel',
  './advancedmodel'
], function(Wizard, wizardTemplate, page1, page2, page3, page4, summary, launchconfigModel, imageModel, typeModel, securityModel, advancedModel) {
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
  var typeModel = new typeModel();
  var securityModel = new securityModel();
  var advancedModel = new advancedModel();

  var viewBuilder = wizard.viewBuilder(wizardTemplate)
          .add(new page1({model: imageModel}))
          .add(new page2({model: typeModel}))
          .add(new page3({model: securityModel}))
          .add(new page4({model: advancedModel}))
          .setHideDisabledButtons(true)
          .setFinishText('Launch Instance(s)').setFinishChecker(canFinish)
          .finisher(finish)
          .summary(new summary( {imageModel: imageModel, typeModel: typeModel, securityModel: securityModel, advancedModel: advancedModel} ));
//  var ViewType = wizard.makeView(options, wizardTemplate);
  var ViewType = viewBuilder.build()
  return ViewType;
});

