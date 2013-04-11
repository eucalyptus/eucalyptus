define([
  'wizard',
  'text!./template.html',
  './image',
  './type',
  './security',
  './advanced',
  './summary',
  'models/launchconfig',
  './model/imagemodel',
  './model/typemodel',
  './model/securitygroup',
  './model/keypair',
  './model/advancedmodel',
  './model/blockmaps',
  './model/snapshots'
], function(Wizard, wizardTemplate, page1, page2, page3, page4, summary, launchconfigModelClz, imageModelClz, typeModelClz, securityModelClz, keyPairClz, advancedModelClz, blockMapsClz, snapShotsClz) {
  var res = function() {
      var wizard = new Wizard();

      function canFinish(position, problems) {
        // VALIDATE THE MODEL HERE AND IF THERE ARE PROBLEMS,
        // ADD THEM INTO THE PASSED ARRAY
        return position === 2;
      }

      function finish() {
        alert("Placeholder function for saving the new launch config.");
      }

      var launchConfigModel = new launchconfigModelClz();
      var imageModel = new imageModelClz();
      var typeModel = new typeModelClz();
      var securityModel = new securityModelClz();
      var keyModel = new keyPairClz();
      var advancedModel = new advancedModelClz();
      var blockMaps = new blockMapsClz();
      var snapShots = new snapShotsClz();

      var viewBuilder = wizard.viewBuilder(wizardTemplate)
              .add(new page1({model: imageModel, blockMaps: blockMaps}))
              .add(new page2({model: typeModel}))
              .add(new page3({model: securityModel, keymodel: keyModel}))
              .add(new page4({model: advancedModel, blockMaps: blockMaps, snapshots: snapShots}))
              .setHideDisabledButtons(true)
              .setFinishText('Create launch configuration').setFinishChecker(canFinish)
              .finisher(finish)
              .summary(new summary( {imageModel: imageModel, typeModel: typeModel, securityModel: securityModel, keymodel: keyModel, advancedModel: advancedModel} ));
    //  var ViewType = wizard.makeView(options, wizardTemplate);
      var ViewType = viewBuilder.build();

      return ViewType;
  }
  return res;
});

