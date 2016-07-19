/**
 * AngularJS fixed header scrollable table directive
 * @author Jason Watmore <jason@pointblankdevelopment.com.au> (http://jasonwatmore.com)
 * @version 1.2.0
 */
(function () {
    angular
        .module('anguFixedHeaderTable', [])
        .directive('fixedHeader', fixedHeader);

    fixedHeader.$inject = ['$timeout', '$window'];

    function fixedHeader($timeout, $window) {
        return {
            restrict: 'A',
            link: link
        };

        function link($scope, $elem, $attrs, $ctrl) {
            var elem = $elem[0];
            // wait for data to load and then transform the table
            $scope.$watch(tableDataLoaded, function(isTableDataLoaded) {
                if (isTableDataLoaded) {
                    transformTable();
                }
            });
            $scope.$on("transformTable", function() {
                transformTable();
            });

            function tableDataLoaded() {
                // first cell in the tbody exists when data is loaded but doesn't have a width
                // until after the table is transformed

                var firstCell = elem.querySelector('table[fixed-header] > tbody > tr:first-child > td:first-child');
                return firstCell && !firstCell.style.width;
            }

            // make sure to accommodate the scroll bar width if the tbody changes from/to not overflowing
            $scope.$watch(numRows, function (newVal, oldVal) {
                if (newVal !== oldVal) {
                    transformTable();
                }
            });

            function numRows() {
                return elem.querySelectorAll('table[fixed-header] > tbody > tr').length;
            }

            function transformTable() {
                $scope.someValue = false;
                // wrap in $timeout to give table a chance to finish rendering
                $timeout(function () {
                    // Instead of doing any calculations on elem, we will do them on a clone.
                    // This way we won't change the display of the table itself, preventing FOUC when transforming.
                    var clone = elem.cloneNode(true);
                    var wrapper = document.createElement('div');
                    wrapper.style.height = '0px';
                    wrapper.style.overflow = 'hidden';
                    wrapper.style.visibility = 'invisible';
                    wrapper.appendChild(clone);
                    angular.element(clone.querySelectorAll('thead, tbody, tfoot')).css('display', '');
                    $elem.after(wrapper);

                    var longestRowBySection = {};

                    // set widths of columns
                    var tableSections = ['tbody', 'thead', 'tfoot'];
                    for (var i = 0; i < tableSections.length; i++) {
                        var section = tableSections[i];
                        var rows = elem.querySelectorAll('table[fixed-header] > ' + section +' > tr');
                        var cloneRows = clone.querySelectorAll('table[fixed-header] > ' + section +' > tr');
                        // find the row with the most cells
                        var mostCells = 0;
                        longestRowBySection[section] = 0;
                        for (var j = 0; j < rows.length; j++) {
                            var cellCount = rows[j].childElementCount;
                            if (cellCount > mostCells) {
                                mostCells = cellCount;
                                longestRowBySection[section] = j;
                            }
                        }
                        var cells = angular.isDefined(rows[longestRowBySection[section]]) ? rows[longestRowBySection[section]].children : [];
                        for (var k = 0; k < cells.length; k++) {
                            cells[k].style.width = cloneRows[longestRowBySection[section]].children[k].offsetWidth + 'px';
                        }
                    }

                    //Done with the math. We can get rid of the clone.
                    angular.element(wrapper).remove();

                    // set css styles on thead and tbody
                    angular.element(elem.querySelectorAll('table[fixed-header] > thead, table[fixed-header] > tfoot')).css('display', 'block');
                    angular.element(elem.querySelector('table[fixed-header] > tbody')).css({
                        'display': 'block',
                        'height': $attrs.tableHeight || 'inherit',
                        'max-height': $attrs.maxTableHeight || 'inherit',
                        'overflow': 'auto'
                    });

                    // Fix tr inheritance of tbody height in IE9.
                    // This whole directive doesn't actually work in IE9 but this fix
                    // makes the table look ok when it degrades to a normal table
                    angular.element(elem.querySelectorAll('table[fixed-header] > tbody > tr')).css('height', 'auto');

                    // reduce width of last column by width of scrollbar
                    var tbody = elem.querySelector('table[fixed-header] > tbody');
                    var scrollBarWidth = tbody.offsetWidth - tbody.clientWidth;

                    if (scrollBarWidth > 0) {
                        // for some reason trimming the width by 4px lines everything up better
                        scrollBarWidth -= 4;
                        var lastColumn = elem.querySelector('table[fixed-header] > tbody > tr:nth-child(' + (longestRowBySection.tbody + 1) + ') > td:last-child');
                        lastColumn.style.width = (lastColumn.offsetWidth - scrollBarWidth) + 'px';
                    }
                });
            }

            angular.element($window).bind('resize', resizeThrottler);
            var resizeTimeout;
            function resizeThrottler() {
                if (!resizeTimeout) {
                    resizeTimeout = $timeout(function() {
                        resizeTimeout = null;
                        transformTable();
                    }, 100);
                }
            }
        }
    }
})();
