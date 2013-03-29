var old = alert;

alert = function() {
      console.log(new Error().stack);
        old.apply(window, arguments);
};

console.log('REQUIRE CONFIG');
require.config({
        baseUrl: 'js',
        paths: {
		'underscore': 'underscore-1.4.3',
		'backbone': 'backbone-0.9.10',
        'visualsearch' : 'visualsearch/build/visualsearch',
		'backbone-validation': 'backbone-validation-min'
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
