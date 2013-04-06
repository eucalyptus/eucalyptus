define([
   'backbone',
   'rivets',
   'dataholder',
], function(Backbone, rivets, dataholder) {
    return Backbone.View.extend({
        _do_init : function() {
            var self = this;
            $tmpl = $(this.template);

            this.scope.$el = this.$el;

            this.$el.append($tmpl);

            this.rivetsView = rivets.bind(this.$el, this.scope);
            this.render();
        },
        rebind: function(el, scope) {
            this.rivetsView.unbind(this.$el, this.scope);
            this.rivetsView = rivets.bind(el, scope);
        },
        render : function() {
            this.rivetsView.sync();
            return this;
        },
        stopListening: function() {
            console.log('stop listening');
            this.rivetsView.unbind(this.$el, this.scope);
        }
	});
});
