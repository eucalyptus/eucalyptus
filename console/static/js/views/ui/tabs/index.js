define([
        'rivets'
	], function( rivets ) {
	return Backbone.View.extend({
		initialize : function(args) {
            console.log('tabs');
			this.$el.html('<div id="tabs">' + args.innerHtml + '</div>');
            $('#tabs', this.$el).tabs();
            console.log('EL', this.$el);
			this.rview = rivets.bind(this.$el, args.model);
		},
		render : function() {
			this.rview.sync();
			return this;
		}
	});
});
