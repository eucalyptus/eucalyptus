define([
	'text!./template.html!strip',
        'rivets'
	], function( template, rivets ) {
    console.log('TAGS');
	return Backbone.View.extend({
		initialize : function(args) {
			this.$el.html(template);
            var model = args.model ? args.model : [{}];
			this.rview = rivets.bind(this.$el, model);
		},
		render : function() {
			this.rview.sync();
			return this;
		}
	});
});
