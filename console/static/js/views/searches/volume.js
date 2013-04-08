define([
  'views/searches/generic',
  'views/searches/tagsearch'
], function(Search, TagSearch) {
  
    var config = {
      facets : ['all_text', 'attach_data'],
      propertyForFacet : {
        attach_data : 'status'
      },
      localize : function(what) {
        switch (what) {
          case 'attach_data' : 
            return 'Attachment';
        }
        // 'Attached' is actually an object
        // of some sort
        if (typeof what === 'object') {
          return what.toString()
        }
      }
    };
  
    return function(volumes) {
      return new Search(volumes, new TagSearch(config, volumes));
    }
});
