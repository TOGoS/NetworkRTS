(function() {
	var debugTextNode = document.getElementById('debug-text').firstChild;
	var setDebugText = function(text) {
		debugTextNode.textContent = text;
	};
	
	var PortShape = function( name ) {
		this.name = name;
	}
	PortShape.prototype.canConnectTo = function( otherGender ) { return false; }

	var makePortShapePair = function(name) {
		var male = new PortShape(name + " (male)");
		var female = new PortShape(name + " (female)");
		male.canConnectTo = function( other ) { return other == female; }
		female.canConnectTo = function( other ) { return other == male; }
		return {"male": male, "female": female};
	};
	
	var portShapes = {
		rj45: makePortShapePair("RJ45"),
		barrel: {
			dc2100um: makePortShapePair("2.1mm DC barrel")
		}
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

	var eth   = loadTypes['ethernet'];
	var pwr5v = loadTypes['power-5v'];
	
	var Port = function( id, loadType, shape, x, y, dx, dy ) {
		this.id = id;
		this.loadType = loadType;
		this.shape = shape;
		this.x = x;
		this.y = y;
		this.dx = dx;
		this.dy = dy;
		this.loadType = loadType;
		this.connectedTo = null;
	};
	Port.prototype.canConnectTo = function( other ) {
		return this.shape.canConnectTo( other.shape );
	};
	
	var Component = function( elem, ports ) {
		this.ports = ports;
		this.elem = elem;
		this.x = 0;
		this.y = 0;
		this.flexible = false;
	};
	Component.prototype.show = function() {
		document.firstChild.appendChild( this.elem );
	};
	Component.prototype.recombobulate = function() {
		var mtrx = this.elem.transform.baseVal.getItem(0).matrix;
		mtrx.e = this.x;
		mtrx.f = this.y;
	};
	Component.prototype.setPosition = function( x, y ) {
		this.x = x;
		this.y = y;
		this.recombobulate();
	};
	
	var Wire = function( path, loadType, portShape ) {
		this.constructor( path, [
			new Port('left' , loadType, portShape, -1, 0, -1, 0),
			new Port('right', loadType, portShape, +1, 0, +1, 0)
		]);
		this.flexible = true;
	};
	Wire.prototype = new Component();
	Wire.prototype.connect = function(end, otherPort, x, y) {
		var p = this.ports[end];
		if( p == null ) {
			throw new Error("No such end '"+end+"' on wire!");
		}
		p.dx = -otherPort.dx;
		p.dy = -otherPort.dy;
		p.x = x;
		p.y = y;
		this.recombobulate();
	};
	Wire.prototype.recombobulate = function() {
		var r = this.ports[0];
		var l = this.ports[1];
		this.path.d =
			"M"+r.x+","+r.y+" "+
			"C"+(r.x-r.dx*100)+","+(r.y-r.dy*100)+" "+
			(l.x-l.dx*100)+","+(l.y-l.dy*100)+" "+
			l.x+","+l.y;
	};
	
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
		new Port('e0', eth, portShapes.rj45.female, -25,-25,-1, 0),
		new Port('e1', eth, portShapes.rj45.female, -25,+25,-1, 0),
		new Port('e2', eth, portShapes.rj45.female, +25,-25,+1, 0),
		new Port('e3', eth, portShapes.rj45.female, +25,+25,+1, 0),
		new Port('pwr',pwr5v, portShapes.barrel.dc2100um.male, 0,+50, 0,+1)
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
	
	var switch0, wire0;
	
	switch0 = new ZLES400(); 
	switch0.setPosition( 300, 200 );
	switch0.show();
	
	switch0 = new ZLES400(); 
	switch0.setPosition( 200, 300 );
	switch0.show();

	//wire0 = new EthernetCable();	
	
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
