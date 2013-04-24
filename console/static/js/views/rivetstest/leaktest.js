define([
    'app',
    'rivets',
	'text!./leaktemplate.html!strip',
 ], function( app, rivets, template ) {
	return Backbone.View.extend({
		initialize : function() {
			var self = this;
			this.$el.html(template);
			this.rivetsView = rivets.bind(this.$el, this.model);
			this.render();
		},
		render : function() {
            this.rivetsView.sync();
			return this;
		}
	});
});
