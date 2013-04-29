define([
   'text!./tag_edit_nub.html!strip',
   'backbone',
   'models/tag'
], function(template, Backbone, Tag) {
    return Backbone.View.extend({
        initialize : function(args) {
            var self = this;

            this.scope = {
                model: args.model.clone(),
            }
            this.render();
        },
	});
});
