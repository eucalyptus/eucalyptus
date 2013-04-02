define([
    'views/searches/generic',
], function(Search) {
  
    var config = {
      facets : ['all_text', 'attach_data'],
      propertyForFacet : {
        attach_data : 'status'
      },
      localize : function(what) {
        switch (what) {
          case 'attach_data' : 
            return 'Attachement';
        }
        // 'Attached' is actually an object
        // of some sort
        if (typeof what === 'object') {
          return what.toString()
        }
      }
    };
  
    return function(images) {
      return new Search(images, config);
    }
});
