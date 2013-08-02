define([
   'backbone',
   'rivets',
   'text!./landing_page.html!strip',
], function(Backbone, rivets, template) {
    return Backbone.View.extend({
        _do_init : function(id) {
            $tmpl = template;
            var self = this;

            this.$el.append($tmpl);
            $('#euca-main-container').children().remove();
            this.$el.appendTo($('#euca-main-container'));
            console.log("LANDING_PAGE: _do_init() end");
        },
        close : function() {
            this.$el.empty();
        },
        render : function() {
            this.rivetsView.sync();
            return this;
        },
        initialize: function(args) {
            console.log("LANDING_PAGE: initialize " + args.id);
            this.scope = {
              items: ''
            };
            this._do_init(args.id);
            console.log("LANDING_PAGE: initialize end");
        },
        get_element: function() {
            return this.$el;
        },
        bind_items: function(args) {
            this.scope.items = args;
            console.log("LANDING PAGE: items = " + JSON.stringify(this.scope.items));
            this.rivetsView = rivets.bind(this.$el, this.scope);
            this.render();
        },
        test: function(args){
            console.log("LANDING PAGE: " + args );
        },
    });
});

