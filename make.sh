#!/bin/sh

#//tb/1501/06

#only works with /bin/bash
#DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
FULLPATH="`pwd`/$0"
DIR=`dirname "$FULLPATH"`

src="$DIR"/src
build="$DIR"/_build
archive="$DIR"/archive
classes="$build"/classes
doc="$DIR"/doc

jsource=1.6
jtarget=1.6

#linux / osx different mktemp call
TMPFILE=`mktemp 2>/dev/null || mktemp -t /tmp`

JAVAC="javac -source $jsource -target $jtarget -nowarn"

#relative to $src
package_path=ch/lowres/wavegraph

#used for ../**/*.java syntax
#only works with /bin/bash
#shopt -s globstar

#========================================================================
checkAvail()
{
	which "$1" >/dev/null 2>&1
	ret=$?
	if [ $ret -ne 0 ]
	then
		echo "tool \"$1\" not found. please install"
		exit 1
	fi
}

#========================================================================
create_build_info()
{
	now="`date`"
	uname="`uname -s -p`"
	jvm="`javac -version 2>&1 | head -1 | sed 's/"/''/g'`"
	cd "$DIR"
	git_master_ref=`git show-ref master | head -1`

	cat - << __EOF__
//generated at build time
package ch.lowres.wavegraph;
public class BuildInfo
{
	public static String get()
	{
		return "date: $now\nuname -s -p: $uname\njavac -version: $jvm\njavac Options: $JAVAC\ngit show-ref master: $git_master_ref";
	}
	public static String getGitCommit()
	{
		return "$git_master_ref";
	}
	public static void main(String[] args)
	{
		System.out.println(get());
	}
}
__EOF__
}

#========================================================================
compile_wavegraph()
{
	echo ""
	echo ""
	echo "building wavegraph application"
	echo "=============================="

	mkdir -p "$classes"

	#apple extension are stubs used just at compile time on non-osx machines
	unzip -p "$archive"/AppleJavaExtensions.zip \
		AppleJavaExtensions/AppleJavaExtensions.jar > "$classes"/AppleJavaExtensions.jar

	find "$src" -name *.java > "$TMPFILE"
	$JAVAC -classpath "$classes":"$classes"/AppleJavaExtensions.jar -sourcepath "$src" -d "$classes" @"$TMPFILE"

	ret=$?
	if [ $ret -ne 0 ]
	then
		echo "error while compiling."
		exit 1
	fi
}

#========================================================================
handle_ubuntu_font()
{
	mkdir -p "$classes"/resources/fonts
	mkdir -p "$classes"/resources/licenses/ubuntu-font-family

	cp "$archive"/ubuntu-font-family-0.80.zip "$build"
	cd "$build"
	unzip ubuntu-font-family-0.80.zip
	cd "$DIR"

#	cp "$build"/ubuntu-font-family-0.80/Ubuntu-C.ttf "$classes"/resources/fonts/Ubuntu-C.ttf
	cp "$build"/ubuntu-font-family-0.80/UbuntuMono-R.ttf "$classes"/resources/fonts/

	cp "$build"/ubuntu-font-family-0.80/LICENCE-FAQ.txt "$classes"/resources/licenses/ubuntu-font-family
	cp "$build"/ubuntu-font-family-0.80/copyright.txt "$classes"/resources/licenses/ubuntu-font-family
	cp "$build"/ubuntu-font-family-0.80/README.txt "$classes"/resources/licenses/ubuntu-font-family
	cp "$build"/ubuntu-font-family-0.80/TRADEMARKS.txt "$classes"/resources/licenses/ubuntu-font-family
	cp "$build"/ubuntu-font-family-0.80/LICENCE.txt "$classes"/resources/licenses/ubuntu-font-family
}

#========================================================================
build_jar()
{
	echo ""
	echo ""
	echo "creating wavegraph application jar (wavegraph_xxx.jar)"
	echo "======================================================"

#	mkdir -p "$classes"/resources/etc
	mkdir -p "$classes"/resources/images

	cp "$src"/gfx/wavegraph_icon.png "$classes"/resources/images
	cp "$src"/gfx/wavegraph_splash_screen.png "$classes"/resources/images
	cp "$src"/gfx/wavegraph_about_screen.png "$classes"/resources/images

	handle_ubuntu_font

	echo "Manifest-Version: 1.0" > "$build"/Manifest.txt
	echo "SplashScreen-Image: resources/images/wavegraph_splash_screen.png" >> "$build"/Manifest.txt
#	echo "Main-Class: ch.lowres.wavegraph.Main" >> "$build"/Manifest.txt
	echo "Main-Class: Wavegraph" >> "$build"/Manifest.txt
	echo "" >> "$build"/Manifest.txt

	cd "$classes"

	now=`date +"%s"`

	echo "creating jar..."

	jar cfvm wavegraph_"$now".jar \
		"$build"/Manifest.txt \
		ch/ \
		resources/ \
		Wavegraph.class
	ls -l wavegraph_"$now".jar
	echo "move wavegraph_$now.jar to build dir..."
	mv wavegraph_"$now".jar "$build"

	echo "build_jar done."

	echo "start with"
	echo "java -Xms1024m -Xmx1024m -jar "$build"/wavegraph_$now.jar"

#osx:
#-Xdock:name="Wavegraph"

	#start now
	cd "$DIR"
	java -Xms1024m -Xmx1024m -jar "$build"/wavegraph_$now.jar #testdata
}

#========================================================================
build_javadoc()
{
	package=`echo "$package_path" | sed 's/\//./g'`

	echo ""
	echo ""
	echo "creating javadoc for $package"
	echo "======================================================"

	mkdir -p "$doc"

	javadoc -private -linksource -sourcetab 2 -d "$doc" \
		-classpath "$classes" \
		-sourcepath "$src" \
		"$package"

	echo "build_javadoc done."
}

#========================================================================
#execute:

for tool in java javac jar javadoc cat mkdir ls cp sed date uname git unzip; \
	do checkAvail "$tool"; done

mkdir -p "$build"
rm -rf "$build"/*

create_build_info > "$src"/$package_path/BuildInfo.java
cat "$src"/$package_path/BuildInfo.java

compile_wavegraph
#build_javadoc
build_jar
