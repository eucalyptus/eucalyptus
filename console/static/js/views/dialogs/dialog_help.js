define([
   'text!./dialog_help.html!strip',
   'backbone',
   'rivets',
], function(template, Backbone, rivets) {
    return Backbone.View.extend({
        initialize : function(args) {
            var self = this;
            $tmpl = template;
            console.log("da template = "+template);
            this.$el.append($tmpl);
            this.scope = {
                revertButton: {
                  id: 'button-dialog-help-revert',
                  click: function() {
                    // flip back, probably
                    //self.close();
                  }
                }
              };
            this.rivetsView = rivets.bind(this.$el, this.scope);
            this.render();
            this.scope.$el = this.$el;
        },
        render : function() {
            this.rivetsView.sync();
            return this;
        }
    });
});
