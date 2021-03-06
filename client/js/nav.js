var page = location.pathname;

function highlightCode() {
    $("#page-content pre").addClass("prettyprint");
    prettyPrint();
}

function loadPage(link, needPushState) {
    //console.log("loadpage: " + link);
    needPushState = (typeof needPushState !== 'undefined') ? needPushState : true;
    if (page == link) return true;
    page = link;
    if (link.indexOf("search?q=") != -1) {
        var query = link.substr(link.indexOf("search?q=") + "search?q=".length);
        searchFor(query, needPushState);
        return false;
    } else {
        if (needPushState) {
            window.history.pushState(null, null, link);
        }
    }
    expandMenu(location.pathname);
    $.get(link + "-json", function(res) {
        $("#page-content").html('<div class="row">' +
            '<div class="col-lg-17" id="article-content">' +
            '<a class="btn btn-default btn-small github-fork pull-right hidden-xs" id="github-edit" href="https://github.com/AnyChart/docs.anychart.com">' +
            '<span><i class="ac ac-net"></i></span> Improve this Doc' +
            '</a>'
            + res.page.content + '</div>' +
            '<script>' +
            'tryUpdateSampleInit();' +
            '</script>' +
            '  <div class="col-lg-6 hidden-sm hidden-xs hidden-md visible-lg"><div id="table-of-content-large"></div></div>' +
            '</div>');
        document.title = res['title-prefix'];
        // scroll page to appropriate text
        if (location.hash.length > 1) {
            var el = $('a[href="' + location.hash + '"]');
            if (el.length > 0) {
                el[0].click();
            }
        } else {
            $("#page-content").scrollTop(0);
        }
        fixLinks();
        fixToc();
        highlightCode();
        updateMenu(res.versions);

        var url = res.url;
        $("#warning a[data-last-version=latest]").attr("href", "/latest/" + url);
    });
    $("#bar").hide();
    $("#shadow").hide();
    return false;
}

function updateMenu(versions) {
    var menu = $("ul.dropdown-menu").empty();
    versions.forEach(function(item, i, arr) {
        menu.append("<li><a href=" + item.url + ">Version " + item.key + "</a></li>");
    })
}

function fixLinks() {
    $("#page-content a").each(function() {
        var $this = $(this);
        if ($this.attr("href") && $this.attr("href").match(/^[a-zA-Z\(\)]/gi)) {
            if ($this.attr("href").indexOf("#") >= 0) return;
            if ($this.attr("href").indexOf("http://") == 0) return;
            if ($this.attr("href").indexOf("https://") == 0) return;

            $this.click(function() {
                var res = false;
                try {
                    var current = location.pathname.split("/");
                    current.pop();
                    res = loadPage(current.join("/") + "/" + $this.attr("href"));
                } catch (e) {
                    console.error(e);
                }
                return res;
            });
        }
    });
}

window.onpopstate = function(e) {
    if (location.pathname == page) return;
    loadPage(location.pathname, false);
};

fixLinks();
highlightCode();
