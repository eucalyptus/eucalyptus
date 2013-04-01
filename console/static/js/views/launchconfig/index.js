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
  'models/sgroup',
  'models/keypair',
  './model/advancedmodel',
  './model/blockmaps',
  './model/snapshots'
], function(Wizard, wizardTemplate, page1, page2, page3, page4, summary, launchconfigModel, imageModel, typeModel, securityModel, keyPair, advancedModel, blockMaps, snapShots) {
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
  var keyModel = new keyPair();
  var advancedModel = new advancedModel();
  var blockMaps = new blockMaps();
  var snapShots = new snapShots();

  var viewBuilder = wizard.viewBuilder(wizardTemplate)
          .add(new page1({model: imageModel, blockMaps: blockMaps}))
          .add(new page2({model: typeModel}))
          .add(new page3({model: securityModel, keymodel: keyModel}))
          .add(new page4({model: advancedModel, blockMaps: blockMaps, snapshots: snapShots}))
          .setHideDisabledButtons(true)
          .setFinishText('Launch Instance(s)').setFinishChecker(canFinish)
          .finisher(finish)
          .summary(new summary( {imageModel: imageModel, typeModel: typeModel, securityModel: securityModel, keymodel: keyModel, advancedModel: advancedModel} ));
//  var ViewType = wizard.makeView(options, wizardTemplate);
  var ViewType = viewBuilder.build()
  return ViewType;
});

