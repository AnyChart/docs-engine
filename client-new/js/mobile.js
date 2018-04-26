var showPage = true;

function toggleMobileMenu() {
    showPage = !showPage;
    if (showPage) {
        hideMobileMenu();
    } else {
        showMobileMenu();
    }
}

function showMobileMenu() {
    showPage = false;
    $(".left-sidebar-container").show();
    $("#page-content").hide();
    $(".ac.ac-bars").hide();
    $(".ac.ac-remove").show();
    // make nice view under white shadow
    $("#page-content").addClass("show-tablet-menu");
    $(".right-bar-side").addClass("show-tablet-menu");
    $(".mobile-search-container").addClass("show-tablet-menu");
}

function hideMobileMenu() {
    showPage = true;
    $(".left-sidebar-container").hide();
    $("#page-content").show();
    $(".ac.ac-bars").show();
    $(".ac.ac-remove").hide();
    // make nice view under white shadow - return back
    $("#page-content").removeClass("show-tablet-menu");
    $(".right-bar-side").removeClass("show-tablet-menu");
    $(".mobile-search-container").removeClass("show-tablet-menu");
}

$(".sidebar-switcher").click(function() {
    toggleMobileMenu();
});

$(".white-shadow").click(function() {
    toggleMobileMenu();
});

$(window).keyup(function(e) {
    if (e.keyCode == 27) {
        toggleMobileMenu();
    }
});