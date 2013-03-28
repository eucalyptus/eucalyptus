define([
   './eucadialogview',
   'text!./deletelaunchconfig.html!strip',
], function(EucaDialogView, template) {
    return EucaDialogView.extend({
        initialize : function(args) {
            var self = this;
            this.template = template;

            this.scope = {
                status: '',
                items: args.items, 

                cancelButton: {
                    click: function() {
                       self.close();
                    }
                },

                deleteButton: {
                    click: function() {
                       self.scope.status = 'Deleting';
                       self.render();
                       setTimeout(function() {
                          self.scope.status = 'Still deleting';
                          self.render();
                       }, 1000);
                       setTimeout(function() {
                           self.close();
                       }, 2000);
                    }
                }
            }

            this._do_init();
        },
	});
});
