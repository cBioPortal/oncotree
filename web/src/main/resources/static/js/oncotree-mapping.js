$(function (){

	$('#mappingsBtn').on('click',function (e){   
		
		var vocabularyId = $('#vocabularyId').val();
		var conceptId = $('#conceptId').val();
		var histologyCode = $('#histologyCode').val();
		var siteCode = $('#siteCode').val();
		
		var host = window.location.host;
		var path = window.location.pathname;
        path = path.substring(0, path.lastIndexOf('/')+1); // now we have a filename that needs dropping
		var protocol = window.location.protocol;
		path = path == null ? '/' : path;
		
		var url = protocol + '//' + host + path + 'api/crosswalk?' + 'vocabularyId=' + vocabularyId + '&conceptId=' + conceptId + '&histologyCode=' + histologyCode + '&siteCode=' + siteCode;
		
		   $.ajax({
		   type:'GET',
		   url :url,
		   dataType: 'json',
		   success: function(data) {
		        var content = '';
		        var results = data['oncotreeCode'];
		        
		        if( results.length == 0 ){
		        	$("#mappings").val("No Mapping Oncotree Concepts Found")
		        	
		        	$("#mapping-api").empty();
			        $("#mapping-api").append('<blockquote><p>The API call to get this mapping programmatically</p></blockquote><p><code> ' + url + '</code></p>')
			        
			        $("#result").empty();
			        $("#result").append('<blockquote><p>And the result</p></blockquote>');
			        
			        $("#resJson").hide();
			        $("#resJson").html(JSON.stringify(data, undefined, 2));
			        $("#resJson").show();
		        
		        	return;
		        }
		        for( var i = 0; i < results.length; i++ ){
		        	content += results[i] + '\n';
		        }
		        
		        $("#mappings").val(content);
		        
		        $("#mapping-api").empty();
		        $("#mapping-api").append('<blockquote><p>The API call to get this mapping programmatically</p></blockquote><p><code> ' + url + '</code></p>')
		        
		        $("#result").empty();
		        $("#result").append('<blockquote><p>And the result</p></blockquote>');
		        
		        $("#resJson").hide();
		        $("#resJson").html(JSON.stringify(data, undefined, 2));
		        $("#resJson").show();
		   },
		   error:function(exception){console.log('Exception:'+exception);}
	}); 
	 e.preventDefault();
	});
});
