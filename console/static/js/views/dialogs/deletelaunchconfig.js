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
                    require(['app'], function(app) {
                      _.each(self.scope.items, function(item) {
                        app.data.launchconfig.get(item).destroy({wait: true});
                      });
                      self.close();
                    });
                  }
                }
            }

            this._do_init();
        },
	});
});
