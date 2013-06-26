define([
   'app',
   './eucadialogview',
   'text!./deletelaunchconfig.html!strip',
], function(app, EucaDialogView, template) {
    return EucaDialogView.extend({
        initialize : function(args) {
            var self = this;
            this.template = template;

            this.scope = {
                status: '',
                items: args.items, 
                help: {title: null,
                       content: help_scaling.dialog_delete_content,
                       url: help_scaling.dialog_delete_content_url,
                       pop_height: 600},
                

                cancelButton: {
                    click: function() {
                       self.close();
                    }
                },

                deleteButton: {
                  click: function() {
                      doMultiAction(args.items, App.data.launchconfigs,
                                    function(model, options) {
                                      options['wait'] = true;
                                      model.destroy(options);
                                    },
                                    'delete_launch_config_progress',
                                    'delete_launch_config_done',
                                    'delete_launch_config_fail');
                      self.close();
                  }
                }
            }

            this._do_init();
        },
	});
});
