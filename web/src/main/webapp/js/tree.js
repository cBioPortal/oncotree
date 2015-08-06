var tree = (function () {
    "use strict";

    var m = [20, 120, 20, 50],
        w = 500 - m[1] - m[3],
        h = 500 - m[0] - m[2],
        i = 0,
        radius = 4.5,
        fontSize = '12px',
        root;

    var tree, diagonal, vis, numOfTumorTypes = 0, numOfTissues = 0;
    var nodesName = [];
    var searchResult = [];

    //d3.json("flare.json", build);

    function initDataAndTree() {
        tree = d3.layout.tree()
            .nodeSize([20, null]);

        diagonal = d3.svg.diagonal()
            .projection(function (d) {
                return [d.y, d.x];
            });

        vis = d3.select("#tree").append("svg:svg")
            .attr("width", w + m[1] + m[3])
            .attr("height", h + m[0] + m[2])
            .append("svg:g")
            .attr("transform", "translate(" + m[3] + "," + 300 + ")");

        d3.tsv("tumorType", function (csv) {
            var rootName = "Tissue";
            var tree = {};
            tree[rootName] = {};
            csv.forEach(function (row) {
                var node = tree[rootName];
                for (var col in row) {
                    var type = row[col];
                    if (!type) break;
                    if (!(type in node)) {
                        node[type] = {};
                    }
                    node = node[type];
                }
            });

            function formatTree(name, tree) {
                var myRegexp = /\((\w+)\)/g;
                var match = myRegexp.exec(name);
                var acronym = undefined;
                if (match instanceof Array && match[1]) {
                    acronym = match[1];
                }
                var ret = {name: name, acronym: acronym};
                var root = tree[name];
                var children = [];

                nodesName.push({name: name, acronym: acronym});

                for (var child in root) {
                    children.push(formatTree(child, root));
                }
                if (children.length === 0) {
                    ret["size"] = 4000;
                } else {
                    ret["children"] = children;
                }
                return ret;
            }

            var json = formatTree(rootName, tree);
            build(json);
            var dups = searchDupAcronym();

            if (Object.keys(dups).length > 0) {
                var htmlStr = '<table class="table">'
                for (var key in dups) {
                    htmlStr += "<tr><td>" + key + "</td><td>" + dups[key].join('<br/>') + '</td><tr>';
                }
                htmlStr += '</table>';
                $('#summary-duplicates p').html(htmlStr);
            } else {
                $('#summary-duplicates').css('display', 'none');
            }
            $("#summary-info").text(function () {
                return "( " + numOfTumorTypes + " tumor type" + ( numOfTumorTypes === 1 ? "" : "s" ) + " from " + numOfTissues + " tissue" + ( numOfTissues === 1 ? "" : "s" ) + " )";
            });
            $('[data-toggle="tooltip"]').tooltip({
                'container': 'body',
                'placement': 'top'
            });
        });
    }

    function searchDupAcronym() {
        var dups = {};

        nodesName.forEach(function (e, i) {
            nodesName.forEach(function (e1, i1) {
                if (e.acronym && e.acronym === e1.acronym && i !== i1) {
                    if (!dups.hasOwnProperty(e.acronym)) {
                        dups[e.acronym] = [];
                    }
                    dups[e.acronym].push(e1.name);
                }
            });
        });
        return dups;
    }

    function build(json) {
        root = json;
        root.x0 = h / 2;
        root.y0 = 0;
        // Initialize the display to show a few nodes.
        root.children.forEach(toggleAll);
        // toggle(root.children[1]);
        // toggle(root.children[2]);
        // toggle(root.children[5]);
        // toggle(root.children[5].children[0]);
        // toggle(root.children[8]);
        // toggle(root.children[8].children[0]);
        // toggle(root.children[15]);
        update(root);
        numOfTissues = root.children.length;
        root.children.forEach(searchLeaf);
    }

    function update(source) {
        var duration = d3.event && d3.event.altKey ? 5000 : 500;
        var translateY = Number(vis.attr('transform').split(',')[1].split(')')[0]);
        var nodes = tree.nodes(root).reverse();

        var overStep = false;
        var minX = 0;
        nodes.forEach(function (d) {
            if (minX > d.x) {
                minX = d.x;
            }
        });

        minX = Math.abs(minX);

        if (minX > (translateY - 50)) {
            var aftetTranslateY = minX + 50;
            vis.transition()
                .duration(duration)
                .attr('transform', 'translate(' + m[3] + ',' + aftetTranslateY + ')');
            nodes = tree.nodes(root).reverse();
        } else if (minX + 50 < translateY) {
            var aftetTranslateY = minX + 50;
            vis.transition()
                .duration(duration)
                .attr('transform', 'translate(' + m[3] + ',' + aftetTranslateY + ')');
            nodes = tree.nodes(root).reverse();
        }

        //Indicate the left side depth for specific level (circal point as center)
        var leftDepth = {0: 0};
        //Indicate the right side depth for specific level (circal point as center)
        var rightDepth = {0: 0};

        var numOfPoints = {},
            maxNumOfPoints = 0;

        //Calculate maximum length of selected nodes in different levels
        nodes.forEach(function (d) {
            var _nameLength = d.name.length * 6 + 50;

            if (d.depth !== 0) {
                if (!leftDepth.hasOwnProperty(d.depth)) {
                    leftDepth[d.depth] = 0;
                    rightDepth[d.depth] = 0;
                }
                ;

                //Only calculate the point without child and without showed child
                if (!d.children && !d._children && rightDepth[d.depth] < _nameLength) {
                    rightDepth[d.depth] = _nameLength;
                }

                //Only calculate the point with child(ren) or with showed child(ren)
                if ((d.children || d._children) && leftDepth[d.depth] < _nameLength) {
                    leftDepth[d.depth] = _nameLength;
                }
            }
        });

        //Calculate the transform information for each node.
        nodes.forEach(function (d) {
            if (d.depth === 0) {
                d.y = 0;
            } else {
                var _y = 0,
                    _length = d.depth;

                for (var i = 1; i <= _length; i++) {
                    if (leftDepth[i] === 0) {
                        rightDepth[i - 1] !== 0 ? _y += rightDepth[i - 1] : _y += 50; //Give constant depth if no point has child or has showed child
                    } else {
                        if (i > 1) {
                            _y += leftDepth[i] + rightDepth[i - 1];
                            (leftDepth[i] > 0 && rightDepth[i - 1] > 0) ? _y -= 50 : _y -= 0;
                        } else {
                            _y += leftDepth[i];
                        }
                    }
                }
                ;
                d.y = _y;
            }
        });

        // Update the nodes…
        var node = vis.selectAll("g.node")
            .data(nodes, function (d) {
                return d.id || (d.id = ++i);
            });

        // Enter any new nodes at the parent's previous position.
        var nodeEnter = node.enter().append("svg:g")
            .attr("class", "node")
            .attr("transform", function (d) {
                return "translate(" + source.y0 + "," + source.x0 + ")";
            })
            .on("click", function (d) {
                toggle(d);
                update(d);
            });

        nodeEnter.append("svg:circle")
            .attr("r", 1e-6)
            .style("fill", function (d) {
                return d._children ? "lightsteelblue" : "#fff";
            });


        var nodeContent = '';
        var nodeText = nodeEnter.append("svg:text")
            .attr("x", function (d) {
                return d.children || d._children ? -10 : 10;
            })
            .attr("dy", ".35em")
            .attr('font-size', fontSize)
            .attr("text-anchor", function (d) {
                return d.children || d._children ? "end" : "start";
            })
            .text(function (d) {
                var _position = '';
                if((d.children || d._children) && d.depth > 1){
                    _position = {my:'bottom right',at:'top left', viewport: $(window)};
                }else {
                    _position = {my:'bottom left',at:'top right', viewport: $(window)};
                }

                nodeContent = d.acronym;;
                $(this).qtip({
                    content:{text: nodeContent},
                    style: { classes: 'qtip-light qtip-rounded qtip-shadow qtip-grey'},
                    hide: {fixed:true, delay: 100},
                    position: _position
                });

                if (d.depth === 1) {
                    return d.name.replace(/\(\w+\)/gi, '');
                } else {
                    return d.name;
                }
            })
            .style("fill-opacity", 1e-6);

        // Transition nodes to their new position.
        var nodeUpdate = node.transition()
            .duration(duration)
            .attr("transform", function (d) {
                return "translate(" + d.y + "," + d.x + ")";
            });

        nodeUpdate.select("circle")
            .attr("r", radius)
            .style("fill", function (d) {
                return d._children ? "lightsteelblue" : "#fff";
            });

        nodeUpdate.select("text")
            .style("fill-opacity", 1)
            .style("font-size", fontSize);

        // Transition exiting nodes to the parent's new position.
        var nodeExit = node.exit().transition()
            .duration(duration)
            .attr("transform", function (d) {
                return "translate(" + source.y + "," + source.x + ")";
            })
            .remove();

        nodeExit.select("circle")
            .attr("r", 1e-6);

        nodeExit.select("text")
            .style("fill-opacity", 1e-6);

        // Update the links…
        var link = vis.selectAll("path.link")
            .data(tree.links(nodes), function (d) {
                return d.target.id;
            });

        // Enter any new links at the parent's previous position.
        link.enter().insert("svg:path", "g")
            .attr("class", "link")
            .attr("d", function (d) {
                var o = {x: source.x0, y: source.y0};
                return diagonal({source: o, target: o});
            })
            .transition()
            .duration(duration)
            .attr("d", diagonal);

        // Transition links to their new position.
        link.transition()
            .duration(duration)
            .attr("d", diagonal);

        // Transition exiting nodes to the parent's new position.
        link.exit().transition()
            .duration(duration)
            .attr("d", function (d) {
                var o = {x: source.x, y: source.y};
                return diagonal({source: o, target: o});
            })
            .remove();

        // Stash the old positions for transition.
        nodes.forEach(function (d) {
            d.x0 = d.x;
            d.y0 = d.y;
        });

        resizeSVG(rightDepth);
        $('[data-toggle="tooltip"]').tooltip({
            'container': 'body',
            'placement': 'top'
        });
    }

    // Toggle children.
    function toggle(d) {
        if (d.children) {
            d._children = d.children;
            d.children = null;
        } else {
            d.children = d._children;
            d._children = null;
        }
    }

    function toggleAll(d) {
        if (d.children) {
            d.children.forEach(toggleAll);
            toggle(d);
        }
    }

    function stretchAll(d) {
        if (d._children) {
            d._children.forEach(stretchAll);
            toggle(d);
        }
    }

    function expandWithArray(nodesArray) {
        for (var i = 0, nodesLength = nodesArray.length; i < nodesLength; i++) {
            toggle(root.children[nodesArray[i]]);
        }
        ;
        update(root);
    }

    function expandAll() {
        root.children.forEach(stretchAll);
        update(root);
    }

    function collapseAll() {
        root.children.forEach(toggleAll);
        update(root);
    }

    function resizeSVG(rightDepth) {
        var nodes = tree.nodes(root).reverse(),
            maxHeight = 0,
            maxWidth = 0,
            lastDepth = 0;

        nodes.forEach(function (d) {
            if (d.x0 > maxHeight) {
                maxHeight = d.x0;
            }

            if (d.y0 > maxWidth) {
                maxWidth = d.y0;
            }
        })

        maxHeight *= 2;
        maxHeight += 150;

        lastDepth = rightDepth[Math.max.apply(Math, (Object.keys(rightDepth)).map(function (item) {
            return Number(item);
        }))];
        maxWidth = maxWidth + 100 + lastDepth;

        if (500 < maxWidth) {
            d3.select("#tree").select("svg").attr("width", maxWidth);
        } else {
            d3.select("#tree").select("svg").attr("width", 500);
        }

        if (500 < maxHeight) {
            d3.select("#tree").select("svg").attr("height", maxHeight);
        } else {
            d3.select("#tree").select("svg").attr("height", 500);
        }
    }

    function searchByNodeName(searchKey) {
        searchResult.length = 0;
        root.children.forEach(toggleAll);
        update(root);

        if (searchKey !== "") {
            for (var i = 0, numOfChild = root.children.length; i < numOfChild; i++) {
                findChildContain(i, searchKey, root.children[i]);
            }

            searchResult.forEach(function (content, index) {
                var _indexes = content.split('-'),
                    _indexesLength = _indexes.length,
                    _node = root.children[_indexes[0]];

                for (var i = 1; i < _indexesLength; i++) {
                    if (!_node.children) {
                        toggle(_node);
                    }
                    _node = _node.children[_indexes[i]];
                }
            })
            update(root);
        }

        highlightSearchKey(searchKey);

        return searchResult;
    }

    function highlightSearchKey(searchKey) {
        d3.select("#tree svg").selectAll('text').each(function (d, i) {
            if (searchKey === '') {
                d3.select(this).style('fill', 'black');
            } else {
                if (d.name.toLowerCase().indexOf(searchKey) !== -1) {
                    d3.select(this).style('fill', 'red');
                } else {
                    d3.select(this).style('fill', 'black');
                }
            }
        })
    }

    function findChildContain(parentId, searchKey, node) {
        parentId = String(parentId);
        if (node._children) {
            if (node.name.toLowerCase().indexOf(searchKey) !== -1) {
                searchResult.push(parentId);
            }
            for (var i = 0, numOfChild = node._children.length; i < numOfChild; i++) {
                findChildContain(parentId + "-" + i, searchKey, node._children[i]);
            }
        } else {
            if (node.name.toLowerCase().indexOf(searchKey) !== -1) {
                searchResult.push(parentId);
            }
        }
    }

    function searchLeaf(node) {
        if (node._children || node.children) {
            if (node.children) {
                for (var i = 0, _length = node.children.length; i < _length; i++) {
                    searchLeaf(node.children[i]);
                }
            }
            if (node._children) {
                for (var i = 0, _length = node._children.length; i < _length; i++) {
                    searchLeaf(node._children[i]);
                }
            }
        } else {
            numOfTumorTypes++;
        }
    }

    return {
        init: initDataAndTree,
        expand: expandWithArray,
        search: searchByNodeName,
        expandAll: expandAll,
        collapseAll: collapseAll,
        getNumOfTissues: function () {
            return numOfTissues;
        },
        getNumOfTumorTypes: function () {
            return numOfTumorTypes;
        }
    }
})();