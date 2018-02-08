function loadNews(callback) {
  var newsURL = "https://raw.githubusercontent.com/cBioPortal/oncotree/master/docs/News.md";
  $.ajax({
      type: 'GET',
      url: newsURL,
  }).done(function(data) {
      if (data !== null) {
          callback(data);
      }
  }).fail(function(data) {
      callback("An unknown error occurred. Unable to load news.");
  });
}

function displayNews(news) {
  if (news === null) {
      $("#news").html("An unknown error occurred. Unable to download news.");
  } else {
      showdown.setFlavor('github');
      var converter = new showdown.Converter(),
          text = news,
          html = converter.makeHtml(text);
      $("#news").html(html);
  }
}

$(document).ready(function() {
    loadNews(displayNews);
    $('#api-embed-dev').css('height', $(window).height() +'px');
    $('#api-embed-dev').css('width', ($(window).width() - 500) +'px');
});
