var page = location.pathname;

function highlightCode() {
    $("#content pre").addClass("prettyprint");
    prettyPrint();

    $("div.iframe .btns").each(function() {
        var href = $(this).find(".btn-playground:not(.jsfiddle-btn)").attr("href").replace(/-plain$/, "");
        $(this).find(".jsfiddle-btn").attr("href", href + "-jsfiddle");
    });
};

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
    expandMenu(location.pathname);
    $.get(link + "-json", function(res) {
        $("#content").html('<div id="table-of-content-small" class="hidden-lg"></div>'+
                           '<div class="row">'+
                           '  <div class="col-lg-17">'+res.page.content+'</div>'+
                           '  <div id="table-of-content-large" class="col-lg-6 hidden-sm hidden-xs hidden-md visible-lg"></div>'+
                           '</div>');
        document.title = res.url + " - AnyChart JavaScript Chart Documentation ver. " + version;
        $("#content").scrollTop(0);
        fixLinks();
        fixToc();
        fixHeaders();
        highlightCode();
    });
    $("#bar").hide();
    $("#shadow").hide();
    return false;
};

function fixLinks() {
    $("#content a").each(function() {
        var $this = $(this);
        if ($this.attr("href") && $this.attr("href").match(/^[a-zA-Z]/gi)) {
            if ($this.attr("href").indexOf("#") == 0) return;
            if ($this.attr("href").indexOf("http://") == 0) return;
            if ($this.attr("href").indexOf("https://") == 0) return;
    
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
    if (location.pathname == page) return;
    loadPage(location.href);
};

fixLinks();
highlightCode();
