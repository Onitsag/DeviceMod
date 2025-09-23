package fr.onitsag.faritech.object.tools;

import fr.onitsag.faritech.object.Canvas;
import fr.onitsag.faritech.object.Tool;

public class ToolEraser extends Tool {

	@Override
	public void handleClick(Canvas canvas, int x, int y) 
	{
		canvas.setPixel(x, y, 0);
	}

	@Override
	public void handleRelease(Canvas canvas, int x, int y) {}

	@Override
	public void handleDrag(Canvas canvas, int x, int y) 
	{
		canvas.setPixel(x, y, 0);
	}

}
