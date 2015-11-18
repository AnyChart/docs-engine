var $search = $("div.search");

function initSearch() {
    $('#content button').click(function() {
        history.back();
    });
    fixLinks();
};

function searchFor(query) {
     if (query) {
        $("#content").html("Searching...");
        window.history.pushState(null, null, "/" + version + "/search?q=" + query);
        $.post("/" + version + "/search-data", {"q": query}, function(res) {
            $("#content").html('<button type="button" class="btn btn-default btn-blue visible-xs"> <i class="glyphicon glyphicon-arrow-left"></i> Back</button><h1 class="search"><button type="button" class="btn btn-default btn-blue hidden-xs"> <i class="glyphicon glyphicon-arrow-left"></i> Back</button>Search results for <span>' + query + '</span> </h1>');
            if (res.length) {
                $(res).each(function() {
                    $("#content").append('<div class="result-block"><h2><a href="./' + this.url + '"> ' + this.url + '</a></h2><p>' + this.sn + '</p></div>');
                });
            }else {
                $("#content").append('Nothing found');
            }
            initSearch();
            
        });
        
    }
};

$search.find("button").click(function() {
    var query = $(this).parent().parent().find("input").val();
    searchFor(query);
    return false;
});

$(".404-search").find("button").click(function() {
    var query = $(this).parent().parent().find("input").val();
    location.href = "/latest/search?q=" + query;
    return false;
});
