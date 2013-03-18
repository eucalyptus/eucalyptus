// Wizard (Constructor)
// ====================
// Creates a new wizard.  Pages is an optional pre-populated array of views.
// navigationController is an optional function which determines whether
// navigation is premitted.
//
// A wizard can have pages added to it.  A "page" is either a backbone view
// or a function which will create one on demand.
function Wizard(pages, navigationController, closedViewFactory) {
  var self = this;
  if (typeof pages === 'function') {
    closedViewFactory = navigationController;
    navigationController = pages;
    pages = [];
  }
  var pages = pages || [];
  var position = -1;

  if (closedViewFactory && typeof closedViewFactory !== 'function') {
    throw new Error("closedViewFactory must be undefined or a function: "
            + closedViewFactory + " - " + typeof closedViewFactory);
  }

  // size (property)
  // ---------------
  // The number of pages
  self.__defineGetter__('size', function() {
    return pages.length;
  });

  // add
  // ---
  // Add a page to this view, including an optional view that should
  // be used when the page is not active
  self.add = function(viewOrFactoryForView, viewWhenClosed) {
    var ix = pages.length;
    switch (typeof viewOrFactoryForView) {
      case 'function' :
      case 'object' :
        break;
      default :
        throw new Error("viewOrFactoryForView must be an object or function")
    }
    switch (typeof viewWhenClosed) {
      case 'function' :
      case 'undefined' :
      case 'object' :
        break;
      default :
        throw new Error('viewWhenClosed must be an object, function or undefined');
    }
    if (typeof viewOrFactoryForView !== 'function' &&
            typeof viewOrFactoryForView !== 'object') {
      throw new Error("Must be a function or an object");
    }
    var toAdd = {};

    toAdd.__defineGetter__("view", function() {
      return viewOrFactoryForView;
    });
    toAdd.__defineGetter__('closedView', function() {
      return viewWhenClosed ? viewWhenClosed : closedViewFactory;
    });
    pages.push(toAdd);
    return self;
  }

  // hasNext (property)
  // ------------------
  // Determine if there is a next page
  self.__defineGetter__("hasNext", function() {
    return position < pages.length - 1;
  });

  // hasPrev (property)
  // ------------------
  // Determine if there is a previous page
  self.__defineGetter__("hasPrev", function() {
    return position > 0;
  });

  // current (property)
  // ------------------
  // Get the current active page.  Will be null if the
  // wizard has never been shown
  self.__defineGetter__("current", function() {
    if (position === -1) {
      return null;
    }
    if (typeof pages[position] === 'undefined') {
      return null;
    }
    return pages[position].view;
  });

  // position (property)
  // ------------------
  // Get the current integer position
  self.__defineGetter__('position', function() {
    return position;
  });

  // currentView (property)
  // ----------------------
  // Get the view that should currently be used for an index.  If it is
  // the current  position, returns the view;  if not, returns the closed
  // view (if any)
  self.currentView = function(index) {
    return index === position ? self.view(index) : self.closedView(index);
  }

  // view (property)
  // ---------------
  // Get the view for a given index
  self.view = function(index) {
    if (index < 0) {
      throw new Error("Index " + index + " is less than 0")
    }
    if (index >= pages.length) {
      throw new Error("Index " + index + " larger than size " + pages.size)
    }
    return pages[index].view;
  };

  // closedView (property)
  // ---------------
  // Get the closed view for a given index
  self.closedView = function(index) {
    if (index < 0) {
      throw new Error("Index " + index + " is less than 0")
    }
    if (index >= pages.length) {
      throw new Error("Index " + index + " larger than size " + pages.size)
    }
    return pages[index].closedView;
  }

  // views (property)
  // ---------------
  // Get the list of views for this wizard.  This will be "closed" views for
  // all but the current index
  self.__defineGetter__('views', function() {
    var res = [];
    for (var i = 0; i < pages.length; i++) {
      res.push(i === position ? pages[i].view : pages[i].closedView);
    }
    return res;
  })

  // show
  // ----
  // Initialize this wizard to zero if necessary and return the current view
  self.show = function() {
    if (position === -1) {
      position = 0;
    }
    return self.current;
  }

  // goTo
  // ----
  // Navigate to a specific page.  Throws an error
  // if out of range.
  self.goTo = function(index) {
    if (index < 0 || index >= pages.length) {
      throw new Error("Out of range: " + index + " max " + pages.length);
    }
    var offset = -(position - index);
    if (!navigationController || navigationController(offset)) {
      position = offset;
    }
    return self.current;
  }

  // next
  // ----
  // Navigate to the next page and return its view.  Throws an error
  // if out of range.
  self.next = function() {
    if (position === pages.length - 1) {
      throw new Error("Nav past end: " + pages.length);
    }
    if (!navigationController || navigationController(1)) {
      position++;
    }
    return self.current;
  }

  // prev
  // ----
  // Navigate to the preceding page and return its view.  Throws an error
  // if out of range.
  self.prev = function() {
    if (position <= 0) {
      throw new Error("Nav before start: " + pages.length);
    }
    if (!navigationController || navigationController(-1)) {
      position--;
    }
    return self.current;
  }

  // Creates a mapping of { a : '#a'} and so forth based on the arguments array
  function trivialMapping() {
    var result = {};
    for (var i = 0; i < arguments.length; i++) {
      result[arguments[i]] = '#' + arguments[i];

    }
    return result;
  }

  // Merge b's properties into a
  function merge(a, b) {

    if (b) {
      for (var key in b) {
        // avoid immediately resolving b's contents
        a.__defineGetter__(key, function() {
          return b[key];
        });
      }
    }
    return a;
  }

  self.makeView = function(element, options, template, mapping) {
    // Assume default component names, but let mapping override them if it wants
    mapping = merge(trivialMapping('nextButton', 'prevButton', 'finishButton', 'problems', 'wizardContent', 'wizardAbove', 'wizardBelow'), mapping || {});
    // Make sure options is defined
    options = options || {};
    // If there is no canFinish function, provide a default one that only
    // enables the Finish button on the last page
    if (typeof options.canFinish !== 'function') {
      options.canFinish = function(arr) {
        return position === pages.length - 1;
      };
    }
    // Predefine click handlers, using the selectors provided in the mapping
    // (if any)
    var events = {};
    events['click ' + mapping.nextButton] = function() {
      self.next();
      this.render();
    };
    events['click ' + mapping.prevButton] = function() {
      self.prev();
      this.render();
    };
    events['click ' + mapping.finishButton] = function() {
      if (options.finish) {
        options.finish()
      }
      this.render();
    };

    var Result = Backbone.View.extend({
      initialize: function() {
        self.show();
        this.$el.html(template);
        this.nextButton = this.$el.find(mapping.nextButton);
        this.prevButton = this.$el.find(mapping.prevButton);
        this.finishButton = this.$el.find(mapping.finishButton);
        this.problems = this.$el.find(mapping.problems);
        this.wizardContent = this.$el.find(mapping.wizardContent);
        this.wizardAbove = this.$el.find(mapping.wizardAbove);
        this.wizardBelow = this.$el.find(mapping.wizardBelow);
        this.render();
      },
      render: function() {
        var Type = self.current;
        var page = new Type($(this.wizardContent));
        this.wizardContent.empty();
        this.wizardContent.append(page.$el);
        this.nextButton.attr('disabled', !self.hasNext);
        this.prevButton.attr('disabled', !self.hasPrev);
        var problems = [];
        this.finishButton.attr('disabled', !options.canFinish(self.position, problems));
        this.problems.empty();
        for (var i = 0; i < problems.length; i++) {
          var problem = problems[i];
          var problemLabel = $('<span>', {
            text: problem,
            id: 'problem-' + i,
            class: 'error-msg wizardProblem'
          });
          this.problems.append(problemLabel);
        }

        this.wizardAbove.empty();
        this.wizardBelow.empty();
        for (var i = 0; i < self.position; i++) {
          var CV = self.closedView(i);
          this.wizardAbove.append(new CV($(this.wizardAbove)).$el);
        }
        for (var i = self.position + 1; i < pages.length; i++) {
          var CV = self.closedView(i);
          this.wizardBelow.append(new CV($(this.wizardBelow)).$el);
        }

      },
      events: events
    });
    return new Result(element);
  };
}

function trivialClosedViewFactory(index) {
  return Backbone.view.extend({
    render: function() {
      $(this.el).append('<h1>Page ' + index + "</h1>");
    }
  });
}

// Modules, for tests with node
if (typeof module !== 'undefined' && typeof module.exports !== 'undefined') {
  module.exports.Wizard = Wizard;
}
