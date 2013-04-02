define([
   'app',
   './eucadialogview',
   'text!./quickscaledialog.html!strip',
], function(app, EucaDialogView, template) {
    return EucaDialogView.extend({
        initialize : function(args) {
            var self = this;
            var original = args.model[0];
            var model = original.clone();
            this.template = template;
            this.scope = {
                errors: new Backbone.Model(),

                qscale: model,

                cancelButton: {
                    click: function() {
                       self.close();
                    }
                },

                submitButton: {
                    click: function() {
                       original.set(model.toJSON());
                       self.close();
                    }
                }
            }

            this.scope.qscale.on('change', function(model) {
                console.log('CHANGE', arguments);
                self.scope.qscale.validate(model.changed);
            });

            this.scope.qscale.on('validated', function(valid, model, errors) {
                _.each(_.keys(model.changed), function(key) { 
                    self.scope.errors.set(key, errors[key]); 
                });
            });

            this._do_init();
        },
	});
});
