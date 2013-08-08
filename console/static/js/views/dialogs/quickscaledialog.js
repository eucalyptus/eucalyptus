define([
   'app',
   './eucadialogview',
   'text!./quickscaledialog.html!strip',
], function(app, EucaDialogView, template) {
    return EucaDialogView.extend({
        initialize : function(args) {
            var self = this;
            var original = args.model.at(0);
            var model = original.clone();
            this.template = template;

            // validate straight away, so we can activate the button
            model.validate();

            if (model.get('instances')) {
                model.set('size', model.get('instances').length);
            }
            else {
                model.set('size', '0');
            }

            // uncomment to turn min and max into form fields for testing the model bindings
            // that make min and max accomodating to the value of desired_capacity and so on
            //model.set('enable_formfields', true);

            this.scope = {
                help: {title: null, content: help_scaling.dialog_quick_scale_content, url: help_scaling.dialog_quick_scale_url, pop_height: 600},
                errors: new Backbone.Model(),

                qscale: model,

                cancelButton: {
                    id: 'button-dialog-quickscale-cancel',
                    click: function() {
                       self.close();
                    }
                },

                submitButton: new Backbone.Model({
                    id: 'button-dialog-quickscale-save',
                    disabled: false, //!model.isValid(),
                    click: function() {
                       if (model.isValid(true)) {
                         original.set(model.toJSON());
                         original.setDesiredCapacity(original.get('desired_capacity'), original.get('honor_cooldown'), {
                           success: function(model, response, options){
                             if(response == 'success'){
                               notifySuccess(null, $.i18n.prop('quick_scale_success'));
                             }else{
                               notifyError($.i18n.prop('quick_scale_error'), undefined_error);
                             }
                           },
                           error: function(model, jqXHR, options){
                             notifyError($.i18n.prop('quick_scale_error'), getErrorMessage(jqXHR));
                           }
                         });
                         self.close();
                       }
                    }
                })
            }

          /* Live typing validation is annoying - EUCA-7073, EUCA-7075, EUCA-6865 
            this.scope.fireChange = function(e) {
              if(e.keyCode != 9) {
                self.scope.errors.clear();
                setTimeout( function() { 
                  $(e.target).change();
                }, 2000);
              }
            }

            this.scope.qscale.on('change', function(model) {
                self.scope.qscale.validate(model.changed);
            });
         */ 

            this.scope.qscale.on('validated', function(valid, model, errors) {
                _.each(_.keys(model.changed), function(key) { 
                    self.scope.errors.set(key, errors[key]); 
                });
                //self.scope.submitButton.set('disabled', !self.scope.qscale.isValid());
                setTimeout(function() {
                  self.scope.errors.clear();
                }, 6000);
            });

            

            this._do_init();
        },
	});
});
