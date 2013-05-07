define([
	'text!./template.html!strip',
        'rivets'
	], function( template, rivets ) {
	return Backbone.View.extend({
		initialize : function(args) {
            var $tmpl = $(template);
            var $wrk = $('<span />');
            var $el = this.$el;
            this.$el.replaceWith($wrk);
            $wrk.append($el);
            $wrk.wrapInner($tmpl);
            var adp = rivets.config.adapter;
            if (adp.read(args.model, 'disabled') == undefined) adp.publish(args.model, 'disabled', false);
			this.rview = rivets.bind($wrk, args.model);
            return $tmpl;
		},
		click : function() {
			if (this.model.click) {
				this.model.click();
			} else {
				console.log('no bound click event');
			}
		},
		render : function() {
			this.rview.sync();
			return this;
		}
	});
});
