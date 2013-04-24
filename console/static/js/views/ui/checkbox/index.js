define([
	'text!./template.html!strip',
    'rivets'
	], function( template, rivets ) {
	return Backbone.View.extend({
		initialize : function(args) {
            template = template.replace(/%%KEYPATH%%/g, args.binding.keypath);
			this.$el.html(template);
			this.rview = rivets.bind(this.$el, args.binding.model);
			this.render(this.model);
		},
		render : function(newValue) {
		this.model = newValue;
            this.rview.sync();
			return this;
		}
	});
});
