
var ACTIVE_COL = '0000FF';
var INACTIVE_COL = 'CCCCCC';
var ACTIVE_WIDTH = 3;
var INACTIVE_WIDTH = 1;
var O_RIGHT = 'image_files/outline_right.png';
var O_DOWN = 'image_files/outline_down.png';
var O_FILLER = 'image_files/outline_filler.png';
var fd, icicle, sb, tm, tl;

(function() {
    var ua = navigator.userAgent,
    iStuff = ua.match(/iPhone/i) || ua.match(/iPad/i),
    typeOfCanvas = typeof HTMLCanvasElement,
    nativeCanvasSupport = (typeOfCanvas == 'object' || typeOfCanvas == 'function'),
    textSupport = nativeCanvasSupport
       && (typeof document.createElement('canvas').getContext('2d').fillText == 'function');
    //I'm setting this based on the fact that ExCanvas provides text support for IE
    //and that as of today iPhone/iPad current text support is lame
    labelType = (!nativeCanvasSupport || (textSupport && !iStuff))? 'Native' : 'HTML';
    nativeTextSupport = labelType == 'Native';
    useGradients = nativeCanvasSupport;
    animate = !(iStuff || !nativeCanvasSupport);
 })();

// Turn a table marked with class treeTable into a tree-table.  Rows in
// the table are marked with classes depth1, depth2, etc.
function treeTable() {
   $(".treeTable td:first-child:not(.leaf)")
      .prepend("<img class='icon' src='" + O_DOWN + "'></img>")      
      .find("> .icon")
      .click(toggleTree);
   $(".treeTable td.leaf").prepend("<img class='icon' src='" + O_FILLER + "'></img>");
   $(".treeTable td.depth1:not(.leaf) > .icon").each(toggleTree);
}

function toggleTree() {
   var icon = $(this);
   var doOpen = icon.attr('src') == O_RIGHT;
   icon.attr('src', doOpen ? O_DOWN : O_RIGHT);
   var elem = icon.parent();
   var depth = elemDepth(elem);
   var row = elem.parent().next();
   while(row.length) {
      var rowDepth = elemDepth(row.children().first());
      if(rowDepth <= depth) {
         return;
      }
      if(doOpen) {
         if(rowDepth == depth + 1) {
            var rowIcon = row.find('.icon');
            if(rowIcon.attr('src') != O_FILLER) {
               rowIcon.attr('src',O_RIGHT);
            }
            row.show();
         } else {
            row.hide();
         }
      } else {
         row.hide();
      }
      row = row.next();
   }
}

var depths = {
   "depth1" : 1,
   "depth2" : 2,
   "depth3" : 3,
   "depth4" : 4,
   "depth5" : 5
};

var MAX_DEPTH = 5;
function hasDepth(node) {
    for(var x in depths) {
        if(node.hasClass(x)) {
         return true;   
        }
    }
    return false;
}

function depthClass(depth) {
    switch (depth) {
        case 1 : return "depth1";
        case 2 : return "depth2";
        case 3 : return "depth3";
        case 4 : return "depth4";
        case 5 : return "depth5";
    }
    throw 'depth of ' + depth + 'not supported';
}

function elemDepth(elem) {
   for (var d in depths) {
      if(elem.hasClass(d)) {
         return depths[d];
      }
   }
  return -1;
};

// Construct a tree-table in the given html node, backed by a json
// object.
function jsonTreeTable(html, json, filter, header, row) {
    var table = '<table><thead><tr>' + header() + '</tr></thead><tbody><tr></tr></tbody></table>';
    html.append(table);
    var firstRow = html.find('> table > tbody > tr:first-child');
    firstRow.get(0).json = json;
    tableExpand(firstRow, 1, filter, row);
    firstRow.remove();
}

function tableExpand(tr, depth, filter, row) {
    var json = tr.get(0).json;
    tr.find('> td > .icon').attr('src', O_DOWN).one('click', function (e) {
        tableCollapse($(this).parent().parent(), depth, filter, row);
    });

    var next = tr;
    for(var x in json) {
        var elem = json[x];
        if(filter(elem)) {
            var hasChildren = false;
            if(elem.children != undefined) {
                for(var child in elem.children) {
                    hasChildren = true;
                    break;
                }
            }
            var r = row(elem);
            next.after('<tr><td class="' + depthClass(depth) + '"><img class="icon" src="' + 
                     (hasChildren ? O_RIGHT : O_FILLER)+ '"/>'+ r.first +'</td>' +  r.rest + '</tr>');
            next = next.next();
            if(r.register != undefined) {
                r.register(next);
            }
            if(hasChildren) {
                next.get(0).json = elem.children;
                next.find('> td > .icon').one('click',function (e) {
                    tableExpand($(this).parent().parent(),depth+1,filter,row);
                });
            }
        }
    }
}

function tableCollapse(tr, depth, filter, row) {
    var json = tr.get(0).json;
    tr.find('> td > .icon').attr('src', O_RIGHT).one('click', function () {
        tableExpand(tr,depth,filter,row);
    });
    for(var n = tr.next(); elemDepth(n.find('td:first-child')) >= depth; n = tr.next()) { 
        n.remove();
    }
}


function coverageOutline(outline,threads) {
    function filter(node) {
        return intersects(threads, node.threadsSeen);
    }
    function siteTag(hasChildren, node) {
        var site = sites[node.site];
        var span = '<span>' + site.pakkage + '.' + site.clazz + '.' + escapeHtml(site.location) + '</span>';
        var link = '<a href="index.html?loc=&Package=' + site.pakkage + '&Class=' + site.clazz + 
            '&Method=' + encodeURI(site.location) + '&Line=' + site.line + '">(' + site.file + ':' + 
            site.line + ')</a>';
        return { text: span + link };
    }
    function children(node) {
        return $.map(node.children, function (i) { return coverage[i]; });
    }
    jsonOutline(outline,coverage[0],filter,children,siteTag);
}

// Construct an outline in the given html node, backed by a json object.
function jsonOutline(outline,json,filter,children,show) {
    outline.get(0).json = json;
    outline.find('> ul').empty();
    outlineExpand(outline,filter,children,show);
}


function outlineExpand(node,filter,children, show) {
    node.find('> .icon').attr('src', O_DOWN).one('click', 
                                                 function() {
                                                     outlineCollapse($(this).parent(), filter, children, show);
                                                 });
    var childList = node.find('> ul');
    var json = children(node.get(0).json);
    for(var n in json) {
        //this is a child of the selected node
        var elem = json[n];
        if(filter(elem)) {
            var hasChildren = false;
            if(elem.children != undefined) {
                for(var child in elem.children) {
                    hasChildren = true;
                    break;
                }
            }
            var toShow = show(hasChildren, elem);
            var li = childList.append('<li><img class="icon" src="' + (hasChildren ? O_RIGHT : O_FILLER) + '"></img>' + 
                                      toShow.text + (hasChildren ? '<ul></ul>' : '') + 
                                      '</li>').children().last();
            if(toShow.register != undefined) {
                toShow.register(li);
            }
            if(hasChildren) {
                li.get(0).json = elem;
                li.find('> .icon').one('click', 
                                       function() {outlineExpand($(this).parent(), filter, children, show);});
            }
        }
    }
}

function outlineCollapse(node, filter, children, show) {
    node.find('> .icon').attr('src', O_RIGHT).one('click',
                                                  function() {
                                                      outlineExpand(node, filter, children, show);
                                                  });
    node.find('> ul').empty();
}

// Turn a list into an outline.
function outline() {
   $(".outline > li:has(ul)")
      .prepend("<img class='icon' alt='Expand/' src='" + O_DOWN + "'></img>")
      .find('> .icon')
      .each(toggle)
      .click(toggle);
   $(".outline > li:not(:has(ul))").prepend("<img class='filler' src='" + O_FILLER+ "'></img>");
   $(".outline .field").click(
      function (event) {
         event.preventDefault();
         var fieldElem = $(this);
         var classElem = fieldElem.parent().parent().prev();
         var packageElem = classElem.parent().parent().prev();
         var url = "index.html?loc=" + "&Field=" + fieldElem.text() + "&Package=" + packageElem.text() + "&Class=" + classElem.text();
         $(window.location).attr('href',url);
      });
}

function toggle() {
   var current = $(this).attr('src');
   if(current == O_RIGHT) {
      $(this).attr('src', O_DOWN);
      var toShow = $(this).parent().children('ul');
      var subtrees = toShow.find('> li:has(ul)');
      subtrees.find('.icon').attr('src', O_RIGHT);
      // Add the img icon to any trees that don't have it yet
      subtrees.not(':has(> .icon)')
         .prepend("<img class='icon' alt='Expand' src='" + O_RIGHT + "'></img>")
         .find('> .icon')
         .click(toggle);
      toShow.find('> li:not(:has(ul))').prepend("<img class='filler' src='" + O_FILLER+ "'></img>");
      var toHide = toShow.find('li > ul');
      toShow.show();
      toHide.hide();
   } else {
      $(this).attr('src', O_RIGHT);
      $(this).parent().children('ul').hide();
   }
}


function initForceDirected() {
    // Edge type used to represent a bidirectional edge
    $jit.ForceDirected.Plot.EdgeTypes.implement(
        {
            'doubleArrow': {
                'render': function(adj, canvas) {
                    var from = adj.nodeFrom.pos.getc(true),
                    to = adj.nodeTo.pos.getc(true),
                    dim = adj.getData('dim'),
                    direction = adj.data.$direction,
                    inv = (direction && direction.length>1 && direction[0] != adj.nodeFrom.id);
                    this.edgeHelper.arrow.render(from, to, dim, inv, canvas);
                    this.edgeHelper.arrow.render(to, from, dim, inv, canvas);
                },
                'contains': function(adj, pos) {
                    var from = adj.nodeFrom.pos.getc(true),
                    to = adj.nodeTo.pos.getc(true);
                    return this.edgeHelper.arrow.contains(from, to, pos, this.edge.epsilon) ||
                        this.edgeHelper.arrow.contains(to, from, pos, this.edge.epsilon);
                }
            }
        });


 if($('#deadlock-widget').val() != undefined) {
    fd = new $jit.ForceDirected(
       {
   //id of the visualization container
   injectInto: 'deadlock-widget',
   //Enable zooming and panning
   //by scrolling and DnD
   Navigation: {
      enable: true,
      //Enable panning events only if we're dragging the empty
      //canvas (and not a node).
      panning: 'avoid nodes',
      zooming: 10 //zoom speed. higher is more sensible
   },
   // Change node and edge styles such as
   // color and width.
   // These properties are also set per node
   // with dollar prefixed data-properties in the
   // JSON structure.
   Node: {
      overridable: true
   },
   Edge: {
      overridable: true,
      color: '#23A4FF',
      lineWidth: 0.4
   },
   //Native canvas text styling
   Label: {
      type: labelType, //Native or HTML
      size: 10,
      style: 'bold'
   },
   //Add Tips
   //     Tips: {
   //        enable: true,
   //        onShow: function(tip, node) {
   //count connections
   //    var count = 0;
   //    node.eachAdjacency(function() { count++; });
   //display node info in tooltip
   //    tip.innerHTML = "<div class=\"tip-title\">" + node.name + "</div>"
   //       + "<div class=\"tip-text\"><b>connections:</b> " + count + "</div>";
   //        }
   //     },
   // Add node events
   Events: {
      enable: true,
      //Change cursor style when hovering a node
      onMouseEnter: function() {
         fd.canvas.getElement().style.cursor = 'move';
      },
      onMouseLeave: function() {
         fd.canvas.getElement().style.cursor = '';
      },
      //Update node positions when dragged
      onDragMove: function(node, eventInfo, e) {
         var pos = eventInfo.getPos();
         node.pos.setc(pos.x, pos.y);
         fd.plot();
      },
      //Implement the same handler for touchscreens
      onTouchMove: function(node, eventInfo, e) {
         $jit.util.event.stop(e); //stop default touchmove event
         this.onDragMove(node, eventInfo, e);
      },
      //Add also a click handler to nodes
      onClick: function(node, eventInfo, e) {
         //TODO
      }
   },
   //Number of iterations for the FD algorithm
   iterations: 200,
   //Edge length
   levelDistance: 130,
   // Add text to the labels. This method is only triggered
   // on label creation and only for DOM labels (not native canvas ones).
   onCreateLabel: function(domElement, node){
      domElement.innerHTML = node.name;
      var style = domElement.style;
      style.fontSize = "0.8em";
      style.color = "#ddd";
   },
   // Change node styles when DOM labels are placed
   // or moved.
   onPlaceLabel: function(domElement, node){
      var style = domElement.style;
      var left = parseInt(style.left);
      var top = parseInt(style.top);
      var w = domElement.offsetWidth;
      style.left = (left - w / 2) + 'px';
      style.top = (top + 10) + 'px';
      style.display = '';
   }
       });
 }
}

function initGraphs() {
      initForceDirected();
}

function loadDeadlockGraph(data) {
      // load JSON data.
   fd.loadJSON(data);
   // compute positions incrementally and animate.
   fd.computeIncremental({
       iter: 40,
       property: 'end',
       onStep: function(perc){

       },
       onComplete: function(){
          fd.animate({
          modes: ['linear'],
          transition: $jit.Trans.Elastic.easeOut,
          duration: 2500
       });
       }
    });
   $('#deadlock-threads').empty();
   for(var i = 0; i < data.threads.length; i++) {
      $('#deadlock-threads').append('<h3 class="deadlockThread">' + data.threads[i] + '</h3>');
   }
   var edgeUl = $('#deadlock-edges .deadlock-trace-menu');
   edgeUl.empty();
   for(i = 0; i < data.edges.length; i++) {
      edgeUl.append('<li><a href=\"#deadlock-trace-' + data.edges[i].id + '">' +
                    data.edges[i].name + '</a></li>');
   }

   $(".deadlock-trace-menu li a").click(
      function (event) {
         event.preventDefault();
         $('.deadlock-trace-edge').hide();
         var edgeId = $(this).attr('href');
         $(jq(edgeId)).show();
         fd.graph.eachNode(function (n) {
          n.eachAdjacency(function (adj) {
              var found = false;
              for(var i = 0; i < adj.data.edgeIds.length; i++) {
                  var edgeCheck = '#deadlock-trace-' + adj.data.edgeIds[i];
                  if(edgeCheck == edgeId) {
                      found = true;
                      adj.setDataset('end', {
                          'color': ACTIVE_COL,
                          'lineWidth': ACTIVE_WIDTH
                      });
                  }
              }
              if (!found) {
                  adj.setDataset('end', {
                      'color': INACTIVE_COL,
                      'lineWidth': INACTIVE_WIDTH
                  });
              }
          });
         });
         fd.fx.animate({
           modes: ['edge-property:lineWidth:color'],
                   duration: 500
         });
      }
   );
   $("#deadlock-threads .deadlockThread").hover(function () {
    var thread = $(this).html();
    fd.graph.eachNode(function (n) {
     n.eachAdjacency(function (adj) {
      if(adj.data.threads.indexOf(thread) >= 0) {
       adj.setDataset('end', {
        'color': ACTIVE_COL,
        'lineWidth': ACTIVE_WIDTH
       });
      } else {
       adj.setDataset('end', {
        'color': INACTIVE_COL,
        'lineWidth': INACTIVE_WIDTH
       });
      }
     });
    });
    fd.fx.animate({
     modes: ['edge-property:lineWidth:color'],
     duration: 500
    });
   }, function () {
      var thread = $(this).html();
    fd.graph.eachNode(function (n) {
     n.eachAdjacency(function (adj) {
      adj.setDataset('end', {
       'color': INACTIVE_COL,
       'lineWidth': INACTIVE_WIDTH
      });
     });
    });
    fd.fx.animate({
     modes: ['edge-property:lineWidth:color'],
     duration: 500
    });
   });
}

var eventSource;
function loadTimeline() {
   SimileAjax.History.enabled = false;
   if (timeline_data == 'none') {
      return;
   }
   eventSource = new Timeline.DefaultEventSource();
   var tl_el = document.getElementById("tl");
   var theme1 = Timeline.ClassicTheme.create();
   theme1.autoWidth = true; // Set the Timeline's "width" automatically.
                            // Set autoWidth on the Timeline's first band's theme,
                            // will affect all bands.
   theme1.timeline_start = timeline_data.first;
   theme1.timeline_stop  = timeline_data.last;

   var d = timeline_data.first;
   var bandInfos;
   if(timeline_data.needsOverview) {
      bandInfos = [
         Timeline.createBandInfo({
            width:          40, //"70%", // set to a minimum, autoWidth will then adjust
            intervalUnit:   timeline_data.mainBandInterval, // Timeline.DateTime.SECOND,
            intervalPixels: timeline_data.mainBandIntervalPixels,
            eventSource:    eventSource,
            date:           d,
            theme:          theme1,
            layout:         'original'  // original, overview, detailed
                                 }),
         Timeline.createBandInfo({
            width:          40, //"30%", // set to a minimum, autoWidth will then adjust
            intervalUnit:   timeline_data.overviewBandInterval, // Timeline.DateTime.MINUTE,
            intervalPixels: timeline_data.overviewBandIntervalPixels,
            eventSource:    eventSource,
            date:           d,
            theme:          theme1,
            layout:         'overview'  // original, overview, detailed
          })
      ];
      bandInfos[1].syncWith = 0;
      bandInfos[1].highlight= true;
   } else {
      bandInfos = [
         Timeline.createBandInfo({
            width:          40, //"70%", // set to a minimum, autoWidth will then adjust
            intervalUnit:   timeline_data.mainBandInterval, // Timeline.DateTime.SECOND,
            intervalPixels: timeline_data.mainBandIntervalPixels,
            eventSource:    eventSource,
            date:           d,
            theme:          theme1,
            layout:         'original'  // original, overview, detailed
   })];
   }
   // create the Timeline
    tl = Timeline.create(tl_el, bandInfos);

   var url = '.'; // The base url for image, icon and background image
   // references in the data
   eventSource.loadJSON(timeline_data, url); // The data was stored into the
                                             // timeline_data variable.
   tl.finishedEventLoading();
   tl.layout(); // display the Timeline
}

function displayLockSetTable(node) {
    var lockset = node.lockset;
    var locks = [];
    for (i in lockset) {
        locks.push(lockset[i]);
    }
    if(locks.length == 0) {
        return '<div class="info locksetoutline">No locks where held when this field was accessed.</div>';
    }
    locks.sort(function(a,b) {
        var an = Number(a.heldPercentage);
        var bn = Number(b.heldPercentage);
        return bn - an;
    });
    var table = '<table class="locksetoutline treeTable">';
    table +='<thead><tr><th>Lock</th><th># Acquisitions</th><th>% Held</th></tr></thead>';
    table += '<tbody>';
    for(var i = 0; i < locks.length; i++) {
        table += displayLockSetRow(locks[i]);
    }
    if(node.locksetlink) {
        table += '<tr><td class="cell-text leaf more-results-td depth1" colspan="3">' + 
            '<a href="' +  node.locksetlink.href + '">' + node.locksetlink.text + '</a></td></tr>';
    }
    table += '</tbody></table>';
    return table;
}

function strcmp(a,b) {
    return a == b ? 0 : (a < b ? -1 : 1);
}

function sortfn(fn,sort) {
    return function (a,b) {
        return sort(fn(a), fn(b));
    };
}

function sortBySource(a,b) {
    if(a.pakkage == b.pakkage) {
        if(a.clazz == b.clazz) {
            return strcmp(a.location, b.location);
        } else {
            return strcmp(a.clazz, b.clazz);
        }
    } else {
        return strcmp(a.pakkage, b.pakkage);
    }
}

function displayLockSetRow(lock) {
    function heldAtFn(l) {
        return '<span> at ' + displaySite(l.site) + ' via ' + displaySite(l.acquiredAt);
    }
    function notHeldAtFn(l) {
        return '<span> at ' + displaySite(l.site);
    }
    var rows = '<tr><td class="cell-text depth1">' + lock.name + '</td>';
    rows += '<td class="cell-number">' + lock.acquisitions + '</td>';
    rows += '<td class="cell-number">' + lock.heldPercentage + '</td>';
    rows += '</tr>';
    rows += '<tr><td class="cell-text depth2">held at</td><td></td><td></td>';
    rows += '<tr><td class="cell-text depth3 leaf">';
    rows += displayClassTree(lock.heldAt, heldAtFn);
    rows += '</td><td></td><td></td></tr>';
    rows += '<tr><td class="cell-text depth2">not held at</td><td></td><td></td>';
    rows += '<tr><td class="cell-text depth3 leaf">';
    rows += displayClassTree(lock.notHeldAt, notHeldAtFn);
    rows += '</td><td></td><td></td></tr>';
    return rows;
}

function displayClassTree(leaves, leafFn) {
    var pakkage = null, clazz = null;
    var rows = '<ul class="outline">';
    for(var i = 0; i < leaves.length; i++) {
        var leaf = leaves[i];
        var site = leaf.site;
        if(site.pakkage != pakkage) {
            if(pakkage != null) {
                rows += '</ul></li></ul></li>';
            }
            pakkage = site.pakkage;
            clazz = null;
            rows += '<li><span class="package">' + pakkage + '</span><ul>';
        }
        if(site.clazz != clazz) {
            if(clazz != null) {
                rows += '</ul></li>';
            }
            clazz = site.clazz;
            rows += '<li><span class="class">' + clazz + '</span><ul>';
        }
        rows += '<li>';
        rows += leafFn(leaf);
        rows += '</li>';
    }
    rows += '</ul>';
    return rows;
}

function displaySite(site) {
    return site.location + 
        '<a href="?loc=&Package=' + site.pakkage + '&Class='  + site.clazz + '&Method=' + 
        encodeURI(site.location) + '&Line=' + site.line + '">(' + site.clazz + ':' + site.line + 
        ')</a>';
}

function initRaceConditionTab() {
    function filter(node) { return true; }
    function selectLockSet(rc) {
        var lsSelected = $('#lockset-locks');
        lsSelected.empty();
        lsSelected.append(displayLockSetTable(rc));
        outline();              
        treeTable();
        // Hack to make the filler icons not crap out
        $(".locksetoutline .depth3 > .icon").hide();
    }
    function tag(hasChildren, node) {
        if(node.pakkage != undefined) {
            return { text: '<span class="package">' + node.pakkage + '</span>' };
        } else if (node.clazz != undefined) {
            return { text: '<span class="class">' + node.clazz + '</span>' } ;
        } else {
            return { text: '<a class="locksetlink field" href="#locksets-' + node.qualified + '">' + node.field + '</a>',
                     register: function (elem) { 
                         elem.find('> a').click(function (e) { 
                             e.preventDefault();
                             selectLockSet(node); 
                         });
                     }
                   };
        }
    }
    function children(node) {
        return node.children;
    }
    jsonOutline($('#lockset-outline'),lockSets,filter,children,tag);
}

function initBadPublishTab() {
    function showTrace(trace) {
        var show = '';
        for(var i = 0; i < trace.length; i++) {
            var t = trace[i];
            show += '<li>at ' + t.pakkage + '.' + t.clazz + '.' + escapeHtml(t.location);
            show += '<a href="?loc=&Package=' + t.pakkage + '&Class=' + t.clazz + '&Method=' + encodeURI(t.location) + '&Line=' + t.line;
            show += '">(' + t.file + ':' + t.line + ')</a></li>';
        }
        return show;
    }
    function showAccess(access) {
        var show = '<ul class="badPublishTrace">Access of ';
        show += '<li>' + access.qualified + '</li>';
        show += showTrace(access.trace);
        show += '</ul>';
        return show;
    }
    function filter() {
        return true;
    }
    function header() {
        return '<th>Package/Class/Field/Time</th><th>Thread</th><th>Read</th>';
    }
    function row(json) {
        var t;
        var r = '<td></td><td></td>';
        var reg = undefined;
        if(json.pakkage != undefined) {
            t = '<span class="package">' + json.pakkage + '</span>';
        } else if (json.clazz != undefined) {
            t =  '<span class="class">' + json.clazz + '</span>';
        } else if (json.field != undefined) {
            t = '<span class="field">' + json.field + '</span>';
        } else if (json.text != undefined) {
            // More results
            t = '<a href="' + json.href + '">' + json.text + '</a>';
        } else {
            t = '<a class="badPublishTraceLink" href="#badPublishTrace-'+ json.id + '">' + json.time + '</a>';
            r = '<td>' + json.thread + '</td><td>' + (json.read ? 'Read' : 'Write') + '</td>';
            reg = function (node) {
                node.find('.badPublishTraceLink').click(
                    function(event) {
                        event.preventDefault();
                        var trace = $('#badpublish-trace');
                        trace.empty();
                        trace.append(showAccess(json));
                    });
            };
        }
        return { first : t, rest : r, register : reg};
    }
    jsonTreeTable($('#badpublish-table'),badPublishes,filter,header,row);
}


function displayDeadlock(cyc) {
   $('.deadlock-cycle-edges').hide();
   $('#deadlock-edges-' + cyc).show();
   $('.deadlock-cycle-traces').hide();
   $("#deadlock-traces-" + cyc).show();
   loadDeadlockGraph(deadlocks[cyc]);
}

function initDeadlockGraphTab() {
   $("#deadlock-list li.deadlock-cycle-link").click(
      function (event) {
         var val = $(this).attr("id");
         displayDeadlock(val);
      });
   $('.deadlock-trace-edge').hide();
   initGraphs();
   var cyc = $("#deadlock-list li").first().attr("id");
   displayDeadlock(cyc);
}

function initSharedFieldsTab() {
    function filter(node) {
        return intersects(getSelectedNumbers(selectedThreads), node.threadsSeen);
    }
    function tag(hasChildren, node) {
        if(node.pakkage != undefined) {
            return { text: '<span class="package">' + node.pakkage + '</span>' };
        } else if (node.clazz != undefined) {
            return { text: '<span class="class">' + node.clazz + '</span>' } ;
        } else {
            return { text: '<a class="locksetlink field" href="#locksets-' + node.qualified + '">' + node.field + '</a>',
                     register: function (elem) { 
                         elem.find('> a').click(function (e) { 
                             e.preventDefault();
                             // Do something here
                         });
                     }
                   };
        }
    }
    function children(node) {
        return node.children;
    }
    jsonOutline($('#shared-outline'),fields,filter,children,tag);
    initThreads(function (e) {
        jsonOutline($('#shared-outline'),fields,filter,children,tag);
    });
}

var selectedThreads = {};
var threadList = [];
function initThreads(callback) {
    var threadDiv = $('#threads');
    for (var t in threads) {
        threadList.push({id: t, name: threads[t].name});
        selectedThreads[t] = false;
    }
    threadList.sort(function(a,b) {
        var an = a.name;
        var bn = b.name;
        if (an < bn) {
            return -1;
        } else if (an > bn) {
            return 1;
        } else {
            return 0;
        }
    });
    for (var i = 0; i < threadList.length; i++) {
        threadDiv.append('<li id="' + threadList[i].id + '">' + threadList[i].name + '</li>');
    }
    threadDiv.find('li').click(function (e) {
        var threadId = Number($(this).attr('id'));
        if (e.shiftKey || e.ctrlKey) {
            selectedThreads[threadId] = !selectedThreads[threadId];
            $('#' + threadId).toggleClass('selected');
        } else {
            for (var t in threads) {
                selectedThreads[t]= false;
                $('#' + t).removeClass('selected');
            }
            selectedThreads[threadId] = true;
            $('#' + threadId).addClass('selected');
        }
        callback(e);
    }).mousedown(
        function(e) {
            e.preventDefault();
        }
    );
}

function getSelectedNumbers(map) {
    var selected = [];
    for(var t in map) {
        if(map[t]) {
            selected.push(Number(t));
        }
    }
    return selected;
}

function initCoverageTab() {
    coverageOutline($('#coverage'), getSelectedNumbers(selectedThreads));
    initThreads(function (e) {
        coverageOutline($('#coverage'), getSelectedNumbers(selectedThreads));
    });
}

$(document).ready(
   function() {
      $("#main #content .section").hide();
      //Init outlines and tree tables
      outline();
      treeTable();

      // We load timeline separately b/c it takes too long to do on startup

      $('.sectionHeaders td.selected > a[href="index2.html"]').each(
         function () {
               initDeadlockGraphTab();
         }
      );
      //Init race condition logic
      $('.sectionHeaders td.selected > a[href="index3.html"]').each(
         function () {
               initSharedFieldsTab();
         }
      );
      //Init race condition logic
      $('.sectionHeaders td.selected > a[href="index4.html"]').each(
         function () {
               initRaceConditionTab();
         }
      );
      //Init bad publishes logic
      $('.sectionHeaders td.selected > a[href="index5.html"]').each(
         function () {
               initBadPublishTab();
         }
      );
      $('.sectionHeaders td.selected > a[href="index6.html"]').each(
         function () {
               loadTimeline();
         }
      );
      $('.sectionHeaders td.selected > a[href="index7.html"]').each(
         function () {
             initCoverageTab();
         }
      );
      $('.timeline-copyright').hide();
   });



var resizeTimerID = null;
$(document).resize(
 function() {
  if (resizeTimerID == null) {
          resizeTimerID = window.setTimeout(function() {
              resizeTimerID = null;
              tl.layout();
          }, 500);
      }
 }
);

//Escaping function for selectors, escapes everything but #
function jq(myid) {
   return myid.replace(/([!"$%&'()*+,./:;<=>?@\[\\\]^`{|}~])/g,'\\$1');
}
function escapeHtml(html) {
   return html.replace('<','&lt;').replace('>', '&gt');
}

//Array intersection function
function intersects(arr1,arr2) {
    for(var i = 0; i < arr1.length; i++) {
        if(arr2.indexOf(arr1[i]) == -1) {
            return false;
        }
    }
    return true;
}

/*
 * Create a link with the given params.  Parameters are based in order.
 * First name, then value. For example: <code>link(name, "bd72", "name",
 * "foo")</code> would produce a link pointing to
 * <code>index.html?query=bd72&name=foo</code>
 */
function buildQueryLink(query) {
    var link = 'index.html?query=' + query;
    for(var i = 0; i < arguments.length; i += 2) {
        link += '&' + arguments[i] + '=' + arguments[i+1];
    }
    return link;
}