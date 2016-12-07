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
        $("#content").html('<div class="row">'+
                           '<div class="col-lg-17" id="article-content">' +
                               '<a class="btn btn-default btn-small github-fork pull-right hidden-xs" id="github-edit" href="https://github.com/AnyChart/docs.anychart.com">'+
                               '<span><i class="ac ac-andrews-pitchfork"></i></span> Improve this Doc'+
                               '</a>'
                           +res.page.content+'</div>'+
                            '<div id="disqus_thread" class="col-lg-17"></div>'+
                            '<script>'+
                            '(function() { '+
                                'DISQUS.reset({' +
                                'reload: true,' +
                                'config: function () {' +
                                    'this.page.url = window.location.href.split("?")[0];'+
                                    'this.page.identifier = "' + res.page.url + '".split("\/").join("_").toLowerCase();'+
                                    'this.page.title = "' + res.page.url + '".split("_").join(" ").split("/").join(" - ");'+
                                    'this.language = "en";'+
                                '}});'+
                            '})();'+
                            'tryUpdateSampleInit();' +
                            '</script>'+
                            '<noscript>Please enable JavaScript to view the <a href="https://disqus.com/?ref_noscript" rel="noclilow">comments powered by Disqus.</a>'+
                            '</noscript>'+
                            '  <div id="table-of-content-large" class="col-lg-6 hidden-sm hidden-xs hidden-md visible-lg"></div>'+
                           '</div>');
        document.title = res.title + " - AnyChart JavaScript Chart Documentation ver. " + version;
        $("#content").scrollTop(0);
        fixLinks();
        fixToc();
        highlightCode();

        var url = res.url;
        $("#warning a[data-last-version=latest]").attr("href", "/latest/" + url);
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
