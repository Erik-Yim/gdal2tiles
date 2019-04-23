package com.walkgis.tiles.util;

import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.core.contents.Contents;
import mil.nga.geopackage.core.contents.ContentsDao;
import mil.nga.geopackage.core.contents.ContentsDataType;
import mil.nga.geopackage.core.srs.SpatialReferenceSystem;
import mil.nga.geopackage.core.srs.SpatialReferenceSystemDao;
import mil.nga.geopackage.extension.ExtensionsDao;
import mil.nga.geopackage.extension.index.FeatureTableIndex;
import mil.nga.geopackage.features.columns.GeometryColumnsDao;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureResultSet;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.io.GeoPackageIOUtils;
import mil.nga.geopackage.manager.GeoPackageManager;
import mil.nga.geopackage.metadata.MetadataDao;
import mil.nga.geopackage.metadata.reference.MetadataReferenceDao;
import mil.nga.geopackage.schema.columns.DataColumnsDao;
import mil.nga.geopackage.schema.constraints.DataColumnConstraintsDao;
import mil.nga.geopackage.tiles.TileBoundingBoxUtils;
import mil.nga.geopackage.tiles.TileGrid;
import mil.nga.geopackage.tiles.matrix.TileMatrix;
import mil.nga.geopackage.tiles.matrix.TileMatrixDao;
import mil.nga.geopackage.tiles.matrixset.TileMatrixSet;
import mil.nga.geopackage.tiles.matrixset.TileMatrixSetDao;
import mil.nga.geopackage.tiles.user.*;
import mil.nga.sf.Geometry;
import mil.nga.sf.proj.ProjectionConstants;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.sql.SQLException;
import java.util.List;

public class GeopackageUtil {
    private Integer tileWidth = null;
    private Integer tileHeight = null;
    private GeoPackage geoPackage;
    private TileMatrixSet tileMatrixSet;
    private TileGrid tileGrid;
    private String extension;
    private Contents contents;
    private TileMatrixDao tileMatrixDao;


    public void initGeopackage(String file) throws FileAlreadyExistsException {
        File newGeoPackage = new File(file);
        if (newGeoPackage.exists())
            throw new FileAlreadyExistsException("文件存在");
        boolean created = GeoPackageManager.create(newGeoPackage);
        if (created) {
            geoPackage = GeoPackageManager.open(newGeoPackage);
            // GeoPackage Table DAOs
//        ContentsDao contentsDao = geoPackage.getContentsDao();
//        GeometryColumnsDao geomColumnsDao = geoPackage.getGeometryColumnsDao();
//        TileMatrixSetDao tileMatrixSetDao = geoPackage.getTileMatrixSetDao();
//        TileMatrixDao tileMatrixDao = geoPackage.getTileMatrixDao();
//        DataColumnsDao dataColumnsDao = geoPackage.getDataColumnsDao();
//        DataColumnConstraintsDao dataColumnConstraintsDao = geoPackage.getDataColumnConstraintsDao();
//        MetadataDao metadataDao = geoPackage.getMetadataDao();
//        MetadataReferenceDao metadataReferenceDao = geoPackage.getMetadataReferenceDao();
//        ExtensionsDao extensionsDao = geoPackage.getExtensionsDao();

//            // Feature and tile tables
//            List<String> features = geoPackage.getFeatureTables();
//            List<String> tiles = geoPackage.getTileTables();
//
//            // Query Features
//            FeatureDao featureDao = geoPackage.getFeatureDao(features.get(0));
//            queryFeature(featureDao);
//
//            // Query Tiles
//            TileDao tileDao = geoPackage.getTileDao(tiles.get(0));
//            queryTile(tileDao);
//
//            // Index Features
//            FeatureTableIndex indexer = new FeatureTableIndex(geoPackage, featureDao);
//            int indexedCount = indexer.index();

            // Close database when done
        }
    }

    public void close() {
        if (geoPackage != null) {
            geoPackage.close();
        }
    }


    public void createTileTable(String tbName, BoundingBox boundingBox, int minZoom, int maxZoom, String ext) throws IOException, SQLException {
        this.extension = ext;
        geoPackage.createTileMatrixSetTable();
        geoPackage.createTileMatrixTable();
//        BoundingBox bitsBoundingBox = new BoundingBox(-11667347.997449303, 4824705.2253603265, -11666125.00499674, 4825928.217812888);
//        createTiles("bit_systems", bitsBoundingBox, 15, 17, "png");

//        BoundingBox ngaBoundingBox = new BoundingBox(-8593967.964158937, 4685284.085768163, -8592744.971706374, 4687730.070673289);
//        createTiles("nga", ngaBoundingBox, 15, 16, "png");

        createTiles(tbName, boundingBox, minZoom, maxZoom, ext);
    }

    private void createTiles(String name, BoundingBox boundingBox, int minZoomLevel, int maxZoomLevel, String extension) throws SQLException, IOException {
        SpatialReferenceSystemDao srsDao = geoPackage.getSpatialReferenceSystemDao();
        SpatialReferenceSystem srs = srsDao.getOrCreateCode(ProjectionConstants.AUTHORITY_EPSG, (long) ProjectionConstants.EPSG_WEB_MERCATOR);

        TileGrid totalTileGrid = TileBoundingBoxUtils.getTileGrid(boundingBox, minZoomLevel);
        BoundingBox totalBoundingBox = TileBoundingBoxUtils.getWebMercatorBoundingBox(totalTileGrid, minZoomLevel);

        ContentsDao contentsDao = geoPackage.getContentsDao();

        contents = new Contents();
        contents.setTableName(name);
        contents.setDataType(ContentsDataType.TILES);
        contents.setIdentifier(name);
        contents.setDescription(name);
        contents.setMinX(totalBoundingBox.getMinLongitude());
        contents.setMinY(totalBoundingBox.getMinLatitude());
        contents.setMaxX(totalBoundingBox.getMaxLongitude());
        contents.setMaxY(totalBoundingBox.getMaxLatitude());
        contents.setSrs(srs);

        TileTable tileTable = buildTileTable(contents.getTableName());
        geoPackage.createTileTable(tileTable);

        contentsDao.create(contents);

        TileMatrixSetDao tileMatrixSetDao = geoPackage.getTileMatrixSetDao();

        tileMatrixSet = new TileMatrixSet();
        tileMatrixSet.setContents(contents);
        tileMatrixSet.setSrs(contents.getSrs());
        tileMatrixSet.setMinX(contents.getMinX());
        tileMatrixSet.setMinY(contents.getMinY());
        tileMatrixSet.setMaxX(contents.getMaxX());
        tileMatrixSet.setMaxY(contents.getMaxY());
        tileMatrixSetDao.create(tileMatrixSet);

        tileMatrixDao = geoPackage.getTileMatrixDao();

        tileGrid = totalTileGrid;
    }

    public void createMatrix(int zoom) throws SQLException {
        if (tileWidth == null) {
            tileWidth = 256;
        }
        if (tileHeight == null) {
            tileHeight = 256;
        }

        long matrixWidth = tileGrid.getMaxX() - tileGrid.getMinX() + 1;
        long matrixHeight = tileGrid.getMaxY() - tileGrid.getMinY() + 1;
        double pixelXSize = (tileMatrixSet.getMaxX() - tileMatrixSet.getMinX()) / (matrixWidth * tileWidth);
        double pixelYSize = (tileMatrixSet.getMaxY() - tileMatrixSet.getMinY()) / (matrixHeight * tileHeight);

        TileMatrix tileMatrix = new TileMatrix();
        tileMatrix.setContents(contents);
        tileMatrix.setZoomLevel(zoom);
        tileMatrix.setMatrixWidth(matrixWidth);
        tileMatrix.setMatrixHeight(matrixHeight);
        tileMatrix.setTileWidth(tileWidth);
        tileMatrix.setTileHeight(tileHeight);
        tileMatrix.setPixelXSize(pixelXSize);
        tileMatrix.setPixelYSize(pixelYSize);
        tileMatrixDao.create(tileMatrix);

        tileGrid = TileBoundingBoxUtils.tileGridZoomIncrease(tileGrid, 1);
    }

    public void insertTile(File tileFile, int zoom, int x, int y) throws IOException {
        TileDao dao = geoPackage.getTileDao(tileMatrixSet);
        if (tileFile != null && tileFile.exists()) {
            byte[] tileBytes = GeoPackageIOUtils.fileBytes(tileFile);

            TileRow newRow = dao.newRow();

            newRow.setZoomLevel(zoom);
            newRow.setTileColumn(x - tileGrid.getMinX());
            newRow.setTileRow(y - tileGrid.getMinY());
            newRow.setTileData(tileBytes);

            dao.create(newRow);
        }
    }

    private TileTable buildTileTable(String tableName) {
        List<TileColumn> columns = TileTable.createRequiredColumns();
        TileTable table = new TileTable(tableName, columns);
        return table;
    }


    private void queryTile(TileDao tileDao) {
        TileResultSet tileResultSet = tileDao.queryForAll();
        try {
            while (tileResultSet.moveToNext()) {
                TileRow tileRow = tileResultSet.getRow();
                byte[] tileBytes = tileRow.getTileData();
                // ...
            }
        } finally {
            tileResultSet.close();
        }
    }

    private void queryFeature(FeatureDao featureDao) {
        FeatureResultSet featureResultSet = featureDao.queryForAll();
        try {
            while (featureResultSet.moveToNext()) {
                FeatureRow featureRow = featureResultSet.getRow();
                GeoPackageGeometryData geometryData = featureRow.getGeometry();
                Geometry geometry = geometryData.getGeometry();
                // ...
            }
        } finally {
            featureResultSet.close();
        }
    }
}
