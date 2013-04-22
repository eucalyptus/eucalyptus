define(['app', 'dataholder'], function(app, dh) {
  var self = this;
  
  function isArray(o) {
    return o && typeof o === 'object' 
            && typeof o.forEach === 'function' 
            && typeof o.length === 'number';
  }
  
  return function(images, config /*, allowedFacetNames, localizer, explicitFacets, searchers, propertyMapping*/) {
    var self = this;
    searchContext = self;
    
    if (isArray(config)) {
      var nue = {
        facets : config
      }
      config = nue;
    }
    
    // Make sure if the argument isn't there, that at least
    // something works
    if (!config.facets) {
      config.facets = [];
      var done = {};
      images.forEach(function(img) {
        for (var key in img) {
          if (!isIgnored(key, img, done)) {
            config.facets.push(key);
            done[key] = true;
          }
        }
      })
    }
    
    var sortKeyList = function(list, keyName) {
      return _.chain(list)
              .sort(keyName)
              .uniq()
              .value()
    }

    function localize(name) {
      var result = name;
      if (config.localize) {
        if (typeof config.localize === 'function') {
          var localized = config.localize(name);
          if (localized) {
            return localized;
          }
        } else if (config.localize[name]) {
          return config.localize[name];
        }
      }
      function capitalize(words) {
        for (var i = 0; i < words.length; i++) {
          words[i] = words[i].charAt(0).toUpperCase() + words[i].slice(1);
        }
        return words;
      }
      result = capitalize(name.split(/_/g)).join(' ');
      result = capitalize(result.split(/-/g)).join(' ');
      result = capitalize(result.split(/\./g)).join(' ');
      result = result.replace(/&#x2f;/g, '/')
      result = result.replace(/&#.{0,3};/g, '')
      return result;
    }

    var siftKeyList = function(list, search) {
      return _.chain(list).filter(function(item) {
        return RegExp('.*' + search + '.*', 'i').test(item.value);
      }).map(function(item) {
        return item === '' ? {value: item, label: 'None'} : item
      }).value();
    }

    function isIgnored(key, on, done) {
      return /^_.*/.test(key) 
              || typeof on[key] === 'function'
              || done[key];
    }
    
    var searchOptions = {
      preserveOrder : true
    };

    function deriveFacets() {
      var derivedFacets = [];
      config.facets.forEach(function(facet) {
        derivedFacets.push({label: localize(facet), value: facet});
      });
      var appended = [];
      if (typeof config.facetsCustomizer === 'function') {
        function add(property, label) {
          derivedFacets.push({value: property, label: label ? label : localize(property)});
        }
        function append(property, label, category) {
          var atts = {
            value: property, 
            label: label ? label : localize(property)
          };
          if (category) atts.category = category;
          appended.push(atts);
        }
        config.facetsCustomizer.apply(self, [add, append]);
      }
      var result = derivedFacets;
      derivedFacets.sort(function(a, b) {
        return a.label > b.label ? 1 : a.label < b.label ? -1 : 0;
      })
      appended.forEach(function(additional) {
        result.push(additional);
      });
      return _.uniq(result);
    }

    function findMatches(facet, searchTerm, img, add) {
        if (config.match && typeof config.match[facet] === 'function') {
          var doneMatching = config.match[facet].apply(self, [searchTerm, img, add]);
          if (doneMatching) {
            return;
          }
        }
        var val = img[facet];
        if (val && typeof val !== 'object' && typeof val !== 'function') {
            add(val);
        } else if (isArray(val)) {
            val.forEach(function(item) {
              add(item, localize(item + ''));
            });
        } else if (typeof val === 'object') {
          if (config.propertyForFacet && config.propertyForFacet[facet]) {
              var nm = val[config.propertyForFacet[facet]];
              if (nm) { // avoid null
                add(val + '', localize(nm));
              }
          }
        } else if (val == undefined && facet.indexOf('_tag') != -1) {
            var sfacet = facet.replace(' _tag','');
            _.each(img.tags,function(t) { 
                if (t.name == sfacet) add(t.value); 
            });
        }

    }

    function deriveMatches(facet, searchTerm) {
      var result = [];
      var found = [];
      images.toJSON().forEach(function(img) {
        img.tags = img.tags.toJSON();
        findMatches(facet, searchTerm, img, function(val, label){
          if (found.indexOf(val) < 0) {
            found.push(val);
            result.push({name : facet, value : val, label : label ? label : localize(val) })
          }
        });
      });
      result = sortKeyList(result, 'label')
      return siftKeyList(result, searchTerm);
    }

    var MAX_RECURSE = 5;
    function drillThrough(obj, rex, depth) {
      if (MAX_RECURSE === depth) {
        return false;
      }
      var result = false;
      switch (typeof obj) {
        case 'string' :
          result = rex.test(obj);
          break;
        case 'number' :
          result = rex.test('' + obj)
          break;
        case 'object' :
          if (isArray(obj)) {
            for (var i = 0; i < obj.length; i++) {
              result = drillThrough(obj[i], rex, depth + 1);
              if (result) {
                break;
              }
            }
          } else {
            for (var key in obj) {
              result = drillThrough(obj[key], rex, depth + 1);
              if (result) {
                break;
              }
            }
          }
      }
      return result;
    }
    
    this.images = images;
    this.filtered = images.clone();
    this.lastSearch = '';
    this.lastFacets = new Backbone.Model({});
    this.search = function(search, facets) {
        self.lastSearch = search;
        self.lastFacets = facets;
        var jfacets = facets.toJSON();
        var results = self.images.filter(function(model) {
        return _.every(jfacets, function(facet) {
          var curr = model.get(facet.category);
          if (facet.category.indexOf('_tag') != -1) curr = model.get('tags').toJSON();
          if (config.search && config.search[facet.category]) {
            var isMatch = false;
            function hit() {
              isMatch = true;
            }
            var doneSearching = config.search[facet.category].apply(self, [search, facet.value, model.toJSON(), curr, hit]);
            if (doneSearching || isMatch) {
              return isMatch;
            }
          }
          var rex = new RegExp('.*' + facet.value + '.*', 'img');
          if ('all_text' === facet.category || 'text' === facet.category) {
            return drillThrough(model, rex, 0);
          } else {
            if (typeof curr === 'object' || typeof curr === 'array') {
              // Allow for searching inside tags and such
              return drillThrough(curr, rex, 0);
            } else {
              return rex.test(curr);
            }
          }
        });
      }).map(function(model) {
        return model.toJSON();
      });
      self.filtered.set(results);
    }
    this.facetMatches = function(callback) {
      callback(deriveFacets(), searchOptions);
    }
    
    this.valueMatches = function(facet, searchTerm, callback) {
      callback(deriveMatches(facet, searchTerm), searchOptions)
    }
    
    function up() {
      self.search(self.lastSearch, self.lastFacets);
    }
    
    images.on('add remove destroy change', up);
    images.on('sync reset', function() { /*console.log('upstream data was reset');*/ });
    up();
  }
});
