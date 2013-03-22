define([
	'text!./template.html!strip',
        'rivets'
	], function( template, rivets ) {
	return Backbone.View.extend({
		initialize : function() {
			this.$el.html(template);
			this.rivetsView = rivets.bind(this.$el, this);
		},
		render : function() {
                        this.rivetsView.sync();
			return this;
		}
	});
});
