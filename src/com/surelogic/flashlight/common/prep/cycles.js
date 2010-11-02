
var ACTIVE_COL = '0000FF';
var INACTIVE_COL = 'CCCCCC';
var ACTIVE_WIDTH = 3;
var INACTIVE_WIDTH = 1;
var O_RIGHT = 'image_files/outline_right.png';
var O_DOWN = 'image_files/outline_down.png';
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

function initOutline() {
   $(".outline li:has(ul)").prepend("<img class='icon' alt='Expand' src='" + O_DOWN + "'></img>");
   $(".outline .icon").click(toggleThis);
   $(".outline.collapsed > li > .icon").each(toggleThis);
}

function toggleThis() {
   var current = $(this).attr('src');
   if(current == O_RIGHT) {
      $(this).attr('src', O_DOWN);
      var toShow = $(this).parent().children('ul');
      var toHide = toShow.find('li > ul');
      toShow.show();
      toHide.hide();
      toHide.parent().children('.icon').attr('src', O_RIGHT);
   } else {
      $(this).attr('src', O_RIGHT);
      $(this).parent().children('ul').hide();
   }

}

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

function initForceDirected() {
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
		 //	    Tips: {
		 //	       enable: true,
		 //	       onShow: function(tip, node) {
		 //count connections
		 //		  var count = 0;
		 //		  node.eachAdjacency(function() { count++; });
		 //display node info in tooltip
		 //		  tip.innerHTML = "<div class=\"tip-title\">" + node.name + "</div>"
		 //		     + "<div class=\"tip-text\"><b>connections:</b> " + count + "</div>";
		 //	       }
		 //	    },
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
		    onClick: function(node) {
		       if(!node) return;
		       // Build the right column relations list.
		       // This is done by traversing the clicked node connections.
		       var html = "<h4>" + node.name + "</h4><b> connections:</b><ul><li>",
		       list = [];
		       node.eachAdjacency(function(adj){
					     list.push(adj.nodeTo.name);
					  });
		       //append connections information
		       //$jit.id('inner-details').innerHTML = html + list.join("</li><li>") + "</li></ul>";
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


function initSquarified() {
	tm = new $jit.TM.Squarified({
  //where to inject the visualization
  injectInto: 'packages2',
  //parent box title heights
  titleHeight: 15,
  //enable animations
  animate: animate,
  //box offsets
  offset: 1,
  //Attach left and right click events
  Events: {
    enable: true,
    onClick: function(node) {
      if(node) tm.enter(node);
    },
    onRightClick: function() {
      tm.out();
    }
  },
  duration: 1000,
  //Enable tips
  Tips: {
    enable: true,
    //add positioning offsets
    offsetX: 20,
    offsetY: 20,
    //implement the onShow method to
    //add content to the tooltip when a node
    //is hovered
    onShow: function(tip, node, isLeaf, domElement) {
      var html = "<div class=\"tip-title\">" + node.name
        + "</div><div class=\"tip-text\">";
      var data = node.data;
      if(data.playcount) {
        html += "play count: " + data.playcount;
      }
      if(data.image) {
        html += "<img src=\""+ data.image +"\" class=\"album\" />";
      }
      tip.innerHTML =  html;
    }
  },
  //Add the name of the node in the correponding label
  //This method is called once, on label creation.
  onCreateLabel: function(domElement, node){
      domElement.innerHTML = node.name;
      var style = domElement.style;
      style.display = '';
      style.border = '1px solid transparent';
      domElement.onmouseover = function() {
        style.border = '1px solid #9FD4FF';
      };
      domElement.onmouseout = function() {
        style.border = '1px solid transparent';
      };
  }
});
tm.loadJSON(packages2);
tm.refresh();
	tm1 = new $jit.TM.Squarified({
  //where to inject the visualization
  injectInto: 'packages3',
  //parent box title heights
  titleHeight: 15,
  //enable animations
  animate: animate,
  //box offsets
  offset: 1,
  //Attach left and right click events
  Events: {
    enable: true,
    onClick: function(node) {
      if(node) tm.enter(node);
    },
    onRightClick: function() {
      tm.out();
    }
  },
  duration: 1000,
  //Enable tips
  Tips: {
    enable: true,
    //add positioning offsets
    offsetX: 20,
    offsetY: 20,
    //implement the onShow method to
    //add content to the tooltip when a node
    //is hovered
    onShow: function(tip, node, isLeaf, domElement) {
      var html = "<div class=\"tip-title\">" + node.name
        + "</div><div class=\"tip-text\">";
      var data = node.data;
      if(data.playcount) {
        html += "play count: " + data.playcount;
      }
      if(data.image) {
        html += "<img src=\""+ data.image +"\" class=\"album\" />";
      }
      tip.innerHTML =  html;
    }
  },
  //Add the name of the node in the correponding label
  //This method is called once, on label creation.
  onCreateLabel: function(domElement, node){
      domElement.innerHTML = node.name;
      var style = domElement.style;
      style.display = '';
      style.border = '1px solid transparent';
      domElement.onmouseover = function() {
        style.border = '1px solid #9FD4FF';
      };
      domElement.onmouseout = function() {
        style.border = '1px solid transparent';
      };
  }
});
tm1.loadJSON(packages2);
tm1.refresh();
}

function initSunburst() {
		 sb = new $jit.Sunburst({
	     //id container for the visualization
	     injectInto: 'packages1',
	     //Distance between levels
	     levelDistance: 90,
	     //Change node and edge styles such as
	     //color, width and dimensions.
	     Node: {
	       overridable: true,
	       type: useGradients? 'gradient-multipie' : 'multipie'
	     },
	     //Select canvas labels
	     //'HTML', 'SVG' and 'Native' are possible options
	     Label: {
	       type: labelType
	     },
	     //Change styles when hovering and clicking nodes
	     NodeStyles: {
	       enable: true,
	       type: 'Native',
	       stylesClick: {
	         'color': '#33dddd'
	       },
	       stylesHover: {
	         'color': '#dd3333'
	       }
	     },
	     //Add tooltips
	     Tips: {
	       enable: true,
	       onShow: function(tip, node) {
	         var html = "<div class=\"tip-title\">" + node.name + "</div>";
	         var data = node.data;
	         if("days" in data) {
	           html += "<b>Last modified:</b> " + data.days + " days ago";
	         }
	         if("size" in data) {
	           html += "<br /><b>File size:</b> " + Math.round(data.size / 1024) + "KB";
	         }
	         tip.innerHTML = html;
	       }
	     },
	     //implement event handlers
	     Events: {
	       enable: true,
	       onClick: function(node) {
	         if(!node) return;
	         //Build detailed information about the file/folder
	         //and place it in the right column.
	         var html = "<h4>" + node.name + "</h4>", data = node.data;
	         if("days" in data) {
	           html += "<b>Last modified:</b> " + data.days + " days ago";
	         }
	         if("size" in data) {
	           html += "<br /><br /><b>File size:</b> " + Math.round(data.size / 1024) + "KB";
	         }
	         if("description" in data) {
	           html += "<br /><br /><b>Last commit was:</b><br /><pre>" + data.description + "</pre>";
	         }
//	         $jit.id('inner-details').innerHTML = html;
	         //hide tip
	         sb.tips.hide();
	         //rotate
	         sb.rotate(node, animate? 'animate' : 'replot', {
	           duration: 1000,
	           transition: $jit.Trans.Quart.easeInOut
	         });
	       }
	     },
	     // Only used when Label type is 'HTML' or 'SVG'
	     // Add text to the labels.
	     // This method is only triggered on label creation
	     onCreateLabel: function(domElement, node){
	       var labels = sb.config.Label.type,
	           aw = node.getData('angularWidth');
	       if (labels === 'HTML' && (node._depth < 2 || aw > 2000)) {
	         domElement.innerHTML = node.name;
	       } else if (labels === 'SVG' && (node._depth < 2 || aw > 2000)) {
	         domElement.firstChild.appendChild(document.createTextNode(node.name));
	       }
	     },
	     // Only used when Label type is 'HTML' or 'SVG'
	     // Change node styles when labels are placed
	     // or moved.
	     onPlaceLabel: function(domElement, node){
	       var labels = sb.config.Label.type;
	       if (labels === 'SVG') {
	         var fch = domElement.firstChild;
	         var style = fch.style;
	         style.display = '';
	         style.cursor = 'pointer';
	         style.fontSize = "0.8em";
	         fch.setAttribute('fill', "#fff");
	       } else if (labels === 'HTML') {
	         var style = domElement.style;
	         style.display = '';
	         style.cursor = 'pointer';
	         style.fontSize = "0.8em";
	         style.color = "#ddd";
	         var left = parseInt(style.left);
	         var w = domElement.offsetWidth;
	         style.left = (left - w / 2) + 'px';
	       }
	     }
	});
	 //load JSON data.
	 sb.loadJSON(packages2);
	 //compute positions and plot.
	 sb.refresh();
}

function initIcicle() {
  icicle = new $jit.Icicle(
      {
	 // id of the visualization container
	 injectInto: 'packages',
	 // whether to add transition animations
	 animate: animate,
	 // nodes offset
	 offset: 1,
	 // whether to add cushion type nodes
	 cushion: false,
	 //show only three levels at a time
	 constrained: true,
	 levelsToShow: 3,
	 // enable tips
	 Tips: {
	    enable: true,
	    type: 'Native',
	    // add positioning offsets
	    offsetX: 20,
	    offsetY: 20,
	    // implement the onShow method to
	    // add content to the tooltip when a node
	    // is hovered
	    onShow: function(tip, node){
	       // count children
	       var count = 0;
	       node.eachSubnode(function(){
				   count++;
				});
	       // add tooltip info
	       tip.innerHTML = "<div class=\"tip-title\"><b>Name:</b> " + node.name
		  + "</div><div class=\"tip-text\">" + count + " children</div>";
	    }
	 },
	 // Add events to nodes
	 Events: {
	    enable: true,
	    onMouseEnter: function(node) {
	       //add border and replot node
	       node.setData('border', '#33dddd');
	       icicle.fx.plotNode(node, icicle.canvas);
	       icicle.labels.plotLabel(icicle.canvas, node, icicle.controller);
	    },
	    onMouseLeave: function(node) {
	       node.removeData('border');
	       icicle.fx.plot();
	    },
	    onClick: function(node){
	       if (node) {
		  //hide tips and selections
		  icicle.tips.hide();
		  if(icicle.events.hoveredNode)
		     this.onMouseLeave(icicle.events.hoveredNode);
		  //perform the enter animation
		  icicle.enter(node);
	       }
	    },
	    onRightClick: function(){
	       //hide tips and selections
	       icicle.tips.hide();
	       if(icicle.events.hoveredNode)
	    	   this.onMouseLeave(icicle.events.hoveredNode);
	       //perform the out animation
	       icicle.out();
	    }
	 },
	 // Add canvas label styling
	 Label: {
	    type: labelType // "Native" or "HTML"
	 },
	 // Add the name of the node in the corresponding label
	 // This method is called once, on label creation and only for DOM and not
	 // Native labels.
	 onCreateLabel: function(domElement, node){
	    domElement.innerHTML = node.name;
	    var style = domElement.style;
	    style.fontSize = '0.9em';
	    style.display = '';
	    style.cursor = 'pointer';
	    style.color = '#333';
	    style.overflow = 'hidden';
	 },
	 // Change some label dom properties.
	 // This method is called each time a label is plotted.
	 onPlaceLabel: function(domElement, node){
	    var style = domElement.style,
	    width = node.getData('width'),
	    height = node.getData('height');
	    if(width < 7 || height < 7) {
	       style.display = 'none';
	    } else {
	       style.display = '';
	       style.width = width + 'px';
	       style.height = height + 'px';
	    }
	 }
      });
   // load data
   icicle.loadJSON(packages2);
   // compute positions and plot
   icicle.refresh();
}

function initGraphs() {
	initForceDirected();
//	initSquarified();
//	initSunburst();
//	initIcicle();
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
   })
}

var eventSource;
function loadTimeline() {
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


$(document).ready(
   function() {
	  // For some reason there is a history piece that doesn't work, I should look into this.  For now, disable.
	  SimileAjax.History.enabled = false;
      $("#deadlock-list li").click(
    		  function (event) {
    			  var val = $(this).attr("id");
    			  loadDeadlockGraph(deadlocks[val]);
    		  });
      initGraphs();
      var cyc = $("#deadlock-list li").first().attr("id");
      if(cyc != undefined) {
    	  loadDeadlockGraph(deadlocks[cyc]);
      }
      loadTimeline();
      $(".tab").hide();
      $("#main #bar a").click(function(event) {
          event.preventDefault();
          $("#main #bar a").removeClass("selected");
          $(this).addClass("selected");
    	  var div = $(this).attr("href");
    	  $(".tab").hide();
    	  $(div).fadeIn('slow');
      });
      initOutline();
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
