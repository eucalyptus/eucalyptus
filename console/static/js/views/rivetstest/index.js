define([
	'dataholder',
	'text!./template.html!strip',
        'rivets',
	], function( dh, template, rivets) {
	return Backbone.View.extend({
		initialize : function() {
			this.view = this;
			this.test = new Backbone.Model({
				value: 'foobarbaz'
			});
			this.sGroups = dh.scalingGroups;
			this.render();
		},
		doit : function(e, context) {
			console.log('DOIT', arguments);
			this.test.set({value: context.sg.get('name')});
		},
		render : function() {
			this.$el.html(template);
			rivets.bind(this.$el, this);
			$(':input[data-value]', this.$el).keyup(function() { $(this).trigger('change'); });
			return this;
		}
	});
});
