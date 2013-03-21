console.log('REQUIRE CONFIG');
require.config({
        baseUrl: 'js',
        shim: {
                rivetsbase : {
                        exports: 'rivets',
                },
                rivets : {
			deps: ['rivetsbase'],
                        exports: 'rivets',
                },
	}
});
