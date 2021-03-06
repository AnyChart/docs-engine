var $search = $("div.search");

function initSearch() {
    $('#page-content button').click(function() {
        history.back();
    });
    fixLinks();
}

function searchFor(query, needPushState) {
    needPushState = (typeof needPushState !== 'undefined') ? needPushState : true;
    if (query) {
        $("#page-content").html("Searching...");
        $menu.find(".active").removeClass("active");
        var url = (isUrlVersion ? "/" + version : "") + "/search?q=" + query;
        page = url;
        if (needPushState) {
            window.history.pushState(null, null, url);
        }
        $.post("/" + version + "/search-data", {"q": query}, function(res) {
            $("#page-content").html('<div class="row"><div class="col-lg-17"><button type="button" class="btn btn-default btn-blue visible-xs"> <i class="ac ac-arrow-left-thin"></i> Back</button><h1 class="search"><button type="button" class="btn btn-default btn-blue hidden-xs"> <i class="ac ac-arrow-left-thin"></i> Back</button>Search results for <span>' + query + '</span> </div></div></h1>');
            if (res.length) {
                $(res).each(function() {
                    $("#page-content .col-lg-17").append('<div class="result-block"><h2>' + this.title + '</a></h2><p>' + this.sn + '</p></div>');
                });
            } else {
                $("#page-content").append('Nothing found');
            }
            initSearch();
        });
    }
}

$search.find("button").click(function() {
    var query = $(this).parent().parent().find("input").val();
    searchFor(query);
    return false;
});

$search.find("input").keypress(function(e) {
    if (e.which == 13)
        searchFor($(this).val());
});

$(".404-search").find("button").click(function() {
    var query = $(this).parent().parent().find("input").val();
    location.href = "/latest/search?q=" + query;
    return false;
});

$(".404-search").find('input').keypress(function(e) {
    if (e.which == 13) {
        var query = $(this).val();
        location.href = "/latest/search?q=" + query;
    }
});
