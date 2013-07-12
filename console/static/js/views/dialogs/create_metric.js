define([
   'app',
   './eucadialogview',
   'text!./create_metric.html!strip',
], function(app, EucaDialogView, template) {
    return EucaDialogView.extend({
        initialize : function(args) {
            var self = this;
            this.template = template;

            var scope = {
                status: '',
                items: args.items, 

                cancelButton: {
                    id: 'button-dialog-createmetric-cancel',
                    click: function() {
                       self.close();
                    }
                },

                submitButton: {
                  id: 'button-dialog-createmetric-save',
                  click: function() {
                      self.close();
                  }
                }
            }
            this.scope = scope;

            this._do_init();
        },
	});
});
