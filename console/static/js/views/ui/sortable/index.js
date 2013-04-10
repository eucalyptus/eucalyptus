define([
        'rivets'
	], function( rivets ) {
	return Backbone.View.extend({
		initialize : function(args) {
			this.$el.html('<div class="ui-sortable">' + args.innerHtml + '</div>');
            $('.ui-sortable', this.$el).sortable();
			this.rview = rivets.bind(this.$el, args.model);
		},
		render : function() {
			this.rview.sync();
			return this;
		}
	});
});
