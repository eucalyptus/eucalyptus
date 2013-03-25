define([
   'backbone',
   'rivets',
], function(Backbone, rivets) {
    return Backbone.View.extend({
        _do_init : function() {
            this.scope.$el = this.$el;
            this.scope.close = this.close;
            this.rivetsView = rivets.bind(this.$el, this.scope);
            this.render();
            this.$el.appendTo('body');
            this.$el.eucadialog();
            this.$el.dialog('open');
        },
        close : function() {
            this.$el.dialog('close');
        },
        render : function() {
            this.rivetsView.sync();
            return this;
        }
	});
});
