define([
   './eucadialogview',
   'text!./quickscaledialog.html!strip',
], function(EucaDialogView, template) {
    return EucaDialogView.extend({
        initialize : function(args) {
            var self = this;
            this.template = template;
            this.name = args.items.name,
            this.original = args.items.desired,
            this.scope = {
                qscale: new Backbone.Model({
                    name: args.items.name,
                    size: args.items.size,
                    minimum: args.items.min,
                    maximum: args.items.max,
                    desired: args.items.desired
                }),

                cancelButton: {
                    click: function() {
                       self.close();
                    }
                },

                submitButton: {
                    click: function() {
                      var grps;
                      if (self.original != self.scope.qscale.get('desired')) {
                        require(['models/scalinggrps'], function(collection) {
                          grps = new collection();
                          grps.fetch({success: function() {
                              grps.get(self.name).setDesiredCapacity(self.scope.qscale.get('desired'));
                          }});
                          self.close();
                        });
                      }
                    }
                }
            }

            this._do_init();
        },
	});
});
