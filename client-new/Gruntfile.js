module.exports = function(grunt) {
    grunt.loadNpmTasks('grunt-contrib-less');
    grunt.loadNpmTasks('grunt-contrib-uglify');
    grunt.loadNpmTasks('grunt-contrib-cssmin');
    grunt.loadNpmTasks('grunt-contrib-copy');

    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),
        less: {
            production: {
                options: {
                    paths: ["bower_components/bootstrap/less", "src/less"],
                    cleancss: true
                },
                files: {
                    "styles-less.css": "src/less/styles.less"
                }
            }
        },
        cssmin: {
            target: {
                files: {
                    "../resources/public/main.css":
                      ["styles-less.css",
                       "src/prettify-tomorrow.css"]
                }
            }
        },
        uglify: {
            build: {
                files: [{
                    src: ['bower_components/jquery/dist/jquery.min.js',
                        'bower_components/bootstrap/dist/js/bootstrap.min.js',
                        'bower_components/google-code-prettify/bin/prettify.min.js',
                        'js/*.js'],
                    dest: '../resources/public/main.min.js'
                }, {
                    src: ['bower_components/jquery/dist/jquery.min.js',
                        'bower_components/bootstrap/dist/js/bootstrap.min.js',
                        'bower_components/google-code-prettify/bin/prettify.min.js',
                        'local_js/*.js'],
                    dest: '../resources/public/local_main.min.js'
                }]
            }
        },
        copy: {
            main: {
                files: [
                    {expand: true, flatten: true, src: ["./media/i/*"], dest: '../resources/public/i/'},
                    {expand: true, flatten: true, src: ["./media/fonts/*", "./bower_components/font-awesome/fonts/*"], dest: '../resources/public/fonts'},
                ]
            }
        }
    });

    grunt.registerTask('default',['less', 'cssmin', 'copy', 'uglify', 'uglify_local'])
};
