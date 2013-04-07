define([
  'app',
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
], function(app, Wizard, wizardTemplate, page1, page2, page3, page4, summary, launchconfigModel, imageModel, typeModel, securityModel, keyPair, advancedModel, blockMaps, snapShots) {
  var wizard = new Wizard();

  var launchConfigModel = new launchconfigModel();
  var imageModel = new imageModel();
  var typeModel = new typeModel();
  var securityModel = new securityModel();
  var keyModel = new keyPair();
  var advancedModel = new advancedModel();
  var blockMaps = new blockMaps();
  var snapShots = new snapShots();

  function canFinish(position, problems) {
    // VALIDATE THE MODEL HERE AND IF THERE ARE PROBLEMS,
    // ADD THEM INTO THE PASSED ARRAY
   //    return position === 2;

     return imageModel.isValid() & typeModel.isValid(); // & securityModel.isValid();
  }

  function finish() {
    imageModel.finish(launchConfigModel);
    typeModel.finish(launchConfigModel);
    securityModel.finish(launchConfigModel);
    advancedModel.finish(launchConfigModel);
    keyModel.finish(launchConfigModel);
    blockMaps.finish(launchConfigModel);

    launchConfigModel.on('validated.invalid', function(e, errors) {
    });

    launchConfigModel.validate();
    if(launchConfigModel.isValid()) {
      launchConfigModel.sync('create', launchConfigModel);
      //alert("Wizard complete. Check the console log for debug info.");
    } else {
      // what do we do if it isn't valid?
      alert('Final checklist was invalid.');
    }
  }

  var viewBuilder = wizard.viewBuilder(wizardTemplate)
          .add(new page1({model: imageModel, blockMaps: blockMaps}))
          .add(new page2({model: typeModel}))
          .add(new page3({model: securityModel, keymodel: keyModel}))
          .add(new page4({model: advancedModel, blockMaps: blockMaps, snapshots: snapShots}))
          .setHideDisabledButtons(true)
          .setFinishText(app.msg('create_launch_config_btn_create')).setFinishChecker(canFinish)
          .finisher(finish)
          .summary(new summary( {imageModel: imageModel, typeModel: typeModel, securityModel: securityModel, keymodel: keyModel, advancedModel: advancedModel} ));
//  var ViewType = wizard.makeView(options, wizardTemplate);
  var ViewType = viewBuilder.build();

  return ViewType;
});

