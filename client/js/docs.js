$(".dropdown-toggle").dropdown();

function fixHeaders() {
    $($("#content > .row > .col-lg-17 > h1").get(0)).detach().prependTo("#content");
}

fixHeaders();
