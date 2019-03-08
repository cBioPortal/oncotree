function loadMarkdown(callback, url, label, flavor, divId) {
  $.ajax({
      type: 'GET',
      url: url,
  }).done(function(data) {
      if (data !== null) {
          callback(data, label, flavor, divId);
      }
  }).fail(function(data) {
      callback("An unknown error occurred. Unable to load " + label + ".", label, flavor, divId);
  });
}

function displayMarkdown(data, label, flavor, divId) {
  if (data === null) {
      $(divId).html("An unknown error occurred. Unable to download " + label + ".");
  } else {
      showdown.setFlavor(flavor);
      var converter = new showdown.Converter(),
          text = data,
          html = converter.makeHtml(text);
      $(divId).html(html);
  }
}

$(document).ready(function() {
    var newsURL = "https://raw.githubusercontent.com/cBioPortal/oncotree/master/docs/News.md";
    var oncotreeMappingToolURL = "https://raw.githubusercontent.com/cBioPortal/oncotree/master/docs/OncoTree-Mapping-Tool.md";
    loadMarkdown(displayMarkdown, newsURL, "news", "github", "#news");
    loadMarkdown(displayMarkdown, oncotreeMappingToolURL, "Oncotree Mapping tool documentation", "github", "#oncotree-mapping-tool");
});
