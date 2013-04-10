define([
   'backbone',
   'rivets',
   'dataholder',
], function(Backbone, rivets, dataholder) {
    return Backbone.View.extend({
        _do_init : function() {
            var self = this;
            this.$el.html(this.template);
            this.rivetsView = rivets.bind(this.$el, this.scope);
            this.render();
        },
        render : function() {
            this.rivetsView.sync();
            return this;
        },
	});
});
