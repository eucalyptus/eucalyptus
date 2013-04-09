var Wizard = require ( '../static/js/wizard' ).Wizard,
        vows = require ( 'vows' ),
        util = require ( 'util' ),
        assert = require ( 'assert' ),
        backbone = require ( './jstests' ).mockBackbone;

// Vows is pretty simple once you get it.  Unlike other testing frameworks,
// it separates creating the object you want to test and testing it.
// So you add "batches" of tests to a suite.  A batch is a name (so
// it flows like a sentence from the suite name) and a hash.  The hash
// has one special function/object, "topic" which makes the object to test
// The rest are a mapping of description:function that takes the topic object
// and tests it

var testSuite = vows.describe ( 'Wizards' ).addBatch ( {
    'A new wizard': {
        topic: new Wizard (),
        'is an empty': function ( wizard ) {
            assert.equal ( wizard.size, 0 );
        },
        'does not have next': function ( wizard ) {
            assert.isFalse ( wizard.hasNext );
        },
        'does not have prev': function ( wizard ) {
            assert.isFalse ( wizard.hasPrev );
        },
        'does not have current': function ( wizard ) {
            assert.isNull ( wizard.current );
        },
        'has a negative position': function ( wizard ) {
            assert.equal ( wizard.position, -1 )
        }
    }
} ).addBatch ( {
    'A wizard with two views ': {
        topic: function () {
            var wiz = new Wizard ();
            var stuff = {
                wiz: wiz,
                back1: wiz.add ( {ix: 0} ),
                back2: wiz.add ( {ix: 1} )
            }
            return stuff;
        },
        'Has a size of two': function ( stuff ) {
            assert.equal ( stuff.wiz.size, 2 );
        },
        'returns itself from its add method': function ( stuff ) {
            assert.isTrue ( stuff.back1 === stuff.wiz );
            assert.isTrue ( stuff.back2 === stuff.wiz );
        },
        'returns the null from current if it has not been shown': function ( stuff ) {
            var w = stuff.wiz;
            assert.equal ( w.current, null );
        },
        'returns the first item for current after show is called': function ( stuff ) {
            var w = stuff.wiz;
            var first = w.show ();
            assert.isNotNull ( first );
            assert.equal ( first, w.current );
            assert.equal ( first.ix, 0 );
        }
    }
} ).addBatch ( {
    'Navigation': {
        topic: new Wizard ().add ( {ix: 0} ).add ( {ix: 1} ),
        'properties are initially correct': function ( wiz ) {
            assert.isTrue ( wiz.hasNext );
            assert.isFalse ( wiz.hasPrev );
        },
        'works correctly': function ( wiz ) {
            assert.equal ( wiz.position, -1 )
            var first = wiz.show ();
            assert.equal ( wiz.position, 0 )
            assert.isFalse ( wiz.hasPrev )
            assert.isTrue ( wiz.hasNext )
            var next = wiz.next ();
            assert.equal ( wiz.position, 1 )
            assert.isFalse ( next === first );
            assert.isTrue ( wiz.hasPrev )
            assert.isFalse ( wiz.hasNext );
            assert.equal ( next.ix, 1 );
            var prev = wiz.prev ();
            assert.equal ( wiz.position, 0 )
            assert.isTrue ( prev === first );
            assert.equal ( prev.ix, 0 );
            var next2 = wiz.next ();
            assert.isTrue ( next === next2 );
            assert.equal ( wiz.position, 1 )
            assert.isTrue ( wiz.hasPrev )
            assert.isFalse ( wiz.hasNext );
            try {
                wiz.next ();
                assert.isFalse ( true, 'Exception should have been thrown navigating past end' )
            } catch ( err ) {
                // ok
            }
            wiz.prev ();
            try {
                wiz.prev ();
                assert.isFalse ( true, 'Exception should have been thrown navigating before first' )
            } catch ( err ) {
                // ok
            }
        }
    }
} ).addBatch ( {
    'Navigation control': {
        topic: function () {
            var stuff = {
                enabled: false
            }
            var nav = function () {
                return stuff.enabled;
            }
            stuff.wiz = new Wizard ( nav );
            return stuff;
        },
        'can disable navigation': function ( stuff ) {
            assert.equal ( stuff.wiz.position, -1 );
            stuff.wiz.show ();
            assert.equal ( stuff.wiz.position, 0 );
            stuff.wiz.next ();
            assert.equal ( stuff.wiz.position, 0 );
            stuff.wiz.next ();
            assert.equal ( stuff.wiz.position, 0 );
            stuff.enabled = true;
            stuff.wiz.next ();
            assert.equal ( stuff.wiz.position, 1 );
            stuff.enabled = false;
            stuff.wiz.prev ();
            assert.equal ( stuff.wiz.position, 1 );
            stuff.enabled = true;
            stuff.wiz.prev ();
            assert.equal ( stuff.wiz.position, 0 );
        }
    }
} ).addBatch ( {
    'Views': {
        topic: function () {
            return new Wizard ().add ( {ix: 0}, {cx: 0} ).add ( {ix: 1}, {cx: 1} ).add ( {ix: 2}, {cx: 2} );
        },
        'are listed correctly': function ( wiz ) {
            var views = wiz.views;
            assert.isNotNull ( views );
            assert.isTrue ( util.isArray ( views ) );
            for (var i = 0; i < views.length; i++) {
                assert.typeOf ( views[i].cx, 'number' );
                assert.equal ( i, views[i].cx );
                assert.typeOf ( views[i].ix, 'undefined' );
            }
            wiz.show ();
            views = wiz.views;
            for (var i = 0; i < views.length; i++) {
                switch (i) {
                    case 0 :
                        assert.typeOf ( views[i].cx, 'undefined' );
                        assert.equal ( views[i].ix, 0 );
                        break;
                    default :
                        assert.typeOf ( views[i].cx, 'number' );
                        assert.equal ( i, views[i].cx );
                        assert.typeOf ( views[i].ix, 'undefined' );

                }
            }
        }
    }
} ).addBatch ( {
    'Closed view factories': {
        topic: function () {
            var stuff = {
                enabled: true,
                callCount: 0
            };
            var nav = function () {
                return stuff.enabled;
            };
            stuff.cvf = function ( ix ) {
                stuff.callCount++;
                return {cx: ix};
            }
            var custom = function ( ix ) {
                return {cst: ix};
            }
            stuff.wizard = new Wizard ( nav, stuff.cvf ).add ( {ix: 0}, custom ).add ( {ix: 1} ).add ( {ix: 2} );
            return stuff;
        },
        'are used if a default is not present': function ( stuff ) {
            var w = stuff.wizard;
            w.show ();
            assert.deepEqual ( w.current, {ix: 0} );
            assert.deepEqual ( w.view ( 0 ), {ix: 0} );
            assert.deepEqual ( w.closedView.apply(this, [0]).apply(this, [0]), {cst: 0} );
            assert.deepEqual ( w.closedView.apply(this, [1]).apply(this, [1]), {cx: 1} );
        }
    }
} );

module.exports = testSuite;
