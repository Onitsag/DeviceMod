package fr.onitsag.faritech.object.tiles;

import fr.onitsag.faritech.api.utils.RenderUtil;
import fr.onitsag.faritech.object.Game;
import fr.onitsag.faritech.object.Game.Layer;

public class TileWheat extends Tile
{
	public TileWheat(int id, int x, int y)
	{
		super(id, x, y);
	}

	@Override
	public void render(Game game, int x, int y, Layer layer)
	{
		RenderUtil.drawRectWithTexture(game.xPosition + x * Tile.WIDTH, game.yPosition + y * Tile.HEIGHT - 6, this.x * 16, this.y * 16, WIDTH, HEIGHT + 1, 16, 16);
		RenderUtil.drawRectWithTexture(game.xPosition + x * Tile.WIDTH, game.yPosition + y * Tile.HEIGHT - 2, this.x * 16, this.y * 16, WIDTH, HEIGHT + 1, 16, 16);
	}
	
	@Override
	public boolean isFullTile()
	{
		return false;
	}
}
