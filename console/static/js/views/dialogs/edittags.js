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
                       args.model.trigger('confirm');
                       args.model.get('tags').each(function(t) {
                           if (t.get('_new') && !t.get('_deleted')) { 
                               t.save();
                           }
                           if (t.get('_deleted')) {
                               t.destroy();
                           }
                           if (t.get('_edited')) {
                               t.save();
                           }
                       });
                       self.close();
                  }
                }
            }

            this._do_init();
        },
	});
});
