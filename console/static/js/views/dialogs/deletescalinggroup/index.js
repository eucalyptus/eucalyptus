define([
   'backbone',
   'text!./template.html!strip',
   'rivets',
], function(Backbone, template, rivets) {
    return Backbone.View.extend({
        initialize : function() {
            var self = this;
            this.$el.html(template);
            this.rivetsView = rivets.bind(this.$el, this);
            this.render();
        },
        render : function() {
            this.rivetsView.sync();
            return this;
        }
	});
});
