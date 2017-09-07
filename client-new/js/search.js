var $search = $("div.search");

function initSearch() {
    $('#page-content button').click(function () {
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
        if (needPushState){
            window.history.pushState(null, null, url);
        }
        $.post("/" + version + "/search-data", {"q": query}, function (res) {
            $("#page-content").html('<div id="article-content">' +
                                        '<div id="search-content">' +
                                            '<button type="button" class="btn btn-default btn-white btn-sm visible-xs">' +
                                                '<i class="ac ac-arrow-left-thin"></i> Back</button>' +
                                            '<h1 class="search">' +
                                                '<button type="button" class="btn btn-default btn-white btn-sm hidden-xs">' +
                                                    '<i class="ac ac-arrow-left-thin"></i> Back</button>Search results for <span>' + query + '</span>' +
                                            '</h1>' +
                                        '</div>' +
                                    '</div>');
            if (res.length) {
                $(res).each(function () {
                    $("#search-content").append('<div class="result-block"><h2>' + this.title + '</h2><p>' + this.sn + '</p></div>');
                });
            } else {
                $("#search-content").append('Nothing found');
            }
            $("#table-of-content-large").hide();
            initSearch();
        });
    }
}

/*$search.find("button").click(function () {
    var query = $(this).parent().parent().find("input").val();
    searchFor(query);
    return false;
});*/

$search.find("input").keypress(function (e) {
    if (e.which == 13)
        searchFor($(this).val());
});

/*$(".404-search").find("button").click(function () {
    var query = $(this).parent().parent().find("input").val();
    location.href = "/latest/search?q=" + query;
    return false;
});*/

$(".search404").find('input').keypress(function (e) {
    if (e.which == 13) {
        var query = $(this).val();
        location.href = "/latest/search?q=" + query;
    }
});
