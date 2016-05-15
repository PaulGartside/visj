##############################################################################
# VI-Simplified (vis) Java Implementation                                    #
# Copyright (c) 07 Sep 2015 Paul J. Gartside                                 #
##############################################################################
# Permission is hereby granted, free of charge, to any person obtaining a    #
# copy of this software and associated documentation files (the "Software"), #
# to deal in the Software without restriction, including without  limitation #
# the rights to use, copy, modify, merge, publish, distribute, sublicense,   #
# and/or sell copies of the Software, and to permit persons to whom the      #
# Software is furnished to do so, subject to the following conditions:       #
#                                                                            #
# The above copyright notice and this permission notice shall be included in #
# all copies or substantial portions of the Software.                        #
#                                                                            #
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR #
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,   #
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL    #
# THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER #
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING    #
# FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER        #
# DEALINGS IN THE SOFTWARE.                                                  #
##############################################################################

.PHONE: source jar clean install run

jar: source
	jar -cvfe vis.jar Vis -C classes .

clean:
	\rm -rf classes
	\rm -rf vis.jar

install:
	@echo "Add your own install command to the Makefile"

classes/%.class: %.java
	javac -cp . -d classes $<

classes:
	mkdir classes

source: classes \
        classes/ChangeHist.class \
        classes/ChangeType.class \
        classes/Colon.class \
        classes/Console.class \
        classes/Cover.class \
        classes/CrsPos.class \
        classes/Diff.class \
        classes/Diff_Type.class \
        classes/FileBuf.class \
        classes/File_Type.class \
        classes/Help.class \
        classes/HiKeyVal.class \
        classes/Highlight_Type.class \
        classes/Highlight_Base.class \
        classes/Highlight_Bash.class \
        classes/Highlight_CMAKE.class \
        classes/Highlight_Code.class \
        classes/Highlight_CPP.class \
        classes/Highlight_IDL.class \
        classes/Highlight_Java.class \
        classes/Highlight_SQL.class \
        classes/Highlight_STL.class \
        classes/Highlight_Text.class \
        classes/Highlight_XML.class \
        classes/IntList.class \
        classes/Line.class \
        classes/LineChange.class \
        classes/LineInfo.class \
        classes/LineUpdate.class \
        classes/OS_Type.class \
        classes/Paste_Mode.class \
        classes/Paste_Pos.class \
        classes/Ptr_InputStream.class \
        classes/Ptr_Int.class \
        classes/Ptr_String.class \
        classes/Ptr_StringBuilder.class \
        classes/SameArea.class \
        classes/SameLineSec.class \
        classes/Shell.class \
        classes/Style.class \
        classes/Tile_Pos.class \
        classes/Update_Type.class \
        classes/Utils.class \
        classes/View.class \
        classes/ViewList.class \
        classes/Vis.class \

