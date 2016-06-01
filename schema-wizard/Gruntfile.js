/*global module:false*/
module.exports = function (grunt) {

    require('jit-grunt')(grunt, {
        useminPrepare: 'grunt-usemin'
    });
    require('time-grunt')(grunt);

    // Project configuration.
    grunt.initConfig({
        // Metadata.
        pkg: grunt.file.readJSON('package.json'),
        banner: '/*! <%= pkg.title || pkg.name %> - v<%= pkg.version %> - ' +
        '<%= grunt.template.today("yyyy-mm-dd") %> */\n',
        // Task configuration.
        meta: {
            app_files: ['src/main/webapp/**/*.js', '!src/main/webapp/bower_components/**/*', '!src/main/webapp/assets/lib/**/*'],
            lib_files: ['src/main/webapp/assets/lib/**/*'],
            dist_dir:  'target'
        },
        jshint: {
            options: {
                jshintrc: true
            },
            gruntfile: {
                src: 'Gruntfile.js'
            },
            app_files: {
                src: '<%= meta.app_files %>'
            }
        },
        jscs: {
            options: {
                config: '.jscsrc',
                fix: true
            },
            src: '<%= meta.app_files %>'
        },
        clean: {
            dist: '<%= meta.dist_dir %>',
            temp: '.tmp',
            coverage: 'coverage'
        },
        connect: {
            server: {
                options: {
                    port: 9000,
                    base: 'src/main/webapp',
                    open: true,
                    livereload: true
                }
            },
            dist: {
                options: {
                    base: '<%= meta.dist_dir %>',
                    open: true
                }
            }
        },
        watch: {
            livereload: {
                files: ['<%= meta.app_files %>', 'src/main/webapp/**/*.html', 'src/main/webapp/**/*.css'],
                options: {
                    livereload: true
                }
            },
            gruntfile: {
                files: '<%= jshint.gruntfile.src %>',
                tasks: ['jshint:gruntfile']
            },
            jshint: {
                files: '<%= meta.app_files %>',
                tasks: ['newer:jshint:app_files', 'newer:jscs']
            }
        },
        useminPrepare: {
            html: 'src/main/webapp/index.html',
            options: {
                dest: '<%= meta.dist_dir %>/schwiz'
            }
        },
        usemin: {
            html: '<%= meta.dist_dir %>/schwiz/index.html'
        },
        copy: {
            dist: {
                files: [{
                    expand: true,
                    dot: true,
                    cwd: 'src/main/webapp',
                    dest: '<%= meta.dist_dir %>/schwiz',
                    filter: 'isFile',
                    src: [
                        '**/*',
                        '!**/*.js',
                        '!**/*.css',
                        '!bower_components/**/*'
                    ]
                }]
            },
            assets: {
                files: [{
                    expand: true,
                    dot: true,
					cwd: 'src/main/webapp/assets/lib',
					dest: 'target/schwiz/assets/lib',
                    src: [
                        'chart-1.0.1.js',
                        'angular-chart.js',
                        'angular-google-chart-0.1.0-beta.1.js',
                        'Chart.HorizontalBar.js'
                    ]
                }]
            }
        },
        htmlmin: {
            dist: {
                options: {
                    collapseWhitespace: true,
                    collapseBooleanAttributes: true,
                    removeCommentsFromCDATA: true,
                    removeOptionalTags: true
                },
                files: [{
                    expand: true,
                    cwd: '<%= meta.dist_dir %>/schwiz',
                    src: ['**/*.html'],
                    dest: '<%= meta.dist_dir %>/schwiz'
                }]
            }
        },
        karma: {
            all: {
                configFile: 'karma.conf.js',
                singleRun: true
            },
            chrome: {
                configFile: 'karma.conf.js',
                singleRun: true,
                browsers: ['Chrome']
            },
            firefox: {
                configFile: 'karma.conf.js',
                singleRun: true,
                browsers: ['Firefox']
            },
            ie: {
                configFile: 'karma.conf.js',
                singleRun: true,
                browsers: ['IE']
            },
            phantomjs: {
                configFile: 'karma.conf.js',
                singleRun: true,
                browsers: ['PhantomJS']
            }
        },
        wiredep: {
            app: {
                src: [ 'src/main/webapp/index.html' ]
            },
            test: {
                src: ['karma.conf.js'],
                devDependencies: true
            }
        },
        uglify: {
            options: {
                mangle: false
            }
        },
        cssmin: {
            options: {
                beautify: true
            }
        }
    });

    grunt.registerTask('serve', function (target) {
        if (target === 'dist') {
            return grunt.task.run('connect:dist:keepalive');
        }

        grunt.task.run(['wiredep', 'connect:server', 'watch:livereload']);
    });

    grunt.registerTask('test', function (target) {
        grunt.task.run(['clean:coverage', 'wiredep:test', 'karma:' + (target ? target : 'all')]);
    });

    grunt.registerTask('check-code', ['newer:jshint', 'newer:jscs']);

    grunt.registerTask('build', [
        'clean:dist',
        'wiredep:app',
        'useminPrepare',
        'concat',
        'copy:dist',
        'copy:assets',
        'cssmin',
        'uglify',
        'usemin',
        'clean:temp'
    ]);

    grunt.registerTask('default', [
//        'newer:jshint',
        'newer:jscs',
//        'test:phantomjs',
        'build'
    ]);
};
