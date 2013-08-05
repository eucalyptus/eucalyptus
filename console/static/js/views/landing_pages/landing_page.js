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
            var self = this;
            console.log("LANDING_PAGE: initialize " + args.id);
            this.scope = {
              id: args.id,
              items: '',
     	      expanded_row_callback: self.handle_expanded_row,   // THIS FUNCTION WILL BE PASSED IN BY LANDING PAGES
              expand_row: function(context, event){              // ISSUE: THE EXPANDED ROW GETS CANCELED WHEN RE-RENDER()
                console.log("Clicked to expand: " + event.item.id);
                if( this.items.get(event.item.id).get('expanded') === true ){
                  this.items.get(event.item.id).set('expanded', false);
                }else{
                  this.items.get(event.item.id).set('expanded', true);
                  var $expand = this.expanded_row_callback(event.item.id);
                  console.log($expand);
                  $(context.srcElement.parentNode.parentNode).after($('<tr>').addClass('expanded').append($expand));
                }
//                console.log(context);
//                console.log(event);
//                console.log(context.srcElement.parentNode.parentNode.innerHTML);
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
            this.rivetsView = rivets.bind(this.$el, this.scope);
            this.render();
        },
        handle_expanded_row : function(ip){ 
          var $el = $('<div />');
          require(['app', 'views/expandos/ipaddress'], function(app, expando) {
            new expando({el: $el, model: app.data.eip.get(ip) });
          });
          return $el;
        },
        test: function(args){
            console.log("LANDING PAGE: " + args );
        },
    });
});

