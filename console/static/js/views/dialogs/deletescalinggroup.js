define([
   './eucadialogview',
   'text!./deletescalinggroup.html!strip',
   'text!./deletescalinggroup2.html!strip',
], function(EucaDialogView, template, template2) {
    return EucaDialogView.extend({
        initialize : function(args) {
            var self = this;
            this.template = args.items.not_allowed ? template2 : template;

            this.scope = {
                status: '',
                items: args.items.groups, 

                cancelButton: {
                  click: function() {
                    self.close();
                  }
                },

                deleteButton: {
                  click: function() {
                    require(['app'], function(app) {
                      _.each(self.scope.items, function(item) {
                        app.data.sclaingGroup.get(item).destroy({wait: true});
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
