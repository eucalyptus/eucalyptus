define([
  'app',
  'wizard',
  'text!./template.html',
  '../shared/image',
  './type',
  '../shared/security',
  './advanced',
  './summary',
  'models/launchconfig',
  '../shared/model/imagemodel',
  './model/typemodel',
  './model/securitygroup',
  '../shared/model/keypair',
  './model/advancedmodel',
  '../shared/model/blockmaps',
  '../shared/model/snapshots'
], function(app, Wizard, wizardTemplate, page1, page2, page3, page4, summary, launchconfigModel, image, type, security, keyPair, advanced, block, snap) {
  var config = function(options) {
    var wizard = new Wizard();

    var launchConfigModel = new launchconfigModel();
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

       return imageModel.isValid() & typeModel.isValid(); // & securityModel.isValid();
    }

    function finish() {
      imageModel.finish(launchConfigModel);
      typeModel.finish(launchConfigModel);
      securityModel.finish(launchConfigModel);
      advancedModel.finish(launchConfigModel);
      keyModel.finish(launchConfigModel);
      blockMaps.finish(launchConfigModel);

      launchConfigModel.on('validated:invalid', function(e, errors) {
        console.log('INVALID MODEL', arguments);
      });

      launchConfigModel.validate();
      if(launchConfigModel.isValid()) {
        launchConfigModel.save({}, {
            overrideUpdate: true,
            success: function(model, response, options){  
              if(model != null){
                var name = model.get('name');
                notifySuccess(null, $.i18n.prop('create_launch_config_run_success', name));  
              }else{
                notifyError($.i18n.prop('create_launch_config_run_error'), undefined_error);
              }
            },
            error: function(model, jqXHR, options){  
              notifyError($.i18n.prop('create_launch_config_run_error'), getErrorMessage(jqXHR));
            }
        });
        var $container = $('html body').find(DOM_BINDING['main']);
          $container.maincontainer("changeSelected", null, {selected:'launchconfig'});
        //alert("Wizard complete. Check the console log for debug info.");
      } else {
        // what do we do if it isn't valid?
        alert('Final checklist was invalid.');
      }
    }

    var viewBuilder = wizard.viewBuilder(wizardTemplate)
            .add(new page1({model: imageModel, blockMaps: blockMaps, image: options.image}))
            .add(new page2({model: typeModel}))
            .add(new page3({model: securityModel, keymodel: keyModel}))
            .add(new page4({model: advancedModel, blockMaps: blockMaps, snapshots: snapShots, image: imageModel}))
            .setHideDisabledButtons(true)
            .setFinishText(app.msg('create_launch_config_btn_create')).setFinishChecker(canFinish)
            .map('optionLink', '#optionLink')
            .finisher(finish)
            .summary(new summary( {imageModel: imageModel, typeModel: typeModel, securityModel: securityModel, keymodel: keyModel, advancedModel: advancedModel} ));
  //  var ViewType = wizard.makeView(options, wizardTemplate);
    var ViewType = viewBuilder.build();
    return ViewType;
  }
  return config;
});

