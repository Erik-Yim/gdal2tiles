package com.walkgis.tiles.util;

import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.gdal.osr.SpatialReference;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.spatial4j.context.SpatialContextFactory;
import org.locationtech.spatial4j.io.WKTReader;
import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;

public class PDFReader {
    private CoordinateReferenceSystem referenceSystem;
    private Envelope envelope;

    private int width;
    private int height;
    private BufferedImage bufferedImage;

    private void printEnvelop(COSBase cosBase) {
        if (cosBase instanceof COSArray) {
            COSArray cosArray = (COSArray) cosBase;
            Coordinate[] coordinates = new Coordinate[5];
            Coordinate coordinate = null;
            int flag = 0;
            for (int i = 0; i < cosArray.size(); i++) {
                cosBase = cosArray.get(i);
                if (cosBase instanceof COSFloat) {
                    if (i % 2 == 0) {
                        coordinate = new Coordinate();
                        coordinate.y = ((COSFloat) cosBase).floatValue();
                    } else {
                        coordinate.x = ((COSFloat) cosBase).floatValue();
                        coordinates[flag] = coordinate;
                        flag++;
                    }
                }
            }
            coordinates[4] = coordinates[0];
            Polygon polygon = new GeometryFactory().createPolygon(coordinates);
            envelope = polygon.getEnvelopeInternal();
        }
    }

    private void readReferenceSystem(COSBase cosBase) {
        if (cosBase instanceof COSDictionary) {
            COSDictionary dictionary = (COSDictionary) cosBase;
            cosBase = dictionary.getDictionaryObject("WKT");
            if (cosBase instanceof COSString) {
                Integer wkid = new SpatialReference().ImportFromWkt(((COSString) (cosBase)).getString());
                referenceSystem = new CRSFactory().createFromName("EPSG:" + wkid);
            }
        }
    }

    public void init(String file, String imgSavePath) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            PDDocument document = PDDocument.load(fileInputStream);
            int count = 0;

            PDResources resources = document.getPage(0).getResources();

//            Iterable xobjects = resources.getXObjectNames();
//            if (xobjects != null) {
//                Iterator imageIter = xobjects.iterator();
//                while (imageIter.hasNext()) {
//                    COSName key = (COSName) imageIter.next();
//                    if (resources.isImageXObject(key)) {
//                        try {
//                            PDImageXObject image = (PDImageXObject) resources.getXObject(key);
//                            BufferedImage bimage = image.getImage();
//                            ImageIO.write(bimage, "png", new File(imgSavePath + File.separator + count + ".png"));
//                            count++;
//                            System.out.println(count);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                }
//            }

            COSDictionary dictionary = document.getPage(0).getCOSObject();

            COSDictionary dictionaryRes = (COSDictionary) dictionary.getDictionaryObject("Resources");
            COSDictionary dictionaryXObject = (COSDictionary) dictionaryRes.getDictionaryObject("XObject");
            Iterator iteratorRes = dictionaryXObject.getValues().iterator();
            count = 0;

            while (iteratorRes.hasNext()) {
                COSBase item = (COSBase) iteratorRes.next();
                if (item instanceof COSObject) {
                    COSObject object = (COSObject) item;
                    if (object.getObject() instanceof COSDictionary) {
                        width = ((COSInteger) ((COSDictionary) object.getObject()).getDictionaryObject("Width")).intValue() - 1;
                        height += ((COSInteger) ((COSDictionary) object.getObject()).getDictionaryObject("Height")).intValue() - 1;
                    }
                }
                count++;
            }

            COSArray cosArrayMediaBox = (COSArray) dictionary.getDictionaryObject("MediaBox");
            Iterator<COSBase> iteratorMediaBox = cosArrayMediaBox.iterator();
            while (iteratorMediaBox.hasNext()) {
                COSBase cosBase = iteratorMediaBox.next();
                if (cosBase instanceof COSInteger) {
                    COSInteger cosInteger = (COSInteger) cosBase;
                } else if (cosBase instanceof COSFloat) {
                    COSFloat cosFloat = (COSFloat) cosBase;
                }
            }


            COSArray cosArray = (COSArray) dictionary.getDictionaryObject("VP");
            Iterator<COSBase> iterator = cosArray.iterator();
            while (iterator.hasNext()) {
                COSBase cosBase = iterator.next();
                if (cosBase instanceof COSDictionary) {
                    dictionary = (COSDictionary) cosBase;
                    cosBase = dictionary.getDictionaryObject("Measure");
                    if (cosBase instanceof COSDictionary) {
                        dictionary = (COSDictionary) cosBase;
                        cosBase = dictionary.getDictionaryObject("GCS");
                        readReferenceSystem(cosBase);
                        cosBase = dictionary.getDictionaryObject("GPTS");
                        printEnvelop(cosBase);
                    }
                }
            }
            PDFRenderer renderer = new PDFRenderer(document);
            bufferedImage = renderer.renderImageWithDPI(0, 300);
//            ImageIO.write(bim, "png", new FileOutputStream(imgSavePath + File.separator + "aaa.png"));
            document.close();
            fileInputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (InvalidPasswordException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CoordinateReferenceSystem getReferenceSystem() {
        return referenceSystem;
    }

    public void setReferenceSystem(CoordinateReferenceSystem referenceSystem) {
        this.referenceSystem = referenceSystem;
    }

    public Envelope getEnvelope() {
        return envelope;
    }

    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public BufferedImage getImage() {
        return bufferedImage;
    }

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    public void setBufferedImage(BufferedImage bufferedImage) {
        this.bufferedImage = bufferedImage;
    }
}
