package togos.networkrts.experimental.simpleclient;

import java.io.Serializable;

import togos.networkrts.tfunc.ColorFunction;

public interface BackgroundType extends Serializable
{
	public ColorFunction getColorFunction();
}
