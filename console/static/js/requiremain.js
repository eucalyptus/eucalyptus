console.log('REQUIRE CONFIG');
require.config({
        baseUrl: 'js',
        paths: {
        },
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
