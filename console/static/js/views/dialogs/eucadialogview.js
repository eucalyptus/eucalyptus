define([
   'backbone',
   'rivets',
], function(Backbone, rivets) {
    return Backbone.View.extend({
        _do_init : function() {
            $tmpl = $(this.template);
            console.log('TEMPLATE',this.template, $tmpl);

            this.scope.$el = this.$el;
            this.scope.close = this.close;

            this.$el.append($('.body', $tmpl));
            this.$el.appendTo('body');

            var title = $('.title', $tmpl).text();
            this.$el.eucadialog({title: title});

            this.rivetsView = rivets.bind(this.$el, this.scope);
            this.render();

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
