define([
        'rivets'
	], function( rivets ) {
	return Backbone.View.extend({
		initialize : function(args) {
			this.$el.html('<div id="tabs">' + args.innerHtml + '</div>');
            $('#tabs', this.$el).tabs();
			this.rview = rivets.bind(this.$el, args.model);
		},
		render : function() {
			this.rview.sync();
			return this;
		}
	});
});
