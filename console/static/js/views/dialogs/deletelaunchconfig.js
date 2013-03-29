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
                    require(['models/launchconfigs'], function(collection) {
                      var lc = new collection();
                      _.each(self.scope.items, function(item) {

                        lc.fetch({success: function() { lc.get(item).destroy({wait: true}); }});
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
