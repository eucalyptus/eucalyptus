define([
	'text!./template.html!strip',
        'rivets'
	], function( template, rivets ) {
	return Backbone.View.extend({
		initialize : function(args) {
			var $innerHtml = $(args.innerHtml);
			this.$el.html('<div class="crud"></div>');
			this.$edit = $('.edit', $innerHtml);
			this.$display = $('.display', $innerHtml);
			this.rview = rivets.bind(this.$el, args.model);
		},
		render : function() {
			this.rview.sync();
			return this;
		}
	});
});
