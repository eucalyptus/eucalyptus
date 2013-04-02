define([
    'views/searches/generic',
], function(Search) {
    return function(keypairs) {
      return new Search(keypairs, [], {}, null);
    }
});
