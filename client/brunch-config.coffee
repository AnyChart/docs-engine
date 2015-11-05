exports.config =
  sourceMaps: false
  paths:
    public: "./../resources/public/"
  files:
    javascripts:
      joinTo:
        "js/vendor.js": /bower_components/
      order:
        before: ["bower_components/jquery/dist/jquery.min.js"]
    stylesheets:
      joinTo:
        "css/app.css": /^(bower_components|src)/
  plugins:
    less:
      dumpLineNumbers: 'comments'
