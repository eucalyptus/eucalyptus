#!/usr/bin/env node
var fs = require ( 'fs' ),
        util = require ( 'util' ),
        path = require ( 'path' );

var base = path.dirname ( module.filename );
console.log ( "Run tests in " + base );

var failureCount = 0;
var runningCount = 1;

// A toy mock of backbone
exports.mockBackbone = {
    View: function () {
        this.extend = function (what) {
            return what;
        };
    }
};

// Allow requirejs'isms
global['define'] = function() {
    if (typeof arguments[0] === 'function') {
        var f = arguments[0];
        return f.apply(this);
    } else if (typeof arguments[1] === 'function') {
        var f = arguments[1];
        return f.apply(this);
    } else {
        throw new Error("Don't know what to do with " + util.inspect(arguments));
    }
}

fs.readdir ( base, function ( err, files ) {
    // Make a list of js files to run
    if (err) throw err;
    var toRun = [];
    files.forEach ( function ( file ) {
        if (/^.*\.js$/.test ( file ) && file !== path.basename ( module.filename )) {
            var moduleName = /(.*)\.js$/.exec ( file )[1];
            toRun.push ( moduleName );
        }
    } );

    // Asynchronously iterate the array of tests
    function runOne () {
        // If really done, process exit
        if (toRun.length === 0) {
            return oneDone ();
        }
        var moduleName = toRun.pop ();
        console.log ( "Run " + moduleName );
        try {
            runningCount++;
            // Load it
            var suite = require ( './' + moduleName );
            // Module.exports is set to a function - just run it
            if (typeof suite === 'function') {
                try {
                    suite ();
                } finally {
                    oneDone ()
                }
            } else if (typeof suite['run'] === 'function') {
                // If it is a vows test, prep its reporter so we are informed
                // of failures
                prepareVowsSuite ( suite );
                // Run it, calling oneDone whien it completes
                suite.run ( {}, oneDone );
            }
        } catch ( err ) {
            // log it and make sure we'll exit non-zero
            failureCount++;
            console.log ( err.stack || err );
            oneDone ();
        } finally {
            process.nextTick ( runOne );
        }
    }
    runOne ();
} );

function onFailure () {
    failureCount++;
}

function oneDone () {
    // Called for each completed test suite/file
    runningCount--;
    process.nextTick ( function () {
        if (runningCount === 0) {
            if (failureCount !== 0) {
                console.log ( failureCount + ' failures.' );
            }
            process.exit ( failureCount );
        }
    } );
}

function prepareVowsSuite ( suite ) {
    // Wrapper the test report writer in vows so that we capture
    // whether it passed or failed
    if (typeof suite.reporter !== 'undefined' && typeof suite.reporter.report === 'function') {
        var old = suite.reporter.report;
        suite.reporter.report = function ( data ) {
            if (util.isArray ( data ) && data[0] === 'vow' && data[1].status !== 'honored') {
                failureCount++;
            }
            old.apply ( this, arguments );
        }
    }
}
