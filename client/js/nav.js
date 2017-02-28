var page = location.pathname;

function highlightCode() {
    $("#content pre").addClass("prettyprint");
    prettyPrint();

    $("div.iframe .btns").each(function() {
        var href = $(this).find(".btn-playground:not(.jsfiddle-btn)").attr("href").replace(/-plain$/, "");
        $(this).find(".jsfiddle-btn").attr("href", href + "-jsfiddle");
    });
}

function loadPage(link, needPushState) {
    needPushState = (typeof needPushState !== 'undefined') ?  needPushState : true;
    if (page == link) return true;
    page = link;
    if (link.indexOf("search?q=") != -1) {
        var query = link.substr(link.indexOf("search?q=") + "search?q=".length);
        searchFor(query, needPushState);
        return false;
    }else{
        if (needPushState){
            window.history.pushState(null, null, link);
        }
    }
    expandMenu(location.pathname);
    $.get(link + "-json", function(res) {
        $("#content").html('<div class="row">'+
                           '<div class="col-lg-17" id="article-content">' +
                               '<a class="btn btn-default btn-small github-fork pull-right hidden-xs" id="github-edit" href="https://github.com/AnyChart/docs.anychart.com">'+
                               '<span><i class="ac ac-net"></i></span> Improve this Doc'+
                               '</a>'
                           +res.page.content+'</div>'+
                            '<div id="disqus_thread" class="col-lg-17"></div>'+
                            '<script>'+
                            '(function() { '+
                                'DISQUS.reset({' +
                                'reload: true,' +
                                'config: function () {' +
                                    'this.page.url = "http://docs.anychart.com/' + res.page.url + '";' +
                                    'this.page.identifier = "' + res.page.url + '".split("\/").join("_").split("%").join("_").toLowerCase();'+
                                    'this.page.title = "' + res.page.url + '".split("_").join(" ").split("/").join(" - ");'+
                                    'this.language = "en";'+
                                '}});'+
                            '})();'+
                            'tryUpdateSampleInit();' +
                            '</script>'+
                            '<noscript>Please enable JavaScript to view the <a href="https://disqus.com/?ref_noscript" rel="noclilow">comments powered by Disqus.</a>'+
                            '</noscript>'+
                            '  <div class="col-lg-6 hidden-sm hidden-xs hidden-md visible-lg"><div id="table-of-content-large"></div></div>'+
                           '</div>');
        document.title = res['title-prefix'] + " | AnyChart Documentation ver. " + version;
        $("#content").scrollTop(0);
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

function updateMenu(versions){
    var menu = $("ul.dropdown-menu").empty();
    versions.forEach(function(item, i, arr) {
        menu.append("<li><a href=" + item.url + ">Version " + item.key +"</a></li>");
    })
}

function fixLinks() {
    $("#content a").each(function() {
        var $this = $(this);
        if ($this.attr("href") && $this.attr("href").match(/^[a-zA-Z]/gi)) {
            if ($this.attr("href").indexOf("#") >= 0) return;
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
}

window.onpopstate = function(e) {
    if (location.pathname == page) return;
    loadPage(location.href, false);
};

fixLinks();
highlightCode();
