
$(document).ready(function(){
	"use strict";
    var version_ = '';
    var versions_ = {};

    loadVersions(function() {
      checkURL();
      tree.init(version_);
      initVersionsLink();
      initEvents();
      OutJS.backToTop();
      initQtips();
    });

    function loadVersions(callback) {
      $.get('api/versions')
        .done(function(data) {
          if (data instanceof Object
            && data.hasOwnProperty('data')
            && data.data instanceof Array) {
            versions_ = data.data.reduce(function(acc, cur) {
              acc[cur.api_identifier] = cur;
              return acc;
            }, {});
          }
        })
        .fail(function() {
          // Error handling
        }).always(function() {
        if (callback instanceof Function) {
          callback();
        }
      })
    }
    function initEvents() {
    	$('#tumor_search button').click(function() {
	        OutJS.search();
	    });

	    $('#expand-nodes-btn').click(function() {
	    	tree.expandAll();
    		OutJS.backToTop();
	    });

	    $('#collapse-nodes-btn').click(function() {
	    	tree.collapseAll();
    		OutJS.backToTop();
	    });

	    $("#searchRemoveIcon").hide();

	    $("#searchRemoveIcon").hover(function() {
	    	$(this).css('cursor', 'pointer');
	    });

	    $("#searchRemoveIcon").hover(function() {
	    	$(this).css('cursor', 'pointer');
	    });

	    $("#searchRemoveIcon").click(function() {
	    	$("#tumor_search input").val("");
	    	$("#searchRemoveIcon").hide();
	    	OutJS.search();
	    	OutJS.backToTop();
	    });

	    $("#tumor_search input").keyup(function () {
	    	var _content = $(this).val();
	    	if(_content.length > 0) {
		  		$("#searchRemoveIcon").show();
	    	}else {
	    		$("#searchRemoveIcon").hide();
	    		OutJS.search();
	    		OutJS.backToTop();
	    	}
		});
    }

    function initVersionsLink() {
      // Update other versions info
      var _str = [];
      var _version = version_ === '' ? 'realtime' : version_;
      Object.keys(versions_).sort().filter(function(item) {
        return item !== _version;
      }).forEach(function(item) {
        var _hash = '#/home';
        var _content = 'Lastest';

        if (item !== 'realtime') {
          _hash += '?version=' + item;
          _content = item;
        }
        _str.push('<option title="' + versions_[item].description + '" ' + 'class="item" hash="' + _hash + '">' + _content + '</option>');
      });
      $('#other-version .other-version-content').html(_str.join(''));
      $('#other-version .other-version-content').change(function() {
        var _hash = $(this)[0].selectedOptions[0].attributes['hash'].value;
        window.location.hash = _hash;
        window.location.reload();
      })
    }

    function initQtips() {
    	$('#expand-nodes-btn').qtip({
	        content:{text: "Expand all branches"},
	        style: { classes: 'qtip-light qtip-rounded qtip-shadow qtip-grey' },
            show: {event: "mouseover"},
            hide: {event: "mouseout"},
            position: {my:'bottom left',at:'top center', viewport: $(window)}
	    });

	    $('#collapse-nodes-btn').qtip({
	        content:{text: "Collapse all branches"},
	        style: { classes: 'qtip-light qtip-rounded qtip-shadow qtip-grey' },
            show: {event: "mouseover"},
            hide: {event: "mouseout"},
            position: {my:'bottom left',at:'top center', viewport: $(window)}
	    });
    }

    function checkURL() {
      var hash = window.location.hash;
      var hashRegexStr = /^#\/home\?(.+)/;

      if (new RegExp(hashRegexStr, 'gi').test(hash)) {
        // there is additional parameters, currently we only handle version
        var match = new RegExp(hashRegexStr, 'gi').exec(hash);

        // Since this passed the test, match will definitely have second element.
        var parameters = match[1].split('&').filter(function(item) {
          return !!item;
        }).map(function(item) {
          var content = item.trim().split('=');
          return {
            key: content[0],
            val: content[1] || ''
          }
        }).filter(function(item) {
          return !!item.key;
        }).reduce(function(acc, cur) {
          acc[cur.key] = cur;
          return acc;
        }, {});

        if (parameters.hasOwnProperty('version') && parameters.version.val) {
          for (var _version in versions_) {
            if (_version === parameters.version.val) {
              version_ = _version;
            }
          }
          var _hash = '/home';
          if (version_) {
            _hash += '?version=' + version_;
          }
          window.location.hash = _hash;
        }
      } else {
          version_ = 'oncotree_latest_stable';
        window.location.hash = '/home';
      }
      $('#version-name').text(version_);
    }
});

$(document).keypress(function(e) {
    if(e.which == 13) {
        OutJS.search();
        OutJS.backToTop();
    }
});

var OutJS = (function() {
	"use strict";

	function search() {
		var searchKeywards = $('#tumor_search input').val().toLowerCase(),
			result = tree.search(searchKeywards),
			resutlLength = result.length,
			infoText = (resutlLength === 0 ? "No" : resutlLength) + " result" + (resutlLength <= 1 ? "" :"s" );

		$("#searchResult").hide();
		$("#searchResult").css('z-index', 1);

		if(searchKeywards.length > 0) {
			$("#searchResult").text(infoText);
			$("#searchResult").css('z-index', 2);
        	$("#searchResult").show();
	    }
	    result = null;
	}

	function backToTop() {
		if ( 	($(window).height() + 100) < $(document).height() ||
				($(window).width() + 50) < $(document).width() ) {
		    $('#top-link-block').removeClass('hidden').affix({
		        offset: {top:100}
		    });
		}else {
			 $('#top-link-block').addClass('hidden');
		}
	}
	return {
		search: search,
		backToTop: backToTop
	};
})();
