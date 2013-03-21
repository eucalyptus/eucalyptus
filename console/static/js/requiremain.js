console.log('REQUIRE CONFIG');
require.config({
        baseUrl: 'js',
/*
        paths: {
            'rivets': 'rivets.js',
            'rivetsbase': 'rivetsbase.js'
        },
*/
        shim: {
                rivetsbase : {
                       exports: 'rivets',
                },
                rivets : {
       			deps: ['rivetsbase'],
                        exports: 'rivets'
                },
	}
});
