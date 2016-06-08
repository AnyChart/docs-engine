//Because in Firefox data isn't saved between pages when using localStorage
Object.defineProperty(window, "cookieStorage", new (function () {
    var aKeys = [], oStorage = {};
    Object.defineProperty(oStorage, "getItem", {
        value: function (sKey) {
            return sKey ? this[sKey] : null;
        },
        writable: false,
        configurable: false,
        enumerable: false
    });
    Object.defineProperty(oStorage, "key", {
        value: function (nKeyId) {
            return aKeys[nKeyId];
        },
        writable: false,
        configurable: false,
        enumerable: false
    });
    Object.defineProperty(oStorage, "setItem", {
        value: function (sKey, sValue) {
            if (!sKey) {
                return;
            }
            document.cookie = escape(sKey) + "=" + escape(sValue) + "; expires=Tue, 19 Jan 2038 03:14:07 GMT; path=/";
        },
        writable: false,
        configurable: false,
        enumerable: false
    });
    Object.defineProperty(oStorage, "length", {
        get: function () {
            return aKeys.length;
        },
        configurable: false,
        enumerable: false
    });
    Object.defineProperty(oStorage, "removeItem", {
        value: function (sKey) {
            if (!sKey) {
                return;
            }
            document.cookie = escape(sKey) + "=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/";
        },
        writable: false,
        configurable: false,
        enumerable: false
    });
    this.get = function () {
        var iThisIndx;
        for (var sKey in oStorage) {
            iThisIndx = aKeys.indexOf(sKey);
            if (iThisIndx === -1) {
                oStorage.setItem(sKey, oStorage[sKey]);
            }
            else {
                aKeys.splice(iThisIndx, 1);
            }
            delete oStorage[sKey];
        }
        for (aKeys; aKeys.length > 0; aKeys.splice(0, 1)) {
            oStorage.removeItem(aKeys[0]);
        }
        for (var aCouple, iKey, nIdx = 0, aCouples = document.cookie.split(/\s*;\s*/); nIdx < aCouples.length; nIdx++) {
            aCouple = aCouples[nIdx].split(/\s*=\s*/);
            if (aCouple.length > 1) {
                oStorage[iKey = unescape(aCouple[0])] = unescape(aCouple[1]);
                aKeys.push(iKey);
            }
        }
        return oStorage;
    };
    this.configurable = false;
    this.enumerable = true;
})());

if (navigator.userAgent.indexOf("Firefox") > -1) {
    //console.log("cookieStorage");
    window["storage"] = window["cookieStorage"];
} else {
    //console.log("localStorage");
    window["storage"] = window["localStorage"];
}

var open_folders = [];

function supports_html5_storage() {
    try {
        return 'localStorage' in window && window['localStorage'] !== null;
    } catch (e) {
        return false;
    }
}

function getOpenFolders() {
    var item = window['storage'].getItem("anychart_docs_local");
    if (item != null) {
        return JSON.parse(item);
    }
    return null;
}

function saveOpenFolders(folders) {
    window['storage'].setItem("anychart_docs_local", JSON.stringify(folders));
}

function expandCurrentMenu(prefix_path, target) {
    $menu.find(".active").removeClass("active");
    target = target.split("/");
    var path = [];
    for (var i = target.length - 1; i >= 0; i--) {
        if (target[i] == version)
            break;
        path.push(target[i]);
    }
    path = path.reverse();
    var $el;
    var str = "";
    var href;
    for (var i = 0; i < path.length; i++) {
        if (i == 0) str = path[i];
        else str += "/" + path[i];
        href = prefix_path + str + ".html";
        $el = $menu.find("a[href='" + href + "']");
        var $ul = $el.parent().find(">ul");
        if ($ul.length && !$ul.is(":visible")) {
            $ul.toggle();
            $el.find("i").removeClass('fa-folder').addClass('fa-folder-open');
            addFolder($el.attr("href").substr(prefix_path.length));
        }
    }
    $el.addClass("active");
}

function addFolder(href) {
    if (open_folders.indexOf(href) == -1) {
        open_folders.push(href);
        //console.log("add folder: " + href + " " + open_folders);
        saveOpenFolders(open_folders);
    }
}

function removeFolder(href) {
    var ind = open_folders.indexOf(href);
    if (ind > -1) {
        open_folders.splice(ind, 1);
        removeFolder(href); //if there are two
    } else {
        //console.log("remove folder: " + href + " " + open_folders);
        saveOpenFolders(open_folders);
    }
}

function addListeners(prefix_path) {
    $menu.find('a>i.fa-folder').each(function () {
        var $this = $(this);
        var $link = $this.parent();
        var $ul = $link.parent().find(">ul");
        var href = $link.attr("href").substr(prefix_path.length);
        $link.click(function () {
            if ($ul.is(":visible")) {
                addFolder(href);
            } else {
                removeFolder(href);
            }
            return false;
        });
    });
}

function openFolders(prefix_path, folders) {
    for (var i = 0; i < folders.length; i++) {
        var href = prefix_path + folders[i];
        $el = $menu.find("a[href='" + href + "']");
        var $ul = $el.parent().find(">ul");
        if ($ul.length && !$ul.is(":visible")) {
            $ul.toggle();
            $el.find("i").removeClass('fa-folder').addClass('fa-folder-open');
        }
    }
}

function initFolders() {
    open_folders = getOpenFolders();
    if (open_folders === null) {
        open_folders = [];
    }
    //console.log("init folders: " + open_folders);
}
