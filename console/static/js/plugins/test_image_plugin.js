define([
    'backbone',
    'plugins'
], function(Backbone, plugins) {
    var plugin = new Backbone.Model.extend({
    });
    plugins.register(plugin);

    plugins.on('init:expando:instance', function(component) {
        console.log('INSTANCE PLUGIN', component);
        $('.expando-instance-status', component.$el).css('background-color', 'red');
    });

    return plugin;
});
