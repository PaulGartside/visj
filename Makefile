##############################################################################
# VI-Simplified (vis) Java Implementation                                    #
# Copyright (c) 11 Feb 2017 Paul J. Gartside                                 #
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

.PHONE: source_fx source_sw jar_fx jar_sw clean install

jar_fx: source_fx
	jar -cvfe vis_fx.jar VisFx -C classes_fx .

jar_sw: source_sw
	jar -cvfe vis_sw.jar VisSw -C classes_sw .

clean:
	\rm -rf classes_fx
	\rm -rf classes_sw
	\rm -rf *.jar

install:
	@echo "Add your own install command to the Makefile"

classes_fx/%.class: %.java
	javac -cp . -d classes_fx $<

classes_sw/%.class: %.java
	javac -cp . -d classes_sw $<

classes_fx:
	mkdir classes_fx

classes_sw:
	mkdir classes_sw

source_fx: classes_fx \
           classes_fx/ChangeHist.class \
           classes_fx/ChangeType.class \
           classes_fx/ConsoleIF.class \
           classes_fx/ConsoleFx.class \
           classes_fx/Cover.class \
           classes_fx/CrsPos.class \
           classes_fx/Diff.class \
           classes_fx/Diff_Type.class \
           classes_fx/Encoding.class \
           classes_fx/FileBuf.class \
           classes_fx/File_Type.class \
           classes_fx/Help.class \
           classes_fx/HiKeyVal.class \
           classes_fx/Highlight_Base.class \
           classes_fx/Highlight_Bash.class \
           classes_fx/Highlight_BufferEditor.class \
           classes_fx/Highlight_CMAKE.class \
           classes_fx/Highlight_Code.class \
           classes_fx/Highlight_CPP.class \
           classes_fx/Highlight_Dir.class \
           classes_fx/Highlight_HTML.class \
           classes_fx/Highlight_IDL.class \
           classes_fx/Highlight_Java.class \
           classes_fx/Highlight_JS.class \
           classes_fx/Highlight_Make.class \
           classes_fx/Highlight_Python.class \
           classes_fx/Highlight_SQL.class \
           classes_fx/Highlight_STL.class \
           classes_fx/Highlight_Text.class \
           classes_fx/Highlight_Type.class \
           classes_fx/Highlight_XML.class \
           classes_fx/IntList.class \
           classes_fx/Line.class \
           classes_fx/LineView.class \
           classes_fx/LineChange.class \
           classes_fx/LineInfo.class \
           classes_fx/LineUpdate.class \
           classes_fx/OS_Type.class \
           classes_fx/Paste_Mode.class \
           classes_fx/Paste_Pos.class \
           classes_fx/Ptr_InputStream.class \
           classes_fx/Ptr_Int.class \
           classes_fx/Ptr_Double.class \
           classes_fx/Ptr_String.class \
           classes_fx/Ptr_StringBuilder.class \
           classes_fx/SameArea.class \
           classes_fx/Shell.class \
           classes_fx/Style.class \
           classes_fx/Tile_Pos.class \
           classes_fx/Update_Type.class \
           classes_fx/Utils.class \
           classes_fx/View.class \
           classes_fx/ViewList.class \
           classes_fx/VisIF.class \
           classes_fx/VisFx.class

source_sw: classes_sw \
           classes_sw/ChangeHist.class \
           classes_sw/ChangeType.class \
           classes_sw/ConsoleIF.class \
           classes_sw/ConsoleSw.class \
           classes_sw/Cover.class \
           classes_sw/CrsPos.class \
           classes_sw/Diff.class \
           classes_sw/Diff_Type.class \
           classes_sw/Encoding.class \
           classes_sw/FileBuf.class \
           classes_sw/File_Type.class \
           classes_sw/Help.class \
           classes_sw/HiKeyVal.class \
           classes_sw/Highlight_Base.class \
           classes_sw/Highlight_Bash.class \
           classes_sw/Highlight_BufferEditor.class \
           classes_sw/Highlight_CMAKE.class \
           classes_sw/Highlight_Code.class \
           classes_sw/Highlight_CPP.class \
           classes_sw/Highlight_Dir.class \
           classes_sw/Highlight_HTML.class \
           classes_sw/Highlight_IDL.class \
           classes_sw/Highlight_Java.class \
           classes_sw/Highlight_Make.class \
           classes_sw/Highlight_Python.class \
           classes_sw/Highlight_SQL.class \
           classes_sw/Highlight_STL.class \
           classes_sw/Highlight_Text.class \
           classes_sw/Highlight_Type.class \
           classes_sw/Highlight_XML.class \
           classes_sw/IntList.class \
           classes_sw/Line.class \
           classes_sw/LineView.class \
           classes_sw/LineChange.class \
           classes_sw/LineInfo.class \
           classes_sw/LineUpdate.class \
           classes_sw/OS_Type.class \
           classes_sw/Paste_Mode.class \
           classes_sw/Paste_Pos.class \
           classes_sw/Ptr_InputStream.class \
           classes_sw/Ptr_Int.class \
           classes_sw/Ptr_Double.class \
           classes_sw/Ptr_String.class \
           classes_sw/Ptr_StringBuilder.class \
           classes_sw/SameArea.class \
           classes_sw/Shell.class \
           classes_sw/Style.class \
           classes_sw/Tile_Pos.class \
           classes_sw/Update_Type.class \
           classes_sw/Utils.class \
           classes_sw/View.class \
           classes_sw/ViewList.class \
           classes_sw/VisIF.class \
           classes_sw/VisSw.class

