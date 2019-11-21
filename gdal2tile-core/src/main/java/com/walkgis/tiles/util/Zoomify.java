package com.walkgis.tiles.util;

import java.util.Collections;
import java.util.LinkedList;

/**
 * Created by JerFer
 * Date: 2017/12/13.
 */
public class Zoomify {
    private int tilesize;
    private String tileFormat;
    private LinkedList<double[]> tierSizeInTiles;
    private LinkedList tierImageSize;
    private int numberOfTiers;
    private double[] tileCountUpToTier;

    public Zoomify(int width, int height, int tilesize, String tileFormat) {
        this.tilesize = tilesize;
        this.tileFormat = tileFormat;
        int[] imagesize = new int[]{width, height};
        double[] tiles = new double[]{Math.ceil(width / tilesize), Math.ceil(height / tilesize)};

        tierSizeInTiles = new LinkedList<>();
        tierSizeInTiles.add(tiles);

        tierImageSize = new LinkedList<>();
        tierImageSize.add(imagesize);

        while (imagesize[0] > tilesize || imagesize[1] > tilesize) {
            imagesize = new int[]{(int) Math.floor(imagesize[0] / 2), (int) Math.floor(imagesize[1] / 2)};
            tiles = new double[]{Math.ceil(imagesize[0] / tilesize), Math.ceil(imagesize[1] / tilesize)};

            this.tierSizeInTiles.add(tiles);
            this.tierImageSize.add(imagesize);
        }

//        tierSizeInTiles.reverse();
        Collections.reverse(this.tierSizeInTiles);
//        tierImageSize.reverse();
        Collections.reverse(this.tierImageSize);

//        numberOfTiers = len(tierSizeInTiles);
        this.numberOfTiers = this.tierSizeInTiles.size();

        this.tileCountUpToTier = new double[this.numberOfTiers];
        this.tileCountUpToTier[0] = 0;

        for (int i = 1; i < this.numberOfTiers + 1; i++) {
            this.tileCountUpToTier[i] =
                    (this.tierSizeInTiles.get(i - 1)[0] * this.tierSizeInTiles.get(i - 1)[1] +
                            this.tileCountUpToTier[i - 1]);
        }
    }

    private String tileFilename(int x, int y, int z) {
        double tileIndex = x + y * this.tierSizeInTiles.get(z)[0] + this.tileCountUpToTier[z];
        String xxx = String.format("TileGroup%.0f", Math.floor(tileIndex / 256));
        String xxxx = String.format("%s-%s-%s.%s", z, x, y, this.tileFormat);
        return xxx + xxxx;
    }
}
