define([
   './landing_page_base',
   'backbone',
   'rivets',
   'text!./landing_page_eips.html!strip',
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
                // ISSUE: EIP MODEL DOESN'T HAVE AN ID ATTRIBUTE - KYO 080613
                var thisItem = e.item.get('public_ip');
                var thisEscaped = String(thisItem).replace(/\./g, "-");
                var $placeholder = $('<div>').attr('id', "expanded-" + thisEscaped).addClass("expanded-row-inner-wrapper");
                if( e.item.get('expanded') === true ){
                  // IF EXPANDED, APPEND THE RENDER EXPANDED ROW VIEW TO THE PREVIOUS PLACEHOLDER, MATCHED BY ITEM'S ID
                  require(['app', 'views/expandos/ipaddress'], function(app, expando) {
                    var $el = $('<div>');
                    new expando({el: $el, model: app.data.eip.get(thisItem) });
                    $('#expanded-' + thisEscaped).children().remove();
                    $('#expanded-' + thisEscaped).append($el);
                  });
                }
                // IF NOT EXPANDED, RETURN THE PLACEHOLDER DIV
                return $('<div>').append($placeholder).html();
              },
            };
            this._do_init();
            console.log("LANDING_PAGE: initialize end");
        },
    });
});

