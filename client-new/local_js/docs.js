$(".dropdown-toggle").dropdown();

function fixHeaders() {
    $($("#page-content > .row > .col-lg-17 > h1").get(0)).detach().prependTo("#page-content");
}

fixHeaders();
