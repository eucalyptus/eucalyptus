define([
  'app',
  'wizard',
  'text!./template.html',
  './image',
  './type',
  './security',
  './advanced',
  './summary',
  'models/instance',
  './model/imagemodel',
  './model/typemodel',
  './model/securitygroup',
  './model/keypair',
  './model/advancedmodel',
  './model/blockmaps',
  './model/snapshots'
], function(app, Wizard, wizardTemplate, page1, page2, page3, page4, summary, instanceModel, imageModel, typeModel, securityModel, keyPair, advancedModel, blockMaps, snapShots) {
  var wizard = new Wizard();

  var instanceModel = new instanceModel();
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
    imageModel.finish(instanceModel);
    typeModel.finish(instanceModel);
    securityModel.finish(instanceModel);
    advancedModel.finish(instanceModel);
    keyModel.finish(instanceModel);
    blockMaps.finish(instanceModel);

    instanceModel.on('validated:invalid', function(e, errors) {
      console.log("INSTANCEMODEl INVALID:", errors);
    });

    instanceModel.validate();
    if(instanceModel.isValid()) {
      //alert("Wizard complete. Check the console log for debug info.");
      instanceModel.sync('create', instanceModel);
    } else {
      alert('Final checklist was invalid.');

    }
  }

  var viewBuilder = wizard.viewBuilder(wizardTemplate)
          .add(new page1({model: imageModel, blockMaps: blockMaps}))
          .add(new page2({model: typeModel}))
          .add(new page3({model: securityModel, keymodel: keyModel}))
          .add(new page4({model: advancedModel, blockMaps: blockMaps, snapshots: snapShots}))
          .setHideDisabledButtons(true)
          .setFinishText(app.msg('launch_instance_btn_launch')).setFinishChecker(canFinish)
          .finisher(finish)
          .summary(new summary( {imageModel: imageModel, typeModel: typeModel, securityModel: securityModel, keymodel: keyModel, advancedModel: advancedModel} ));
//  var ViewType = wizard.makeView(options, wizardTemplate);
  var ViewType = viewBuilder.build();

  return ViewType;
});

