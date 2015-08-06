
$(document).ready(function(){
	"use strict";

    tree.init();
    initEvents();
    OutJS.backToTop();
    initQtips();

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
	}
})();