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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.DirectoryStream;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;

class FileBuf
{
  // fname should contain full path and filename
  FileBuf( Vis vis, String fname, final boolean mutable )
  {
    m_vis       = vis;
    m_path      = FileSystems.getDefault().getPath( fname );
    m_isDir     = Files.isDirectory( m_path );
    m_mutable   = m_isDir ? false : mutable;

    if( m_isDir ) m_pname = fname;
    else {
      Path parent = m_path.getParent();
      if( null != parent ) m_pname = parent.normalize().toString();
      else {
        // fname passed in was not full path filename, so use process path
        m_pname = Utils.GetCWD();
          fname = m_pname + fname;
      }
    }
    m_fname = fname;
    m_hname = Utils.FnameHead( m_fname );

    Find_File_Type_Suffix();
  }
  // fname should contain full path and filename
  FileBuf( Vis vis, String fname, FileBuf fb )
  {
    m_vis       = vis;
    m_path      = FileSystems.getDefault().getPath( fname );
    m_LF_at_EOF = fb.m_LF_at_EOF;
    m_mod_time  = Utils.ModificationTime( m_path );
    m_isDir     = Files.isDirectory( m_path );
    m_mutable   = m_isDir ? false : true;

    if( m_isDir ) m_pname = fname;
    else {
      Path parent = m_path.getParent();
      if( null != parent ) m_pname = parent.normalize().toString();
      else {
        // fname passed in was not full path filename, so use process path
        m_pname = Utils.GetCWD();
          fname = m_pname + fname;
      }
    }
    m_fname = fname;
    m_hname = Utils.FnameHead( m_fname );

    Find_File_Type_Suffix();

    // Add copies of m_lines, m_styles and m_lineOffsets:
    final int FB_NUM_LINES   = fb.m_lines.size();
    final int FB_NUM_OFFSETS = fb.m_lineOffsets.size(); //< Should be the same as FB_NUM_LINES

    m_lines      .ensureCapacity( FB_NUM_LINES );
    m_styles     .ensureCapacity( FB_NUM_LINES );
    m_lineOffsets.ensureCapacity( FB_NUM_OFFSETS );

    for( int k=0; k<FB_NUM_LINES; k++ )
    {
      m_lines .add( new Line( fb.m_lines .get( k ) ) );
      m_styles.add( new Line( fb.m_styles.get( k ) ) );
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
      // Directories default to TEXT
      m_file_type = File_Type.TEXT;
      m_Hi = new Highlight_Text( this );
    }
    else {
      if( Find_File_Type_Bash()
       || Find_File_Type_CMAKE()
       || Find_File_Type_CPP ()
       || Find_File_Type_IDL ()
       || Find_File_Type_Java()
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
  }
  boolean Find_File_Type_Bash()
  {
    if( m_fname.endsWith(".sh"      )
     || m_fname.endsWith(".sh.new"  )
     || m_fname.endsWith(".sh.old"  )
     || m_fname.endsWith(".bash"    )
     || m_fname.endsWith(".bash.new")
     || m_fname.endsWith(".bash.old")
     || m_hname.startsWith(".alias")
     || m_hname.startsWith(".bash_profile")
     || m_hname.startsWith(".bash_logout")
     || m_hname.startsWith(".bashrc")
     || m_hname.startsWith(".profile") )
    {
      m_file_type = File_Type.BASH;
      m_Hi = new Highlight_Bash( this );
      return true;
    }
    return false;
  }
  boolean Find_File_Type_CPP()
  {
    if( m_fname.endsWith(".h"      )
     || m_fname.endsWith(".h.new"  )
     || m_fname.endsWith(".h.old"  )
     || m_fname.endsWith(".c"      )
     || m_fname.endsWith(".c.new"  )
     || m_fname.endsWith(".c.old"  )
     || m_fname.endsWith(".hh"     )
     || m_fname.endsWith(".hh.new" )
     || m_fname.endsWith(".hh.old" )
     || m_fname.endsWith(".cc"     )
     || m_fname.endsWith(".cc.new" )
     || m_fname.endsWith(".cc.old" )
     || m_fname.endsWith(".hpp"    )
     || m_fname.endsWith(".hpp.new")
     || m_fname.endsWith(".hpp.old")
     || m_fname.endsWith(".cpp"    )
     || m_fname.endsWith(".cpp.new")
     || m_fname.endsWith(".cpp.old")
     || m_fname.endsWith(".cxx"    )
     || m_fname.endsWith(".cxx.new")
     || m_fname.endsWith(".cxx.old") )
    {
      m_file_type = File_Type.CPP;
      m_Hi = new Highlight_CPP( this );
      return true;
    }
    return false;
  }
  boolean Find_File_Type_IDL()
  {
    if( m_fname.endsWith(".idl"    )
     || m_fname.endsWith(".idl.new")
     || m_fname.endsWith(".idl.old") )
    {
      m_file_type = File_Type.IDL;
      m_Hi = new Highlight_IDL( this );
      return true;
    }
    return false;
  }
  boolean Find_File_Type_Java()
  {
    if( m_fname.endsWith(".java"    )
     || m_fname.endsWith(".java.new")
     || m_fname.endsWith(".java.old") )
    {
      m_file_type = File_Type.JAVA;
      m_Hi = new Highlight_Java( this );
      return true;
    }
    return false;
  }
  boolean Find_File_Type_STL()
  {
    if( m_fname.endsWith(".stl"    )
     || m_fname.endsWith(".stl.new")
     || m_fname.endsWith(".stl.old")
     || m_fname.endsWith(".ste"    )
     || m_fname.endsWith(".ste.new")
     || m_fname.endsWith(".ste.old") )
    {
      m_file_type = File_Type.STL;
      m_Hi = new Highlight_STL( this );
      return true;
    }
    return false;
  }
  boolean Find_File_Type_SQL()
  {
    if( m_fname.endsWith(".sql"    )
     || m_fname.endsWith(".sql.new")
     || m_fname.endsWith(".sql.old") )
    {
      m_file_type = File_Type.SQL;
      m_Hi = new Highlight_SQL( this );
      return true;
    }
    return false;
  }
  boolean Find_File_Type_XML()
  {
    if( m_fname.endsWith(".xml"    )
     || m_fname.endsWith(".xml.new")
     || m_fname.endsWith(".xml.old") )
    {
      m_file_type = File_Type.XML;
      m_Hi = new Highlight_XML( this );
      return true;
    }
    return false;
  }
  boolean Find_File_Type_CMAKE()
  {
    if( m_fname.endsWith(".cmake"    )
     || m_fname.endsWith(".cmake.new")
     || m_fname.endsWith(".cmake.old")
     || m_fname.endsWith(".cmake"    )
     || m_fname.endsWith(".cmake.new")
     || m_fname.endsWith(".cmake.old")
     || m_fname.equals("CMakeLists.txt")
     || m_fname.equals("CMakeLists.txt.old")
     || m_fname.equals("CMakeLists.txt.new") )
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
      else if( syn.equals("idl") )
      {
        m_file_type = File_Type.IDL;
        m_Hi = new Highlight_IDL( this );
      }
      else if( syn.equals("java") )
      {
        m_file_type = File_Type.JAVA;
        m_Hi = new Highlight_Java( this );
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
    long elapsed_time = System.currentTimeMillis() - m_mod_check_time;

    if( 1000 <= elapsed_time )
    {
      final long curr_mod_time = Utils.ModificationTime( m_path );

      if( m_mod_time < curr_mod_time )
      {
        if( m_isDir )
        {
          // Dont ask the user, just read in the directory.
          // m_mod_time will get updated in ReReadFile()
          ReReadFile();

          Update();
        }
        else { // Regular file
          // Update file modification time so that the message window
          // will not keep popping up:
          m_mod_time = curr_mod_time;

          m_vis.Window_Message("\n"+ m_fname +"\n\nhas changed since it was read in\n\n");
        }
      }
    }
  }

  String Relative_2_FullFname( final String relative_fname )
  {
    Path fpath;

    if( Files.isDirectory( m_path ) )
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
    ClearLines();

    m_save_history = false; //< Gets turned back on in ReadFile()

    boolean ok = ReadFile();

    // To be safe, put cursor at top,left of each view of this file:
    for( int w=0; ok && w<Vis.MAX_WINS; w++ )
    {
      View v = m_views.get( w );

      v.Clear_Context();
    }
    return ok;
  }
  private boolean ReadFile_p() throws FileNotFoundException, IOException
  {
    if( Files.isDirectory( m_path ) )
    {
      ReadExistingDir();
      m_mod_time = Utils.ModificationTime( m_path );
    }
    else {
      if( Files.isRegularFile( m_path ) )
      {
        ReadExistingFile();
        m_mod_time = Utils.ModificationTime( m_path );
      }
      else {
        // File does not exist, so add an empty line:
        PushLine();
        // File has not been written, so cant get m_mod_time
      }
    }
    if( m_mutable ) m_save_history = true;

    return true;
  }
  void ReadExistingDir() throws IOException
  {
    ArrayList<String> files = new ArrayList<>();
    ArrayList<String> dirs  = new ArrayList<>();

    // Java DirectoryStream does not have parent directory, so add it:
    if( 1<m_fname.length() ) dirs.add(".."); // Dont add '..' to '/'

    DirectoryStream<Path> dir_paths = Files.newDirectoryStream( m_path );

    for( Path path : dir_paths )
    {
      String fname = path.getFileName().toString();

      if( Files.isDirectory( path ) )
      {
        if( false == fname.endsWith( Utils.DIR_DELIM_STR ) )
        {
          fname += Utils.DIR_DELIM_STR;
        }
        dirs.add( fname );
      }
      else {
        files.add( fname );
      }
    }
    dir_paths.close();

    Collections.sort( files );
    Collections.sort( dirs );

    for( String d : dirs  ) PushLine( d );
    for( String f : files ) PushLine( f );
  }
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
    try {
      return Write_p();
    }
    catch( Exception e )
    {
      m_vis.Window_Message( e.toString() );
    }
    return false;
  }
  private boolean Write_p() throws FileNotFoundException, IOException
  {
    File outfile = m_path.toFile();

    FileOutputStream     fos = new FileOutputStream( outfile );
    BufferedOutputStream bos = new BufferedOutputStream( fos, 512 );

    final int NUM_LINES = m_lines.size();

    for( int k=0; k<NUM_LINES; k++ )
    {
      final int LL = m_lines.get(k).length();
      for( int i=0; i<LL; i++ )
      {
        final int c = m_lines.get(k).charAt(i);
        bos.write( c );
      }
      if( k<NUM_LINES-1 || m_LF_at_EOF )
      {
        bos.write( '\n' );
      }
    }
    bos.flush();
    fos.close();

    m_history.Clear();
    m_mod_time = Utils.ModificationTime( m_path );

    // Wrote to file message:
    m_vis.CmdLineMessage("\""+ m_fname +"\" written" );

    return true;
  }

  void ClearLines()
  {
    m_lines      .clear();
    m_styles     .clear();
    m_lineOffsets.clear();
    m_history    .Clear();
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
    }
    else {
      while( line_num < m_lineOffsets.size() )
      {
        m_lineOffsets.remove( m_lineOffsets.size()-1 );
      }
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
  // Returns file size and fills in m_lineOffsets if needed
  int GetSize()
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
  int GetCursorByte( int CL, int CC )
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
        while( m_lineOffsets.size() < CL+1 ) m_lineOffsets.add( 0 );

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

  // Find styles up to but not including up_to_line number
  void Find_Styles( final int up_to_line )
  {
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

    ClearStars_In_Range( st, fn );
    Find_Stars_In_Range( st, fn );
  }

  // Find stars starting on st up to but not including fn line
  void Find_Stars_In_Range( final CrsPos st, final int fn )
  {
    final String  star_str  = m_vis.m_star;
    final int     STAR_LEN  = m_vis.m_star.length();
    final boolean SLASH     = m_vis.m_slash;
    final int     NUM_LINES = NumLines();

    for( int l=st.crsLine; 0<STAR_LEN && l<NUM_LINES && l<fn; l++ )
    {
      Line lp = m_lines.get( l );
      final int LL = lp.length();
      if( LL<STAR_LEN ) continue;

      final int st_pos = st.crsLine==l ? st.crsChar : 0;

      for( int p=st_pos; p<LL; p++ )
      {
        boolean matches = SLASH || Utils.line_start_or_prev_C_non_ident( lp, p );
        for( int k=0; matches && (p+k)<LL && k<STAR_LEN; k++ )
        {
          if( star_str.charAt(k) != lp.charAt(p+k) ) matches = false;
          else {
            if( k+1 == STAR_LEN ) // Found pattern
            {
              matches = SLASH || Utils.line_end_or_non_ident( lp, LL, p+k );
              if( matches ) {
                for( int m=p; m<p+STAR_LEN; m++ ) SetStarStyle( l, m );
                // Increment p one less than STAR_LEN, because p
                // will be incremented again by the for loop
                p += STAR_LEN-1;
              }
            }
          }
        }
      }
    }
  }

  Line GetLine( final int l_num )
  {
    return m_lines.get( l_num );
  }
  Line GetStyle( final int l_num )
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

  void BufferEditor_Sort()
  {
    final int NUM_BUILT_IN_FILES = 5;
    final int FNAME_START_CHAR   = 0;
 
    // Sort lines (file names), least to greatest:
    for( int i=NumLines()-1; NUM_BUILT_IN_FILES<i; i-- )
    {
      for( int k=NUM_BUILT_IN_FILES; k<i; k++ )
      {
        Line l_0 = m_lines.get( k   );
        Line l_1 = m_lines.get( k+1 );

        // This should never be false, but check just in case.
        // Out of order BUFFER_EDITOR is better than crashing.
        if( FNAME_START_CHAR<l_0.length()
         && FNAME_START_CHAR<l_1.length() )
        {
          String fn_0 = l_0.toStr().substring( FNAME_START_CHAR );
          String fn_1 = l_1.toStr().substring( FNAME_START_CHAR );

          // This if statement is not needed, but keep it in for now:
          if( 0<fn_0.length() && 0<fn_1.length() )
          {
            if( 0<fn_0.compareTo( fn_1 ) )
            {
              SwapLines( k, k+1 );
            }
          }
        }
      }
    }
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
    if( m_vis.m_console.m_get_from_dot_buf ) return;

    m_vis.Update_Change_Statuses();

    for( int w=0; w<Vis.MAX_WINS; w++ )
    {
      View rV = m_views.get( w );

      for( int w2=0; w2<m_vis.m_num_wins; w2++ )
      {
        if( rV == m_vis.GetWinView( w2, 0 ) )
        {
          // rV is a view of this file currently displayed, perform needed update:
          rV.Update();
        }
      }
    }
    // Put cursor back into current window
    m_vis.CV().PrintCursor();
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

    if( SavingHist() ) m_history.Save_RemoveChar( l_num, c_num, C );

    m_hi_touched_line = Math.min( m_hi_touched_line, l_num );

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
      m_hi_touched_line = Math.min( m_hi_touched_line, l_num );
    }
  }
  // Remove a line from FileBuf without deleting it and return pointer to it.
  // Caller of RemoveLine is responsible for deleting line returned.
  //
  Line RemoveLine( final int l_num )
  {
    Line lr = m_lines.remove( l_num );
    Line sr = m_styles.remove( l_num );

    ChangedLine( l_num );

    if( SavingHist() ) m_history.Save_RemoveLine( l_num, lr );

    m_hi_touched_line = Math.min( m_hi_touched_line, l_num );

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

    ChangedLine( l_num );

    if( SavingHist() ) m_history.Save_InsertLine( l_num );

    m_hi_touched_line = Math.min( m_hi_touched_line, l_num );

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

    ChangedLine( l_num );

    if( SavingHist() ) m_history.Save_InsertLine( l_num );

    m_hi_touched_line = Math.min( m_hi_touched_line, l_num );

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

    ChangedLine( l_num );

    if( SavingHist() ) m_history.Save_InsertChar( l_num, c_num );
    // Add to the m_updates list:

    m_hi_touched_line = Math.min( m_hi_touched_line, l_num );
  }

  // Append line to end of line l_num
  //
  void AppendLineToLine( final int l_num, final Line line )
  {
    Line lr =  m_lines.get( l_num );
    Line sr = m_styles.get( l_num );

    lr.append_l( line );

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
    m_hi_touched_line = Math.min( m_hi_touched_line, l_num );
  }
  void AppendLineToLine( final int l_num, final String s )
  {
    AppendLineToLine( l_num, new Line( s ) );
  }

  void InsertLine_Adjust_Views_topLines( final int l_num )
  {
    for( int w=0; w<Vis.MAX_WINS && w<m_views.size(); w++ )
    {
      final View rV = m_views.get( w );

      rV.InsertedLine_Adjust_TopLine( l_num );
    }
  }
  void RemovedLine_Adjust_Views_topLines( final int l_num )
  {
    for( int w=0; w<Vis.MAX_WINS && w<m_views.size(); w++ )
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

    ChangedLine( l_num );

    if( SavingHist() ) m_history.Save_InsertChar( l_num, lr.length()-1 );

    m_hi_touched_line = Math.min( m_hi_touched_line, l_num );
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

    boolean ok = m_lines.add( lr ) && m_styles.add( sr );

    if( SavingHist() ) m_history.Save_InsertLine( m_lines.size()-1 );
  }

  // Add a new empty line at the end of FileBuf
  //
  void PushLine()
  {
    Line lr = new Line();
    Line sr = new Line();

    boolean ok = m_lines.add( lr ) && m_styles.add( sr );

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
  void ClearStars()
  {
    if( m_need_2_clear_stars )
    {
      final int NUM_LINES = m_styles.size();

      for( int l=0; l<NUM_LINES; l++ )
      {
        Line s_line = m_styles.get( l );
        final int LL = s_line.length();

        for( int p=0; p<LL; p++ )
        {
          char s = s_line.charAt( p );
          s &= ~Highlight_Type.STAR.val;
          s_line.setCharAt( p, s );
        }
      }
      m_need_2_clear_stars = false;
    }
  }
  // Clear stars starting on st up to but not including fn line
  void ClearStars_In_Range( final CrsPos st, final int fn )
  {
    final int NUM_LINES = m_styles.size();

    for( int l=st.crsLine; l<NUM_LINES && l<fn; l++ )
    {
      Line s_line = m_styles.get( l );
      final int LL = s_line.length();
      final int st_pos = st.crsLine==l ? st.crsChar : 0;

      for( int p=st_pos; p<LL; p++ )
      {
        char s = s_line.charAt( p );
        s &= ~Highlight_Type.STAR.val;
        s_line.setCharAt( p, s );
      }
    }
  }

  void Find_Stars()
  {
    if( !m_need_2_find_stars ) return;

    final String  star_str  = m_vis.m_star;
    final int     STAR_LEN  = m_vis.m_star.length();
    final boolean SLASH     = m_vis.m_slash;
    final int     NUM_LINES = NumLines();

    for( int l=0; 0<STAR_LEN && l<NUM_LINES; l++ )
    {
      Line lr = m_lines.get( l );
      final int LL = lr.length();
      if( LL<STAR_LEN ) continue;

      for( int p=0; p<LL; p++ )
      {
        boolean matches = SLASH || Utils.line_start_or_prev_C_non_ident( lr, p );
        for( int k=0; matches && (p+k)<LL && k<STAR_LEN; k++ )
        {
          if( star_str.charAt(k) != lr.charAt(p+k) ) matches = false;
          else {
            if( k+1 == STAR_LEN ) // Found pattern
            {
              matches = SLASH || Utils.line_end_or_non_ident( lr, LL, p+k );
              if( matches ) {
                for( int m=p; m<p+STAR_LEN; m++ ) SetStarStyle( l, m );
                // Increment p one less than STAR_LEN, because p
                // will be incremented again by the for loop
                p += STAR_LEN-1;
              }
            }
          }
        }
      }
    }
    m_need_2_find_stars = false;
  }
  Vis                   m_vis;   // Not sure if we need this or should use m_views
  final String          m_fname; // Full path and filename head = m_pname + m_hname
  final String          m_pname; // Full path     = m_fname - m_hname, (for directories this is the same a m_fname)
  final String          m_hname; // Filename head = m_fname - m_pname, (for directories this is empty)
  final Boolean         m_isDir;
  private final Path    m_path;
  ArrayList<Line>       m_lines        = new ArrayList<>();
  ArrayList<Line>       m_styles       = new ArrayList<>();
  ArrayList<Integer>    m_lineOffsets  = new ArrayList<>();
  ArrayList<View>       m_views        = new ArrayList<>(); // MAX_WINS views
  ChangeHist            m_history      = new ChangeHist( this );
  boolean               m_LF_at_EOF    = true;
  File_Type             m_file_type    = File_Type.UNKNOWN;
  Highlight_Base        m_Hi;
  long                  m_mod_time;
  boolean               m_need_2_clear_stars = false;
  boolean               m_need_2_find_stars  = true;
  private final boolean m_mutable;
  private boolean       m_save_history       = false;
  private long          m_mod_check_time     = 0;
  int                   m_hi_touched_line; // Line before which highlighting is valid
}

