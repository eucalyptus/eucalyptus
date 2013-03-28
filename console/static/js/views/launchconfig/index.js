define([
  'wizard',
  'text!./template.html',
  './image',
  './type',
  './security',
  './advanced',
  './summary',
  './instancemodel'
], function(Wizard, wizardTemplate, page1, page2, page3, page4, summary, model) {
  var wizard = new Wizard();

  function canFinish(position, problems) {
    // VALIDATE THE MODEL HERE AND IF THERE ARE PROBLEMS,
    // ADD THEM INTO THE PASSED ARRAY
    return position === 2;
  }

  function finish() {
    alert("Congratulations!  You finished a pointless wizard!")
  }

  var dataModel = new model(); 
  var viewBuilder = wizard.viewBuilder(wizardTemplate)
          .add(page1).add(page2).add(page3).add(page4)
          .setHideDisabledButtons(true)
          .setFinishText('Launch Instance(s)').setFinishChecker(canFinish)
          .finisher(finish)
          .summary(new summary( {model: dataModel} ))
          .setPageModel(dataModel);
//  var ViewType = wizard.makeView(options, wizardTemplate);
  var ViewType = viewBuilder.build()
  return ViewType;
});
