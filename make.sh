#!/bin/bash

#//tb/1501

cur=`pwd`

src="$cur"/src
build="$cur"/build
archive="$cur"/archive
classes="$build"/classes
doc="$cur"/doc

jsource=1.6
jtarget=1.6

package_path=ch/lowres/wavegraph

#needs tool check
#

#========================================================================
function create_build_info()
{
	now="`date`"
	uname="`uname -s -p`"
	jvm="`javac -version 2>&1 | head -1 | sed 's/"/''/g'`"
	javac_opts=" -source $jsource -target $jtarget"
	git_head_commit_id="`git rev-parse HEAD`"

	cat - << __EOF__
//generated at build time
package ch.lowres.wavegraph;
public class BuildInfo
{
	public static String get()
	{
		return "date: $now\nuname -s -p: $uname\njavac -version: $jvm\njavac Options: $javac_opts\ngit rev-parse HEAD: $git_head_commit_id";
	}
	public static String getGitCommit()
	{
		return "$git_head_commit_id";
	}
	public static void main(String[] args)
	{
		System.out.println(get());
	}
}
__EOF__
}

#========================================================================
function compile_wavegraph()
{
	echo "building wavegraph application"
	echo "=============================="

	mkdir -p "$classes"

	#apple extension are stubs used just at compile time on non-osx machines
	unzip -p "$archive"/AppleJavaExtensions.zip \
		AppleJavaExtensions/AppleJavaExtensions.jar > "$classes"/AppleJavaExtensions.jar

	javac -source $jsource -target $jtarget -classpath "$classes":"$classes"/AppleJavaExtensions.jar -sourcepath "$src" -d "$classes" "$src"/$package_path/*.java

	ret=$?
	if [ $ret -ne 0 ]
	then
		echo "error while compiling."
		exit 1
	fi

	echo "start with:"
	echo "java -Xms1024m -Xmx1024m -cp .:build/classes/ ch.lowres.wavegraph.Main"
}

#========================================================================
function handle_ubuntu_font
{
	mkdir -p "$classes"/resources/fonts
	mkdir -p "$classes"/resources/licenses/ubuntu-font-family

	cp "$archive"/ubuntu-font-family-0.80.zip "$build"
	cd "$build"
	unzip ubuntu-font-family-0.80.zip
	cd "$cur"

#	cp "$build"/ubuntu-font-family-0.80/Ubuntu-C.ttf "$classes"/resources/fonts/Ubuntu-C.ttf
	cp "$build"/ubuntu-font-family-0.80/UbuntuMono-R.ttf "$classes"/resources/fonts/

	cp "$build"/ubuntu-font-family-0.80/LICENCE-FAQ.txt "$classes"/resources/licenses/ubuntu-font-family
	cp "$build"/ubuntu-font-family-0.80/copyright.txt "$classes"/resources/licenses/ubuntu-font-family
	cp "$build"/ubuntu-font-family-0.80/README.txt "$classes"/resources/licenses/ubuntu-font-family
	cp "$build"/ubuntu-font-family-0.80/TRADEMARKS.txt "$classes"/resources/licenses/ubuntu-font-family
	cp "$build"/ubuntu-font-family-0.80/LICENCE.txt "$classes"/resources/licenses/ubuntu-font-family
}

#========================================================================
function build_jar
{
	echo "creating wavegraph application jar (wavegraph_xxx.jar)"
	echo "======================================================"

	cur="`pwd`"

#	mkdir -p "$classes"/resources/etc
	mkdir -p "$classes"/resources/images

	cp "$src"/gfx/wavegraph_icon.png "$classes"/resources/images
	cp "$src"/gfx/wavegraph_splash_screen.png "$classes"/resources/images
	cp "$src"/gfx/wavegraph_about_screen.png "$classes"/resources/images

	handle_ubuntu_font

	echo "Manifest-Version: 1.0" > "$build"/Manifest.txt
	echo "SplashScreen-Image: resources/images/wavegraph_splash_screen.png" >> "$build"/Manifest.txt
	echo "Main-Class: ch.lowres.wavegraph.Main" >> "$build"/Manifest.txt
	echo "" >> "$build"/Manifest.txt

	cd "$classes"

	now=`date +"%s"`

	echo "creating jar..."

	jar cfvm wavegraph_"$now".jar "$build"/Manifest.txt ch/ resources/
	ls -l wavegraph_"$now".jar
	echo "move wavegraph_$now.jar to build dir..."
	mv wavegraph_"$now".jar "$build"

	echo "start with"
	echo "java -Xms1024m -Xmx1024m -jar build/wavegraph_$now.jar"

#osx:
#-Xdock:name="Wavegraph"

	#start now
	cd "$cur"
	java -Xms1024m -Xmx1024m -jar build/wavegraph_$now.jar

	echo "build_jar done."
}

#========================================================================
function build_javadoc
{
	mkdir -p "$doc"
	javadoc -private -linksource -sourcetab 2 -d "$doc" \
	-classpath "$classes" \
	-sourcepath "$src" \
		ch.lowres.wavegraph
}

#execute:

mkdir -p "$build"
rm -rf "$build"/*

create_build_info > "$src"/$package_path/BuildInfo.java
cat "$src"/$package_path/BuildInfo.java

compile_wavegraph
#build_javadoc
build_jar
