function toggleMobileMenu(){
    $(".left-sidebar-container").toggle();
    $("#shadow").toggle();
    $("#page-content").toggle();
}

function hideMobileMenu(){
    $(".left-sidebar-container").hide();
    $("#shadow").hide();
    $("#page-content").show();
}

$(".sidebar-switcher").click(function() {
    toggleMobileMenu();
});

$("#shadow").click(function() {
    toggleMobileMenu();
});

$(window).keyup(function(e) {
    if (e.keyCode == 27) {
        toggleMobileMenu();
    }
});