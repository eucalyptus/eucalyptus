define([
    'backbone'
], function(Backbone) {
    var plugins, p;

    plugins = Backbone.Collection.extend({
        init: function(component) {
            this.trigger('init:' + component.EUCA_ID, component);
        },
        register: function(plugin) {
            p.add(plugin);
        }
    }); 
    p = new plugins();

    require(['plugins/test_image_plugin'], function(plug) { p.register(plug); });

    return p;
});
