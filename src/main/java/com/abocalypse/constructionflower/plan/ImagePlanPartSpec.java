package com.abocalypse.constructionflower.plan;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import com.abocalypse.constructionflower.lib.ConfigHandler;

public class ImagePlanPartSpec extends PlanPartSpec {
	
	private File imageFile;
	private BufferedImage partImage;
	private int spawnColor;
	private int spreadColor;
	
	public ImagePlanPartSpec(Map<String,Object> properties) {
		super(properties);
		imageFile = new File(ConfigHandler.imageDir, (String)(properties.get("image file")));
		this.spawnColor = (Integer)(properties.getOrDefault("spawn color", 0xF7921E));
		this.spreadColor = (Integer)(properties.getOrDefault("spread color", 0xFFFFFF));
	}

	private boolean spawnBlock(int deltaX, int deltaZ) {
		try {
			int value = partImage.getRGB(deltaX, deltaZ);
			// the two high bytes are the alpha value and the rest is R, G, B
			int alpha = (value >> 24) & 0xFF;
			int color = value & 0xFFFFFF;
			// For now rejecting anything at all transparent (certainly don't want to
			// be triggered by pixels that have no visible color in the image). Maybe
			// this should be configurable.
			return alpha == 0xFF && color == spawnColor;
		} catch (ArrayIndexOutOfBoundsException e) {
			return false;
		}
	}
	
	private boolean spreadBlock(int deltaX, int deltaZ) {
		try {
			int value = partImage.getRGB(deltaX, deltaZ);
			// the two high bytes are the alpha value and the rest is R, G, B
			int alpha = (value >> 24) & 0xFF;
			int color = value & 0xFFFFFF;
			// For now rejecting anything at all transparent (certainly don't want to
			// be triggered by pixels that have no visible color in the image). Maybe
			// this should be configurable.
			return alpha == 0xFF && color == spreadColor;
		} catch (ArrayIndexOutOfBoundsException e) {
			return false;
		}
	}
	
	@Override
	protected void doLoad(int worldX, int worldZ, ChunkPlanBlocks chunkPlanBlocks) throws IOException {
		
		partImage = ImageIO.read(imageFile);
		// if ( partImage.getType() != BufferedImage.TYPE_BYTE_INDEXED ) {
		//	BufferedImage eightBitImage = new BufferedImage(partImage.getWidth(), partImage.getHeight(), BufferedImage.TYPE_BYTE_INDEXED);
		//	eightBitImage.createGraphics().drawImage(partImage, 0, 0, null);
		//	partImage = eightBitImage;
		// }
		for ( AnchoredChunk chunk : new AnchoredChunks(0, partImage.getWidth(), 0, partImage.getHeight()) ) {
				for ( AnchoredBlock block : chunk ) {
					if ( spawnBlock(block.deltaX, block.deltaZ) ) {
						chunkPlanBlocks.addChunkBlock(chunk.coord, block.x, block.z, EnumConstructionFlowerLevel.SPAWN);
					} else if ( spreadBlock( block.deltaX, block.deltaZ) ) {
						chunkPlanBlocks.addChunkBlock(chunk.coord, block.x, block.z, EnumConstructionFlowerLevel.SPREAD);
					}
				}
		}
		
	}

}
