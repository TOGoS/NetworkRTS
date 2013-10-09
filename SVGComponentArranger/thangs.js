(
function() {
	var cablesGroup = document.getElementById('cables');
	var componentsGroup = document.getElementById('components');
	var allComponents = [];
	
	var debugTextNode = document.getElementById('debug-text').firstChild;
	var setDebugText = function(text) {
		debugTextNode.textContent = text;
	};
	
	var PortShape = function( name ) {
		this.name = name;
	};
	PortShape.prototype.canConnectTo = function( otherGender ) { return false; };
	
	var makePortShapePair = function(name) {
		var male = new PortShape(name + " (male)");
		var female = new PortShape(name + " (female)");
		male.canConnectTo = function( other ) { return other == female; };
		female.canConnectTo = function( other ) { return other == male; };
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

	var portDistance = function( p0, p1 ) {
		var dx = (p0.x + p0.component.x) - (p1.x + p1.component.x);
		var dy = (p0.y + p0.component.y) - (p1.y + p1.component.y);
		return Math.sqrt(dx*dx + dy*dy);
	};
	
	var portsAtSamePosition = function( p0, p1 ) {
		return p0.x + p0.component.x == p1.x + p1.component.x &&
		   p0.y + p0.component.y != p1.y + p1.component.y;
	};
	
	var Port = function( id, loadType, shape, x, y, dx, dy ) {
		this.id = id;
		this.loadType = loadType;
		this.shape = shape;
		this.x = x;
		this.y = y;
		this.dx = dx;
		this.dy = dy;
		this.loadType = loadType;
		this.component = null;
		this.connectedTo = null;
	};
	Port.prototype.getAbsX = function() { return this.x + this.component.x; };
	Port.prototype.getAbsY = function() { return this.y + this.component.y; };
	Port.prototype.canConnectTo = function( other ) {
		return this.shape.canConnectTo( other.shape );
	};
	Port.prototype.disconnect = function() {
		if( this.connectedTo != null ) {
			this.connectedTo.connectedTo = null;
			this.connectedTo = null;
			if( this.component.isFlexible ) {
				// Pop out a bit
				this.setPosition( this.x - this.dx * 10, this.y - this.dy * 10 );
			}
		}
	};
	Port.prototype.connectTo = function( other ) {
		if( this.connectedTo == other ) return true;
		
		if( !portsAtSamePosition(this, other) ) {
			// Need to overlap to connect
			if( this.component.isFlexible ) {
				this.setPosition( other.getAbsX(), other.getAbsY(), -other.dx, -other.dy );
			} else if( other.component.isFlexible ) {
				other.setPosition( this.getAbsX(), this.getAbsY(), -this.dx, -this.dy );
			} else {
				// Can't connect!
				return false;
			}
		}
		
		this.disconnect();
		other.disconnect();
		
		this.connectedTo = other;
		other.connectedTo = this;
		
		return true;
	};
	Port.prototype.recombobulate = function() {
		if( this.component != null ) {
			this.component.recombobulate();
		}
	};
	Port.prototype.fixConnection = function() {
		if( this.connectedTo != null ) {
			if( this.connectedTo.component.isFlexible ) {
				this.connectedTo.x = this.getAbsX();
				this.connectedTo.y = this.getAbsY();
				this.connectedTo.recombobulate();
			} else {
				this.connectedTo.connectedTo = null;
				this.connectedTo = null;
			}
		}
	};
	Port.prototype.setPosition = function( x, y, dx, dy, snap ) {
		this.x = x;
		this.y = y;
		if( dx != null ) this.dx = dx;
		if( dy != null ) this.dy = dy;
		
		this.fixConnection();
		
		if( snap && this.connectedTo == null ) {
			var closestOther = null;
			var closestDist = null;
			for( var ci=0; ci<allComponents.length; ++ci ) {
				var otherComponent = allComponents[ci];
				for( var pi=0; pi<otherComponent.ports.length; ++pi ) {
					var other = otherComponent.ports[pi];
					if( other != this && other.connectedTo == null && this.canConnectTo(other) ) {
						var dist = portDistance(this, other);
						if( closestOther == null || dist < closestDist ) {
							closestDist = dist;
							closestOther = other;
						}
					}
				}
			}
			
			if( closestDist < 20 ) {
				this.connectTo(closestOther);
			}
		}
		
		this.recombobulate();
	};
	
	var instantiatePorts = function( comp, portList ) {
		var instantiated = [];
		var PortInstance;
		for( var i=0; i<portList.length; ++i ) {
			PortInstance = function( comp ) {
				this.component = comp;
			};
			PortInstance.prototype = portList[i];
			instantiated[i] = new PortInstance();
			instantiated[i].component = comp;
		}
		return instantiated;
	};
	
	var Component = function( elem, ports ) {
		this.ports = ports;
		if( elem != null ) {
			// TODO: Prepend transform with translate(0,0)
			this.elem = elem.cloneNode(true);
			this.elem.id = null;
			this.elem.className.baseVal += ' component';
			registerComponentUiEventHandlers( this );
		}
		if( ports != null ) {
			this.ports = instantiatePorts( this, ports );
		}
		this.x = 0;
		this.y = 0;
		this.isFlexible = false;
	};
	Component.prototype.show = function() {
		if( this.elem ) {
			this.recombobulate();
			(this.isFlexible ? cablesGroup : componentsGroup).appendChild( this.elem );
		} else {
			throw new Error("Can't show component; no element associated.");
		}
		// TODO: Make sure not adding to the list if already in it
		allComponents.push(this);
	};
	Component.prototype.recombobulate = function() {
		if( this.elem ) {
			var mtrx = this.elem.transform.baseVal.getItem(0).matrix;
			mtrx.e = this.x;
			mtrx.f = this.y;
		}
	};
	Component.prototype.setPosition = function( x, y ) {
		this.x = x;
		this.y = y;
		for( var pi=0; pi<this.ports.length; ++pi ) {
			this.ports[pi].fixConnection();
		}
		this.recombobulate();
	};
	
	var Cable = function( elem, loadType, port0Shape, port1Shape ) {
		if( port1Shape == null ) port1Shape = port0Shape;
		Component.call( this, elem, [
			new Port('end0' , loadType, port0Shape, -1, 0, -1, 0),
			new Port('end1', loadType, port1Shape, +1, 0, +1, 0)
		]);
		elem = this.elem;
		this.boundingBoxElem = null;
		this.pathElem = null;
		if( elem != null ) for( var cni=0; cni<elem.childNodes.length; ++cni ) {
			var cn = elem.childNodes[cni];
			if( cn.localName == 'rect' ) {
				this.boundingBoxElem = cn;
			} else if( cn.localName == 'path' ) {
				this.pathElem = cn;
			}
		}
		this.isFlexible = true;
	};
	Cable.prototype = new Component();
	Cable.prototype.connect = function(end, otherPort, x, y) {
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
	Cable.prototype.recombobulate = function() {
		var r = this.ports[0];
		var l = this.ports[1];
		
		var bgRect = this.boundingBoxElem;
		if( bgRect != null ) {
			var bgX = Math.min(r.x, l.x) - 5;
			var bgY = Math.min(r.y, l.y) - 5;
			bgRect.x.baseVal.value = bgX;
			bgRect.y.baseVal.value = bgY;
			bgRect.width.baseVal.value  = Math.max(r.x, l.x) - bgX + 5;
			bgRect.height.baseVal.value = Math.max(r.y, l.y) - bgY + 5;
		}
		
		this.pathElem.setAttribute("d",
			"M"+r.x+","+r.y+" "+
			"C"+(r.x-r.dx*100)+","+(r.y-r.dy*100)+" "+
			(l.x-l.dx*100)+","+(l.y-l.dy*100)+" "+
			l.x+","+l.y);
	};
	
	var makeComponentClass = function( templateElement, templatePorts ) {
		var f = function() {
			Component.call( this, templateElement, templatePorts );
		};
		f.prototype = new Component();
		return f;
	};
	
	var makeCableClass = function( templateElement, loadType, port0Shape, port1Shape ) {
		var f = function() {
			Cable.call( this, templateElement, loadType, port0Shape, port1Shape );
		};
		f.prototype = new Cable();
		return f;
	};
	
	var EthernetCable = makeCableClass( document.getElementById('cat5-cable'), eth, portShapes.rj45.male );
	var FiveVoltPowerCable = makeCableClass( document.getElementById('5v-power-cable'), pwr5v, portShapes.barrel.dc2100um.female );
	
	var ZLES400 = makeComponentClass( document.getElementById('Z-LES-400'), [
		new Port('e0', eth, portShapes.rj45.female, -25,-25,-1, 0),
		new Port('e1', eth, portShapes.rj45.female, -25,+25,-1, 0),
		new Port('e2', eth, portShapes.rj45.female, +25,-25,+1, 0),
		new Port('e3', eth, portShapes.rj45.female, +25,+25,+1, 0),
		new Port('pwr',pwr5v, portShapes.barrel.dc2100um.male, 0,+50, 0,+1)
	] );
	
	var dragged = null;
	var draggedElement = null;
	var dragDistance = 0;
	var selected = [];
	var dragX, dragY, draggedX, draggedY;
	
	var startDrag = function( evt, comp, elem ) {
		if( dragged != null ) return; // Already dragging something?
		
		if( comp.isFlexible ) {
			var closestPort = null;
			var closestDist = 9999;
			for( var i in comp.ports ) {
				var dx = comp.ports[i].x - evt.clientX;
				var dy = comp.ports[i].y - evt.clientY;
				var dist = Math.sqrt(dx*dx + dy*dy);
				if( closestPort == null || dist < closestDist ) {
					closestDist = dist;
					closestPort = comp.ports[i];
				}
			}
			dragged = closestPort;
		} else {
			dragged = comp;
		}
		draggedElement = elem;
		draggedElement.setAttributeNS(null, 'pointer-events', 'none');
		dragX = evt.clientX;
		dragY = evt.clientY;
		draggedX = dragged.x;
		draggedY = dragged.y;
		dragDistance = 0;
	};
	
	var registerComponentUiEventHandlers = function( comp ) {
		var elem = comp.elem;
		elem.addEventListener('mousedown', function(evt) {
			startDrag( evt, comp, elem );
		});
		elem.addEventListener('click', function(evt) {
			if( dragDistance == 0 ) {
				//startDrag( evt, comp, elem );
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
	
	var switch0, ethcab0;
	
	switch0 = new ZLES400();
	switch0.setPosition( 300, 200 );
	switch0.show();
	
	switch0 = new ZLES400();
	switch0.setPosition( 200, 300 );
	switch0.show();
	
	ethcab0 = new EthernetCable();
	ethcab0.ports[0].setPosition( 50, 50, -1, 0 );
	ethcab0.ports[1].setPosition( 75, 75, +1, 0 );
	ethcab0.show();
	
	ethcab0 = new EthernetCable();
	ethcab0.ports[0].setPosition( 50, 150, -1, 0 );
	ethcab0.ports[1].setPosition( 75, 175, +1, 0 );
	ethcab0.show();
	
	ethcab0 = new EthernetCable();
	ethcab0.ports[0].setPosition( 50, 250, -1, 0 );
	ethcab0.ports[1].setPosition( 75, 275, +1, 0 );
	ethcab0.show();
	
	ethcab0 = new FiveVoltPowerCable();
	ethcab0.ports[0].setPosition( 50, 350, -1, 0 );
	ethcab0.ports[1].setPosition( 75, 375, +1, 0 );
	ethcab0.show();
	
	document.addEventListener('mousemove', function(evt) {
		if( dragged != null ) {
			var newX = draggedX + evt.clientX-dragX;
			var newY = draggedY + evt.clientY-dragY;
			dragged.setPosition( newX, newY, null, null, true );
		}
		dragDistance += 1;
	});
	document.addEventListener('mouseup', function(evt) {
		//if( dragDistance == 0 ) return;
		if( draggedElement != null ) {
			draggedElement.setAttributeNS(null, 'pointer-events', 'all');
		}
		dragged = null;
		draggedElement = null;
	});
	document.addEventListener('keydown', function(evt) {
		setDebugText("Key "+evt.keyCode);
	});
})();
