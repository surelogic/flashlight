
var ACTIVE_COL = '0000FF';
var INACTIVE_COL = 'CCCCCC';
var ACTIVE_WIDTH = 3;
var INACTIVE_WIDTH = 1;
var O_RIGHT = 'image_files/outline_right.png';
var O_DOWN = 'image_files/outline_down.png';
var O_FILLER = 'image_files/outline_filler.png';
var fd, icicle, sb, tm, tl;

//Escaping function for selectors
function jq(myid) {
   return myid.replace(/(:|\.)/g,'\\$1');
}

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


function treeTable() {
   $(".treeTable td:first-child:not(.leaf)")
      .prepend("<img class='icon' src='" + O_DOWN + "'></img>")
      .find("> .icon")
      .click(toggleTree);
   $(".treeTable td.leaf").prepend("<img class='icon' src='" + O_FILLER + "'></img>");
   $(".treeTable td.depth1:not(.leaf) > .icon").each(toggleTree);
}

var depths = {
   "depth1" : 1,
   "depth2" : 2,
   "depth3" : 3,
   "depth4" : 4,
   "depth5" : 5
};
var MAX_DEPTH = 5;

function elemDepth(elem) {
   for (var d in depths) {
      if(elem.hasClass(d)) {
         return depths[d];
      }
   }
  return MAX_DEPTH;
};

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

function jsonOutline(outline,json,threads) {
    function filter(node) {
        for (var i = 0; i < node.threadsSeen.length; i++) {
            if(threads[node.threadsSeen[i]]) {
                return true;
            }
        } 
        return false;
    }
    outline.get(0).json = json;
    outline.find('> ul').empty();
    outlineExpand(outline,filter);
}


function outlineExpand(node,filter) {
    node.find('> .icon').attr('src', O_DOWN).one('click', 
                                                 function() {
                                                     outlineCollapse($(this).parent(), filter);
                                                 });
    var childList = node.find('> ul');
    var json = node.get(0).json;
    for(var n in json) {
        //this is a child of the selected node
        var elem = json[n];
        if(filter(elem)) {
            var hasChildren = false;
            for(var child in elem.children) {
                hasChildren = true;
                break;
            }
            var li = childList.append(siteTag(hasChildren, elem.site)).children().last();
            li.get(0).json = elem.children;
            if(hasChildren) {
            li.find('> .icon').one('click', 
                                   function() {outlineExpand($(this).parent(), filter);});
            }
        }
    }
}

function outlineCollapse(node, filter) {
    node.find('> .icon').attr('src', O_RIGHT).one('click',
                                                  function() {
                                                      outlineExpand(node, filter);
                                                  });
    node.find('> ul').empty();
}

function siteTag(hasChildren, site) {
    var image = '<img class="icon" src="' + (hasChildren ? O_RIGHT : O_FILLER) + '"></img>';
    var span = '<span>' + site.pakkage + '.' + site.clazz + '.' + site.location + '</span>';
    var link = '<a href="index.html?loc=&Package=' + site.pakkage + '&Class=' + site.clazz + 
        '&Method=' + site.location + '&Line=' + site.line + '">(' + site.file + ':' + 
        site.line + ')</a>';
    var childList = hasChildren ? '<ul></ul>' : '';
    return '<li>' + image + span + link + childList + '</li>';
}

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

var rcSelected = null;
function initRaceConditionTab() {
   $(".locksetoutline .depth3 > .icon").hide();
   $(".locksetoutline").hide();
   // Hack to make the filler icons not crap out
   $(".locksetlink").click(
      function(event) {
         event.preventDefault();
         if(rcSelected) {
            rcSelected.hide();
         }
         rcSelected = $(jq($(this).attr("href")));
         rcSelected.show();
      }
   );
}

var bpSelected = null;
function initBadPublishTab() {
   $(".badPublishTrace").hide();
   $(".badPublishTraceLink").click(
      function(event) {
         event.preventDefault();
         if(bpSelected) {
            bpSelected.hide();
         }
         bpSelected = $(jq($(this).attr("href")));
         bpSelected.show();
      }
   );
}


function displayDeadlock(cyc) {
   $('.deadlock-cycle-edges').hide();
   $('#deadlock-edges-' + cyc).show();
   $('.deadlock-cycle-traces').hide();
   $("#deadlock-traces-" + cyc).show();
   loadDeadlockGraph(deadlocks[cyc]);
}

function initDeadlockGraphTab() {
   $("#deadlock-list li").click(
      function (event) {
         var val = $(this).attr("id");
         displayDeadlock(val);
      });
   $('.deadlock-trace-edge').hide();
   initGraphs();
   var cyc = $("#deadlock-list li").first().attr("id");
   displayDeadlock(cyc);
}

function initCoverageTab() {
    var threadDiv = $('#threads');
    var selectedThreads = {};
    var threadList = [];
    for (var t in threads) {
        threadList.push({id: t, name: threads[t].name});
        selectedThreads[t] = true;
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
    jsonOutline($('#coverage'), coverage, selectedThreads);
    threadDiv.find('li').click(
        function (e) {
            var threadId = Number($(this).attr('id'));
            if (e.shiftKey) {
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
            var someSelected = false;
            for (var t in threads) {
                someSelected |= selectedThreads[t];
            }
            if (!someSelected) {
                //When no threads are selected, we show everything
                for (var t in threads) {
                    selectedThreads[t] = true;
                }
            }
            jsonOutline($('#coverage'), coverage, selectedThreads);
        }).
        mousedown(function(e) {e.preventDefault();})
    ;
}

$(document).ready(
   function() {

      $("#main #content .section").hide();
      //Init outlines and tree tables
      outline();
      treeTable();

      // We load timeline separately b/c it takes too long to do on startup

      $('.sectionList > li.selected > a[href="index2.html"]').each(
         function () {
               initDeadlockGraphTab();
         }
      );
      //Init race condition logic
      $('.sectionList > li.selected > a[href="index3.html"]').each(
         function () {
               initRaceConditionTab();
         }
      );
      //Init bad publishes logic
      $('.sectionList > li.selected > a[href="index4.html"]').each(
         function () {
               initBadPublishTab();
         }
      );
      $('.sectionList > li.selected > a[href="index5.html"]').each(
         function () {
               loadTimeline();
         }
      );
      $('.sectionList > li.selected > a[href="index6.html"]').each(
         function () {
             initCoverageTab();
         }
      );
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
