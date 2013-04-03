define([
   'app',
   './eucadialogview',
   'text!./deletescalinggroup.html!strip',
   'text!./deletescalinggroup2.html!strip',
], function(app, EucaDialogView, template, template2) {
    return EucaDialogView.extend({
        initialize : function(args) {
            var self = this;
//            this.template = args.model.not_allowed ? template2 : template;
            this.template = template;

            this.scope = {
                status: '',
                items: args.model, 

                cancelButton: {
                  click: function() {
                    self.close();
                  }
                },

                deleteButton: {
                  click: function() {
                      self.scope.items.each(function(item) {
                        app.data.scalingGroup.get(item).destroy({wait: true});
                      });
                      self.close();
                  }
                }
            }

            this._do_init();
        },
	});
});
