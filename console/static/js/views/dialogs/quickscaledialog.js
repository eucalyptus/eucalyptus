define([
   './eucadialogview',
   'text!./quickscaledialog.html!strip',
], function(EucaDialogView, template) {
    return EucaDialogView.extend({
        initialize : function(args) {
            var self = this;
            this.template = template;
            this.scope = {
                qscale: new Backbone.Model({
                    name: 'some name',
                    size: 4,
                    minimum: 1,
                    maximum: 10,
                    desired: 4,
                }),

                cancelButton: {
                    click: function() {
                       self.close();
                    }
                },

                submitButton: {
                    click: function() {
                       self.scope.status = 'Deleting';
                       self.close();
                    }
                }
            }

            this._do_init();
        },
	});
});
