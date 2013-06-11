var old = alert;

alert = function() {
      console.log(new Error().stack);
      old.apply(window, arguments);
};

require.config({
        baseUrl: '../js',
        paths: {
		'underscore': '../lib/underscore-1.4.4',
		'backbone': '../lib/backbone-1.0',
		'backbone-validation': '../lib/backbone-validation',
        'visualsearch' : '../lib/visualsearch',
        'rivets' : '../lib/rivets',
        'rivetsbase' : '../lib/rivetsbase',
        'text' : '../lib/text',
        'order' : '../lib/order',
        'domReady' : '../lib/domReady',
        'backbone-relational': '../lib/backbone-relational',
        },
        shim: {
                underscore : {
                       exports: '_',
                },
                visualsearch: {
                    exports: 'VS'
                },
                backbone : {
                    deps: ['underscore'],
                	exports: 'Backbone',
                },
                'backbone-relational': ['backbone'],
                'backbone-validation' : {
                    deps: ['backbone'],
                	exports: 'Backbone.Validation',
                },
                rivetsbase : {
                	exports: 'rivets',
                },
                rivets : {
                        deps: ['rivetsbase'],
                        exports: 'rivets'
                },
	}
});

require(['underscore', 'backbone', 'backbone-validation', 'backbone-relational'], function(_, Backbone) {
    _.extend(Backbone.Model.prototype, Backbone.Validation.mixin);
    //_.extend(Backbone.Model.prototype, BackboneRelational.mixin);
});

var oldClean = jQuery.cleanData;

$.cleanData = function( elems ) {
    for ( var i = 0, elem;
    (elem = elems[i]) !== undefined; i++ ) {
        //console.log('cleandata', elem);
        $(elem).triggerHandler("destroyed");
    }
    oldClean.apply(this, arguments);
};
