
"use strict";
$(document).ready(function(){
    var AVAILABLE_TABS = {
          tree : 0,
          mapping : 0,
          news : 0,
          api : 0
        };
    var DEFAULT_TAB_TO_SHOW = "tree";
    var DEFAULT_VERSION_TO_SHOW = "oncotree_latest_stable";
    var displayed_version = '';
    var displayed_search_term = '';
    var initial_search_timer = NaN;
    var initial_search_timer_inhibit_warning = false;
    var available_versions = {};
    var location_ = window.location.href;

    loadVersions(function() {
      checkURL();
      tree.init(displayed_version);
      setDisplayedSearch(displayed_search_term);
      initVersionsLink();
      initEvents();
      OutJS.backToTop();
      initQtips();
    });

    $(function () {
      $('#tree-tab').click(function (e) {
        window.location.hash = "/home";
      })
      $('#mapping-tab').click(function (e) {
         window.location.hash = "/home?tab=mapping";
      })
      $('#news-tab').click(function (e) {
        window.location.hash = "/home?tab=news";
      })
      $('#api-tab').click(function (e) {
        window.location.hash = "/home?tab=api";
      })
    });

    function loadVersions(callback) {
      $.get('api/versions')
        .done(function(data) {
          if (data instanceof Array) {
            available_versions = data.reduce(
              function(acc, cur) {
                acc[cur.api_identifier] = cur;
                return acc;
              },
              {}
            );
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

    function initVersionsLink() {
      var option_list_html = [];
      var sorted_version_list = Object.keys(available_versions);
      sorted_version_list.sort(
        function(a,b) {
          if (available_versions[a].visible && !available_versions[b].visible) {
            return -1;
          }
          if (available_versions[b].visible && !available_versions[a].visible) {
            return 1;
          }
          return available_versions[b].release_date.localeCompare(available_versions[a].release_date);
        });
      var previous_version_was_visible = false;
      sorted_version_list.forEach(
        function(item) {
          var _hash = '#/home?version=' + item;
          var selected_attribute = '';
          if (displayed_version === item) {
            selected_attribute = ' selected';
          }
          var option = '<option data-desc="' + available_versions[item].description + '" class="item" hash="' + _hash + '" ' + selected_attribute + '>' + item + '</option>';
          var this_item_is_visible = available_versions[item].visible;
          if (!this_item_is_visible && previous_version_was_visible) {
            option_list_html.push('<option class="item" hash="disabled" disabled>' + '&HorizontalLine;'.repeat(10) + '</option>')
          }
          option_list_html.push(option);
          previous_version_was_visible = this_item_is_visible;
        });
      $('#other-version .other-version-content').html(option_list_html.join(''));
      $('#oncotree-version-note').append($("#other-version .other-version-content :selected").data("desc"));
      $('#other-version .other-version-content').change(function() {
        var _hash = $(this)[0].selectedOptions[0].attributes['hash'].value;
        window.location.hash = _hash;
        window.location.reload();
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
        if (_content.length > 0) {
          $("#searchRemoveIcon").show();
        } else {
          $("#searchRemoveIcon").hide();
          OutJS.search();
          OutJS.backToTop();
        }
      });
    }

    function setDisplayedSearch(search_term) {
      $("#tumor_search input").val(search_term);
      var initial_search_timer = window.setInterval(function(e) {
        if (OutJS.readyForSearch()) {
          OutJS.search();
          OutJS.backToTop();
          if (isNaN(initial_search_timer)) {
            if (!initial_search_timer_inhibit_warning) { 
                window.console.log("warning : could not clear the initial search interval - timer handle not captured");
            }
            initial_search_timer_inhibit_warning = true;
          } else {
            window.clearInterval(initial_search_timer);
          }
        }
      }, 128);
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

    function parseQueryParameters(queryParametersString) {
      var parameters = queryParametersString
        .split('&')
        .filter(
          function(item) {
            return !!item;
          })
        .map(
          function(item) {
            var content = item.trim().split('=');
            return {
              key: content[0],
              val: content[1] || ''
            }
          })
        .filter(
          function(item) {
            return !!item.key;
          })
        .reduce(
          function(acc, cur) {
            acc[cur.key] = cur;
            return acc;
          },
          {});
        return parameters;
    }

    function constructHash(displayed_tab) {
      var tab_clause = '';
      var version_clause = '';
      var search_term_clause = '';
      if (displayed_tab !== DEFAULT_TAB_TO_SHOW) {
        tab_clause = 'tab=' + displayed_tab;
      }
      if (displayed_version !== DEFAULT_VERSION_TO_SHOW) {
        version_clause = 'version=' + displayed_version;
      }
      if (displayed_search_term) {
        search_term_clause = 'search_term=' + displayed_search_term;
      }
      var new_hash = '/home';
      var initial_new_hash_length = new_hash.length;
      for (var clause of [tab_clause, version_clause, search_term_clause]) {
        if (clause) {
          if (new_hash.length === initial_new_hash_length) {
            new_hash += '?' + clause;
          } else {
            new_hash += '&' + clause;
          }
        }
      }
      return new_hash;
    }

    function checkURL() {
      var displayed_tab = DEFAULT_TAB_TO_SHOW;
      displayed_version = DEFAULT_VERSION_TO_SHOW;
      var starting_hash = window.location.hash;
      var hashRegexStr = /^#\/home\?(.+)/;
      if (new RegExp(hashRegexStr, 'gi').test(starting_hash)) {
        // there are additional parameters
        var match = new RegExp(hashRegexStr, 'gi').exec(starting_hash);
        var parameters = parseQueryParameters(match[1]);
        if ('tab' in parameters && parameters.tab.val in AVAILABLE_TABS) {
          displayed_tab = parameters.tab.val;
        }
        if ('search_term' in parameters && parameters.search_term.val) {
          displayed_search_term = parameters.search_term.val;
        }
        if ('version' in parameters && parameters.version.val in available_versions) {
          displayed_version = parameters.version.val;
        }
      }
      $("#tabs a[href='#" + displayed_tab + "']").tab('show');
      window.location.hash = constructHash(displayed_tab);
    }
});

$(document).keypress(function(e) {
  if (e.which == 13) {
    OutJS.search();
    OutJS.backToTop();
  }
});

var OutJS = (function() {
  function readyForSearch() {
    return tree.readyForSearch();
  }

  function search() {
    var searchKeywards = $('#tumor_search input').val().toLowerCase();
    var result = tree.search(searchKeywards);
    var resutlLength = result.length;
    var infoText = (resutlLength === 0 ? "No" : resutlLength) + " result" + (resutlLength <= 1 ? "" : "s" );
    $("#searchResult").hide();
    $("#searchResult").css('z-index', 1);
    if (searchKeywards.length > 0) {
      $("#searchResult").text(infoText);
      $("#searchResult").css('z-index', 2);
      $("#searchResult").show();
    }
    result = null;
  }

  function backToTop() {
    // TODO this never really hides itself since we removed .affix({ offset: {top:100} });
    if (($(window).height() + 100) < $(document).height() || ($(window).width() + 50) < $(document).width()) {
      $('#top-link-block').removeClass('hidden');
      $('#top-link-block').addClass('fixed-bottom');
    } else {
      $('#top-link-block').removeClass('fixed-bottom');
      $('#top-link-block').addClass('hidden');
    }
  }

  return {
    readyForSearch: readyForSearch,
    search: search,
    backToTop: backToTop
  };
})();
