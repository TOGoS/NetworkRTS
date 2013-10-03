(function() {
	var components = document.getElementsByClassName('component');
	var dragging = null;
	var dragDistance = 0;
	var selected = [];
	
	var dragX, dragY;
	for( var c=0; c<components.length; ++c ) (function() {
		var elem = components[c];
		elem.addEventListener('mousedown', function(evt) {
			dragging = elem;
			dragX = evt.clientX;
			dragY = evt.clientY;
			dragDistance = 0;
		});
		elem.addEventListener('click', function(evt) {
			if( dragDistance == 0 ) {
				var si = selected.indexOf(elem);
				if( si > -1 ) {
					selected.splice(si, 1);
					elem.className.baseVal = elem.className.baseVal.replace(/(?:^|\s)selected(?!\S)/g,'');
				} else {
					selected.push(elem);
					elem.className.baseVal += ' selected';
				}
			}
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
		dragDistance += 1;
	});
	document.addEventListener('mouseup', function(evt) {
		dragging = null;
	});
	document.addEventListener('keydown', function(evt) {
		alert(evt.keyCode);
	});
})();
