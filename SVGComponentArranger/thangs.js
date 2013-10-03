(function() {
	var components = document.getElementsByClassName('component');
	var dragging = null;
	var dragX, dragY;
	for( var c=0; c<components.length; ++c ) (function() {
		var comp = components[c];
		comp.addEventListener('mousedown', function(evt) {
			dragging = comp;
			dragX = evt.clientX;
			dragY = evt.clientY;
		});
	})();
	document.addEventListener('mousemove', function(evt) {
		if( dragging != null ) {
			var mtx = dragging.transform.baseVal.getItem(0).matrix;
			mtx.e += evt.clientX-dragX;
			mtx.f += evt.clientY-dragY;
			dragX = evt.clientX;
			dragY = evt.clientY;
		}
	});
	document.addEventListener('mouseup', function(evt) {
		dragging = null;
	});
})();
