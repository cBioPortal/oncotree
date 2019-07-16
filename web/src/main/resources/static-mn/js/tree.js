var tree = (function () {
    "use strict";

    var nci_base_uri = 'https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=NCI_Thesaurus&code=';

    var umls_base_uri = 'https://ncim.nci.nih.gov/ncimbrowser/ConceptReport.jsp?code=';

    var m = [20, 120, 20, 50],
        w = 500 - m[1] - m[3],
        h = 500 - m[0] - m[2],
        i = 0,
        //radius = [1, 2.5, 5, 7.5, 10],
        fontSize = '12px',
        //size = 1,
        root;

    var tree, diagonal, vis, numOfTumorTypes = 0, numOfTissues = 0;

    var oncotreeCodesToNames = {}; // used to find duplicate codes

    var searchResult = [];

    var is_chrome = navigator.userAgent.indexOf('Chrome') > -1;
    var is_safari = navigator.userAgent.indexOf("Safari") > -1;

    var treeBuildComplete = false; // set to true after child elements have all been constructed in DOM

    function getNCILink(nciCode) {
        return (typeof nciCode !== 'undefined' && nciCode != '') ? '<a class="qtip-link" href="' + nci_base_uri + nciCode + '" target="_blank">' + nciCode + '</a>' : 'Not Available';
    }

    function getUMLSLink(umlsCode) {
        return (typeof umlsCode !== 'undefined' && umlsCode != '') ? '<a class="qtip-link" href="' + umls_base_uri + umlsCode + '" target="_blank">' + umlsCode + '</a>' : 'Not Available';
    }

    d3.json("../data/msk-impact-oncotree.csv", function(data) {
        console.log(data);
    });

    function UniqueTreeNodeDatum() {
        this.name = '';
        this.code = '';
        this.mainType = '';
        this.color = '';
        this.nci = [];
        this.umls = [];
        this.history = ''; // comma delimited string
        this.hasRevocations = false;
        this.number = 0;
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
        childNode.code = childData.code;

        if (childData.hasOwnProperty('mainType') && childData.mainType != null && childData.mainType !== '') {
            childNode.mainType = childData.mainType;
        } else {
            childNode.mainType = 'Not Available';
        }

        if (childData.hasOwnProperty('color')) {
            childNode.color = childData.color;
        }

        if (childData.hasOwnProperty('externalReferences')) {
           if (childData.externalReferences.hasOwnProperty('NCI')) {
               childNode.nci = childData.externalReferences.NCI;
           }
           if (childData.externalReferences.hasOwnProperty('UMLS')) {
               childNode.umls = childData.externalReferences.UMLS;
           }
        }

        if (childData.hasOwnProperty('history')) {
            childNode.history = childData.history.join();
        }
        if (childData.hasOwnProperty('revocations')) {
            childData.revocations.forEach(function(revocation) {
                if (revocation !== childNode.code) { // do not show your own code in Previous Codes (e.g. PTCL)
                    // set here rather than after childData.hasOwnProperty because revocations property is always there
                    childNode.hasRevocations = true;
                    if (childNode.history != '') {
                        childNode.history += ", ";
                    }
                   childNode.history += "<text style=\"color: red;\">" + revocation + "<sup>*</sup></text>";
                }
            });
        }
        if (childData.hasOwnProperty('precursors')) {
            childData.precursors.forEach(function(precursor) {
                if (childNode.history != '') {
                    childNode.history += ", ";
                }
                childNode.history += precursor;
            }); //could use this to find sum of larger nodes?
        }

        if (childData.hasOwnProperty('number')) {
            childNode.number = childData.number;
        }

        //var tNum = [['LUAD', 1357], ['IDC', 927], ['COAD', 724], ['PRAD', 698], ['PAAD', 384], ['BLCA', 312], ['GBM', 286], ['CCRCC', 202], ['SKCM', 195], ['ILC', 190], ['LUSC', 170], ['READ', 151], ['STAD', 151], ['CUP', 146], ['GIST', 137], ['HGSOC', 133], ['IHCH', 115], ['ESCA', 112], ['UEC', 95], ['THPA', 93], ['COADREAD', 90], ['AASTR', 86], ['HCC', 85], ['SCLC', 82], ['UTUC', 82], ['NBL', 80], ['SEM', 76], ['PANET', 75], ['DLBCLNOS', 74], ['ACYC', 73], ['BMGCT', 70], ['MCC', 63], ['THPD', 60], ['MFH', 59], ['CHOL', 57], ['ULMS', 57], ['MDLC', 56], ['CSCC', 55], ['URCC', 54], ['BRCA', 50], ['BRCANOS', 49], ['MAAP', 48], ['USC', 47], ['GBC', 46], ['DDLS', 45], ['UM', 44], ['MUP', 43], ['AODG', 43], ['HNSC', 43], ['OCSC', 42], ['OPHSC', 42], ['PLEMESO', 41], ['LMS', 41], ['BRCNOS', 40], ['MACR', 39], ['PLMESO', 39], ['UCS', 38], ['ES', 38], ['LUNE', 37], ['OS', 37], ['ODG', 36], ['ANGS', 36], ['MGCT', 36], ['ASTR', 35], ['VMGCT', 34], ['SYNS', 34], ['PRCC', 33], ['CHRCC', 33], ['SBC', 33], ['THAP', 33], ['SARCNOS', 32], ['ANSC', 32], ['GEJ', 28], ['EHCH', 27], ['FL', 27], ['ACC', 25], ['NSCLCPD', 24], ['CCOV', 24], ['SFT', 24], ['LGSOC', 23], ['THHC', 23], ['ACRM', 23], ['ARMM', 23], ['DSRCT', 22], ['MNG', 21], ['HGGNOS', 20], ['UMEC', 19], ['CHS', 19], ['WDLS', 18], ['APAD', 18], ['PEMESO', 18], ['MRLS', 18], ['SDCA', 18], ['NSCLC', 17], ['ECAD', 17], ['THME', 17], ['VMM', 17], ['EGC', 17], ['FLC', 17], ['SBWDNET', 16], ['AOAST', 16], ['AMPCA', 16], ['CHDM', 14], ['PAAC', 14], ['AITL', 14], ['UCCC', 14], ['ECD', 14], ['LXSC', 14], ['GRCT', 14], ['ALUCA', 13], ['SARCL', 13], ['PTCL', 13], ['MCL', 13], ['HNMUCM', 13], ['TRCC', 13], ['NPC', 13], ['OCS', 12], ['BCC', 12], ['SSRCC', 12], ['PRNE', 12], ['RMS', 11], ['BYST', 11], ['CESC', 11], ['HGNEC', 11], ['LUAS', 11], ['MPNST', 11], ['UPECOMA', 10], ['GSARC', 10], ['NECNOS', 10], ['THYC', 10], ['MPT', 10], ['EPM', 9], ['INTS', 9], ['ESCC', 9], ['ERMS', 9], ['LUCA', 9], ['MBN', 9], ['PAMPCA', 9], ['GINET', 9], ['LNET', 9], ['MOV', 9], ['PDC', 9], ['NETNOS', 8], ['LGGNOS', 8], ['PLLS', 8], ['IPMN', 8], ['THYM', 8], ['BEC', 8], ['NSGCT', 8], ['GCCAP', 8], ['EPIS', 8], ['PAASC', 8], ['MBC', 8], ['RAML', 8], ['PRSCC', 7], ['HNSCUP', 7], ['BLAD', 7], ['MFS', 7], ['TYST', 7], ['EOV', 7], ['ASPS', 6], ['VOEC', 6], ['HNMASC', 6], ['STMYEC', 6], ['GCEMU', 6], ['PSCC', 6], ['SKAC', 6], ['SRCBC', 6], ['VYST', 5], ['EHAE', 5], ['CEAS', 5], ['THFO', 5], ['NHL', 5], ['SRAP', 5], ['MXOV', 5], ['ARMS', 5], ['SPN', 5], ['MYCHS', 5], ['TT', 5], ['ATM', 5], ['SPDAC', 5], ['APE', 5], ['UELMS', 5], ['EMBCA', 5], ['ADNOS', 5], ['BCCA', 5], ['PLBMESO', 5], ['PECOMA', 5], ['FIBS', 5], ['EMCHS', 5], ['MTSCC', 5], ['MYEC', 4], ['PGNG', 4], ['VSC', 4], ['MIXED', 4], ['HGESS', 4], ['UAD', 4], ['AWDNET', 4], ['RWDNET', 4], ['PLRMS', 4], ['HPHSC', 4], ['FDCS', 4], ['HDCN', 4], ['UCP', 4], ['CHOS', 4], ['SNA', 4], ['ESMM', 4], ['LCH', 4], ['PHC', 4], ['HCCIHCH', 4], ['MCCE', 4], ['HNNE', 4], ['MRC', 4], ['SNSC', 4], ['SAAD', 4], ['OAST', 4], ['TMT', 4 -- none found], ['OUSARC', 4], ['MZL', 4], ['RBL', 4], ['ROCY', 3], ['CHL', 3], ['SCCNOS', 3], ['IMT', 3], ['MUCC', 3], ['ESS', 3], ['TCCA', 3], ['EPDCA', 3], ['EMYOCA', 3], ['LIHB', 3], ['SCHW', 3], ['CLLSLL', 3], ['MCHS', 3], ['OSOS', 3], ['PPTID', 3], ['DFSP', 3], ['HMBL', 3], ['UAS', 3], ['SPCC', 3], ['MLYM', 3], ['DDCHS', 3], ['URMM', 3], ['BA', 3], ['UCCA', 3], ['CCS', 3], ['SBMOV', 3], ['UUC', 3], ['SCRMS', 3], ['SBOV', 2], ['SCST', 2], ['UA', 2], ['SWDNET', 2], ['UCU', 2], ['TMESO', 2], ['PAST', 2], ['EMPD', 2], ['SACA', 2], ['AECA', 2], ['ACBC', 2], ['HPCCNS', 2], ['OSACA', 2], ['DIPG', 2], ['SCCE', 2], ['SCBC', 2], ['BLSC', 2], ['USCC', 2], ['SFTCNS', 2], ['PNET', 2], ['SRCCR', 2], ['SEF', 2], ['OFMT', 2], ['HL', 2], ['RCC', 2], ['UUS', 2], ['WT', 2], ['ACCC', 2], ['ANM', 2], ['PLSMESO', 2], ['GCT', 2], ['USTUMP', 2], ['APXA', 2], ['SNUC', 2], ['CCPRC', 2], ['SLCT', 2], ['MCN', 2], ['DA', 2], ['SEBA', 2], ['MBL', 2], ['OHNCA', 2], ['SGAD', 2], ['SCB', 2], ['RGNT', 2], ['MF', 2], ['UMLMS', 2], ['ONBL', 2], ['ODGC', 1], ['CMPT', 1], ['CMC', 1], ['ICEMU', 1], ['LUPC', 1], ['PSC', 1], ['ALCL', 1], ['OOVC', 1], ['CNC', 1], ['DF', 1], ['OIMT', 1], ['ASTB', 1], ['PXA', 1], ['LGESS', 1], ['CDRCC', 1], ['LIPO', 1], ['MTNN', 1], ['HDCS', 1], ['CEMU', 1], ['UNEC', 1], ['PTES', 1], ['OSMCA', 1], ['ISTAD', 1], ['DSTAD', 1], ['GS', 1], ['UDDC', 1], ['EMALT', 1], ['SECOS', 1], ['OVT', 1], ['HGONEC', 1], ['MYXO', 1], ['SCT', 1], ['CECC', 1], ['LGFMS', 1], ['PCM', 1], ['GNG', 1], ['CEEN', 1], ['DCIS', 1], ['CCOC', 1], ['ACA', 1], ['DES', 1], ['SOC', 1], ['BPSCC', 1], ['AML', 1], ['MSCHW', 1], ['PTAD', 1], ['PTCY', 1], ['CSCHW', 1], ['BIMT', 1], ['MPE', 1], ['AGNG', 1], ['WM', 1], ['PB', 1], ['EVN', 1], ['BMT', 1], ['MSTAD', 1], ['IMMC', 1], ['CEMN', 1], ['TSTAD', 1], ['CHBL', 1], ['ACPG', 1], ['OMT', 1], ['PMBL', 1], ['OMGCT', 1], ['SCCO', 1], ['SCCRCC', 1], ['LCLC', 1], ['SM', 1], ['PHCH', 1], ['EPMT', 1], ['SCUP', 1], ['LECLC', 1], ['BLCLC', 1], ['TLL', 1], ['SCOS', 1]];

        // save code and name to check for duplicate codes later
        if (!oncotreeCodesToNames.hasOwnProperty(childNode.code)) {
            oncotreeCodesToNames[childNode.code] = [];
        }
        oncotreeCodesToNames[childNode.code].push(childNode.name);

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
                var total = 0;
                for (i = 0; i in childNode.children; i++) {
                    total += childNode.children[i].number;
                }
                childNode.number += total;
            }
        }
    }

    function initDataAndTree(version) {
        tree = d3.layout.tree()
            .nodeSize([20, null]);

        diagonal = d3.svg.diagonal()
            .projection(function (d) {
                return [d.y, d.x];
            });

        vis = d3.select("#tree-div").append("svg:svg")
            .attr("width", w + m[1] + m[3])
            .attr("height", h + m[0] + m[2])
            .append("svg:g")
            .attr("transform", "translate(" + m[3] + "," + 300 + ")");

        d3.json('data/tumor_types.json', function (oncotree_json) {
            var rootNode = new UniqueTreeNodeDatum();
            rootNode.name = 'Tissue';
            rootNode.children = []

            getOncotreeCodeKeysSortedByName(oncotree_json.TISSUE.children).forEach(function (code) {
                var childData = oncotree_json.TISSUE.children[code];
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
            $("#oncotree-version-statistics").text(function () {
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
        update(root);
        numOfTissues = root.children.length;
        root.children.forEach(searchLeaf);
        treeBuildComplete = true;
    }

    function update(source) {
        var duration = d3.event && d3.event.altKey ? 5000 : 500;
        //IE translate does not have comma to seperate the x, y. Instead, it uses space.
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
            .attr("r", 4.5)
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

                _qtipContent += '<b>Code:</b> ' + d.code +

                     //clipboard JS is not supported in Safari.
                    ((is_safari && !is_chrome) ?
                        '<button style="margin-left: 5px;" class="btn btn-light btn-sm" ' +
                        ' disabled>"Copy" is not available in Safari</button>' :
                        '<button style="margin-left: 5px;" class="clipboard-copy btn btn-light btn-sm" ' +
                        'data-clipboard-text="' + d.code + '"  ' +
                        '>Copy</button>'
                    ) +
                    '<br/>';
                _qtipContent += '<b>Name:</b> ' + d.name.replace(/\(\w+\)$/gi, '') + '<br/>';
                _qtipContent += '<b>Main type:</b> ' + d.mainType + '<br/>';
                _qtipContent += '<b>Number:</b> ' + d.number + '<br/>';
                _qtipContent += '<b>NCI:</b> ' + nci_links.join(",") + '<br/>';
                _qtipContent += '<b>UMLS:</b> ' + umls_links.join(",") + '<br/>';
                _qtipContent += '<b>Color:</b> ' + (d.color||'LightBlue') + '<br/>';
                if (typeof d.history !== 'undefined' && d.history != '') {
                    _qtipContent += '<b>Previous codes:</b> ' + d.history  + '<br/>';
                    if (typeof d.hasRevocations !== 'undefined' && d.hasRevocations) {
                        _qtipContent += '<text style="padding-left: 5px;">* Use of codes shown in red is now discouraged.</text>';
                    }
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

        var clipboard = new ClipboardJS('.clipboard-copy.btn');

        // Transition nodes to their new position.
        var nodeUpdate = node.transition()
            .duration(duration)
            .attr("transform", function (d) {
                return "translate(" + d.y + "," + d.x + ")";
            });

        nodeUpdate.select("circle")
            .attr("r", function (d) {
                if (d.number >= 1000) {
                    return 9;
                } else if (d.number >= 100) {
                    return 7;
                } else if (d.number >= 10) {
                    return 5;
                } else if (d.number >= 1) {
                    return 3;
                } else {
                    return 1;
                }
            })
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
            .attr("r", function (d) {
                if (d.number >= 1000) {
                    return 9;
                } else if (d.number >= 100) {
                    return 7;
                } else if (d.number >= 10) {
                    return 5;
                } else if (d.number >= 1) {
                    return 3;
                } else {
                    return 1;
                }
            });

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

    function readyForSearch() {
      return treeBuildComplete;
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
        readyForSearch: readyForSearch,
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
