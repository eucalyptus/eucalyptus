define([
   'app',
   './eucadialogview',
   'text!./deletescalinggroup.html!strip',
   'text!./deletescalinggroup2.html!strip',
], function(app, EucaDialogView, template, template2) {
    return EucaDialogView.extend({
        initialize : function(args) {
            var self = this;
            this.template = args.model == null ? template2 : template;

            this.scope = {
                status: '',
                items: args.model,
                help: {title: null, content: help_scaling.dialog_delete_content, url: help_scaling.dialog_delete_content_url, pop_height: 600},

                cancelButton: {
                  id: 'button-dialog-deletescalinggroup-cancel',
                  click: function() {
                    self.close();
                  }
                },

                deleteButton: {
                  id: 'button-dialog-deletescalinggroup-delete',
                  click: function() {
                      doMultiAction(self.scope.items.pluck('name'), app.data.scalinggrp,
                                    function(model, options) {
                                      options['wait'] = true;
                                      model.set('force_delete', true);
                                      model.destroy(options);
                                    },
                                    'delete_scaling_group_progress',
                                    'delete_scaling_group_done',
                                    'delete_scaling_group_fail',
                                    function(response) {
                                      if (response.results && response.results.request_id) {
                                        return; // all good
                                      } else {
                                        return undefined_error;
                                      }
                                    });
                      self.close();
                  }
                }
            }

            this._do_init();
        },
	});
});
