# 代码组成
有三个项目<br>
gdal2tile-mapslicer（可以忽略）<br>
gdal2tile-python(参考，如要运行，需要配置python，和gdal，gdal-data等)<br>
gdal2tile-core（真正的java重写版本）

# gdal2tiles
将gdal中python脚本gdal2tiles.py 用java重写

原来gdal2tiles.py支持gdal本身支持的栅格格式，我这边只支持GeoPDF（ArcGIS导出的），如果要支持其他格式，修改读取栅格逻辑部分。

这个版本的支持参考系统为wgs84和mercator，也可以忽略参考系统，单纯切片。通过-profile 参数来设置。

生wgs84切片：<br>
-profile geodetic E:\date\pdf\84.pdf E:\date\tiles

生成mercator切片：<br>
-profile mercator E:\date\pdf\mercator.pdf E:\date\tiles

生成不带参考系统的切片：<br>
-profile raster E:\date\pdf\mercator.pdf E:\date\tiles <br>
-profile raster E:\date\pdf\84.pdf E:\date\tiles
