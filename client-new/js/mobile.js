function toggleMobileMenu(){
    $(".left-sidebar-container").toggle();
    $("#shadow").toggle();
    $("#page-content").toggle();
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