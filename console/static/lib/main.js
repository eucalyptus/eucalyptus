var old = alert;

alert = function() {
      console.log(new Error().stack);
      old.apply(window, arguments);
};

require.config({
        waitSeconds: 20,
        baseUrl: '../js',
        paths: {
		'underscore': '../lib/underscore-1.4.3',
		'backbone': '../lib/backbone-1.0',
		'backbone-validation': '../lib/backbone-validation',
        'visualsearch' : '../lib/visualsearch',
        'rivets' : '../lib/rivets',
        'rivetsbase' : '../lib/rivetsbase',
        'text' : '../lib/text',
        'order' : '../lib/order',
        'domReady' : '../lib/domReady',
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

require(['underscore', 'backbone', 'backbone-validation'], function(_, Backbone) {
    _.extend(Backbone.Model.prototype, Backbone.Validation.mixin);
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
