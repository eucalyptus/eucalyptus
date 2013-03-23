define([
	'dataholder',
	'text!./template.html!strip',
        'rivets',
	], function( dh, template, rivets) {
	return Backbone.View.extend({
		initialize : function() {
			var self = this;
			this.view = this;
			this.test = new Backbone.Model({
				value: 'foobarbaz'
			});
			this.buttonScope = {
				click: function() { self.test.set('value', 'button click'); }
			}
			this.sGroups = dh.scalingGroups;
			this.$el.html(template);
			this.rivetsView = rivets.bind(this.$el, this);
			$(':input[data-value]', this.$el).keyup(function() { $(this).trigger('change'); });
			this.render();
		},
		doit : function(e, context) {
			console.log('DOIT', arguments);
			this.test.set({value: context.sg.get('name')});
		},
		render : function() {
                        this.rivetsView.sync();
			return this;
		}
	});
});
