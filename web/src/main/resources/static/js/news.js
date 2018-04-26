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
      var htmlwithwarning = "<div class=\"alert alert-warning alert-dismissible\" role=\"alert\"> <button type=\"button\" class=\"close\" data-dismiss=\"alert\" aria-label=\"Close\"><span aria-hidden=\"true\">&times;</span></button> <strong>Announcement:</strong> A new version of this website is available now at <a href=\"http://oncotree.mskcc.org/\" class=\"alert-link\">http://oncotree.mskcc.org/</a>. On May 31, 2018 this version of the website will no longer be available at this location. Please update your bookmarks accordingly. </div>" + html;
      $("#news").html(htmlwithwarning);
  }
}

$(document).ready(function() {
    loadNews(displayNews);
    $('#api-embed-dev').css('height', $(window).height() +'px');
    $('#api-embed-dev').css('width', ($(window).width() - 500) +'px');
});
