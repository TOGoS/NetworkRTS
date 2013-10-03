(function() {
	var debugTextNode = document.getElementById('debug-text').firstChild;
	var setDebugText = function(text) {
		debugTextNode.textContent = text;
	};
	
	var LoadType = function( name ) {
		this.name = name;
	};
	
	var loadTypeNames = ["power-5v", "ethernet"];
	var loadTypes = {};
	for( var i in loadTypeNames ) {
		var name = loadTypeNames[i];
		loadTypes[name] = new LoadType(name);
	}
	
	var Port = function( id, loadType, x, y, dx, dy ) {
		this.id = id;
		this.loadType = loadType;
		this.x = x;
		this.y = y;
		this.dx = dx;
		this.dy = dy;
		this.loadType = loadType;
	};
	
	var Component = function( elem, ports ) {
		this.ports = ports;
		this.elem = elem;
		this.x = 0;
		this.y = 0;
	};
	Component.prototype.show = function() {
		document.firstChild.appendChild( this.elem );
	};
	Component.prototype.setPosition = function( x, y ) {
		this.x = x;
		this.y = y;
		var mtrx = this.elem.transform.baseVal.getItem(0).matrix;
		mtrx.e = x;
		mtrx.f = y;
	};
	
	var eth   = loadTypes['ethernet'];
	var pwr5v = loadTypes['power-5v'];
	
	var makeComponentClass = function( templateElement, templatePorts ) {
		var instantiatePorts = function( portList ) {
			var instantiated = [];
			for( var i=0; i<portList.length; ++i ) {
				var PortInstance = function( comp ) {
					this.component = comp;
				};
				PortInstance.prototype = portList[i];
				instantiated[i] = new PortInstance();
			}
			return instantiated;
		};
		var f = function() {
			var elem = templateElement.cloneNode(true);
			// TODO: Prepend transform with translate(0,0)
			elem.className.baseVal += ' component';
			this.constructor( elem, instantiatePorts(templatePorts) );
			registerComponentUiEventHandlers( this );
		};
		f.prototype = new Component();
		return f;
	};
		
	var ZLES400 = makeComponentClass( document.getElementById('Z-LES-400'), [
		new Port('e0', eth, -25,-25,-1, 0),
		new Port('e1', eth, -25,+25,-1, 0),
		new Port('e2', eth, +25,-25,+1, 0),
		new Port('e3', eth, +25,+25,+1, 0),
		new Port('pwr',pwr5v, 0,+50, 0,+1)
	] );
	
	var dragging = null;
	var dragDistance = 0;
	var selected = [];
	var dragX, dragY;
	var registerComponentUiEventHandlers = function( component ) {
		var elem = component.elem;
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
	};
	
	var switch0 = new ZLES400(); 
	switch0.setPosition( 300, 200 );
	switch0.show();
	
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
		setDebugText("Key "+evt.keyCode);
	});
})();
