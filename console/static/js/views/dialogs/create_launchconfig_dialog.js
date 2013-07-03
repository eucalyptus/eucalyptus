define([
  './eucadialogview',
  'text!./create_launchconfig_dialog.html!strip',
  '../shared/advanced',
  'models/launchconfig',
  '../shared/model/advancedmodel',
  '../shared/model/blockmaps',
  'app'
], function(EucaDialogView, template, advanced, LaunchConfig, AdvModel, BlockMaps, app) {
  return EucaDialogView.extend({
    initialize : function(args) {
      var self = this;
      this.template = template;

      var instance = app.data.instances.get(args.model);
      var image = app.data.images.get(instance.get('image_id'));
      var platform = image.get('platform');
      var imgName = inferImage(image.get('location'), image.get('description'), platform);
      this.advancedModel = new AdvModel();
      this.blockMaps = new BlockMaps();

      this.scope = {
        width: 750,
        instance: instance,
        image: image,
        imgName: imgName,
        platform: getImageName(imgName),
        config: new LaunchConfig(),

        error: new Backbone.Model({}),

//        help: {title: null, content help_launchconfig.dialog_newconfig_content, url: help_launchconfig.dialog_newconfig_content_url, pop_height: 600},

        cancelButton: {
          click: function() {
            self.close();
            self.cleanup();
          }
        },

        createButton: new Backbone.Model({
          disabled: true,
          click: function() {
            // validate
            // save
            self.advancedModel.finish(self.scope.config);
            self.blockMaps.finish(self.scope.config);
            var checkbox = self.$el.find("#launchconfiglike-scaling-group-using");
            self.scope.config.save({}, {
              overrideUpdate: true,
              success: function(model, response, options){
                if(model != null){
                  var name = model.get('name');
                  notifySuccess(null, $.i18n.prop('create_launch_config_run_success', name)); 
                  if (checkbox.val()) {
                    self.createScalingGroup(name)
                  }
                }else{
                  notifyError($.i18n.prop('create_launch_config_run_error'), undefined_error);
                }
              },
              error: function(model, jqXHR, options){
                notifyError($.i18n.prop('create_launch_config_run_error'), getErrorMessage(jqXHR));
              }
            });
            self.close();
            self.cleanup();
          }
        }),
        toggleAdvanced: function() {
          var adv_div = self.$el.find('#launch-wizard-advanced-contents')
          adv_div.attr('style', adv_div.is(':visible')?'display: none':'display: inline');
        }
      };

      this.scope.config.set('image_id', this.scope.image.get('id'));
      this.scope.config.set('instance_type', this.scope.instance.get('instance_type'));
      this.scope.config.set('key_name', this.scope.instance.get('key_name'));
      this.scope.config.set('group_name', this.scope.instance.get('group_name'));

      var adv_page = new advanced({model: this.advancedModel, blockMaps: this.blockMaps, hidePrivate: true, removeTitle: true});

      self.scope.config.on('change', function() {
        self.scope.error.clear();
        self.scope.error.set("name", self.scope.config.validate());
      }),

      this.scope.config.on('validated', function() {
        self.scope.createButton.set('disabled', !self.scope.config.isValid());
        self.render();
      });

      this._do_init();

      self.$el.find('#launch-wizard-advanced-contents').append(adv_page.$el);
    },
    cleanup: function() {
      // undo validation overrides -  they leak into other dialogs
      this.scope.config.validation.name.required = false;
    },
    createScalingGroup: function(name) {
      console.log("let's create scaling group with launch config named: "+name);
      this.el.newscalinggroup({launchconfig:name});
    }
  });
});
