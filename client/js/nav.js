var page = location.pathname;

function loadPage(link) {
    if (page == link) return;
    page = link;
    window.history.pushState(null, null, link);
    
    $.get(link + "-json", function(res) {
        $("#content").html(res.page.content);
        fixLinks();
        fixToc();
        prettyPrint();
    });
};

function fixLinks() {
    $("#content a").each(function() {
        var $this = $(this);
        if ($this.attr("href") && $this.attr("href").match(/^[a-zA-Z]/gi)) {
            $this.click(function() {
                try {
                    var current = location.pathname.split("/");
                    current.pop();
                    loadPage(current.join("/") + "/" + $this.attr("href"));
                }catch (e) {
                    console.error(e);
                }
                return false;
            });
        }
    });
};

window.onpopstate = function(e) {
    loadPage(location.href);
};

fixLinks();
