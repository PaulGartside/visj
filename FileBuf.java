////////////////////////////////////////////////////////////////////////////////
// VI-Simplified (vis) Java Implementation                                    //
// Copyright (c) 07 Sep 2015 Paul J. Gartside                                 //
////////////////////////////////////////////////////////////////////////////////
// Permission is hereby granted, free of charge, to any person obtaining a    //
// copy of this software and associated documentation files (the "Software"), //
// to deal in the Software without restriction, including without  limitation //
// the rights to use, copy, modify, merge, publish, distribute, sublicense,   //
// and/or sell copies of the Software, and to permit persons to whom the      //
// Software is furnished to do so, subject to the following conditions:       //
//                                                                            //
// The above copyright notice and this permission notice shall be included in //
// all copies or substantial portions of the Software.                        //
//                                                                            //
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR //
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,   //
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL    //
// THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER //
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING    //
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER        //
// DEALINGS IN THE SOFTWARE.                                                  //
////////////////////////////////////////////////////////////////////////////////

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.DirectoryStream;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.image.Image;

class FileBuf
{
  // fname should contain full path and filename
  FileBuf( VisIF vis, String fname, final boolean mutable )
  {
    m_vis       = vis;
    m_path      = FileSystems.getDefault().getPath( fname );
    m_isDir     = Files.isDirectory( m_path );
    m_isRegular = Files.isRegularFile( m_path );
    m_mutable   = m_isDir ? false : mutable;

    if( m_isDir )
    {
      m_dname = Utils.Append_Dir_Delim( fname );
      fname = m_dname;
    }
    else {
      Path parent = m_path.getParent();
      if( null != parent ) m_dname = parent.normalize().toString();
      else {
        // fname passed in was not full path filename, so use process path
        m_dname = Utils.GetCWD();
          fname = m_dname + fname;
      }
    }
    m_pname = fname;
    m_fname = Utils.Pname_2_Fname( m_pname );

    Find_File_Type_Suffix();
  }
  // fname should contain full path and filename
  FileBuf( VisIF vis, String fname, FileBuf fb )
  {
    m_vis       = vis;
    m_path      = FileSystems.getDefault().getPath( fname );
    m_LF_at_EOF = fb.m_LF_at_EOF;
    m_mod_time  = Utils.ModificationTime( m_path );
    m_isDir     = Files.isDirectory( m_path );
    m_isRegular = Files.isRegularFile( m_path );
    m_mutable   = m_isDir ? false : true;
    m_changed_externally = fb.m_changed_externally;
    m_decoding = fb.m_decoding;
    m_encoding = fb.m_encoding;

    if( m_isDir )
    {
      m_dname = Utils.Append_Dir_Delim( fname );
      fname = m_dname;
    }
    else {
      Path parent = m_path.getParent();
      if( null != parent ) m_dname = parent.normalize().toString();
      else {
        // fname passed in was not full path filename, so use process path
        m_dname = Utils.GetCWD();
          fname = m_dname + fname;
      }
    }
    m_pname = fname;
    m_fname = Utils.Pname_2_Fname( m_pname );

    Find_File_Type_Suffix();

    // Add copies of m_lines, m_styles and m_lineOffsets:
    final int FB_NUM_LINES   = fb.m_lines.size();
    final int FB_NUM_OFFSETS = fb.m_lineOffsets.size(); //< Should be the same as FB_NUM_LINES

    m_lines          .ensureCapacity( FB_NUM_LINES );
    m_styles         .ensureCapacity( FB_NUM_LINES );
    m_lineRegexsValid.ensureCapacity( FB_NUM_LINES );
    m_lineOffsets    .ensureCapacity( FB_NUM_OFFSETS );

    for( int k=0; k<FB_NUM_LINES; k++ )
    {
      m_lines .add( new Line( fb.m_lines .get( k ) ) );
      m_styles.add( new Line( fb.m_styles.get( k ) ) );
      m_lineRegexsValid.add( new Boolean( false ) );
    }
    for( int k=0; k<FB_NUM_OFFSETS; k++ )
    {
      m_lineOffsets.add( new Integer( fb.m_lineOffsets.get( k ) ) );
    }
    if( m_mutable ) m_save_history = true;
  }

  void Find_File_Type_Suffix()
  {
    if( m_isDir )
    {
      m_file_type = File_Type.DIR;
      m_Hi = new Highlight_Dir( this );
    }
    else if( !m_found_BE && m_pname.equals( m_dname + m_vis.EDIT_BUF_NAME ) )
    {
      m_found_BE = true;
      m_file_type = File_Type.BUFFER_EDITOR;
      m_Hi = new Highlight_BufferEditor( this );
    }
    else if( Find_File_Type_Bash()
          || Find_File_Type_CMAKE()
          || Find_File_Type_CPP ()
          || Find_File_Type_CS  ()
          || Find_File_Type_IDL ()
          || Find_File_Type_HTML()
          || Find_File_Type_Java()
          || Find_File_Type_JS()
          || Find_File_Type_Make()
          || Find_File_Type_MIB()
          || Find_File_Type_Python()
          || Find_File_Type_SQL ()
          || Find_File_Type_STL ()
          || Find_File_Type_XML () )
    {
      // File type found
    }
    else {
      // File type NOT found based on suffix.
      // File type will be found in Find_File_Type_FirstLine()
    }
  }
  boolean Find_File_Type_Bash()
  {
    if( m_pname.endsWith(".sh"      )
     || m_pname.endsWith(".sh.new"  )
     || m_pname.endsWith(".sh.old"  )
     || m_pname.endsWith(".bash"    )
     || m_pname.endsWith(".bash.new")
     || m_pname.endsWith(".bash.old")
     || m_fname.startsWith(".alias")
     || m_fname.startsWith(".bash_profile")
     || m_fname.startsWith(".bash_logout")
     || m_fname.startsWith(".bashrc")
     || m_fname.startsWith(".profile") )
    {
      m_file_type = File_Type.BASH;
      m_Hi = new Highlight_Bash( this );
      return true;
    }
    return false;
  }
  boolean Find_File_Type_CPP()
  {
    if( m_pname.endsWith(".h"      )
     || m_pname.endsWith(".h.new"  )
     || m_pname.endsWith(".h.old"  )
     || m_pname.endsWith(".c"      )
     || m_pname.endsWith(".c.new"  )
     || m_pname.endsWith(".c.old"  )
     || m_pname.endsWith(".hh"     )
     || m_pname.endsWith(".hh.new" )
     || m_pname.endsWith(".hh.old" )
     || m_pname.endsWith(".cc"     )
     || m_pname.endsWith(".cc.new" )
     || m_pname.endsWith(".cc.old" )
     || m_pname.endsWith(".hpp"    )
     || m_pname.endsWith(".hpp.new")
     || m_pname.endsWith(".hpp.old")
     || m_pname.endsWith(".cpp"    )
     || m_pname.endsWith(".cpp.new")
     || m_pname.endsWith(".cpp.old")
     || m_pname.endsWith(".cxx"    )
     || m_pname.endsWith(".cxx.new")
     || m_pname.endsWith(".cxx.old") )
    {
      m_file_type = File_Type.CPP;
      m_Hi = new Highlight_CPP( this );
      return true;
    }
    return false;
  }
  boolean Find_File_Type_CS()
  {
    if( m_pname.endsWith(".cs"    )
     || m_pname.endsWith(".cs.new")
     || m_pname.endsWith(".cs.old") )
    {
      m_file_type = File_Type.CS;
      m_Hi = new Highlight_CS( this );
      return true;
    }
    return false;
  }
  boolean Find_File_Type_IDL()
  {
    if( m_pname.endsWith(".idl"    )
     || m_pname.endsWith(".idl.new")
     || m_pname.endsWith(".idl.old")
     || m_pname.endsWith(".idl.in"    )
     || m_pname.endsWith(".idl.in.new")
     || m_pname.endsWith(".idl.in.old") )
    {
      m_file_type = File_Type.IDL;
      m_Hi = new Highlight_IDL( this );
      return true;
    }
    return false;
  }
  boolean Find_File_Type_HTML()
  {
    if( m_pname.endsWith(".html"    )
     || m_pname.endsWith(".html.new")
     || m_pname.endsWith(".html.old")
     || m_pname.endsWith(".htm"    )
     || m_pname.endsWith(".htm.new")
     || m_pname.endsWith(".htm.old") )
    {
      m_file_type = File_Type.HTML;
      m_Hi = new Highlight_HTML( this );
      return true;
    }
    return false;
  }
  boolean Find_File_Type_Java()
  {
    if( m_pname.endsWith(".java"    )
     || m_pname.endsWith(".java.new")
     || m_pname.endsWith(".java.old") )
    {
      m_file_type = File_Type.JAVA;
      m_Hi = new Highlight_Java( this );
      return true;
    }
    return false;
  }
  boolean Find_File_Type_JS()
  {
    if( m_pname.endsWith(".js"    )
     || m_pname.endsWith(".js.new")
     || m_pname.endsWith(".js.old") )
    {
      m_file_type = File_Type.JS;
      m_Hi = new Highlight_JS( this );
      return true;
    }
    return false;
  }
  boolean Find_File_Type_Make()
  {
    if( m_pname.endsWith(".Make"    )
     || m_pname.endsWith(".make"    )
     || m_pname.endsWith(".Make.new")
     || m_pname.endsWith(".make.new")
     || m_pname.endsWith(".Make.old")
     || m_pname.endsWith(".make.old")
     || m_pname.endsWith("Makefile")
     || m_pname.endsWith("makefile")
     || m_pname.endsWith("Makefile.new")
     || m_pname.endsWith("makefile.new")
     || m_pname.endsWith("Makefile.old")
     || m_pname.endsWith("makefile.old") )
    {
      m_file_type = File_Type.MAKE;
      m_Hi = new Highlight_Make( this );
      return true;
    }
    return false;
  }
  boolean Find_File_Type_MIB()
  {
    if( m_pname.endsWith(".mib"    )
     || m_pname.endsWith(".mib.new")
     || m_pname.endsWith(".mib.old")
     || m_pname.endsWith("-MIB.txt")
     || m_pname.endsWith("-MIB.txt.new")
     || m_pname.endsWith("-MIB.txt.old") )
    {
      m_file_type = File_Type.MIB;
      m_Hi = new Highlight_MIB( this );
      return true;
    }
    return false;
  }
  boolean Find_File_Type_STL()
  {
    if( m_pname.endsWith(".stl"    )
     || m_pname.endsWith(".stl.new")
     || m_pname.endsWith(".stl.old")
     || m_pname.endsWith(".ste"    )
     || m_pname.endsWith(".ste.new")
     || m_pname.endsWith(".ste.old") )
    {
      m_file_type = File_Type.STL;
      m_Hi = new Highlight_STL( this );
      return true;
    }
    return false;
  }
  boolean Find_File_Type_Python()
  {
    if( m_pname.endsWith(".py"    )
     || m_pname.endsWith(".py.new")
     || m_pname.endsWith(".py.old") )
    {
      m_file_type = File_Type.PY;
      m_Hi = new Highlight_Python( this );
      return true;
    }
    return false;
  }
  boolean Find_File_Type_SQL()
  {
    if( m_pname.endsWith(".sql"    )
     || m_pname.endsWith(".sql.new")
     || m_pname.endsWith(".sql.old") )
    {
      m_file_type = File_Type.SQL;
      m_Hi = new Highlight_SQL( this );
      return true;
    }
    return false;
  }
  boolean Find_File_Type_XML()
  {
    if( m_pname.endsWith(".xml"    )
     || m_pname.endsWith(".xml.new")
     || m_pname.endsWith(".xml.old")
     || m_pname.endsWith(".xml.in"    )
     || m_pname.endsWith(".xml.in.new")
     || m_pname.endsWith(".xml.in.old")
     || m_pname.endsWith(".xs"    )
     || m_pname.endsWith(".xs.new")
     || m_pname.endsWith(".xs.old")
     || m_pname.endsWith(".xsd"    )
     || m_pname.endsWith(".xsd.new")
     || m_pname.endsWith(".xsd.old") )
    {
      m_file_type = File_Type.XML;
      m_Hi = new Highlight_XML( this );
      return true;
    }
    return false;
  }
  boolean Find_File_Type_CMAKE()
  {
    if( m_pname.endsWith(".cmake"    )
     || m_pname.endsWith(".cmake.new")
     || m_pname.endsWith(".cmake.old")
     || m_pname.endsWith(".cmake"    )
     || m_pname.endsWith(".cmake.new")
     || m_pname.endsWith(".cmake.old")
     || m_pname.endsWith("CMakeLists.txt")
     || m_pname.endsWith("CMakeLists.txt.old")
     || m_pname.endsWith("CMakeLists.txt.new") )
    {
      m_file_type = File_Type.CMAKE;
      m_Hi = new Highlight_CMAKE( this );
      return true;
    }
    return false;
  }

  void Find_File_Type_FirstLine()
  {
    final int NUM_LINES = NumLines();

    if( 0 < NUM_LINES )
    {
      Line lp0 = GetLine( 0 );

      final String p1 = "#!/bin/bash";
      final String p2 = "#!/bin/sh";
      final String p3 = "#!/usr/bin/bash";
      final String p4 = "#!/usr/bin/sh";

      if( lp0.toString().equals( p1 )
       || lp0.toString().equals( p2 )
       || lp0.toString().equals( p3 )
       || lp0.toString().equals( p4 ) )
      {
        m_file_type = File_Type.BASH;
        m_Hi = new Highlight_Bash( this );
      }
    }
    if( File_Type.UNKNOWN == m_file_type )
    {
      // File type NOT found, so default to TEXT
      m_file_type = File_Type.TEXT;
      m_Hi = new Highlight_Text( this );
    }
  }

  void Set_File_Type( String syn )
  {
    if( !m_isDir )
    {
      boolean found_syntax_type = true;

      if( syn.equals("sh")
       || syn.equals("bash") )
      {
        m_file_type = File_Type.BASH;
        m_Hi = new Highlight_Bash( this );
      }
      else if( syn.equals("cmake") )
      {
        m_file_type = File_Type.CMAKE;
        m_Hi = new Highlight_CMAKE( this );
      }
      else if( syn.equals("c")
            || syn.equals("cpp") )
      {
        m_file_type = File_Type.CPP;
        m_Hi = new Highlight_CPP( this );
      }
      else if( syn.equals("cs") )
      {
        m_file_type = File_Type.CS;
        m_Hi = new Highlight_CS( this );
      }
      else if( syn.equals("idl") )
      {
        m_file_type = File_Type.IDL;
        m_Hi = new Highlight_IDL( this );
      }
      else if( syn.equals("html") )
      {
        m_file_type = File_Type.HTML;
        m_Hi = new Highlight_HTML( this );
      }
      else if( syn.equals("java") )
      {
        m_file_type = File_Type.JAVA;
        m_Hi = new Highlight_Java( this );
      }
      else if( syn.equals("js") )
      {
        m_file_type = File_Type.JS;
        m_Hi = new Highlight_JS( this );
      }
      else if( syn.equals("make") )
      {
        m_file_type = File_Type.MAKE;
        m_Hi = new Highlight_Make( this );
      }
      else if( syn.equals("mib") )
      {
        m_file_type = File_Type.MIB;
        m_Hi = new Highlight_MIB( this );
      }
      else if( syn.equals("py") )
      {
        m_file_type = File_Type.PY;
        m_Hi = new Highlight_Python( this );
      }
      else if( syn.equals("sql") )
      {
        m_file_type = File_Type.SQL;
        m_Hi = new Highlight_SQL( this );
      }
      else if( syn.equals("ste")
            || syn.equals("stl") )
      {
        m_file_type = File_Type.STL;
        m_Hi = new Highlight_STL( this );
      }
      else if( syn.equals("text") )
      {
        m_file_type = File_Type.TEXT;
        m_Hi = new Highlight_Text( this );
      }
      else if( syn.equals("xml") )
      {
        m_file_type = File_Type.XML;
        m_Hi = new Highlight_XML( this );
      }
      else {
        found_syntax_type = false;
      }
      if( found_syntax_type )
      {
        m_hi_touched_line = 0;

        Update();
      }
    }
  }

  void CheckFileModTime()
  {
    if( m_isRegular || m_isDir )
    {
      final long NOW = System.currentTimeMillis();

      // Check for updates to current file once per second:
      if( 1000 <= (NOW - m_mod_check_time) )
      {
        final long curr_mod_time = Utils.ModificationTime( m_path );

        if( m_mod_time < curr_mod_time )
        {
          if( m_isRegular )
          {
            // Update file modification time so that the message window
            // will not keep popping up:
            m_mod_time = curr_mod_time;
            m_changed_externally = true;
            Update();
            m_vis.CmdLineMessage(m_fname +" changed on file system");
          }
          else if( m_isDir )
          {
            // Dont ask the user, just read in the directory.
            // m_mod_time will get updated in ReReadFile()
            ReReadFile();

            Update();
          }
        }
        m_mod_check_time = NOW;
      }
    }
  }

  String Relative_2_FullFname( final String relative_fname )
  {
    Path fpath;

    if( m_isDir )
    {
      fpath = m_path.resolve( relative_fname ).toAbsolutePath().normalize();
    }
    else {
      fpath = m_path.resolveSibling( relative_fname ).toAbsolutePath().normalize();
    }
    String full_fname = fpath.toString();

    if( Files.isDirectory( fpath ) )
    {
      if( false == full_fname.endsWith( Utils.DIR_DELIM_STR ) )
      {
        full_fname += Utils.DIR_DELIM_STR;
      }
    }
    return full_fname;
  }

  boolean ReadFile()
  {
    try {
      return ReadFile_p();
    }
    catch( Exception e )
    {
      m_vis.Window_Message( e.toString() );
    }
    return false;
  }
  boolean ReReadFile()
  {
    boolean ok = true;

    // Can only re-read user files
    if( m_vis.USER_FILE <= m_vis.Curr_FileNum() )
    {
      ClearLines();

      m_save_history = false; //< Gets turned back on in ReadFile()

      ok = ReadFile();

      // To be safe, put cursor at top,left of each view of this file:
      for( int w=0; ok && w<VisIF.MAX_WINS; w++ )
      {
        View v = m_views.get( w );

        v.Check_Context();
      }
    }
    return ok;
  }
  private boolean ReadFile_p() throws FileNotFoundException, IOException
  {
    if( m_isDir )
    {
      ReadExistingDir();
    }
    else {
      if( m_isRegular )
      {
        ReadExistingFile();
      }
      else {
        // File does not exist, so add an empty line:
        PushLine();
        // File has not been written, so cant get m_mod_time
      }
    }
    if( m_mutable ) m_save_history = true;

    m_mod_time = Utils.ModificationTime( m_path );
    m_changed_externally = false;

    return true;
  }
  void ReadExistingDir() throws IOException
  {
    ArrayList<String> files = new ArrayList<>();
    ArrayList<String> dirs  = new ArrayList<>();

    // Java DirectoryStream does not have parent directory, so add it:
    if( 1<m_pname.length() ) dirs.add(".."); // Dont add '..' to '/'

    DirectoryStream<Path> dir_paths = Files.newDirectoryStream( m_path );

    for( Path path : dir_paths )
    {
      String fname = path.getFileName().toString();

      if( Files.isDirectory( path ) )
      {
        fname = Check_4_SymbolicLink( fname, path );

        if( false == fname.endsWith( Utils.DIR_DELIM_STR ) )
        {
          fname += Utils.DIR_DELIM_STR;
        }
        dirs.add( fname );
      }
      else {
        fname = Check_4_SymbolicLink( fname, path );

        files.add( fname );
      }
    }
    dir_paths.close();

    Collections.sort( files );
    Collections.sort( dirs );

    for( String d : dirs  ) PushLine( d );
    for( String f : files ) PushLine( f );
  }
  String Check_4_SymbolicLink( String fname, Path path )
  {
    try {
      if( Files.isSymbolicLink( path ) )
      {
        StringBuilder fname_sb = new StringBuilder( fname );
        fname_sb.append(" -> ");
        try {
          Path real = path.toRealPath();
          fname_sb.append( real.toString() );
        }
        catch( Exception e ) {}
        fname = fname_sb.toString();
      }
    }
    catch( Exception e ) {}

    return fname;
  }
  // Original version:
  // File is read in one byte at a time:
  void ReadExistingFile() throws FileNotFoundException, IOException
  {
    File infile = m_path.toFile();

    FileInputStream     fis = new FileInputStream( infile );
    BufferedInputStream bis = new BufferedInputStream( fis, 512 );

    Line l_line = new Line();

    for( boolean done = false; !done; )
    {
      final int C = bis.read();
      if( -1 == C)
      {
        done = true;
        if( 0 < l_line.length() ) PushLine( l_line );
      }
      else if( '\n' == C)
      {
        PushLine( l_line );
        l_line = new Line();
        m_LF_at_EOF = true;
      }
      else {
        l_line.append_c( (char)C);
        m_LF_at_EOF = false;
      }
    }
    fis.close();

    if( File_Type.UNKNOWN == m_file_type )
    {
      Find_File_Type_FirstLine();
    }
  }

//// FileChannel but not decoding:
//void ReadExistingFile() throws FileNotFoundException, IOException
//{
//  File infile = m_path.toFile();
//
//  FileInputStream     fis   = new FileInputStream( infile );
//  FileChannel         fchan = fis.getChannel();
//  ByteBuffer          bbuf  = ByteBuffer.allocate( 1024 );
//
//  Line l_line = new Line();
//
//  for( boolean done = false; !done; )
//  {
//    bbuf.clear();
//    done = ( fchan.read( bbuf ) == -1 );
//    bbuf.flip();
//
//    for( int k=0; k<bbuf.remaining(); k++ )
//    {
//      final char C = (char)bbuf.get(k);
//      if( '\n' == C) {
//        PushLine( l_line );
//        l_line = new Line();
//        m_LF_at_EOF = true;
//      }
//      else {
//        l_line.append_c( (char)C);
//        m_LF_at_EOF = false;
//      }
//    }
//  }
//  if( 0 < l_line.length() ) PushLine( l_line );
//  fis.close();
//
//  if( File_Type.UNKNOWN == m_file_type )
//  {
//    Find_File_Type_FirstLine();
//  }
//}

  void ReadString( final String STR )
  {
    Line l_line = null;

    for( int k=0; k<STR.length(); k++ )
    {
      if( null == l_line ) l_line = new Line();

      final char C = STR.charAt( k );

      if( '\n' == C )
      {
        PushLine( l_line );
        l_line = null;
        m_LF_at_EOF = true;
      }
      else {
        l_line.append_c( (char)C );
        m_LF_at_EOF = false;
      }
    }
    if( null != l_line ) PushLine( l_line );
  }
  void ReadArray( ArrayList<Byte> array )
  {
    final int ARRAY_LEN = array.size();

    Line l_line = new Line();

    for( int k=0; k<ARRAY_LEN; k++ )
    {
      final int C = array.get( k );

      if( '\n' == C )
      {
        PushLine( l_line );
        l_line = new Line();
        m_LF_at_EOF = true;
      }
      else {
        l_line.append_c( (char)C);
        m_LF_at_EOF = false;
      }
    }
    if( 0 < l_line.length() ) PushLine( l_line );
  }

  boolean Write()
  {
    boolean ok = false;
    try {
      if( m_encoding == Encoding.BYTE )
      {
        ok = Write_p( m_lines, m_LF_at_EOF );
      }
      else { // m_lines to
        if( m_encoding == Encoding.HEX )
        {
          ArrayList<Line> n_lines = new ArrayList<>();
          Ptr_Boolean p_LF_at_EOF = new Ptr_Boolean( true );
          ok = HEX_to_BYTE_get_lines( n_lines, p_LF_at_EOF );

          if( ok ) ok = Write_p( n_lines, p_LF_at_EOF.val );
        }
        else if( m_encoding == Encoding.UTF_8
              || m_encoding == Encoding.UTF_16BE
              || m_encoding == Encoding.UTF_16LE
              || m_encoding == Encoding.WIN_1252 )
        {
          // Going from UTF_8, UTF_16BE, UTF_16LE, or WIN_1252 or to NONE
          final long file_size = GetSize();
          if( Integer.MAX_VALUE < file_size ) ok = false;
          else {
            ArrayList<Line> n_lines = new ArrayList<>();
            Ptr_Boolean p_LF_at_EOF = new Ptr_Boolean( true );
            ok = UTF_or_WIN_1252_to_BYTE_get_lines( (int)file_size, m_encoding, n_lines, p_LF_at_EOF );

            if( ok ) ok = Write_p( n_lines, p_LF_at_EOF.val );
          }
        }
        else throw new Exception("Unhandled Encoding: "+ m_encoding );
      }
    }
    catch( Exception e )
    {
      m_vis.Window_Message( e.toString() );
    }
    return ok;
  }

  private
  boolean Write_p( ArrayList<Line> l_lines
                 , final boolean   l_LF_at_EOF ) throws FileNotFoundException
                                                      , IOException
  {
    File outfile = m_path.toFile();

    FileOutputStream     fos = new FileOutputStream( outfile );
    BufferedOutputStream bos = new BufferedOutputStream( fos, 512 );

    final int NUM_LINES = l_lines.size();

    for( int k=0; k<NUM_LINES; k++ )
    {
      final int LL = l_lines.get(k).length();
      for( int i=0; i<LL; i++ )
      {
        final int C = l_lines.get(k).charAt(i);
        bos.write( C );
      }
      if( k<NUM_LINES-1 || l_LF_at_EOF )
      {
        bos.write( '\n' );
      }
    }
    bos.flush();
    fos.close();

    m_history.Clear();
    m_mod_time = Utils.ModificationTime( m_path );
    m_changed_externally = false;

    // Wrote to file message:
    m_vis.CmdLineMessage("\""+ m_pname +"\" written:"+ m_encoding );

    return true;
  }

  void ClearLines()
  {
    m_lines          .clear();
    m_styles         .clear();
    m_lineRegexsValid.clear();
    m_lineOffsets    .clear();
    m_history        .Clear();
    m_hi_touched_line = 0;
  }

  boolean Changed()
  {
    return m_history.Has_Changes();
  }
  void ChangedLine( final int line_num )
  {
    if( line_num<=0 )
    {
      m_lineOffsets.clear();

      m_hi_touched_line = 0;
    }
    else {
      while( line_num < m_lineOffsets.size() )
      {
        m_lineOffsets.remove( m_lineOffsets.size()-1 );
      }
      m_hi_touched_line = Math.min( m_hi_touched_line, line_num );
    }
  }

  int NumLines()
  {
    return m_lines.size();
  }
//int LineLen( final int LINE_NUM )
//{
//  return m_lines.get( LINE_NUM ).length();
//}
  int LineLen( final int LINE_NUM )
  {
    if( 0 <= LINE_NUM && LINE_NUM < m_lines.size() )
    {
      return m_lines.get( LINE_NUM ).length();
    }
    return 0;
  }
//int LongestLineLen()
//{
//  int longest = 0;
//
//  final int NUM_LINES =  m_lines.size();
//
//  for( int k=0; k<NUM_LINES; k++ )
//  {
//    longest = Math.max( m_lines.get( k ).length(), longest );
//  }
//  return longest;
//}
  char Get( final int l_num, final int c_num )
  {
    if( l_num < m_lines.size() )
    {
      Line lr = m_lines.get( l_num );

      if( c_num < lr.length() )
      {
        return lr.charAt( c_num );
      }
    }
    return ' ';
  }
  // Returns number of characters representing the file
  //   and fills in m_lineOffsets if needed.
  // If m_decoding is BYTE, then number characters
  //   representing the file equals the file size.
  int GetSize()
  {
    return m_encoding == Encoding.HEX ? GetSizeHEX()
                                      : GetSizeStd();
  }
  private int GetSizeStd()
  {
    final int NUM_LINES = m_lines.size();

    int size = 0;

    if( 0<NUM_LINES )
    {
      m_lineOffsets.ensureCapacity( NUM_LINES );

      // Absolute byte offset of beginning of first line in file is always zero:
      if( 0 == m_lineOffsets.size() ) m_lineOffsets.add( 0 );

      // Old line offsets length:
      final int OLOL = m_lineOffsets.size();

      // New line offsets length:
      while( m_lineOffsets.size() < NUM_LINES ) m_lineOffsets.add( 0 );

      for( int k=OLOL; k<NUM_LINES; k++ )
      {
        final int offset_k = m_lineOffsets.get( k-1 )
                           + m_lines.get( k-1 ).length()
                           + 1; //< Add 1 for '\n'
        m_lineOffsets.set( k, offset_k );
      }
      size = m_lineOffsets.get( NUM_LINES-1 )
           + m_lines.get( NUM_LINES-1 ).length();
      if( m_LF_at_EOF ) size++;
    }
    return size;
  }
  // Returns number of bytes representing the file
  //   and fills in m_lineOffsets if needed.
  private int GetSizeHEX()
  {
    final int NUM_LINES = m_lines.size();

    int size = 0;

    if( 0<NUM_LINES )
    {
      m_lineOffsets.ensureCapacity( NUM_LINES );

      // Absolute byte offset of beginning of first line in file is always zero:
      if( 0 == m_lineOffsets.size() ) m_lineOffsets.add( 0 );

      // Old line offsets length:
      final int OLOL = m_lineOffsets.size();

      // New line offsets length:
      while( m_lineOffsets.size() < NUM_LINES ) m_lineOffsets.add( 0 );

      for( int k=OLOL; k<NUM_LINES; k++ )
      {
        // For HEX decoded files, each byte is represented by
        // a space and two hex characters, so divide by 3:
        final int offset_k = m_lineOffsets.get( k-1 )
                           + m_lines.get( k-1 ).length()/3;
        m_lineOffsets.set( k, offset_k );
      }
      size = m_lineOffsets.get( NUM_LINES-1 )
           + m_lines.get( NUM_LINES-1 ).length()/3;
    }
    return size;
  }
  int GetCursorByte( int CL, int CC )
  {
    return m_encoding == Encoding.HEX ? GetCursorByteHEX(CL, CC)
                                      : GetCursorByteStd(CL, CC);
  }
  private int GetCursorByteStd( int CL, int CC )
  {
    final int NUM_LINES = m_lines.size();

    int crsByte = 0;

    if( 0<NUM_LINES )
    {
      m_lineOffsets.ensureCapacity( NUM_LINES );

      if( NUM_LINES <= CL ) CL = NUM_LINES-1;

      final int CLL = m_lines.get( CL ).length();

      if( CLL <= CC ) CC = 0<CLL ? CLL-1 : 0;

      // Absolute byte offset of beginning of first line in file is always zero:
      if( 0 == m_lineOffsets.size() ) m_lineOffsets.add( 0 );

      // HVLO = Highest valid line offset
      final int HVLO = m_lineOffsets.size()-1;

      if( HVLO < CL )
      {
        while( m_lineOffsets.size() <= CL ) m_lineOffsets.add( 0 );

        for( int k=HVLO+1; k<=CL; k++ )
        {
          final int offset_k = m_lineOffsets.get( k-1 )
                             + m_lines.get( k-1 ).length()
                             + 1; //< Add 1 for '\n'
          m_lineOffsets.set( k, offset_k );
        }
      }
      crsByte = m_lineOffsets.get( CL ) + CC;
    }
    return crsByte;
  }
  private int GetCursorByteHEX( int CL, int CC )
  {
    final int NUM_LINES = m_lines.size();

    int crsByte = 0;

    if( 0<NUM_LINES )
    {
      m_lineOffsets.ensureCapacity( NUM_LINES );

      if( NUM_LINES <= CL ) CL = NUM_LINES-1;

      final int CLL = m_lines.get( CL ).length();

      if( CLL <= CC ) CC = 0<CLL ? CLL-1 : 0;

      // Absolute byte offset of beginning of first line in file is always zero:
      if( 0 == m_lineOffsets.size() ) m_lineOffsets.add( 0 );

      // HVLO = Highest valid line offset
      final int HVLO = m_lineOffsets.size()-1;

      if( HVLO < CL )
      {
        while( m_lineOffsets.size() <= CL ) m_lineOffsets.add( 0 );

        for( int k=HVLO+1; k<=CL; k++ )
        {
          // For HEX decoded files, each byte is represented by
          // a space and two hex characters, so divide by 3:
          final int offset_k = m_lineOffsets.get( k-1 )
                             + m_lines.get( k-1 ).length()/3;
          m_lineOffsets.set( k, offset_k );
        }
      }
      crsByte = m_lineOffsets.get( CL ) + CC/3;
    }
    return crsByte;
  }

  // Find styles up to but not including up_to_line number
  void Find_Styles( final int up_to_line )
  {
    final int NUM_LINES = NumLines();

    if( 0<NUM_LINES )
    {
      m_hi_touched_line = Math.min( m_hi_touched_line, NUM_LINES-1 );

      if( m_hi_touched_line < up_to_line )
      {
        // Find styles for some EXTRA_LINES beyond where we need to find
        // styles for the moment, so that when the user is scrolling down
        // through an area of a file that has not yet been syntax highlighed,
        // Find_Styles() does not need to be called every time the user
        // scrolls down another line.  Find_Styles() will only be called
        // once for every EXTRA_LINES scrolled down.
        final int EXTRA_LINES = 10;

        CrsPos st = Update_Styles_Find_St( m_hi_touched_line );
        int    fn = Math.min( up_to_line+EXTRA_LINES, NumLines() );

        Find_Styles_In_Range( st, fn );

        m_hi_touched_line = fn;
      }
    }
  }

  CrsPos Update_Styles_Find_St( final int first_line )
  {
    // Default start position is beginning of file
    CrsPos st = new CrsPos( 0, 0 );

    // 1. Find first position without a style before first line
    boolean done = false;
    for( int l=first_line-1; !done && 0<=l; l-- )
    {
      final int LL = LineLen( l );
      for( int p=LL-1; !done && 0<=p; p-- )
      {
        final char S = m_styles.get( l ).charAt( p );
        if( 0==S ) {
          st.crsLine = l;
          st.crsChar = p;
          done = true;
        }
      }
    }
    return st;
  }

  // Find styles starting on st up to but not including fn line
  void Find_Styles_In_Range( final CrsPos st, final int fn )
  {
    if( null == m_Hi )
    {
      m_Hi = new Highlight_Text( this );
    }
    m_Hi.Run_Range( st, fn );
  }

//// Find stars starting on st up to but not including fn line
//void Find_Stars_In_Range( final CrsPos st, final int fn )
//{
//  final String  star_str  = m_vis.m_star;
//  final int     STAR_LEN  = m_vis.m_star.length();
//  final boolean SLASH     = m_vis.m_slash;
//  final int     NUM_LINES = NumLines();
//
//  for( int l=st.crsLine; 0<STAR_LEN && l<NUM_LINES && l<fn; l++ )
//  {
//    Line lp = m_lines.get( l );
//    final int LL = lp.length();
//    if( LL<STAR_LEN ) continue;
//
//    final int st_pos = st.crsLine==l ? st.crsChar : 0;
//
//    for( int p=st_pos; p<LL; p++ )
//    {
//      boolean matches = SLASH || Utils.line_start_or_prev_C_non_ident( lp, p );
//      for( int k=0; matches && (p+k)<LL && k<STAR_LEN; k++ )
//      {
//        if( star_str.charAt(k) != lp.charAt(p+k) ) matches = false;
//        else {
//          if( k+1 == STAR_LEN ) // Found pattern
//          {
//            matches = SLASH || Utils.line_end_or_non_ident( lp, LL, p+k );
//            if( matches ) {
//              for( int m=p; m<p+STAR_LEN; m++ ) SetStarStyle( l, m );
//              // Increment p one less than STAR_LEN, because p
//              // will be incremented again by the for loop
//              p += STAR_LEN-1;
//            }
//          }
//        }
//      }
//    }
//  }
//}

  void Find_Regexs( final int start_line
                  , final int num_lines )
  {
    Check_4_New_Regex();

    final int up_to_line = Math.min( start_line+num_lines, NumLines() );

    for( int k=start_line; k<up_to_line; k++ )
    {
      Find_Regexs_4_Line( k );
    }
  }

  void Check_4_New_Regex()
  {
    boolean have_new_regex = false;

    if( null == m_regex )
    {
      have_new_regex = true;
    }
    else if( !m_regex.equals( m_vis.get_regex() ) )
    {
      // Invalidate all regexes
      for( int k=0; k<m_lineRegexsValid.size(); k++ )
      {
        m_lineRegexsValid.set(k, false);
      }
      have_new_regex = true;
    }

    if( have_new_regex )
    {
      m_regex = m_vis.get_regex();

      try {
        m_pattern = Pattern.compile( m_regex );
      }
      catch( Exception e )
      {
        m_pattern = null;
        m_vis.Window_Message( e.getMessage() );
      }
    }
  }

  // Uses java regex:
  void Find_Regexs_4_Line( final int line_num )
  {
    if( 0 <= line_num && line_num < m_lineRegexsValid.size()
     && !m_lineRegexsValid.get(line_num) )
    {
      Line lr = m_lines.get( line_num );
      final int LL = lr.length();

      // Clear the patterns for the line:
      for( int pos=0; pos<LL; pos++ )
      {
        ClearStarStyle( line_num, pos );
      }
      if( null != m_pattern && 0<m_regex.length() )
      {
        if( m_file_type == File_Type.BUFFER_EDITOR
         || m_file_type == File_Type.DIR )
        {
          if( File_Has_Regex( lr ) )
          {
            for( int k=0; k<LL; k++ ) SetStarStyle( line_num, k );
          }
        }
        Matcher matcher = m_pattern.matcher( lr.toString() );

        while( matcher.find() )
        {
          for( int pos=matcher.start(); pos<matcher.end(); pos++ )
          {
            SetStarStyle( line_num, pos );
          }
        }
      }
      m_lineRegexsValid.set(line_num, true);
    }
  }

  boolean File_Has_Regex( Line file_name )
  {
    boolean has_regex = false;

    if( m_file_type == File_Type.DIR )
    {
      // file_name only has file name, not directory
      String fname = file_name.toString();

      if( Filename_Is_Relevant( fname ) )
      {
        String pname = m_dname + fname;
        m_vis.NotHaveFileAddFile( pname );
        FileBuf fb = m_vis.get_FileBuf( pname );
        has_regex = fb != null
                  ? fb.Has_Pattern( m_pattern )
                  : Utils.Have_Regex_In_File( m_pattern, pname, m_line_buf );
      }
    }
    else if( m_file_type == File_Type.BUFFER_EDITOR )
    {
      // file_name has directory and file name
      String pname = file_name.toString();

      if( !pname.equals( VisIF. EDIT_BUF_NAME )
       && !pname.equals( VisIF. EDIT_BUF_NAME )
       && !pname.equals( VisIF. HELP_BUF_NAME )
       && !pname.equals( VisIF. MSG__BUF_NAME )
       && !pname.equals( VisIF.SHELL_BUF_NAME )
       && !pname.equals( VisIF.COLON_BUF_NAME )
       && !pname.equals( VisIF.SLASH_BUF_NAME )
       && !pname.endsWith( Utils.DIR_DELIM_STR ) )
      {
        FileBuf fb = m_vis.get_FileBuf( pname );
        if( fb != null )
        {
          has_regex = fb.Has_Pattern( m_pattern );
        }
      }
    }
    return has_regex;
  }
  boolean Filename_Is_Relevant( String fname )
  {
    return fname.endsWith(".txt")
        || fname.endsWith(".txt.new")
        || fname.endsWith(".txt.old")
        || fname.endsWith(".sh")
        || fname.endsWith(".sh.new"  )
        || fname.endsWith(".sh.old"  )
        || fname.endsWith(".bash"    )
        || fname.endsWith(".bash.new")
        || fname.endsWith(".bash.old")
        || fname.endsWith(".alias"   )
        || fname.endsWith(".bash_profile")
        || fname.endsWith(".bash_logout")
        || fname.endsWith(".bashrc" )
        || fname.endsWith(".profile")
        || fname.endsWith(".h"      )
        || fname.endsWith(".h.new"  )
        || fname.endsWith(".h.old"  )
        || fname.endsWith(".c"      )
        || fname.endsWith(".c.new"  )
        || fname.endsWith(".c.old"  )
        || fname.endsWith(".hh"     )
        || fname.endsWith(".hh.new" )
        || fname.endsWith(".hh.old" )
        || fname.endsWith(".cc"     )
        || fname.endsWith(".cc.new" )
        || fname.endsWith(".cc.old" )
        || fname.endsWith(".hpp"    )
        || fname.endsWith(".hpp.new")
        || fname.endsWith(".hpp.old")
        || fname.endsWith(".cpp"    )
        || fname.endsWith(".cpp.new")
        || fname.endsWith(".cpp.old")
        || fname.endsWith(".cxx"    )
        || fname.endsWith(".cxx.new")
        || fname.endsWith(".cxx.old")
        || fname.endsWith(".cs"     )
        || fname.endsWith(".cs.new" )
        || fname.endsWith(".cs.old" )
        || fname.endsWith(".idl"    )
        || fname.endsWith(".idl.new")
        || fname.endsWith(".idl.old")
        || fname.endsWith(".html"    )
        || fname.endsWith(".html.new")
        || fname.endsWith(".html.old")
        || fname.endsWith(".htm"     )
        || fname.endsWith(".htm.new" )
        || fname.endsWith(".htm.old" )
        || fname.endsWith(".java"    )
        || fname.endsWith(".java.new")
        || fname.endsWith(".java.old")
        || fname.endsWith(".js"    )
        || fname.endsWith(".js.new")
        || fname.endsWith(".js.old")
        || fname.endsWith(".Make"    )
        || fname.endsWith(".make"    )
        || fname.endsWith(".Make.new")
        || fname.endsWith(".make.new")
        || fname.endsWith(".Make.old")
        || fname.endsWith(".make.old")
        || fname.endsWith("Makefile" )
        || fname.endsWith("makefile" )
        || fname.endsWith("Makefile.new")
        || fname.endsWith("makefile.new")
        || fname.endsWith("Makefile.old")
        || fname.endsWith("makefile.old")
        || fname.endsWith(".stl"    )
        || fname.endsWith(".stl.new")
        || fname.endsWith(".stl.old")
        || fname.endsWith(".ste"    )
        || fname.endsWith(".ste.new")
        || fname.endsWith(".ste.old")
        || fname.endsWith(".py"    )
        || fname.endsWith(".py.new")
        || fname.endsWith(".py.old")
        || fname.endsWith(".sql"    )
        || fname.endsWith(".sql.new")
        || fname.endsWith(".sql.old")
        || fname.endsWith(".xml"     )
        || fname.endsWith(".xml.new" )
        || fname.endsWith(".xml.old" )
        || fname.endsWith(".xml.in"    )
        || fname.endsWith(".xml.in.new")
        || fname.endsWith(".xml.in.old")
        || fname.endsWith(".cmake"     )
        || fname.endsWith(".cmake.new" )
        || fname.endsWith(".cmake.old" )
        || fname.endsWith("CMakeLists.txt")
        || fname.endsWith("CMakeLists.txt.old")
        || fname.endsWith("CMakeLists.txt.new");
  }
  // Returns true if this FileBuf has pattern
  boolean Has_Pattern( Pattern pattern )
  {
    for( int k=0; k<NumLines(); k++ )
    {
      Line l_k = GetLine( k );

      if( 0 < l_k.length() )
      {
        Matcher matcher = pattern.matcher( l_k.toString() );

        if( matcher.find() ) return true;
      }
    }
    return false;
  }

  final Line GetLine( final int l_num )
  {
    return m_lines.get( l_num );
  }
  final Line GetStyle( final int l_num )
  {
    return m_styles.get( l_num );
  }

  // Clear all styles includeing star and syntax
  void ClearAllStyles( final int l_num
                     , final int c_num )
  {
    m_styles.get( l_num ).setCharAt( c_num, (char)0 );
  }

  // Leave star style unchanged, and clear syntax styles
  void ClearSyntaxStyles( final int l_num
                        , final int c_num )
  {
    char s = m_styles.get( l_num ).charAt( c_num );

    // Clear everything except star
    s &= Highlight_Type.STAR.val;

    m_styles.get( l_num ).setCharAt( c_num, s );
  }
//void SetStyle( final int l_num
//             , final int c_num
//             , final int style )
//{
//  m_styles.get( l_num ).setCharAt( c_num, (char)style );
//}

  // Leave syntax styles unchanged, and clear star style
  void ClearStarStyle( final int l_num
                     , final int c_num )
  {
    char s = m_styles.get( l_num ).charAt( c_num );

    // Clear only star
    s &= ~Highlight_Type.STAR.val;

    m_styles.get( l_num ).setCharAt( c_num, s );
  }

  // Leave star style unchanged, and set syntax style
  void SetSyntaxStyle( final int l_num
                     , final int c_num
                     , final int style )
  {
    char s = m_styles.get( l_num ).charAt( c_num );

    s &= Highlight_Type.STAR.val; //< Clear everything except star
    s |= style;                   //< Set style

    m_styles.get( l_num ).setCharAt( c_num, s );
  }

  // Leave syntax styles unchanged, and set star style
  void SetStarStyle( final int l_num
                   , final int c_num )
  {
    char s = m_styles.get( l_num ).charAt( c_num );

    s |= Highlight_Type.STAR.val;

    m_styles.get( l_num ).setCharAt( c_num, s );
  }
//void AddStyle( final int l_num
//             , final int c_num
//             , final int style )
//{
//  char s = m_styles.get( l_num ).charAt( c_num );
//  s |= style;
//  m_styles.get( l_num ).setCharAt( c_num, s );
//}
//void Rm_Style( final int l_num
//             , final int c_num
//             , final int style )
//{
//  char s = m_styles.get( l_num ).charAt( c_num );
//  s &= ~style;
//  m_styles.get( l_num ).setCharAt( c_num, s );
//}
  boolean HasStyle( final int l_num, final int c_num, final int style )
  {
    if( l_num < m_styles.size() )
    {
      Line sr = m_styles.get( l_num );

      if( null != sr )
      {
        if( c_num < sr.length() )
        {
          char s = sr.charAt( c_num );

          return 0 != (s & style);
        }
      }
    }
    return false;
  }

  // Returns true if something changed
  boolean Sort()
  {
    return m_vis.get_sort_by_time()
         ? BufferEditor_SortTime()
         : BufferEditor_SortName();
  }

//private
//boolean BufferEditor_SortName()
//{
//  boolean changed = false;
//  final int NUM_BUILT_IN_FILES = m_vis.USER_FILE;
//
//  // Sort lines (file names), least to greatest:
//  for( int i=NumLines()-1; NUM_BUILT_IN_FILES<i; i-- )
//  {
//    for( int k=NUM_BUILT_IN_FILES; k<i; k++ )
//    {
//      Line l_0 = m_lines.get( k   );
//      Line l_1 = m_lines.get( k+1 );
//
//      // Move largest file name to bottom.
//      // l_0 is greater than l_1, so move it down:
//      if( 0<l_0.toString().compareTo( l_1.toString() ) )
//      {
//        SwapLines( k, k+1 );
//        changed = true;
//      }
//    }
//  }
//  return changed;
//}

  // Return true if l_0 (dname,fname)
  // is greater then l_1 (dname,fname)
  private
  boolean BufferEditor_SortName_Swap( Line l_0, Line l_1 )
  {
    boolean swap = false;

    String l_0_s = l_0.toString();
    String l_1_s = l_1.toString();

    String l_0_dn = Utils.Pname_2_Dname( l_0_s );
    String l_1_dn = Utils.Pname_2_Dname( l_1_s );

    final int dn_compare = l_0_dn.compareTo( l_1_dn );

    if( 0<dn_compare )
    {
      // l_0 dname is greater than l_1 dname
      swap = true;
    }
    else if( 0==dn_compare )
    {
      // l_0 dname == l_1 dname
      String l_0_fn = Utils.Pname_2_Fname( l_0_s );
      String l_1_fn = Utils.Pname_2_Fname( l_1_s );

      if( 0<l_0_fn.compareTo( l_1_fn ) )
      {
        // l_0 fname is greater than l_1 fname
        swap = true;
      }
    }
    return swap;
  }

  // Move largest file name to bottom.
  // Files are grouped under directories.
  // Returns true if any lines were swapped.
  private
  boolean BufferEditor_SortName()
  {
    boolean changed = false;
    final int NUM_BUILT_IN_FILES = m_vis.USER_FILE;

    // Sort lines (file names), least to greatest:
    for( int i=NumLines()-1; NUM_BUILT_IN_FILES<i; i-- )
    {
      for( int k=NUM_BUILT_IN_FILES; k<i; k++ )
      {
        Line l_0 = m_lines.get( k   );
        Line l_1 = m_lines.get( k+1 );

        if( BufferEditor_SortName_Swap( l_0, l_1 ) )
        {
          SwapLines( k, k+1 );
          changed = true;
        }
      }
    }
    return changed;
  }

  private
  boolean BufferEditor_SortTime()
  {
    boolean changed = false;
    final int NUM_BUILT_IN_FILES = m_vis.USER_FILE;

    // Sort lines (file names), least to greatest:
    for( int i=NumLines()-1; NUM_BUILT_IN_FILES<i; i-- )
    {
      for( int k=NUM_BUILT_IN_FILES; k<i; k++ )
      {
        Line l_0 = m_lines.get( k   );
        Line l_1 = m_lines.get( k+1 );

        FileBuf f_0 = m_vis.get_FileBuf( l_0.toString() );
        FileBuf f_1 = m_vis.get_FileBuf( l_1.toString() );

        // Move oldest files to the bottom.
        // f_0 has older time than f_1, so move it down:
        if( f_0.m_foc_time < f_1.m_foc_time )
        {
          SwapLines( k, k+1 );
          changed = true;
        }
      }
    }
    return changed;
  }

  void ClearChanged()
  {
    m_history.Clear();
  }

  void SwapLines( final int l_num_1, final int l_num_2 )
  {
    Line l1 = m_lines.get( l_num_1 );
    Line l2 = m_lines.set( l_num_2, l1 );
              m_lines.set( l_num_1, l2 );

    Line s1 = m_styles.get( l_num_1 );
    Line s2 = m_styles.set( l_num_2, s1 );
              m_styles.set( l_num_1, s2 );

    if( SavingHist() ) m_history.Save_SwapLines( l_num_1, l_num_2 );

    ChangedLine( Math.min( l_num_1, l_num_2 ) );
  }

  void Update()
  {
    if( !m_vis.get_Console().get_from_dot_buf() )
    {
      m_vis.Update_Change_Statuses();

      m_vis.UpdateViewsOfFile( this );

      // Put cursor back into current window
      m_vis.CV().PrintCursor();
    }
  }
  void UpdateCmd()
  {
    if( !m_vis.get_Console().get_from_dot_buf() )
    {
      m_vis.Update_Change_Statuses();

      m_vis.UpdateViewsOfFile( this );

      if( null != m_line_view )
      {
        final LineView pV = m_line_view;

        pV.RepositionView();
        pV.PrintWorkingView();

        // Put cursor back into current window
        pV.PrintCursor();
      }
    }
  }

  // Remove from FileBuf and return the char at line l_num and position c_num
  //
  char RemoveChar( final int l_num, final int c_num )
  {
    Line lr =  m_lines.get( l_num );
    Line sr = m_styles.get( l_num );

    char C = lr.charAt( c_num );
                              lr.deleteCharAt( c_num );
    if( c_num < sr.length() ) sr.deleteCharAt( c_num ); // m_styles not in sync with m_lines for some reason

    ChangedLine( l_num );
    m_lineRegexsValid.set( l_num, false );

    if( SavingHist() ) m_history.Save_RemoveChar( l_num, c_num, C );

    return C;
  }
  boolean SavingHist()
  {
    return m_mutable && m_save_history;
  }

  // Set byte on line l_num at position c_num
  //
  void Set( final int  l_num
          , final int  c_num
          , final char C
          , final boolean continue_last_update )
  {
    Line lr = m_lines.get( l_num );

    final char old_C = lr.charAt( c_num );

    if( old_C != C )
    {
      lr.setCharAt( c_num, C );

      if( SavingHist() )
      {
        m_history.Save_Set( l_num, c_num, old_C, continue_last_update );
      }
      // Did not call ChangedLine(), so need to set m_hi_touched_line here:
      m_hi_touched_line = Math.min( m_hi_touched_line, l_num );
      m_lineRegexsValid.set( l_num, false );
    }
  }
  // Remove a line from FileBuf without deleting it and return pointer to it.
  // Caller of RemoveLine is responsible for deleting line returned.
  //
  Line RemoveLine( final int l_num )
  {
    Line lr = m_lines.remove( l_num );
              m_styles.remove( l_num );
              m_lineRegexsValid.remove( l_num );

    ChangedLine( l_num );

    if( SavingHist() ) m_history.Save_RemoveLine( l_num, lr );

    RemovedLine_Adjust_Views_topLines( l_num );

    return lr;
  }

  // Insert lr on line l_num.
  // l_num can be lines.len();
  //
  void InsertLine( final int l_num, final Line lr )
  {
    Line sr = new Line( lr.length() );
    sr.setLength( lr.length() );

    m_lines.add( l_num, lr );
    m_styles.add( l_num, sr );
    m_lineRegexsValid.add( l_num, false );

    ChangedLine( l_num );

    if( SavingHist() ) m_history.Save_InsertLine( l_num );

    InsertLine_Adjust_Views_topLines( l_num );
  }
  // Insert a new empty line on line l_num.
  // l_num can be lines.len();
  //
  void InsertLine( final int l_num )
  {
    Line lr = new Line();
    Line sp = new Line();

    m_lines.add( l_num, lr );
    m_styles.add( l_num, sp );
    m_lineRegexsValid.add( l_num, false );

    ChangedLine( l_num );

    if( SavingHist() ) m_history.Save_InsertLine( l_num );

    InsertLine_Adjust_Views_topLines( l_num );
  }

  // Insert byte on line l_num at position c_num.
  // c_num can be length of line.
  //
  void InsertChar( final int  l_num
                 , final int  c_num
                 , final char C )
  {
    Line lr =  m_lines.get( l_num );
    Line sr = m_styles.get( l_num );

    lr.insert( c_num, C );
    sr.insert( c_num, '\u0000' );
    m_lineRegexsValid.set( l_num, false );

    ChangedLine( l_num );

    if( SavingHist() ) m_history.Save_InsertChar( l_num, c_num );
  }

  // Append line to end of line l_num
  //
  void AppendLineToLine( final int l_num, final Line line )
  {
    Line lr =  m_lines.get( l_num );
    Line sr = m_styles.get( l_num );

    lr.append_l( line );

    m_lineRegexsValid.set( l_num, false );

    // Simply need to increase sr's length to match lr's new length:
    sr.setLength( lr.length() );

    ChangedLine( l_num );

    final int first_insert = lr.length() - line.length();

    if( SavingHist() )
    {
      for( int k=0; k<line.length(); k++ )
      {
        m_history.Save_InsertChar( l_num, first_insert + k );
      }
    }
  }
  void AppendLineToLine( final int l_num, final String s )
  {
    AppendLineToLine( l_num, new Line( s ) );
  }

  void InsertLine_Adjust_Views_topLines( final int l_num )
  {
    for( int w=0; w<VisIF.MAX_WINS && w<m_views.size(); w++ )
    {
      final View rV = m_views.get( w );

      rV.InsertedLine_Adjust_TopLine( l_num );
    }
  }
  void RemovedLine_Adjust_Views_topLines( final int l_num )
  {
    for( int w=0; w<VisIF.MAX_WINS && w<m_views.size(); w++ )
    {
      final View rV = m_views.get( w );

      rV.RemovedLine_Adjust_TopLine( l_num );
    }
  }
  // Add byte C to the end of line l_num
  //
  void PushChar( final int l_num, final char C )
  {
    Line lr =  m_lines.get( l_num );
    Line sr = m_styles.get( l_num );

    lr.append_c( C );
    sr.append_c( (char)0 );
    m_lineRegexsValid.set( l_num, false );

    ChangedLine( l_num );

    if( SavingHist() ) m_history.Save_InsertChar( l_num, lr.length()-1 );
  }
  // Add byte C to last line.  If no lines in file, add a line.
  //
  void PushChar( final char C )
  {
    if( 0==m_lines.size() ) PushLine();

    final int l_num = m_lines.size()-1;

    PushChar( l_num, C );
  }

  void PushLine( final String s )
  {
    PushLine( new Line( s ) );
  }
  void PushLine( final Line lr )
  {
    Line sr = new Line( lr.length() );
    sr.setLength( lr.length() );

    boolean ok = m_lines.add( lr )
              && m_styles.add( sr )
              && m_lineRegexsValid.add( false );

    if( SavingHist() ) m_history.Save_InsertLine( m_lines.size()-1 );
  }

  // Add a new empty line at the end of FileBuf
  //
  void PushLine()
  {
    Line lr = new Line();
    Line sr = new Line();

    boolean ok = m_lines.add( lr )
              && m_styles.add( sr )
              && m_lineRegexsValid.add( false );

    if( SavingHist() ) m_history.Save_InsertLine( m_lines.size()-1 );
  }

  void Undo( final View rV )
  {
    m_save_history = false;

    m_history.Undo( rV );

    m_save_history = true;
  }
  void UndoAll( final View rV )
  {
    m_save_history = false;

    m_history.UndoAll( rV );

    m_save_history = true;
  }
//void UndoAll( final View rV )
//{
//  m_save_history = false;
//
//  try {
//    m_history.UndoAll( rV );
//  }
//  catch( Throwable t )
//  {
//    Utils.Log( t.toString() );
//  }
//  m_save_history = true;
//}

  void RemoveTabs_SpacesAtEOLs( final int tab_sz )
  {
    int num_tabs_removed = 0;
    int num_spcs_removed = 0;

    final int NUM_LINES = m_lines.size();

    for( int l=0; l<NUM_LINES; l++ )
    {
      num_tabs_removed += RemoveTabs_from_line( l, tab_sz );
      num_spcs_removed += RemoveSpcs_from_EOL( l );
    }
    if( 0 < num_tabs_removed && 0 < num_spcs_removed )
    {
      Update();
      m_vis.CmdLineMessage("Removed "+ num_tabs_removed
                          +" tabs, "+ num_spcs_removed +" spaces");
    }
    else if( 0 < num_tabs_removed )
    {
      Update();
      m_vis.CmdLineMessage("Removed "+ num_tabs_removed +" tabs");
    }
    else if( 0 < num_spcs_removed )
    {
      Update();
      m_vis.CmdLineMessage("Removed "+ num_spcs_removed +" spaces");
    }
    else {
      m_vis.CmdLineMessage("No tabs or spaces removed");
    }
  }
  // Returns number of tabs removed
  int RemoveTabs_from_line( final int l, final int tab_sz )
  {
    int tabs_removed = 0;

    final Line l_c = m_lines.get(l);
    int LL = l_c.length();
    int cnum_t = 0; // char number with respect to tabs

    for( int p=0; p<LL; p++ )
    {
      final int C = l_c.charAt(p);

      if( C != '\t' ) cnum_t += 1;
      else {
        tabs_removed++;
        final int num_spaces = tab_sz-(cnum_t%tab_sz);
        Set( l, p, ' ', false );
        for( int i=1; i<num_spaces; i++ )
        {
          p++;
          InsertChar( l, p, ' ');
          LL++;
        }
        cnum_t = 0;
      }
    }
    return tabs_removed;
  }
  // Returns number of spaces removed
  int RemoveSpcs_from_EOL( final int l )
  {
    int spaces_removed = 0;

    final Line l_c = m_lines.get(l);
    final int LL = l_c.length();

    if( 0 < LL )
    {
      final int end_C = l_c.charAt(LL-1);
      final int logical_EOL = end_C == '\r'
                            ? LL-2  // Windows line ending
                            : LL-1; // Unix line ending
      boolean done = false;
      for( int p=logical_EOL; !done && -1<p; p-- )
      {
        if( ' ' == l_c.charAt( p ) )
        {
          RemoveChar( l, p );
          spaces_removed++;
        }
        else done = true;
      }
    }
    return spaces_removed;
  }

  void dos2unix()
  {
    int num_CRs_removed = 0;

    final int NUM_LINES = m_lines.size();

    for( int l=0; l<NUM_LINES; l++ )
    {
      final Line l_c = m_lines.get(l);
      final int LL = l_c.length();

      if( 0 < LL )
      {
        final int C = l_c.charAt( LL-1 );

        if( C == '\r' )
        {
          RemoveChar( l, LL-1 );
          num_CRs_removed++;
        }
      }
    }
    if( 0 < num_CRs_removed )
    {
      Update();
      m_vis.CmdLineMessage("Removed "+ num_CRs_removed +" CRs");
    }
    else {
      m_vis.CmdLineMessage("No CRs removed");
    }
  }

  void unix2dos()
  {
    int num_CRs_added = 0;

    final int NUM_LINES = m_lines.size();

    for( int l=0; l<NUM_LINES; l++ )
    {
      Line l_c = m_lines.get(l);
      final int LL = l_c.length();

      if( 0 < LL )
      {
        final int C = l_c.charAt( LL-1 );

        if( C != '\r' )
        {
          PushChar( l, '\r' );
          num_CRs_added++;
        }
      }
      else {
        PushChar( l, '\r' );
        num_CRs_added++;
      }
    }
    if( 0 < num_CRs_added )
    {
      Update();
      m_vis.CmdLineMessage("Added "+ num_CRs_added +" CRs");
    }
    else {
      m_vis.CmdLineMessage("No CRs added");
    }
  }

  boolean Set_decoding( final Encoding dec )
  {
    boolean ok = true;
    if( dec != m_decoding )
    {
      final long file_size = GetSize();
      if( Integer.MAX_VALUE < file_size ) ok = false;
      else {
        final int fsz = (int)file_size;
        if( m_decoding == Encoding.BYTE )
        {
          if     ( dec == Encoding.HEX     ) ok = BYTE_to_HEX();
          else if( dec == Encoding.UTF_8   ) ok = BYTE_to_UTF_or_WIN_1252( fsz, dec );
          else if( dec == Encoding.UTF_16BE) ok = BYTE_to_UTF_or_WIN_1252( fsz, dec );
          else if( dec == Encoding.UTF_16LE) ok = BYTE_to_UTF_or_WIN_1252( fsz, dec );
          else if( dec == Encoding.WIN_1252) ok = BYTE_to_UTF_or_WIN_1252( fsz, dec );
          else ok = false;
        }
        else if( dec == Encoding.BYTE )
        {
          if     ( m_decoding == Encoding.HEX     ) ok = HEX_to_BYTE();
          else if( m_decoding == Encoding.UTF_8   ) ok = UTF_or_WIN_1252_to_BYTE( fsz, m_decoding );
          else if( m_decoding == Encoding.UTF_16BE) ok = UTF_or_WIN_1252_to_BYTE( fsz, m_decoding );
          else if( m_decoding == Encoding.UTF_16LE) ok = UTF_or_WIN_1252_to_BYTE( fsz, m_decoding );
          else if( m_decoding == Encoding.WIN_1252) ok = UTF_or_WIN_1252_to_BYTE( fsz, m_decoding );
          else ok = false;
        }
        else ok = false;
      }
    }
    return ok;
  }

  void Set_encoding( final Encoding enc )
  {
    m_encoding = enc;
  }

  // Convert the current from from a byte to a hex representation
  boolean BYTE_to_HEX()
  {
    boolean ok = true;

    ArrayList<Line> n_lines = new ArrayList<>();
    Line n_line = new Line();

    final int NUM_LINES = m_lines.size();
    for( int k=0; k<NUM_LINES; k++ )
    {
      final Line l = m_lines.get(k);
      final int LL = l.length();

      for( int i=0; i<LL; i++ )
      {
        n_line = Append_hex_2_line( n_lines, n_line, l.charAt(i) );
      }
      if( k<NUM_LINES-1 || m_LF_at_EOF )
      {
        n_line = Append_hex_2_line( n_lines, n_line, '\n' );
      }
    }
    if( 0 < n_line.length() )
    {
      n_lines.add( n_line );
    }
    Replace_current_file( n_lines, Encoding.HEX, false );
    Set_File_Type("text");
    return ok;
  }
  Line Append_hex_2_line( ArrayList<Line> n_lines, Line n_line, final char C )
  {
    char C1 = Utils.MS_Hex_Digit( C );
    char C2 = Utils.LS_Hex_Digit( C );

    n_line.append_c(' ');
    n_line.append_c(C1);
    n_line.append_c(C2);

    if( 47 < n_line.length() )
    {
      n_lines.add( n_line );
      n_line = new Line();
    }
    return n_line;
  }

  boolean BYTE_to_UTF_or_WIN_1252( final int file_size, final Encoding E )
  {
    boolean ok = true;
    // Going from NONE to UTF_8, UTF_16BE, UTF_16LE or WIN_1252
    ByteBuffer bbuf = File_2_byte_buf( file_size );
    bbuf.flip();
    CharBuffer cbuf = Decode( bbuf, E );
    if( null == cbuf ) ok = false;
    else {
      Ptr_Boolean p_LF_at_EOF = new Ptr_Boolean( true );
      ArrayList<Line> n_lines = Char_buf_2_lines( cbuf, p_LF_at_EOF );
      Replace_current_file( n_lines, E, p_LF_at_EOF.val );
    }
    return ok;
  }

  // Convert the current from from a hex to a byte representation
  boolean HEX_to_BYTE()
  {
    ArrayList<Line> n_lines = new ArrayList<>();
    Ptr_Boolean p_LF_at_EOF = new Ptr_Boolean( true );

    boolean ok = HEX_to_BYTE_get_lines( n_lines, p_LF_at_EOF );
    if( ok )
    {
      Replace_current_file( n_lines, Encoding.BYTE, p_LF_at_EOF.val );
      Find_File_Type_Suffix();
    }
    return ok;
  }
  boolean HEX_to_BYTE_get_lines( ArrayList<Line> n_lines, Ptr_Boolean p_LF_at_EOF )
  {
    boolean ok = HEX_to_BYTE_check_format();
    if( ok )
    {
      Line n_line = new Line();

      for( int k=0; k<m_lines.size(); k++ )
      {
        final Line l = m_lines.get(k);
        final int LL = l.length();
        for( int i=0; i<LL; i+=3 )
        {
          final char C1 = l.charAt(i+1);
          final char C2 = l.charAt(i+2);
          final char C  = Utils.Hex_Chars_2_Byte( C1, C2 );

          if('\n' == C)
          {
            n_lines.add( n_line );
            n_line = new Line();
            p_LF_at_EOF.val = true;
          }
          else {
            n_line.append_c( C );
            p_LF_at_EOF.val = false;
          }
        }
      }
      if( 0 < n_line.length() ) n_lines.add( n_line );
    }
    return ok;
  }
  boolean HEX_to_BYTE_check_format()
  {
    boolean ok = true;

    final int NUM_LINES = m_lines.size();
    // Make sure lines are all 48 characters long, except last line,
    // which must be a multiple of 3 characters long
    for( int k=0; ok && k<NUM_LINES; k++ )
    {
      final Line l = m_lines.get(k);
      final int LL = l.length();
      if( k<(NUM_LINES-1) && LL != 48 )
      {
        ok = false;
        m_vis.Window_Message("Line: "+(k+1)+" is "+ LL +" characters, not 48");
      }
      else { // Last line
        if( 48 < LL || 0!=(LL%3) )
        {
          ok = false;
          m_vis.Window_Message("Line: "+(k+1)+" is "+ LL +" characters, not multiple of 3");
        }
      }
      // Make sure lines are: ' XX XX'
      for( int i=0; ok && i<LL; i+=3 )
      {
        final char C0 = l.charAt(i);
        final char C1 = l.charAt(i+1);
        final char C2 = l.charAt(i+2);

        if( C0 != ' ' )
        {
          ok = false;
          m_vis.Window_Message("Expected space on Line: "+(k+1)+" pos: "+ i+1);
        }
        else if( !Utils.IsHexDigit( C1 ) )
        {
          ok = false;
          m_vis.Window_Message("Expected hex digit on Line: "+(k+1)+" pos: "+ i+2 +" : "+ C1);
        }
        else if( !Utils.IsHexDigit( C2 ) )
        {
          ok = false;
          m_vis.Window_Message("Expected hex digit on Line: "+(k+1)+" pos: "+ i+3 +" : "+ C2);
        }
      }
    }
    return ok;
  }

  boolean UTF_or_WIN_1252_to_BYTE( final int file_size, final Encoding E )
  {
    ArrayList<Line> n_lines = new ArrayList<>();
    Ptr_Boolean p_LF_at_EOF = new Ptr_Boolean( true );

    boolean ok = UTF_or_WIN_1252_to_BYTE_get_lines( file_size, E, n_lines, p_LF_at_EOF );
    if( ok )
    {
      Replace_current_file( n_lines, Encoding.BYTE, p_LF_at_EOF.val );
    }
    return ok;
  }
  boolean UTF_or_WIN_1252_to_BYTE_get_lines( final int file_size
                                           , final Encoding E
                                           , ArrayList<Line> n_lines
                                           , Ptr_Boolean p_LF_at_EOF )
  {
    boolean ok = true;
    // Going from UTF_8, UTF_16BE, UTF_16LE or WIN_1252 to NONE
    CharBuffer cbuf = File_2_char_buf( file_size );
    cbuf.flip();
    ByteBuffer bbuf = Encode( Encoding_2_name(E), cbuf );
    if( null == bbuf ) ok = false;
    else {
      Byte_buf_2_lines( bbuf, p_LF_at_EOF, n_lines );
    }
    return ok;
  }

  // Puts current file into an allocated byte buffer.
  // returns the byte buffer.
  ByteBuffer File_2_byte_buf( final long file_size )
  {
    ByteBuffer bbuf = ByteBuffer.allocate( (int)file_size );
    final int NUM_LINES = m_lines.size();
    for( int k=0; k<NUM_LINES; k++ )
    {
      final Line l = m_lines.get(k);
      final int LL = l.length();
      for( int i=0; i<LL; i++ ) bbuf.put( (byte)l.charAt(i) );
      if( k<NUM_LINES-1 || m_LF_at_EOF ) bbuf.put((byte)'\n');
    }
    return bbuf;
  }

  // Puts current file into an allocated char buffer.
  // returns the char buffer.
  CharBuffer File_2_char_buf( final long file_size )
  {
    CharBuffer cbuf = CharBuffer.allocate( (int)file_size );
    final int NUM_LINES = m_lines.size();
    for( int k=0; k<NUM_LINES; k++ )
    {
      final Line l = m_lines.get(k);
      final int LL = l.length();
      for( int i=0; i<LL; i++ ) cbuf.put( l.charAt(i) );
      if( k<NUM_LINES-1 || m_LF_at_EOF ) cbuf.put('\n');
    }
    return cbuf;
  }

  // Decodes ByteBuffer bbuf according to Encoding enc into newly
  // allocated CharBuffer.
  // Returns CharBuffer if successfull, else null.
  CharBuffer Decode( ByteBuffer bbuf, final Encoding dec )
  {
    CharBuffer cbuf = null;
    CharsetDecoder decoder = null;

    if     ( dec == Encoding.UTF_8    ) decoder = Charset.forName("UTF-8").newDecoder();
    else if( dec == Encoding.UTF_16BE ) decoder = Charset.forName("UTF-16BE").newDecoder();
    else if( dec == Encoding.UTF_16LE ) decoder = Charset.forName("UTF-16LE").newDecoder();
    else if( dec == Encoding.WIN_1252 ) decoder = Charset.forName("windows-1252").newDecoder();
    else {
      // dec is NONE, so just put plain bytes into cbuf
      final int bbuf_size = bbuf.remaining();
      cbuf = CharBuffer.allocate( bbuf_size );
      for( int k=0; k<bbuf_size; k++ )
      {
        cbuf.put( (char)bbuf.get(k) );
      }
    }
    if( null != decoder )
    {
      try {
        cbuf = decoder.decode( bbuf );
      }
      catch( Exception e )
      {
        cbuf = null;
      }
    }
    return cbuf;
  }

  String Encoding_2_name( Encoding E )
  {
    String name = "Unknown";

    if     ( E == Encoding.BYTE    ) name = "Byte";
    else if( E == Encoding.HEX     ) name = "Hex";
    else if( E == Encoding.UTF_8   ) name = "UTF-8";
    else if( E == Encoding.UTF_16BE) name = "UTF-16BE";
    else if( E == Encoding.UTF_16LE) name = "UTF-16LE";
    else if( E == Encoding.WIN_1252) name = "windows-1252";

    return name;
  }
  // Encodes CharBuffer cbuf according to Charset name into newly
  // allocated ByteBuffer.
  // Returns ByteBuffer if successfull, else null.
  ByteBuffer Encode( String name, CharBuffer cbuf )
  {
    ByteBuffer bbuf = null;

    CharsetEncoder encoder = Charset.forName( name ).newEncoder();

    if( null != encoder )
    {
      try {
        // ByteBuffer bbuf is allocated here:
        bbuf = encoder.encode( cbuf );
      }
      catch( Exception e )
      {
        bbuf = null;
      }
    }
    return bbuf;
  }

  ArrayList<Line> Byte_buf_2_lines( ByteBuffer bbuf
                                  , Ptr_Boolean p_LF_at_EOF
                                  , ArrayList<Line> n_lines )
  {
    Line l_line = new Line();

    for( int k=0; k<bbuf.remaining(); k++ )
    {
      final byte B = bbuf.get(k);
      final char C = Utils.Byte_2_Char( B );

      if('\n' == C)
      {
        n_lines.add( l_line );
        l_line = new Line();
        p_LF_at_EOF.val = true;
      }
      else {
        l_line.append_c( C );
        p_LF_at_EOF.val = false;
      }
    }
    if( 0 < l_line.length() ) n_lines.add( l_line );

    return n_lines;
  }

  ArrayList<Line> Char_buf_2_lines( CharBuffer cbuf, Ptr_Boolean p_LF_at_EOF )
  {
    ArrayList<Line> n_lines = new ArrayList<>();

    Line l_line = new Line();

    for( int k=0; k<cbuf.remaining(); k++ )
    {
      final char C = cbuf.get(k);

      if('\n' == C)
      {
        n_lines.add( l_line );
        l_line = new Line();
        p_LF_at_EOF.val = true;
      }
      else {
        l_line.append_c( C );
        p_LF_at_EOF.val = false;
      }
    }
    if( 0 < l_line.length() ) n_lines.add( l_line );

    return n_lines;
  }

  void Replace_current_file( ArrayList<Line> n_lines
                           , final Encoding enc
                           , final boolean LF_at_EOF )
  {
    m_save_history = false;

    ClearLines();

    for( int k=0; k<n_lines.size(); k++ )
    {
      PushLine( n_lines.get(k) );
    }
    m_save_history = true;

    m_decoding = enc;
    m_encoding = enc;
    m_LF_at_EOF = LF_at_EOF;
  }
  void Comment()
  {
    if( Comment_CPP()
     || Comment_Script()
     || Comment_MIB() )
    {
      Update();
    }
  }
  boolean Comment_CPP()
  {
    boolean commented = false;

    if( File_Type.CS   == m_file_type
     || File_Type.CPP  == m_file_type
     || File_Type.IDL  == m_file_type
     || File_Type.JAVA == m_file_type
     || File_Type.JS   == m_file_type
     || File_Type.STL  == m_file_type )
    {
      final int NUM_LINES = NumLines();

      // Comment all lines:
      for( int k=0; k<NUM_LINES; k++ )
      {
        InsertChar( k, 0, '/' );
        InsertChar( k, 0, '/' );
      }
      commented = true;
    }
    return commented;
  }
  boolean Comment_Script()
  {
    boolean commented = false;

    if( File_Type.BASH  == m_file_type
     || File_Type.CMAKE == m_file_type
     || File_Type.MAKE  == m_file_type
     || File_Type.PY    == m_file_type )
    {
      final int NUM_LINES = NumLines();

      // Comment all lines:
      for( int k=0; k<NUM_LINES; k++ )
      {
        InsertChar( k, 0, '#' );
      }
      commented = true;
    }
    return commented;
  }
  boolean Comment_MIB()
  {
    boolean commented = false;

    if( File_Type.MIB == m_file_type
     || File_Type.SQL == m_file_type )
    {
      final int NUM_LINES = NumLines();

      // Comment all lines:
      for( int k=0; k<NUM_LINES; k++ )
      {
        InsertChar( k, 0, '-' );
        InsertChar( k, 0, '-' );
      }
      commented = true;
    }
    return commented;
  }
  void UnComment()
  {
    if( UnComment_CPP()
     || UnComment_Script()
     || UnComment_MIB() )
    {
      Update();
    }
  }
  boolean UnComment_CPP()
  {
    boolean uncommented = false;

    if( File_Type.CS   == m_file_type
     || File_Type.CPP  == m_file_type
     || File_Type.IDL  == m_file_type
     || File_Type.JAVA == m_file_type
     || File_Type.JS   == m_file_type
     || File_Type.STL  == m_file_type )
    {
      boolean all_lines_commented = true;

      final int NUM_LINES = NumLines();

      // Determine if all lines are commented:
      for( int k=0; all_lines_commented && k<NUM_LINES; k++ )
      {
        Line l_k = GetLine( k );

        if( (l_k.length() < 2)
         || ('/' != l_k.charAt( 0 ))
         || ('/' != l_k.charAt( 1 )) )
        {
          all_lines_commented = false;
        }
      }
      // Un-Comment all lines:
      if( all_lines_commented )
      {
        for( int k=0; k<NUM_LINES; k++ )
        {
          RemoveChar( k, 0 );
          RemoveChar( k, 0 );
        }
        uncommented = true;
      }
    }
    return uncommented;
  }
  boolean UnComment_Script()
  {
    boolean uncommented = false;

    if( File_Type.BASH  == m_file_type
     || File_Type.CMAKE == m_file_type
     || File_Type.MAKE  == m_file_type
     || File_Type.PY    == m_file_type )
    {
      boolean all_lines_commented = true;

      final int NUM_LINES = NumLines();

      // Determine if all lines are commented:
      for( int k=0; all_lines_commented && k<NUM_LINES; k++ )
      {
        Line l_k = GetLine( k );

        if( (l_k.length() < 1)
         || ('#' != l_k.charAt( 0 )) )
        {
          all_lines_commented = false;
        }
      }
      // Un-Comment all lines:
      if( all_lines_commented )
      {
        for( int k=0; k<NUM_LINES; k++ )
        {
          RemoveChar( k, 0 );
        }
        uncommented = true;
      }
    }
    return uncommented;
  }
  boolean UnComment_MIB()
  {
    boolean uncommented = false;

    if( File_Type.MIB == m_file_type
     || File_Type.SQL == m_file_type )
    {
      boolean all_lines_commented = true;

      final int NUM_LINES = NumLines();

      // Determine if all lines are commented:
      for( int k=0; all_lines_commented && k<NUM_LINES; k++ )
      {
        Line l_k = GetLine( k );

        if( (l_k.length() < 2)
         || ('-' != l_k.charAt( 0 ))
         || ('-' != l_k.charAt( 1 )) )
        {
          all_lines_commented = false;
        }
      }
      // Un-Comment all lines:
      if( all_lines_commented )
      {
        for( int k=0; k<NUM_LINES; k++ )
        {
          RemoveChar( k, 0 );
          RemoveChar( k, 0 );
        }
        uncommented = true;
      }
    }
    return uncommented;
  }
  void Create_Image()
  {
    if( null == m_image )
    {
      String url = "file:" + m_pname;
      m_image = new Image( url );
    }
  }
  VisIF                 m_vis;   // Not sure if we need this or should use m_views
  final String          m_pname; // Full path      = m_dname + m_fname
  final String          m_dname; // Full directory = m_pname - m_fname, (for directories this is the same a m_pname)
  final String          m_fname; // Filename       = m_pname - m_dname, (for directories this is empty)
  final boolean         m_isDir;     // Directory
  final boolean         m_isRegular; // Regular File
  static boolean        m_found_BE;
  final Path            m_path;
  ArrayList<Line>       m_lines        = new ArrayList<>();
  ArrayList<Line>       m_styles       = new ArrayList<>();
  ArrayList<Integer>    m_lineOffsets  = new ArrayList<>();
  ArrayList<View>       m_views        = new ArrayList<>(); // MAX_WINS views
  LineView              m_line_view;
  ChangeHist            m_history      = new ChangeHist( this );
  boolean               m_LF_at_EOF    = true;
  File_Type             m_file_type    = File_Type.UNKNOWN;
  Encoding              m_decoding     = Encoding.BYTE;
  Encoding              m_encoding     = Encoding.BYTE;
  Highlight_Base        m_Hi;
  long                  m_mod_time; // modification time
  long                  m_foc_time; // focus time
  boolean               m_changed_externally;
  boolean               m_need_2_clear_stars = false;
  boolean               m_need_2_find_stars  = true;
  private final boolean m_mutable;
  private boolean       m_save_history       = false;
  private long          m_mod_check_time     = 0;
  int                   m_hi_touched_line; // Line before which highlighting is valid
  String                m_regex;
  Pattern               m_pattern; // Will hold compiled regex pattern
  ArrayList<Boolean>    m_lineRegexsValid = new ArrayList<>();
  Line                  m_line_buf = new Line();
  Image                 m_image;
}

