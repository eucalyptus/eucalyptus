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
            this.rivetsView = rivets.bind(this.$el, this.scope);
            this.render();
},
        close : function() {
            this.$el.empty();
        },
        render : function() {
            this.rivetsView.sync();
            return this;
        },
        initialize: function(args) {
            var self = this;
            console.log("LANDING_PAGE: initialize " + args.id);
            this.scope = {
              id: args.id,
              items: '',
     	      //expanded_row_callback: args.expanded_row_handler,
     	      expanded_row_callback: function(e){
                var thisIP = e.item.get('public_ip');
                var $el = $('<div />');
                if( e.item.get('expanded') != true ){
                  return require(['app', 'views/expandos/ipaddress'], function(app, expando) {
                    new expando({el: $el, model: app.data.eip.get(thisIP) });
                    console.log("e: " + JSON.stringify(e));
                    console.log("model: " + JSON.stringify(app.data.eip.get(thisIP)) );
                    console.log("el: " + $el);
                    return $el.html();
                  });
                }
              },
              expand_row: function(context, event){              
                console.log("Clicked to expand: " + event.item.id);
                if( this.items.get(event.item.id).get('expanded') === true ){
                  this.items.get(event.item.id).set('expanded', false);
                }else{
                  this.items.get(event.item.id).set('expanded', true);
                }
              },
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
                },
        test: function(args){
            console.log("LANDING PAGE: " + args );
        },
    });
});

