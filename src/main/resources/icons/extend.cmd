rem thanks to https://www.imagemagick.org/discourse-server/viewtopic.php?t=33366
rem on how to extend png files with a transparent border

"C:\Program Files\ImageMagick-7.0.3-Q16\"magick.exe mogrify -path ./extended -bordercolor transparent -border 4 -format png *.png