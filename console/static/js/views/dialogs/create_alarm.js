define([
   'app',
   './eucadialogview',
   'text!./create_alarm.html!strip',
], function(app, EucaDialogView, template) {
    return EucaDialogView.extend({
        initialize : function(args) {
            var self = this;
            this.template = template;

            var scope = {
                status: '',
                items: args.items, 

                cancelButton: {
                    click: function() {
                       self.close();
                    }
                },

                submitButton: {
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
