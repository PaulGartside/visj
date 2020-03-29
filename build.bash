#!/bin/bash

#set -o xtrace
#set -o noexec

CLASS_DIR=classes_fx

FILES='ChangeHist ChangeType ConsoleIF Cover CrsPos Diff
       Diff_Type Encoding FileBuf File_Type Help HiKeyVal
       Highlight_Base Highlight_Bash Highlight_BufferEditor
       Highlight_CMAKE Highlight_Code Highlight_CPP
       Highlight_CS Highlight_Dir Highlight_HTML
       Highlight_IDL Highlight_Java Highlight_JS
       Highlight_Make Highlight_MIB Highlight_Python
       Highlight_SQL Highlight_STL Highlight_Text
       Highlight_Type Highlight_XML Image_data IntList
       Line LineView LineChange LineInfo LineUpdate OS_Type
       Paste_Mode Paste_Pos Ptr_Boolean Ptr_InputStream
       Ptr_Int Ptr_Double Ptr_String Ptr_StringBuilder
       SameArea Shell Style Tile_Pos Update_Type Utils
       View ViewList VisIF VisFx'

# Running without arguments will make without cleaning
clean=false
make=true

function usage
{
  echo "usage: $0 [clean] [make]"

  clean=false
  make=false
}

function run_cmd
{
  echo "$*"
  $*
}

function do_clean
{
  run_cmd \rm -rf $CLASS_DIR
  run_cmd \rm -rf *.jar
}

function do_make
{
  if [ ! -d $CLASS_DIR ]; then
    run_cmd mkdir -p $CLASS_DIR
  fi

  recompiled_file=false

  for file in $FILES; do
    if [ ! -e $CLASS_DIR/$file.class ] || [ $file.java -nt $CLASS_DIR/$file.class ]
    then
      run_cmd javac -cp . -d $CLASS_DIR $file.java
      recompiled_file=true
    fi
  done

  if [ ! -e vis_fx.jar ] || [ $recompiled_file = true ]; then
    run_cmd jar -cvfe vis_fx.jar VisFx -C $CLASS_DIR .
  fi

  echo "Done making vis_fx.jar"
}

if [ $# -gt 2 ]; then
  # Too many arguments
  usage
elif [ $# -eq 1 ]; then
  # Checkout for clean or make
  if [ $1 = clean ]; then
    clean=true
    make=false
  elif [ $1 = make ]; then
    clean=false
    make=true
  else
    usage
  fi
elif [ $# -eq 2 ]; then
  if [ $1 = clean ] && [ $2 = make ]; then
    clean=true
    make=true
  elif [ $1 = make ] && [ $2 = clean ]; then
    clean=true
    make=true
  else
    usage
  fi
fi

if [ $clean = true ]; then
  do_clean
fi

if [ $make = true ]; then
  do_make
fi

