define([
    'views/searches/generic',
], function(Search) {
    return function(snapshots) {
      return new Search(snapshots, ['progress'], {}, null);
    }
});
