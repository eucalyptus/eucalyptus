define([], function() {
  return function(config, data) {
    var self = this;

    function getTags() {
      return _.groupBy(data.reduce(function(tags, m) { 
          return _.union(tags, _.map(m.get('tags').models, function(m) { return m.toJSON(); })); 
      }, []), function(t) { 
          return t.name; 
      });
    }

    self.facetsCustomizer = function(add, append) {
      if (config.facetsCustomizer) {
        config.facetsCustomizer.apply(this, add, append);
      }
      var tags = getTags();
      var hasTags = 0;
      for (var tagName in tags) {
        hasTags++;
        if (hasTags) break;
      }
      for (var tagName in tags) {
        append(tagName + ' _tag', tagName, 'Tags');
      }
    };

    self.search = {};
    if (config.search) {
      for (var key in config.search) {
        self.search[key] = config.search[key];
      }
    }

    function searchMatch(search, item) {
      var rex = new RegExp('.*' + search + '.*', 'img');
      for (var key in item) {
        var obj = item[key];
        if (typeof obj !== 'function') {
          if (rex.test(obj + '')) {
            return true;
          }
        }
      }
      return false;
    }

    self.__defineGetter__("search", function() {
      var result = {};
      if (config.search) {
        for (var key in config.search) {
          result[key] = config.search[key];
        }
      }
      var tags = getTags();
      
      for (var tagName in tags) {
        var searchName = tagName + ' _tag';
        result[searchName] = function(search, facetSearch, item, itemsFacetValue, hit) {

          // FIXME Whoa, WTF - the search parameter is a *string* like
          // '"Name (tag)" : "foo"'
          //var extractSearchText = /.*"(.*?) _tag":\s?"(.+)"/
          // JP - 20130532 - EUCA-6378 - catch tag values with double quotes in them, as part of the name.
          var extractSearchText = /.*"(.*?) _tag":\s?['"](.+)"/
          if (extractSearchText.test(search)) {
            var sreg = extractSearchText.exec(search);
            var tagStr = sreg[1];
            var actualSearchTerm = sreg[2];

            var currSet = tags[tagStr];
            for (var i = 0; i < currSet.length; i++) {
              var oneItem = currSet[i];
              var theTags = item.tags;
              if (typeof theTags.toJSON === 'function') {
                theTags = theTags.toJSON();
              }
              theTags.forEach(function(oneTag) {
                if (oneItem.id === oneTag.id) {
                  if (searchMatch(actualSearchTerm, oneTag)) {
                    hit();
                  }
                }
              });
            }

          }
          return true;
        };
      }
      return result;
    });

    for (var key in config) {
      if (!self[key]) {
        self[key] = config[key];
      }
    }
  };

});
