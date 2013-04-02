define([
    'views/searches/generic',
], function(Search) {
    return function(addresses) {
      return new Search(addresses, ['assignment'], {}, null);
    }
});
