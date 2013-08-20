define([
   './landing_page_base',
   'backbone',
   'rivets',
   'text!./landing_page_images.html!strip',
], function(LandingPage, Backbone, rivets, template) {
    return LandingPage.extend({
        initialize: function(args) {
            var self = this;
            this.template = template;
            console.log("LANDING_PAGE: initialize " + args.id);
            this.scope = {
              id: args.id,
              collection: args.collection,
              items: '',
              databox: '',
     	      expanded_row_callback: function(e){
                var thisID = e.item.get('id');
                console.log("ITEM ID: " + thisID);
                var $placeholder = $('<div>').attr('id', "expanded-" + thisID).addClass("expanded-row-inner-wrapper");
                if( e.item.get('expanded') === true ){
                  // IF EXPANDED, APPEND THE RENDER EXPANDED ROW VIEW TO THE PREVIOUS PLACEHOLDER, MATCHED BY ITEM'S ID
                  require(['app', 'views/expandos/image'], function(app, expando) {
                    var $el = $('<div>');
                    new expando({el: $el, model: app.data.image.where({id: thisID})[0] });
                    $('#expanded-' + thisID).children().remove();
                    $('#expanded-' + thisID).append($el);
                  });
                }
                // IF NOT EXPANDED, RETURN THE PLACEHOLDER DIV
                return $('<div>').append($placeholder).html();
              },
              expand_row: function(context, event){              
                console.log("Clicked to expand: " + event.item.id);
                if( this.items.get(event.item.id).get('expanded') === true ){
                  this.items.get(event.item.id).set('expanded', false);
                }else{
                  this.items.get(event.item.id).set('expanded', true);
                }
                self.render();
              },
              launch_instance: function(context, event){
                console.log("Clicked to launch: " + event.item.id);
                // TAKEN FROM THE OLD CODE BASE, support.js - KYO 081413
                var image_id = event.item.id;
                var $container = $('html body').find(DOM_BINDING['main']);
                $container.maincontainer("changeSelected", null, { selected:'launcher', filter: {image: image_id}});
              },
            };
            this._do_init();
            console.log("LANDING_PAGE: initialize end");
        },
    });
});

