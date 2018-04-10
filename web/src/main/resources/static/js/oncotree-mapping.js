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
            console.log(url);
    
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
                    $("#mapping-api").append('<blockquote><p>The API call to get this mapping programmatically</p></blockquote><p><code> ' + url + '</code></p>');
                    
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
                $("#mapping-api").append('<blockquote><p>The API call to get this mapping programmatically</p></blockquote><p><code> ' + url + '</code></p>');
                
                $("#result").empty();
                $("#result").append('<blockquote><p>And the result</p></blockquote>');
                
                $("#resJson").hide();
                $("#resJson").html(JSON.stringify(data, undefined, 2));
                $("#resJson").show();
           },
           error:function(exception){
                var errorstatus = JSON.stringify(exception['status'], undefined, 2);
                var errorname = JSON.stringify(exception['responseJSON']['error'], undefined, 2);
                var message = JSON.stringify(exception['responseJSON']['message'], undefined, 2);
                var exceptionname = JSON.stringify(exception['responseJSON']['exception'], undefined, 2);
                $("#mappings").val("No Mapping Oncotree Conecepts Found");

                $("#mapping-api").empty();
                $("#mapping-api").append('<blockquote><p>The API call to get this mapping programmatically</p></blockquote><p><code> ' + url + '</code></p>');

                $("#result").empty();
                $("#result").append('<blockquote><p>And the result</p></blockquote>');

                $("#resJson").hide();
                $("#resJson").html("STATUS: " + errorstatus + "\nERROR: " + errorname + "\nMESSAGE: " + message + "\nEXCEPTION: " + exceptionname);
                $("#resJson").show();

                console.log('Exception:'+exception);
           }
    }); 
     e.preventDefault();
    });
});
