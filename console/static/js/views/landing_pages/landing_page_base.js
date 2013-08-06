define([
   'backbone',
   'rivets',
], function(Backbone, rivets) {
    return Backbone.View.extend({
        _do_init : function() {
            $tmpl = $(this.template);
            var self = this;
            this.$el.append($tmpl);
            $('#euca-main-container').children().remove();
            this.$el.appendTo($('#euca-main-container'));
        },
        close : function() {
            this.$el.empty();
        },
        render : function() {
            this.rivetsView.sync();
            return this;
        },
        get_element: function() {
            return this.$el;
        },
        bind_items: function(args) {
            this.scope.items = args;
            this.rivetsView = rivets.bind(this.$el, this.scope);
            this.render();
        },
    });
});

