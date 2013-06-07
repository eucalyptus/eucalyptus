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

            model.set('size', model.get('instances').length);

            // uncomment to turn min and max into form fields for testing the model bindings
            // that make min and max accomodating to the value of desired_capacity and so on
            //model.set('enable_formfields', true);

            this.scope = {
                errors: new Backbone.Model(),

                qscale: model,

                cancelButton: {
                    click: function() {
                       self.close();
                    }
                },

                submitButton: new Backbone.Model({
                    disabled: !model.isValid(),
                    click: function() {
                       if (model.isValid()) {
                         original.set(model.toJSON());
                         //original.setDesiredCapacity(original.get('desired_capacity'));
                         original.save();
                         self.close();
                       }
                    }
                })
            }

            this.scope.fireChange = function(e) {
              if(e.keyCode != 9) { 
                $(e.target).change();
              }
            }

            this.scope.qscale.on('change', function(model) {
                self.scope.qscale.validate(model.changed);
            });

            this.scope.qscale.on('validated', function(valid, model, errors) {
                _.each(_.keys(model.changed), function(key) { 
                    self.scope.errors.set(key, errors[key]); 
                });
                self.scope.submitButton.set('disabled', !self.scope.qscale.isValid());
            });


            this._do_init();
        },
	});
});
