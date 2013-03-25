console.log('REQUIRE CONFIG');
require.config({
        baseUrl: 'js',
        paths: {
		'underscore': 'underscore-1.4.3',
		'backbone': 'backbone-0.9.10',
		'backbone-validation': 'backbone-validation-amd-min'
        },
        shim: {
                underscore : {
                       exports: '_',
                },
                backbone : {
			deps: ['underscore'],
                	exports: 'Backbone',
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
