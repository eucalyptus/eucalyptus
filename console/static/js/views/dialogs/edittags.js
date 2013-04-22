define([
   'underscore',
   './eucadialogview',
   'text!./edittags.html!strip',
   'backbone',
   'models/tag',
   'sharedtags'
], function(_, EucaDialogView, template, Backbone, Tag, SharedTags) {
    return EucaDialogView.extend({
        initialize : function(args) {
            var self = this;
            this.template = template;

            this.scope = {
                model: args.model.clone(),

                cancelButton: {
                    click: function() {
                       self.close();
                    }
                },

                confirmButton: {
                  click: function() {
                       self.scope.model.trigger('confirm');
                       self.close();
                  }
                }
            }

            this._do_init();
        },
	});
});
