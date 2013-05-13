define([
  'app',
  'wizard',
  'text!./template.html',
  '../shared/image',
  '../shared/type',
  '../shared/security',
  '../shared/advanced',
  '../shared/summary',
  'models/instance',
  '../shared/model/imagemodel',
  '../shared/model/typemodel',
  '../shared/model/securitygroup',
  '../shared/model/keypair',
  '../shared/model/advancedmodel',
  '../shared/model/blockmaps',
  '../shared/model/snapshots'
], function(app, Wizard, wizardTemplate, page1, page2, page3, page4, summary, instance, image, type, security, keyPair, advanced, block, snap) {

  var config = function(options) {
    var wizard = new Wizard();

    var instanceModel = new instance();
    var imageModel = new image(options);
    var typeModel = new type();
    var securityModel = new security();
    var keyModel = new keyPair();
    var advancedModel = new advanced();
    var blockMaps = new block();
    var snapShots = new snap();

    function canFinish(position, problems) {
      // VALIDATE THE MODEL HERE AND IF THERE ARE PROBLEMS,
      // ADD THEM INTO THE PASSED ARRAY
     //    return position === 2;

       return imageModel.isValid() & typeModel.isValid() & (position == 2 || position == 3); // & securityModel.isValid();
    }

    function finish() {
      imageModel.finish(instanceModel);
      typeModel.finish(instanceModel);
      securityModel.finish(instanceModel);
      advancedModel.finish(instanceModel);
      keyModel.finish(instanceModel);
      blockMaps.finish(instanceModel);

      instanceModel.on('validated:invalid', function(e, errors) {
      });

      instanceModel.validate();
      if(instanceModel.isValid()) {
        instanceModel.sync('create', instanceModel);
        var $container = $('html body').find(DOM_BINDING['main']);
        $container.maincontainer("changeSelected", null, {selected:'instance'});
      } else {
        alert('Final checklist was invalid.');

      }
    }
    
    
    var viewBuilder = wizard.viewBuilder(wizardTemplate)
            .add(new page1({model: imageModel, blockMaps: blockMaps, image: options.image}))
            .add(new page2({model: typeModel}))
            .add(new page3({model: securityModel, keymodel: keyModel}))
            .add(new page4({model: advancedModel, blockMaps: blockMaps, snapshots: snapShots, image: imageModel}))
            .setHideDisabledButtons(true)
            .setFinishText(app.msg('launch_instance_btn_launch')).setFinishChecker(canFinish)
            .map('optionLink', '#optionLink')
            .finisher(finish)
            .summary(new summary( {imageModel: imageModel, typeModel: typeModel, securityModel: securityModel, keymodel: keyModel, advancedModel: advancedModel} ));
    var ViewType = viewBuilder.build();
    return ViewType;
  }
  return config;
});

