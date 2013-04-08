define([
    'views/searches/generic',
    'views/searches/tagsearch'
], function(Search, TagSearch) {
  return function(keypairs) {
    var config = {
      facets : ['all_text']
    }
    return new Search(keypairs, new TagSearch(config, keypairs));
  }
});
