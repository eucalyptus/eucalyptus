define([
   'backbone',
   'rivets',
   'dataholder',
], function(Backbone, rivets, dataholder) {
    return Backbone.View.extend({
        _do_init : function() {
            var self = this;

            if (self.id) {
               self.scope.launchConfig = dataholder.launchConfigs.get(self.id); 
               dataholder.launchConfigs.on('change reset', function() {
                    console.log('rerender');
                    self.launchConfig = dataholder.launchConfigs.get(self.id); 
                    self.render();
               });
            }

            $tmpl = $(this.template);

            this.scope.$el = this.$el;

            this.$el.append($tmpl);

            this.rivetsView = rivets.bind(this.$el, this.scope);
            this.render();
        },
        render : function() {
            this.rivetsView.sync();
            return this;
        }
	});
});
