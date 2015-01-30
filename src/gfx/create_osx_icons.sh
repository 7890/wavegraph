#!/bin/sh

output_folder=osx.iconset

mkdir -p "$output_folder"

convert -density 1200 -resize 16x16 wavegraph_icon.svg "$output_folder"/icon_16x16.png
convert -density 1200 -resize 32x32 wavegraph_icon.svg "$output_folder"/icon_16x16@2x.png

convert -density 1200 -resize 32x32 wavegraph_icon.svg "$output_folder"/icon_32x32.png
convert -density 1200 -resize 64x64 wavegraph_icon.svg "$output_folder"/icon_32x32@2x.png

convert -density 1200 -resize 128x128 wavegraph_icon.svg "$output_folder"/icon_128x128.png
convert -density 1200 -resize 256x256 wavegraph_icon.svg "$output_folder"/icon_128x128@2x.png

convert -density 1200 -resize 256x256 wavegraph_icon.svg "$output_folder"/icon_256x256.png
convert -density 1200 -resize 512x512 wavegraph_icon.svg "$output_folder"/icon_256x256@2x.png

convert -density 1200 -resize 512x512 wavegraph_icon.svg "$output_folder"/icon_512x512.png
convert -density 1200 -resize 1024x1024 wavegraph_icon.svg "$output_folder"/icon_512x512@2x.png

echo "on osx: iconutil -c icns $output_folder"
