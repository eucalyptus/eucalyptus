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
                      // unfortunate thing about this is that it reloads from proxy, no local store
                      // TODO: fix that ^^^
                      require(['models/scalinggrps'], function(collection) {
                        var grps = new collection();
                        _.each(self.scope.items, function(item) {
                          grps.fetch({success: function() {
                            grps.get(item).destroy({wait: true});
                          }});
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
