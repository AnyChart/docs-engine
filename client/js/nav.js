var page = location.pathname;

function loadPage(link) {
    if (page == link) return true;
    page = link;
    window.history.pushState(null, null, link);

    if (link.indexOf("search?q=") != -1) {
        var query = link.substr(link.indexOf("search?q=") + "search?q=".length);
        console.log(query);
        searchFor(query);
        return false;
    }
    $.get(link + "-json", function(res) {
        $("#content").html(res.page.content);
        $(window).scrollTop(0);
        fixLinks();
        fixToc();
        prettyPrint();
    });
    return false;
};

function fixLinks() {
    $("#content a").each(function() {
        var $this = $(this);
        if ($this.attr("href") && $this.attr("href").match(/^[a-zA-Z]/gi)) {
            $this.click(function() {
                var res = false;
                try {
                    var current = location.pathname.split("/");
                    current.pop();
                    res = loadPage(current.join("/") + "/" + $this.attr("href"));
                    expandMenu($this.attr("href"));
                }catch (e) {
                    console.error(e);
                }
                return res;
            });
        }
    });
};

window.onpopstate = function(e) {
    loadPage(location.href);
};

fixLinks();
