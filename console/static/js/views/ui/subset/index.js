define([
   'underscore',
   'text!./template.html!strip',
   'backbone',
   'app'
], function(_, template, Backbone, app) {
    return Backbone.View.extend({
        initialize : function(args) {
            var self = this;

            this.template = template;
            var model = args.model;

            this.scope = {
                available: new Backbone.Collection([]);
                selected: new Backbone.Collection([]);
                error: new Backbone.Model({}),

                add: function(element, scope) {
                    console.log('add');
                },

                delete: function(element, scope) {
                    console.log('delete');
                },
            } // end of scope

            this.$el.html(template);
            this.rview = rivets.bind(this.$el, this.scope);
            this.render(this.scope);
        },
        render : function() {
          this.rview.sync();
          return this;
        }
    });
});
