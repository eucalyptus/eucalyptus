Javascript Tests
================

These tests require [Node.js](http://nodejs.org) and [Vows](http://vowsjs.org).

Install Node (0.8 or greater) using your OS's package manager.  Vows should
install automatically if you run ``npm install`` in this directory.

To run tests, just run jstests.js

IF YOU GET AN ERROR, YOU PROBABLY DID NOT RUN ``npm install``

Adding Tests
------------

The test harness will load all the other javascript files in this directory
as Node modules.  If ``module.exports`` is a function, that function will be
invoked.  Else if ``exports.run`` is has a function called run() (as with
a Vows suite), that will be run.

To add a test, simply create a new Javascript file in this folder.

VowsJS
------

The tests are set up to use Vows.  It is a javascript testing framework.

It is different from some test frameworks in that it separates creating the
object you want to test from actually testing it.

First you create a test suite:

    var suite = vows.describe("SystemYouWillTest")

and then you add the first batch of tests.  The batch is a javascript hash of a name
for the object to be tested (hint: make it string together with the description like a sentence),
mapped to a hash with one special entry named 'topic', which creates what you want to test,
and descriptions of what the test does.

For example, if you wanted to create a test that only passes on Tuesdays, you
would write

	var vows = require ( 'vows' ), assert = require ( 'assert' );
	module.exports = vows.describe ( 'If things only work on tuesday' ).addBatch ( {
	    "A new date ": {
		topic: new Date (),
		"will have the right day of the week": function ( date ) {
		    assert.equal ( date.getDay (), 2 )
		}
	    }
	} );


If this test is not run on a Tuesday, we get a human readable description of why it failed:

	A new date 
	      ✗ will have the right day of the week 
		» expected 2, 
	  	got	 3 (==) // tuesday.js:7 
	  ✗ Broken » 1 broken (0.007s) 

