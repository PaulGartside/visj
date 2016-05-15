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

import java.nio.file.Files;
import java.nio.file.Paths;

// Vis helper class to handle getting colon commands:
//
class Colon
{
  Colon( Vis vis )
  {
    m_vis     = vis;
    m_console = m_vis.m_console;
    m_sb      = m_vis.m_sb;
  }

  void Run( View V )
  {
    m_cv = V;
    m_sb.setLength( 0 );
    if( m_vis.m_diff_mode ) m_vis.m_diff.Clear_Console_CrsCell();
    else                            m_cv.Clear_Console_CrsCell();
    m_cv.GoToCmdLineClear(":");
    m_msg_len = 1; // ":"
    m_vis.m_states.addFirst( m_run_colon );

    Reset_File_Name_Completion_Variables();
  }
  void Run_GetCoverKey( View V )
  {
    m_cv = V;
    m_sb.setLength( 0 );
    if( m_vis.m_diff_mode ) m_vis.m_diff.Clear_Console_CrsCell();
    else                            m_cv.Clear_Console_CrsCell();
    m_cv.GoToCmdLineClear("Enter cover key:");
    m_msg_len = 16; // "Enter cover key:"

    m_vis.m_states.addFirst( m_run_get_cover_key );
  }

  void Reset_File_Name_Completion_Variables()
  {
    m_fb           = null;
    m_file_index   = 0;
    m_partial_path = "";
    m_search__head = "";
    m_colon_op     = ColonOp.unknown;
  }

  void run_colon()
  {
    if( 0<m_console.KeysIn() )
    {
      final char C = m_console.GetKey();

      if( Utils.IsEndOfLineDelim( C ) )
      {
        final int WC    = m_cv.WorkingCols();
        final int G_COL = m_cv.Col_Win_2_GL( Math.min( m_sb.length(), WC-2 ) );
        final int G_ROW = m_cv.Cmd__Line_Row();
        // Replace last typed char with space:
        m_console.Set( G_ROW, G_COL+m_msg_len, ' ', Style.NORMAL );

        m_vis.Exe_Colon_Cmd();
      //m_vis.m_states.removeFirst(); //< Drop out of m_run_colon
      }
      else {
        if( '\t' == C )
        {
          HandleTab();
        }
        else {
          Reset_File_Name_Completion_Variables();

          if( Console.BS == C || Console.DEL == C )
          {
            HandleDelete();
          }
          else {
            HandleNormal( C, false );
          }
        }
        m_console.Update();
      }
    }
  }

  void run_get_cover_key()
  {
    if( 0<m_console.KeysIn() )
    {
      final char C = m_console.GetKey();

      if( Utils.IsEndOfLineDelim( C ) )
      {
        final int G_COL = m_cv.Col_Win_2_GL( m_sb.length() );
        final int G_ROW = m_cv.Cmd__Line_Row();
        // Replace last typed char with space:
        m_console.Set( G_ROW, G_COL+m_msg_len, ' ', Style.NORMAL );

        m_vis.m_cover_key = m_sb.toString();
        m_vis.m_states.removeFirst(); //< Drop out of m_run_get_cover_key
        m_cv.PrintCursor();
      }
      else {
        if( Console.BS == C || Console.DEL == C )
        {
          HandleDelete();
        }
        else {
          HandleNormal( C, true );
        }
      }
      m_console.Update();
    }
  }

//void HandleNormal( final char C, final boolean HIDE )
//{
//  m_sb.append( C );
//
//  final int WC        = m_cv.WorkingCols();
//  final int local_COL = Math.min( m_sb.length()+m_msg_len-1, WC-2 );
//  final int     G_COL = m_cv.Col_Win_2_GL( local_COL );
//  final int     G_ROW = m_cv.Cmd__Line_Row();
//
//  final char cd = (HIDE ? '*' : C); // char displayed
//  // Output cd and move cursor forward:
//  m_console.Set( G_ROW, G_COL  , cd , Style.NORMAL );
//  m_console.Set( G_ROW, G_COL+1, ' ', Style.CURSOR );
//}
//  void HandleNormal( final char C, final boolean HIDE )
//  {
//Utils.Log("C="+C+", m_sb.length()="+m_sb.length()+"m_sb="+ m_sb.toString() );
//    if( !HIDE && m_sb.toString().equals("e ") && '~'==C )
//    {
//      String home = System.getenv("HOME");
//      m_sb.append( home );
//      if( '/' != m_sb.charAt( m_sb.length()-1 ) ) m_sb.append('/');
//
//      final int WC = m_cv.WorkingCols();
//      for( int k=3; k<=m_sb.length(); k++ )
//      {
//        final int local_COL = Math.min( k+m_msg_len-1, WC-2 );
//        final int     G_COL = m_cv.Col_Win_2_GL( local_COL );
//        final int     G_ROW = m_cv.Cmd__Line_Row();
//
//        final char cd = m_sb.charAt( k-1 );
//        // Output cd and move cursor forward:
//        m_console.Set( G_ROW, G_COL  , cd , Style.NORMAL );
//        m_console.Set( G_ROW, G_COL+1, ' ', Style.CURSOR );
//      }
//    }
//    else {
//      m_sb.append( C );
//
//      final int WC        = m_cv.WorkingCols();
//      final int local_COL = Math.min( m_sb.length()+m_msg_len-1, WC-2 );
//      final int     G_COL = m_cv.Col_Win_2_GL( local_COL );
//      final int     G_ROW = m_cv.Cmd__Line_Row();
//
//      final char cd = (HIDE ? '*' : C); // char displayed
//      // Output cd and move cursor forward:
//      m_console.Set( G_ROW, G_COL  , cd , Style.NORMAL );
//      m_console.Set( G_ROW, G_COL+1, ' ', Style.CURSOR );
//    }
//  }
  void HandleNormal( final char C, final boolean HIDE )
  {
    if( !HIDE && m_sb.toString().equals("e ~") && '/'==C )
    {
      String home = System.getenv("HOME");
      String s = m_sb.toString();
      String s2 = s.replace( "~", home );
      m_vis.m_sb = m_sb = new StringBuilder( s2 );
      if( '/' != m_sb.charAt( m_sb.length()-1 ) ) m_sb.append('/');

      final int WC = m_cv.WorkingCols();
      for( int k=3; k<=m_sb.length(); k++ )
      {
        final int local_COL = Math.min( k+m_msg_len-1, WC-2 );
        final int     G_COL = m_cv.Col_Win_2_GL( local_COL );
        final int     G_ROW = m_cv.Cmd__Line_Row();

        final char cd = m_sb.charAt( k-1 );
        // Output cd and move cursor forward:
        m_console.Set( G_ROW, G_COL  , cd , Style.NORMAL );
        m_console.Set( G_ROW, G_COL+1, ' ', Style.CURSOR );
      }
    }
    else {
      m_sb.append( C );

      final int WC        = m_cv.WorkingCols();
      final int local_COL = Math.min( m_sb.length()+m_msg_len-1, WC-2 );
      final int     G_COL = m_cv.Col_Win_2_GL( local_COL );
      final int     G_ROW = m_cv.Cmd__Line_Row();

      final char cd = (HIDE ? '*' : C); // char displayed
      // Output cd and move cursor forward:
      m_console.Set( G_ROW, G_COL  , cd , Style.NORMAL );
      m_console.Set( G_ROW, G_COL+1, ' ', Style.CURSOR );
    }
  }

  void HandleDelete()
  {
    if( 0<m_sb.length() )
    {
      final int G_COL = m_cv.Col_Win_2_GL( m_sb.length() );
      final int G_ROW = m_cv.Cmd__Line_Row();

      // Move cursor backwards:
      // Replace last typed char with space:
      m_console.Set( G_ROW, G_COL+1, ' ', Style.NORMAL );
      // Move back onto new space:
      m_console.Set( G_ROW, G_COL  , ' ', Style.CURSOR );
      m_sb.deleteCharAt( m_sb.length()-1 );
    }
  }

  void HandleTab()
  {
    boolean found_tab_fname = false;

    if( null == m_fb )
    {
      // First consecutive tab pressed:
      found_tab_fname = HandleTab_Find_File_Name_Completion_Variables();
    }
    else {
      // Subsequent consecutive tab pressed:
      found_tab_fname = HandleTab_Have_File_Name_Completion_Variables();
    }
    if( found_tab_fname )
    {
      Display_Cmd_Line();
    }
    else {
      // If we fall through, just treat tab like a space:
      HandleNormal(' ', false);
    }
  }
  // Returns true if found tab filename, else false
  boolean HandleTab_Find_File_Name_Completion_Variables()
  {
    boolean found_tab_fname = false;

    Utils.Trim( m_sb ); // Remove leading and trailing white space

    if( m_sb.toString().startsWith("e ")
     || m_sb.toString().equals("e") )
    {
      m_colon_op = ColonOp.e;
    }
    else if( m_sb.toString().startsWith("w ")
          || m_sb.toString().equals("w") )
    {
      m_colon_op = ColonOp.w;
    }

    if( ColonOp.e == m_colon_op
     || ColonOp.w == m_colon_op )
    {
      m_sb.deleteCharAt(0); Utils.Trim_Beg( m_sb ); // Remove initial 'e' and space after 'e'

      if( FindFileBuf() )
      {
        // Have FileBuf, so add matching file names to tab_fnames
        for( int k=0; !found_tab_fname && k<m_fb.NumLines(); k++ )
        {
          Line fname = m_fb.GetLine( k );

          if( fname.toStr().startsWith( m_search__head ) )
          {
            found_tab_fname = true;
            m_file_index    = k;
            m_sb.setLength( 0 );
            m_sb.append( m_partial_path );
            if( 0<m_sb.length() && !m_sb.toString().equals("/") )
            {
              m_sb.append( Utils.DIR_DELIM ); // Dont append '/' if no m_partial_path
            }
            m_sb.append( fname.toStr() );
          }
        }
      }
      // Removed "e" above, so add it back here:
      if( ColonOp.e == m_colon_op ) m_sb.insert( 0, "e ");
      else                          m_sb.insert( 0, "w ");
    }
    return found_tab_fname;
  }
  // Returns true if found tab filename, else false
  boolean HandleTab_Have_File_Name_Completion_Variables()
  {
    boolean found_tab_fname = false;

    // Already have a FileBuf, just search for next matching filename:
    for( int k=m_file_index+1
       ; !found_tab_fname && k<m_file_index+m_fb.NumLines(); k++ )
    {
      Line fname = m_fb.GetLine( k % m_fb.NumLines() );

      if( fname.toStr().startsWith( m_search__head ) )
      {
        found_tab_fname = true;
        m_file_index    = k;
        m_sb.setLength( 0 );
        m_sb.append( m_partial_path );
        if( 0<m_sb.length() && !m_sb.toString().equals("/") )
        {
          m_sb.append( Utils.DIR_DELIM ); // Done append '/' if no m_partial_path
        }
        m_sb.append( fname.toStr() );
      //m_sb.insert( 0, "e " );
        if( ColonOp.e == m_colon_op ) m_sb.insert( 0, "e ");
        else                          m_sb.insert( 0, "w ");
      }
    }
    return found_tab_fname;
  }

  void Display_Cmd_Line()
  {
    // Display buffer on command line:
    final int ROW = m_cv.Cmd__Line_Row();
    final int ST  = m_cv.Col_Win_2_GL( 0 );
    final int WC  = m_cv.WorkingCols();
    m_console.Set( ROW, ST, ':', Style.NORMAL );

    int S_LEN = m_sb.length();

    if( WC-3 < S_LEN )
    {
      // m_sb too long for command line, only display end of m_sb:
      final int DIFF = S_LEN - (WC-3);
      for( int k=DIFF; k<S_LEN; k++ )
      {
        m_console.Set( ROW, ST+k+1-DIFF, m_sb.charAt(k), Style.NORMAL );
      }
      m_console.Set( ROW, m_cv.Col_Win_2_GL( S_LEN+1-DIFF ), ' ', Style.CURSOR );
    }
    else {
      // m_sb fits in command line:
      for( int k=0; k<S_LEN; k++ )
      {
        m_console.Set( ROW, ST+k+1, m_sb.charAt(k), Style.NORMAL );
      }
      for( int k=S_LEN; k<WC-1; k++ )
      {
        m_console.Set( ROW, ST+k+1, ' ', Style.NORMAL );
      }
      m_console.Set( ROW, m_cv.Col_Win_2_GL( S_LEN+1 ), ' ', Style.CURSOR );
    }
  }

  // m_sb goes in as               some/path/partial_file_name
  //   and if successful:
  //   m_partial_path comes out as some/path the relative path to the files
  //   m_search__head comes out as partial_file_name
  //   m_fb           comes out as full path to m_partial_path
  // Returns true if a m_fb is filled in, else false
  boolean FindFileBuf()
  {
    Ptr_StringBuilder path_tail = new Ptr_StringBuilder();
    Ptr_StringBuilder path_head = new Ptr_StringBuilder();

    Path_2_TailHead( m_sb, path_tail, path_head );

    String f_full_path = ".";
    if( 0 < path_tail.val.length() ) f_full_path = path_tail.val.toString();

    final int FILE_NUM = m_vis.m_file_hist[ m_vis.m_win ].get( 0 );

    if( m_vis.CMD_FILE < FILE_NUM )
    {
      // Get full file name relative to path of current file:
      f_full_path = m_cv.m_fb.Relative_2_FullFname( f_full_path );
    }
    else {
      // Get full file name relative to CWD:
      f_full_path = Utils.FindFullFileName_Path( m_vis.m_cwd, f_full_path );
    }

    if( Files.exists( Paths.get( f_full_path ) ) )
    {
      m_partial_path = path_tail.val.toString();
      m_search__head = path_head.val.toString();
      // f_full_path is now the full path to the directory
      // to search for matches to path_head
      Ptr_Int file_index = new Ptr_Int( 0 );
      if( m_vis.HaveFile( f_full_path, file_index ) )
      {
        m_fb = m_vis.m_files.get( file_index.val );
        return true;
      }
      else {
        m_fb = new FileBuf( m_vis, f_full_path, true );
        boolean ok = m_fb.ReadFile();
        if( ok ) {
          m_vis.Add_FileBuf_2_Lists_Create_Views( m_fb, f_full_path );
          return true;
        }
      }
    }
    return false;
  }
//void Path_2_TailHead( StringBuilder     sb_in
//                    , Ptr_StringBuilder path_tail
//                    , Ptr_StringBuilder path_head )
//{
//        String in_fname     = sb_in.toString();
//  final int    in_fname_len = in_fname.length();
//
//  // 1. seperate in_fname into path_tail and path_head
//  path_tail.val = new StringBuilder( in_fname_len );
//  path_head.val = new StringBuilder( in_fname_len );
//
//  final int last_slash_idx = in_fname.lastIndexOf( Utils.DIR_DELIM );
//  if( 0 <= last_slash_idx )
//  {
//    for( int i = 0                 ; i<last_slash_idx; i++ ) path_tail.val.append( in_fname.charAt( i ) );
//    for( int i = last_slash_idx + 1; i<in_fname_len  ; i++ ) path_head.val.append( in_fname.charAt( i ) );
//  }
//  else {
//    // No tail, all head:
//    path_head.val.append( in_fname );
//  }
//  if( path_tail.val.toString().equals("~") )
//  {
//    path_tail.val.setLength( 0 );
//    path_tail.val.append("$HOME");
//  }
//}
  void Path_2_TailHead( StringBuilder     sb_in
                      , Ptr_StringBuilder path_tail
                      , Ptr_StringBuilder path_head )
  {
          String in_fname     = sb_in.toString();
    final int    in_fname_len = in_fname.length();

    // 1. seperate in_fname into path_tail and path_head
    path_tail.val = new StringBuilder( in_fname_len );
    path_head.val = new StringBuilder( in_fname_len );

    final int last_slash_idx = in_fname.lastIndexOf( Utils.DIR_DELIM );
    if( 0 == last_slash_idx )
    {
      path_tail.val.append('/');
      for( int i = last_slash_idx + 1; i<in_fname_len  ; i++ ) path_head.val.append( in_fname.charAt( i ) );
    }
    else if( 0 < last_slash_idx )
    {
      for( int i = 0                 ; i<last_slash_idx; i++ ) path_tail.val.append( in_fname.charAt( i ) );
      for( int i = last_slash_idx + 1; i<in_fname_len  ; i++ ) path_head.val.append( in_fname.charAt( i ) );
    }
    else { // -1 == last_slash_idx
      // No tail, all head:
      path_head.val.append( in_fname );
    }
    if( path_tail.val.toString().equals("~") )
    {
      path_tail.val.setLength( 0 );
      path_tail.val.append( System.getenv("HOME") );
    }
  }

  // File name completion variables:
  Vis           m_vis;
  Console       m_console;
  StringBuilder m_sb;
  View          m_cv;
  FileBuf       m_fb;
  int           m_file_index;
  int           m_msg_len;
  String        m_partial_path;
  String        m_search__head;
  Thread        m_run_colon = new Thread() { public void run() { run_colon(); } };
  Thread        m_run_get_cover_key = new Thread() { public void run() { run_get_cover_key(); } };
  ColonOp       m_colon_op;
}

enum ColonOp
{ 
  unknown,
  e,
  w
}

