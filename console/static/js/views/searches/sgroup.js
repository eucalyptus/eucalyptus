define([
    'views/searches/generic',
], function(Search) {
    return function(sgroups) {
      return new Search(sgroups, ['all_text', 'owner'], {}, null);
    }
});
