var tree = (function () {
    "use strict";

    var nci_base_uri = 'https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=NCI_Thesaurus&code=';

    var umls_base_uri = 'https://ncim.nci.nih.gov/ncimbrowser/ConceptReport.jsp?code=';

    var m = [20, 120, 20, 50],
        w = 500 - m[1] - m[3],
        h = 500 - m[0] - m[2],
        i = 0,
        radius = 4.5,
        fontSize = '12px',
        root;

    var tree, diagonal, vis, numOfTumorTypes = 0, numOfTissues = 0;

    var oncotreeCodesToNames = {}; // used to find duplicate codes

    var searchResult = [];

    var is_chrome = navigator.userAgent.indexOf('Chrome') > -1;
    var is_safari = navigator.userAgent.indexOf("Safari") > -1;

    function getNCILink(nciCode) {
        return (typeof nciCode !== 'undefined' && nciCode != '') ? '<a class="qtip-link" href="' + nci_base_uri + nciCode + '" target="_blank">' + nciCode + '</a>' : 'Not Available';
    }

    function getUMLSLink(umlsCode) {
        return (typeof umlsCode !== 'undefined' && umlsCode != '') ? '<a class="qtip-link" href="' + umls_base_uri + umlsCode + '" target="_blank">' + umlsCode + '</a>' : 'Not Available';
    }

    function UniqueTreeNodeDatum() {
        this.name = '';
        this.acronym = '';
        this.mainType = '';
        this.color = '';
        this.nci = [];
        this.umls = [];
        this.history = ''; // comma delimited string
    }

    function getOncotreeCodeKeysSortedByName(oncotreeNodeDict) {
        return Object.keys(oncotreeNodeDict).sort(function(a,b) {
                var nameA = oncotreeNodeDict[a].name;
                var nameB = oncotreeNodeDict[b].name;
                if (nameA < nameB) {
                    return -1;
                }
                if (nameA > nameB) {
                    return 1;
                }
                return 0;
            });
    }

    function process_children(parentNode, childData) {
        // childData is always for a new unique node
        var childNode = new UniqueTreeNodeDatum();
        childNode.name = childData.name + " (" + childData.code + ")";
        childNode.acronym = childData.code;

        if (childData.hasOwnProperty('mainType') && childData.mainType != null && childData.mainType.hasOwnProperty('name') && childData.mainType.name !== '') {
            childNode.mainType = childData.mainType.name;
        } else {
            childNode.mainType = 'Not Available';
        }

        if (childData.hasOwnProperty('color')) {
            childNode.color = childData.color;
        }
        if (childData.hasOwnProperty('NCI')) {
            childNode.nci = childData.NCI;
        }

        if (childData.hasOwnProperty('UMLS')) {
            childNode.umls = childData.UMLS;
        }

        if (childData.hasOwnProperty('history')) {
            childNode.history = childData.history.join();
        }

        // save code and name to check for duplicate codes later
        if (!oncotreeCodesToNames.hasOwnProperty(childNode.acronym)) {
            oncotreeCodesToNames[childNode.acronym] = [];
        }
        oncotreeCodesToNames[childNode.acronym].push(childNode.name);

        // add new node to children list of parentNode
        parentNode.children.push(childNode);

        // now process this node's children
        if (childData.hasOwnProperty('children')) {
            var codesSortedByName =  getOncotreeCodeKeysSortedByName(childData.children);
            if (codesSortedByName.length > 0) {
                childNode.children = [];
                codesSortedByName.forEach(function (code) {
                    // now childNode's children are added
                    process_children(childNode, childData.children[code]);
                });
            }
        }
    }

    function initDataAndTree(version) {
        var jsonUrl = 'api/tumorTypes?flat=false'

        if (version) {
          jsonUrl += '&version=' + version;
        }
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

        d3.json(jsonUrl, function (oncotree_json) {
            var rootNode = new UniqueTreeNodeDatum();
            rootNode.name = 'Tissue';
            rootNode.children = []

            getOncotreeCodeKeysSortedByName(oncotree_json.data.TISSUE.children).forEach(function (code) {
                var childData = oncotree_json.data.TISSUE.children[code];
                // these nodes all belong at root of tree
                process_children(rootNode, childData);
            });

            build(rootNode);
            var dups = searchDupAcronym();

            if (Object.keys(dups).length > 0) {
                var htmlStr = '<table class="table">';
                for (var key in dups) {
                    htmlStr += "<tr><td>" + key + "</td><td>" + dups[key].join('<br/>') + '</td><tr>';
                }
                htmlStr += '</table>';
                $('#summary-duplicates p').html(htmlStr);
            } else {
                $('#summary-duplicates').css('display', 'none');
            }
            $("#summary-info").text(function () {
                return "Includes " + numOfTumorTypes + " tumor type" + ( numOfTumorTypes === 1 ? "" : "s" ) + " from " + numOfTissues + " tissue" + ( numOfTissues === 1 ? "" : "s" ) + ".";
            });
        });
    }

    function searchDupAcronym() {
        var dups = {};
        for (var code in oncotreeCodesToNames) {
            if (oncotreeCodesToNames[code].length > 1) {
                dups[code] = oncotreeCodesToNames[code];
            }
        }
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
        //IE translate doesnot have comma to seperate the x, y. Instead, it uses space.
        var translateY = Number(vis.attr('transform').split(/[,\s]/)[1].split(')')[0]);
        var nodes = tree.nodes(root).reverse();
        var afterTranslateY;
        var overStep = false;
        var minX = 0;
        nodes.forEach(function (d) {
            if (minX > d.x) {
                minX = d.x;
            }
        });

        minX = Math.abs(minX);

        if (minX > (translateY - 50)) {
            afterTranslateY = minX + 50;
            vis.transition()
                .duration(duration)
                .attr('transform', 'translate(' + m[3] + ',' + afterTranslateY + ')');
            nodes = tree.nodes(root).reverse();
        } else if (minX + 50 < translateY) {
            afterTranslateY = minX + 50;
            vis.transition()
                .duration(duration)
                .attr('transform', 'translate(' + m[3] + ',' + afterTranslateY + ')');
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
                        //Give constant depth if no point has child or has showed child
                        if(rightDepth[i - 1]) {
                            _y += rightDepth[i - 1];
                        }else{
                            _y += 50;
                        }
                    } else {
                        if (i > 1) {
                            _y += leftDepth[i] + rightDepth[i - 1];
                            if(leftDepth[i] > 0 && rightDepth[i - 1] > 0) {
                                _y -= 50;
                            } else {
                                _y -= 0;
                            }
                        } else {
                            _y += leftDepth[i];
                        }
                    }
                }
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
            .style("stroke", function (d) {
                return d.color;
            })
            .style("fill", function (d) {
                return (d._children ? d.color : "#fff");
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
                var _qtipContent = '';
                if((d.children || d._children) && d.depth > 1){
                    _position = {my:'bottom right',at:'top left', viewport: $(window)};
                }else {
                    _position = {my:'bottom left',at:'top right', viewport: $(window)};
                }

                var nci_links = [];
                if (typeof d.nci !== 'undefined' && d.nci.length > 0) {
                    $.each(d.nci, function( index , value ) {
                        nci_links.push(getNCILink(value));
                    });
                } else {
                    // will have 'Not Available' link
                    nci_links.push(getNCILink(""));
                }

                var umls_links = [];
                if (typeof d.umls !== 'undefined' && d.umls.length > 0) {
                    $.each(d.umls, function( index, value ) {
                        umls_links.push(getUMLSLink(value));
                    });
                } else {
                    // will have 'Not Available' link
                    umls_links.push(getUMLSLink(""));
                }

                _qtipContent += '<b>Code:</b> ' + d.acronym +

                     //clipboard JS is not supported in Safari.
                    ((is_safari && !is_chrome) ?
                        '<button style="margin-left: 5px;" class="btn btn-default btn-xs" ' +
                        ' disabled>"Copy" is not available in Safari</button>' :
                        '<button style="margin-left: 5px;" class="clipboard-copy btn btn-default btn-xs" ' +
                        'data-clipboard-text="' + d.acronym + '"  ' +
                        '>Copy</button>'
                    ) +
                    '<br/>';
                _qtipContent += '<b>Name:</b> ' + d.name.replace(/\(\w+\)/gi, '') + '<br/>';
                _qtipContent += '<b>Main type:</b> ' + d.mainType + '<br/>';
                _qtipContent += '<b>NCI:</b> ' + nci_links.join(",") + '<br/>';
                _qtipContent += '<b>UMLS:</b> ' + umls_links.join(",") + '<br/>';
                _qtipContent += '<b>Color:</b> ' + (d.color||'LightBlue') + '<br/>';
                if (typeof d.history !== 'undefined' && d.history != '') {
                    _qtipContent += '<b>Previous codes:</b> ' + d.history  + '<br/>';
                }
                $(this).qtip({
                    content:{text: _qtipContent},
                    style: { classes: 'qtip-light qtip-rounded qtip-shadow qtip-grey qtip-wide'},
                    hide: {fixed: true, delay: 100},
                    position: _position,
                    events: {
                        render: function(element, api) {
                            $(api.elements.content).find('.clipboard-copy').click(function() {
                                $(this).qtip({
                                    content: 'Copied',
                                    style: {classes: 'qtip-light qtip-rounded qtip-shadow qtip-grey qtip-wide'},
                                    show: {ready: true},
                                    position: {
                                        my: 'bottom center',
                                        at: 'top center',
                                        viewport: $(window)
                                    },
                                    events: {
                                        show: function(event, api) {
                                            setTimeout(function() {
                                                api.destroy();
                                            }, 1000);
                                        }
                                    }
                                })
                            });
                        }
                    }
                });

                if (d.depth === 1) {
                    return d.name.replace(/\(\w+\)/gi, '');
                } else {
                    return d.name;
                }
            })
            .style("fill-opacity", 1e-6);

        var clipboard = new Clipboard('.clipboard-copy.btn');

        // Transition nodes to their new position.
        var nodeUpdate = node.transition()
            .duration(duration)
            .attr("transform", function (d) {
                return "translate(" + d.y + "," + d.x + ")";
            });

        nodeUpdate.select("circle")
            .attr("r", radius)
            .style("fill", function (d) {
                return d._children ? d.color : "#fff";
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
        });

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
            });
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
                if (d.name.toLowerCase().indexOf(searchKey) !== -1 ||
                    (typeof d.history !== 'undefined' && d.history != '' && d.history.toLowerCase().split(",").indexOf(searchKey) !== -1)) {
                    d3.select(this).style('fill', 'red');
                } else {
                    d3.select(this).style('fill', 'black');
                }
            }
        });
    }

    function findChildContain(parentId, searchKey, node) {
        parentId = String(parentId);
        if (node._children) {
            if (node.name.toLowerCase().indexOf(searchKey) !== -1 ||
                (typeof node.history !== 'undefined' && node.history != '' && node.history.toLowerCase().split(",").indexOf(searchKey) !== -1)) {
                searchResult.push(parentId);
            }
            for (var i = 0, numOfChild = node._children.length; i < numOfChild; i++) {
                findChildContain(parentId + "-" + i, searchKey, node._children[i]);
            }
        } else {
            if (node.name.toLowerCase().indexOf(searchKey) !== -1 ||
                (typeof node.history !== 'undefined' && node.history != '' && node.history.toLowerCase().split(",").indexOf(searchKey) !== -1)) {
                searchResult.push(parentId);
            }
        }
    }

    function searchLeaf(node) {
        var i, length;
        if (node._children || node.children) {
            if (node.children) {
                length =  node.children.length;
                for (i = 0; i < length; i++) {
                    searchLeaf(node.children[i]);
                }
            }
            if (node._children) {
                length =  node._children.length;
                for (i = 0; i < length; i++) {
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
    };
})();
