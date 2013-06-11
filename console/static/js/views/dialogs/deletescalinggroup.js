define([
   'app',
   './eucadialogview',
   'text!./deletescalinggroup.html!strip',
   'text!./deletescalinggroup2.html!strip',
], function(app, EucaDialogView, template, template2) {
    return EucaDialogView.extend({
        initialize : function(args) {
            var self = this;
            this.template = args.model.instances != null ? template2 : template;

            this.scope = {
                status: '',
                items: args.model, 

                cancelButton: {
                  click: function() {
                    self.close();
                  }
                },

                deleteButton: {
                  click: function() {
                      doMultiAction(args.items, App.data.scalinggrps,
                                    function(model, options) {
                                      options['wait'] = true;
                                      model.destroy(options);
                                    },
                                    'delete_scaling_group_progress',
                                    'delete_scaling_group_done',
                                    'delete_scaling_group_fail');
                      self.close();
                  }
                }
            }

            this._do_init();
        },
	});
});
