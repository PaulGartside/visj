##############################################################################
# VI-Simplified (vis) Java Implementation                                    #
# Copyright (c) 23 Aug 2017 Paul J. Gartside                                 #
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

.PHONY: fx_jar sw_jar run clean install

fx_jar: vis_fx.jar
sw_jar: vis_sw.jar

run:
	java -jar vis_fx.jar

GENERIC_SOURCES = ChangeHist \
                  ChangeType \
                  ConsoleIF \
                  Cover \
                  CrsPos \
                  Diff \
                  Diff_Type \
                  Encoding \
                  FileBuf \
                  File_Type \
                  Help \
                  HiKeyVal \
                  Highlight_Base \
                  Highlight_Bash \
                  Highlight_BufferEditor \
                  Highlight_CMAKE \
                  Highlight_Code \
                  Highlight_CPP \
                  Highlight_CS \
                  Highlight_Dir \
                  Highlight_HTML \
                  Highlight_IDL \
                  Highlight_Java \
                  Highlight_JS \
                  Highlight_Make \
                  Highlight_MIB \
                  Highlight_Python \
                  Highlight_SQL \
                  Highlight_STL \
                  Highlight_Text \
                  Highlight_Type \
                  Highlight_XML \
                  IntList \
                  Line \
                  LineView \
                  LineChange \
                  LineInfo \
                  LineUpdate \
                  OS_Type \
                  Paste_Mode \
                  Paste_Pos \
                  Ptr_Boolean \
                  Ptr_InputStream \
                  Ptr_Int \
                  Ptr_Double \
                  Ptr_String \
                  Ptr_StringBuilder \
                  SameArea \
                  Shell \
                  Style \
                  Tile_Pos \
                  Update_Type \
                  Utils \
                  View \
                  ViewList \
                  VisIF

FX_SOURCES = $(GENERIC_SOURCES) \
             ConsoleFx \
             VisFx

SW_SOURCES = $(GENERIC_SOURCES) \
             ConsoleSw \
             VisSw

FX_CLASS_DIR = classes_fx
FX_CLASS_FILES = $(addprefix $(FX_CLASS_DIR)/,$(addsuffix .class,$(FX_SOURCES)))

SW_CLASS_DIR = classes_sw
SW_CLASS_FILES = $(addprefix $(SW_CLASS_DIR)/,$(addsuffix .class,$(SW_SOURCES)))

vis_fx.jar: $(FX_CLASS_DIR) $(FX_CLASS_FILES)
	jar -cvfe vis_fx.jar VisFx -C $(FX_CLASS_DIR) .
	@echo Done making $@

vis_sw.jar: $(SW_CLASS_DIR) $(SW_CLASS_FILES)
	jar -cvfe vis_sw.jar VisSw -C $(SW_CLASS_DIR) .
	@echo Done making $@

clean:
	\rm -rf $(FX_CLASS_DIR)
	\rm -rf $(SW_CLASS_DIR)
	\rm -rf *.jar

install:
	@echo "Add your own install command to the Makefile"

$(FX_CLASS_DIR):; mkdir -p $(FX_CLASS_DIR)
$(SW_CLASS_DIR):; mkdir -p $(SW_CLASS_DIR)

$(FX_CLASS_FILES): $(FX_CLASS_DIR)/%.class: %.java
	javac -cp . -d $(FX_CLASS_DIR) $<

$(SW_CLASS_FILES): $(SW_CLASS_DIR)/%.class: %.java
	javac -cp . -d $(SW_CLASS_DIR) $<

