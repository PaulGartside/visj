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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowEvent;
import javax.swing.SwingUtilities;
import javax.swing.JFrame;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Deque;
import java.util.ArrayDeque;

public class Vis implements WindowFocusListener
{
  // WindowFocusListener implementation:
  public void windowGainedFocus( WindowEvent we )
  {
    m_console.requestFocusInWindow();
    m_received_focus = true;
  }
  public void windowLostFocus( WindowEvent we )
  {
  }

  public static void main( String[] args )
  {
    try {
      Vis vis = new Vis( args );

      vis.Run();
    }
    catch( Exception e )
    {
      Handle_Exception( e );
    }
  }
  static void PrintEnv()
  {
    java.util.Map<String, String> env = System.getenv();

    // OS = Windows_NT
    for( String env_name : env.keySet() )
    {
      System.out.format("%s=%s%n", env_name, env.get(env_name) );
    }
  }
  public Vis( String[] args )
  {
    m_args = args;

    m_states.add( m_run_init );  // First  state
    m_states.add( m_run_focus ); // Second state
    m_states.add( m_run_idle );  // Third  state

    m_received_focus = false;
  }
  static void Handle_Exception( Exception e )
  {
    e.printStackTrace( System.err );
    System.exit( 0 );
  }
  void Usage()
  {
    System.out.println("usage: Vis [file1 [file2 ...]]");
    System.exit( 0 );
  }
  void Die( String msg )
  {
    System.out.println( msg );
    System.exit( 0 );
  }
  void Run() throws Exception
  {
    while( true )
    {
      SwingUtilities.invokeAndWait( m_states.peekFirst() );
    }
  }
  void run_init()
  {
    m_frame   = new JFrame("Vis");
    m_console = new Console( this );
    m_diff    = new Diff( this, m_console );

    Container c = m_frame.getContentPane();
              c.add( m_console );

    Toolkit   def_tk    = Toolkit.getDefaultToolkit();
    Dimension screen_sz = def_tk.getScreenSize();

  //if( Utils.Get_OS_Type() == OS_Type.Windows )
  //{
      screen_sz.width  *= 0.9;
      screen_sz.height *= 0.9;
  //}
    m_frame.setSize( screen_sz.width, screen_sz.height );
    m_frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

    m_frame.addWindowFocusListener( this );
    m_frame.setVisible( true );

    m_states.removeFirst();
  }
  void run_focus()
  {
    if( m_received_focus )
    {
      m_console.Init();

      run_init_files();
      UpdateViews();

      m_console.Update();

      m_states.removeFirst();
    }
  }
  void run_init_files()
  {
    for( int w=0; w<MAX_WINS; w++ )
    {
      m_views[w]     = new ViewList();
      m_file_hist[w] = new  IntList();
    }
    InitBufferEditor();
    InitHelpBuffer();
    InitSearchEditor();
    InitMsgBuffer();
    InitCmdBuffer();
    boolean run_diff = InitUserFiles();
    InitFileHistory();

    if( run_diff && ( (CMD_FILE+1+2) == m_files.size()) )
    {
      // User supplied: "-d file1 file2", so run diff:
      m_diff_mode = true;
      m_num_wins = 2;
      m_file_hist[ 0 ].set( 0, 5 );
      m_file_hist[ 1 ].set( 0, 6 );
      GetView_Win( 0 ).SetTilePos( Tile_Pos.LEFT_HALF );
      GetView_Win( 1 ).SetTilePos( Tile_Pos.RITE_HALF );

      Exe_Colon_DoDiff();
    }
  }
  void InitBufferEditor()
  {
    // Buffer editor, 0
    FileBuf fb = new FileBuf( this, EDIT_BUF_NAME, false );

    Add_FileBuf_2_Lists_Create_Views( fb, EDIT_BUF_NAME );
  }
  void InitHelpBuffer()
  {
    // Buffer editor, 0
    FileBuf fb = new FileBuf( this, HELP_BUF_NAME, false );
    fb.ReadString( Help.str );

    Add_FileBuf_2_Lists_Create_Views( fb, HELP_BUF_NAME );
  }
  void InitSearchEditor()
  {
    // Search editor buffer, 2
    FileBuf fb = new FileBuf( this, SRCH_BUF_NAME, false );

    Add_FileBuf_2_Lists_Create_Views( fb, SRCH_BUF_NAME );
  }
  void InitMsgBuffer()
  {
    // Message buffer, 3
    FileBuf fb = new FileBuf( this, MSG__BUF_NAME, false );
    fb.PushLine(); // Add an empty line

    Add_FileBuf_2_Lists_Create_Views( fb, MSG__BUF_NAME );
  }
  void InitCmdBuffer()
  {
    // Command buffer, CMD_FILE(4)
    FileBuf fb = new FileBuf( this, CMD__BUF_NAME, false );
    fb.PushLine(); // Add an empty line

    Add_FileBuf_2_Lists_Create_Views( fb, CMD__BUF_NAME );
  }
  boolean InitUserFiles()
  {
    boolean run_diff = false;

    // User file buffers, 5, 6, ...
    for( int k=0; k<m_args.length; k++ )
    {
      if( m_args[k].equals("-d") )
      {
        run_diff = true;
      }
      else {
        String file_name = Utils.FindFullFileName_Process( m_args[k] );

        if( !HaveFile( file_name, null ) )
        {
          FileBuf fb = new FileBuf( this, file_name, true );

          boolean ok = fb.ReadFile();

          if( ok ) Add_FileBuf_2_Lists_Create_Views( fb, file_name );
        }
      }
    }
    return run_diff;
  }
  //-------------------------
  //| u1| be| he| u5| u3| u2|
  //-------------------------
  void InitFileHistory()
  {
    for( int w=0; w<MAX_WINS; w++ )
    {
      m_file_hist[w].add( BE_FILE  );
      m_file_hist[w].add( HELP_FILE );

      for( int f=m_views[w].size()-1; 6<=f; f-- )
      {
        m_file_hist[w].add( f );
      }
      if( 5 < m_views[w].size() )
      {
        m_file_hist[w].add( 0, 5 );
      }
    }
  }
  void Add_FileBuf_2_Lists_Create_Views( FileBuf fb, String fname )
  {
    // 1. Add fb to m_files
    m_files.add( fb );
    // 2. For MAX_WINS, create View of fb, and add View to m_views[k]
    // 3. For MAX_WINS, fb.m_views.add( v );
    for( int w=0; w<MAX_WINS; w++ )
    {
      View v = new View( this, fb, m_console );
      m_views[w].add( v );
      fb.m_views.add( v );
    }
    // 4. Add AddToBufferEditor( fname );
    AddToBufferEditor( fname );
  }
  void AddToBufferEditor( String fname )
  {
    Line line = new Line();
  //int NUM_FILES = m_views[0].size();
  //line.append_i( NUM_FILES-1 );
  //while( line.length()<3 ) line.insert( 0, ' ' );
  //line.append_c( ' ' );
    line.append_s( fname );
    FileBuf fb = m_views[0].get( BE_FILE ).m_fb;
    fb.PushLine( line );
    fb.BufferEditor_Sort();
    fb.ClearChanged();

    // Since buffer editor file has been re-arranged, make sure none of its
    // views have the cursor position past the end of the line
    for( int k=0; k<MAX_WINS; k++ )
    {
      View v = m_views[k].get( BE_FILE );

      int CL = v.CrsLine();
      int CP = v.CrsChar();
      int LL = fb.LineLen( CL );

      if( LL <= CP )
      {
        v.GoToCrsPos_NoWrite( CL, LL-1 );
      }
    }
  }
  boolean HaveFile( String file_name, Ptr_Int file_index )
  {
    boolean already_have_file = false;

    for( int k=0; !already_have_file && k<m_files.size(); k++ )
    {
      if( m_files.get( k ).m_fname.equals( file_name ) )
      {
        already_have_file = true;

        if( null != file_index ) file_index.val = k;
      }
    }
    return already_have_file;
  }
  int FName_2_FNum( String full_fname )
  {
    for( int k=0; k<m_files.size(); k++ )
    {
      if( full_fname.equals( m_files.get( k ).m_fname ) )
      {
        return k;
      }
    }
    return -1;
  }
  boolean File_Is_Displayed( String full_fname )
  {
    final int file_num = FName_2_FNum( full_fname );

    if( 0 <= file_num )
    {
      return File_Is_Displayed( file_num );
    }
    return false;
  }
  boolean File_Is_Displayed( final int file_num )
  {
    for( int w=0; w<m_num_wins; w++ )
    {
      if( file_num == m_file_hist[ w ].get( 0 ) )
      {
        return true;
      }
    }
    return false;
  }
  void ReleaseFileName( String full_fname )
  {
    final int file_num = FName_2_FNum( full_fname );

    if( 0 <= file_num )
    {
      ReleaseFileNum( file_num );
    }
  }
  void ReleaseFileNum( final int file_num )
  {
    m_files.remove( file_num );

    for( int k=0; k<MAX_WINS; k++ )
    {
      m_views[k].remove( file_num );

      IntList m_file_hist_k = m_file_hist[k];

      // Remove all file_num's from m_file_hist
      for( int i=0; i<m_file_hist_k.size(); i++ )
      {
        if( file_num == m_file_hist_k.get( i ) )
        {
          m_file_hist_k.remove( i );
        }
      }
      // Decrement all file_hist numbers greater than file_num
      for( int i=0; i<m_file_hist_k.size(); i++ )
      {
        final int val = m_file_hist_k.get( i );

        if( file_num < val )
        {
          m_file_hist_k.set( i, val-1 );
        }
      }
    }
  }

  void run_idle()
  {
    try {
      m_initialized = true;

      // Done running all command states and back to m_run_idle,
      // so turn off saving to dot buf:
      m_console.m_save_2_dot_buf = false;

      if( 0 < m_console.KeysIn() )
      {
        final char c1 = m_console.GetKey();

        Handle_Cmd( c1 );
      }
      else if( m_console.Resized() )
      {
        // Go to m_run_resize to wait resize events to stop:
        m_states.addFirst( m_run_resize );
      }
      else {
        CV().m_fb.CheckFileModTime();
      }
    }
    catch( Exception e )
    {
      Handle_Exception( e );
    }
  }
  void run_resize()
  {
    if( !m_console.Resized() )
    {
      // Console done re-sizing
      m_console.Init();
      UpdateViewsConsoleSize();
      UpdateViews();
      m_console.Update();

      m_states.removeFirst();
    }
  }
  void Handle_Cmd( final char c1 )
  {
    switch( c1 )
    {
    case 'a': Handle_a();         break;
    case 'A': Handle_A();         break;
    case 'b': Handle_b();         break;
    case 'B': Handle_B();         break;
    case 'c': Handle_c();         break;
    case 'd': Handle_d();         break;
    case 'D': Handle_D();         break;
    case 'e': Handle_e();         break;
    case 'f': Handle_f();         break;
    case 'F': Handle_F();         break;
    case 'g': Handle_g();         break;
    case 'G': Handle_G();         break;
    case 'h': Handle_h();         break;
    case 'H': Handle_H();         break;
    case 'i': Handle_i();         break;
    case 'j': Handle_j();         break;
    case 'J': Handle_J();         break;
    case 'k': Handle_k();         break;
    case 'l': Handle_l();         break;
    case 'L': Handle_L();         break;
    case 'm': Handle_m();         break;
    case 'M': Handle_M();         break;
    case 'n': Handle_n();         break;
    case 'N': Handle_N();         break;
    case 'o': Handle_o();         break;
    case 'O': Handle_O();         break;
    case 'p': Handle_p();         break;
    case 'P': Handle_P();         break;
    case 'Q': Handle_Q();         break;
    case 'R': Handle_R();         break;
    case 's': Handle_s();         break;
    case 'u': Handle_u();         break;
    case 'U': Handle_U();         break;
    case 'v': Handle_v();         break;
    case 'V': Handle_V();         break;
    case 'w': Handle_w();         break;
    case 'W': Handle_W();         break;
    case 'x': Handle_x();         break;
    case 'y': Handle_y();         break;
    case 'z': Handle_z();         break;
    case '0': Handle_0();         break;
    case '$': Handle_Dollar();    break;
    case '%': Handle_Percent();   break;
    case '{': Handle_LeftSquigglyBracket(); break;
    case '}': Handle_RightSquigglyBracket();break;
    case '*': Handle_Star();      break;
    case '~': Handle_Tilda();     break;
    case ';': Handle_SemiColon(); break;
    case ':': Handle_Colon();     break;
    case '/': Handle_Slash();     break;
    case '.': Handle_Dot();       break;
    case '\n':Handle_Return();    break;
    }
  }
  void Handle_j()
  {
    int num = 1;
    while( m_console.FirstKeyIs('j') )
    {
      m_console.GetKey();
      num++;
    }
    if( m_diff_mode ) m_diff.GoDown( num );
    else                CV().GoDown( num );
  }
  void Handle_k()
  {
    int num = 1;
    while( m_console.FirstKeyIs('k') )
    {
      m_console.GetKey();
      num++;
    }
    if( m_diff_mode ) m_diff.GoUp( num );
    else                CV().GoUp( num );
  }
  void Handle_h()
  {
    if( m_diff_mode ) m_diff.GoLeft();
    else                CV().GoLeft();
  }
  void Handle_l()
  {
    if( m_diff_mode ) m_diff.GoRight();
    else                CV().GoRight();
  }
  void Handle_0()
  {
    if( m_diff_mode )  m_diff.GoToBegOfLine();
    else                 CV().GoToBegOfLine();
  }
  void Handle_Dollar()
  {
    if( m_diff_mode )  m_diff.GoToEndOfLine();
    else                 CV().GoToEndOfLine();
  }
  void Handle_Percent()
  {
    if( m_diff_mode ) m_diff.GoToOppositeBracket();
    else                CV().GoToOppositeBracket();
  }
  void Handle_LeftSquigglyBracket()
  {
    if( m_diff_mode ) m_diff.GoToLeftSquigglyBracket();
    else                CV().GoToLeftSquigglyBracket();
  }
  void Handle_RightSquigglyBracket()
  {
    if( m_diff_mode ) m_diff.GoToRightSquigglyBracket();
    else                CV().GoToRightSquigglyBracket();
  }
  void Handle_Star()
  {
    String new_star = m_diff_mode ? m_diff.Do_Star_GetNewPattern()
                                  :   CV().Do_Star_GetNewPattern();

    if( !m_slash && new_star.equals( m_star ) ) return;

    // Un-highlight old star patterns for windows displayed:
    if( 0<m_star.length() )
    { // Since diff_mode does Console::Update(),
      // no need to print patterns here if in diff_mode
      if( !m_diff_mode ) Do_Star_PrintPatterns( false );
    }
    Do_Star_ClearPatterns();

    m_star = new_star;

    if( 0<m_star.length() )
    {
      m_slash = false;

      Do_Star_Update_Search_Editor();
      Do_Star_FindPatterns();
 
      // Highlight new star patterns for windows displayed:
      if( !m_diff_mode ) Do_Star_PrintPatterns( true );
    }
    if( m_diff_mode ) m_diff.Update();
    else {
      // Print out all the changes:
    //m_console.Update();
      // Put cursor back where it was
      CV().PrintCursor();
    }
  }
  void Do_Star_PrintPatterns( final boolean HIGHLIGHT )
  {
    for( int w=0; w<m_num_wins; w++ )
    {
      GetView_Win( w ).PrintPatterns( HIGHLIGHT );
    }
  }
  void Do_Star_ClearPatterns()
  {
    // Tell every FileBuf that it needs to clear the old pattern:
    for( int w=0; w<m_views[ 0 ].size(); w++ )
    {
      m_views[ 0 ].get( w ).m_fb.m_need_2_clear_stars = true;
    }
    // Remove star patterns from displayed FileBuf's only:
    for( int w=0; w<m_num_wins; w++ )
    {
      GetView_Win( w ).m_fb.ClearStars();
    }
  }
  void Do_Star_FindPatterns()
  {
    // Tell every FileBuf that it needs to find the new pattern:
    for( int w=0; w<m_views[ 0 ].size(); w++ )
    {
      m_views[ 0 ].get( w ).m_fb.m_need_2_find_stars = true;
    }
    // Only find new pattern now for FileBuf's that are displayed:
    for( int w=0; w<m_num_wins; w++ )
    {
      GetView_Win( w ).m_fb.Find_Stars();
    }
  }

  // 1. Search for star pattern in search editor.
  // 2. If star pattern is found in search editor,
  //         move pattern to end of search editor
  //    else add star pattern to end of search editor
  // 3. Clear buffer editor un-saved change status
  // 4. If search editor is displayed, update search editor window
  //
  void Do_Star_Update_Search_Editor()
  {
    final View rseV = m_views[ m_win ].get( SE_FILE );
    // Determine whether search editor has the star pattern
    final int NUM_SE_LINES = rseV.m_fb.NumLines(); // Number of search editor lines
    boolean found_pattern_in_search_editor = false;
    int line_in_search_editor = 0;

    for( int ln=0; !found_pattern_in_search_editor && ln<NUM_SE_LINES; ln++ )
    {
      Line s = rseV.m_fb.GetLine( ln );

      if( s.toStr().equals( m_star ) )
      {
        found_pattern_in_search_editor = true;
        line_in_search_editor = ln;
      }
    }
    // 2. If star pattern is found in search editor,
    //         move pattern to end of search editor
    //    else add star pattern to end of search editor
    if( found_pattern_in_search_editor )
    {
      // Move pattern to end of search editor, so newest searches are at bottom of file
      if( line_in_search_editor < NUM_SE_LINES-1 )
      {
        Line p = rseV.m_fb.RemoveLine( line_in_search_editor );
        rseV.m_fb.InsertLine( NUM_SE_LINES-1, p );
      }
    }
    else
    {
      // Push star onto search editor buffer
      Line line = new Line( m_star );
      rseV.m_fb.PushLine( line );
    }
    // 3. Clear buffer editor un-saved change status
    rseV.m_fb.ClearChanged();

    // 4. If search editor is displayed, update search editor window
    for( int w=0; w<m_num_wins; w++ )
    {
      if( SE_FILE == m_file_hist[ w ].get( 0 ) )
      {
        m_views[ w ].get( SE_FILE ).Update();
      }
    }
  }
  void Handle_Tilda()
  {
    if( !m_console.m_get_from_dot_buf )
    {
      m_console.m_dot_buf.setLength( 0 );
      m_console.m_dot_buf.append( '~' );
    }
    if( m_diff_mode ) m_diff.Do_Tilda();
    else                CV().Do_Tilda();
  }
  void Handle_H()
  {
    if( m_diff_mode ) m_diff.GoToTopLineInView();
    else                CV().GoToTopLineInView();
  }
  void Handle_J()
  {
    if( !m_console.m_get_from_dot_buf )
    {
      m_console.m_dot_buf.setLength( 0 );
      m_console.m_dot_buf.append( 'J' );
    }
    if( m_diff_mode ) m_diff.Do_J();
    else                CV().Do_J();
  }
  void Handle_L()
  {
    if( m_diff_mode ) m_diff.GoToBotLineInView();
    else                CV().GoToBotLineInView();
  }
  void Handle_m()
  {
    if( m_console.m_save_2_map_buf || m_console.m_map_buf.length()<=0 )
    {
      // When mapping, 'm' is ignored.
      // If not mapping and map buf len is zero, 'm' is ignored.
      return;
    }
    m_console.m_get_from_map_buf = true;

    m_states.addFirst( m_run_map );
  }
  void Handle_M()
  {
    if( m_diff_mode ) m_diff.GoToMidLineInView();
    else                CV().GoToMidLineInView();
  }
  void Handle_n()
  {
    if( m_diff_mode ) m_diff.Do_n();
    else                CV().Do_n();
  }
  void Handle_N()
  {
    if( m_diff_mode ) m_diff.Do_N();
    else                CV().Do_N();
  }
  void Handle_o()
  {
    if( !m_console.m_get_from_dot_buf )
    {
      m_console.m_dot_buf.setLength( 0 );
      m_console.m_dot_buf.append( 'o' );
      m_console.m_save_2_dot_buf = true;
    }
    if( m_diff_mode ) m_diff.Do_o();
    else                CV().Do_o();
  }
  void Handle_O()
  {
    if( !m_console.m_get_from_dot_buf )
    {
      m_console.m_dot_buf.setLength( 0 );
      m_console.m_dot_buf.append( 'O' );
      m_console.m_save_2_dot_buf = true;
    }
    if( m_diff_mode ) m_diff.Do_O();
    else                CV().Do_O();
  }
  void Handle_p()
  {
    if( !m_console.m_get_from_dot_buf )
    {
      m_console.m_dot_buf.setLength( 0 );
      m_console.m_dot_buf.append( 'p' );
    }
    if( m_diff_mode ) m_diff.Do_p();
    else                CV().Do_p();
  }
  void Handle_P()
  {
    if( !m_console.m_get_from_dot_buf )
    {
      m_console.m_dot_buf.setLength( 0 );
      m_console.m_dot_buf.append( 'P' );
    }
    if( m_diff_mode ) m_diff.Do_P();
    else                CV().Do_P();
  }
  void Handle_Q()
  {
    // run_Q will get run after run_dot is run.
    m_states.addFirst( m_run_Q );

    Handle_Dot();
  }
  void run_Q()
  {
    Handle_j();
    Handle_0();

    m_states.removeFirst();
  }
  void Handle_R()
  {
    if( !m_console.m_get_from_dot_buf )
    {
      m_console.m_dot_buf.setLength( 0 );
      m_console.m_dot_buf.append( 'R' );
      m_console.m_save_2_dot_buf = true;
    }
    if( m_diff_mode ) m_diff.Do_R();
    else                CV().Do_R();
  }
  void Handle_s()
  {
    if( !m_console.m_get_from_dot_buf )
    {
      m_console.m_dot_buf.setLength( 0 );
      m_console.m_dot_buf.append( 's' );
      m_console.m_save_2_dot_buf = true;
    }
    if( m_diff_mode ) m_diff.Do_s();
    else                CV().Do_s();
  }
  void Handle_u()
  {
    if( m_diff_mode ) ; // Need to implement
    else              CV().Do_u();
  }
  void Handle_U()
  {
    if( m_diff_mode ) ; // Need to implement
    else              CV().Do_U();
  }
  void Handle_v()
  {
    if( !m_console.m_get_from_dot_buf )
    {
      m_console.m_vis_buf.setLength( 0 );
      m_console.m_vis_buf.append( 'v' );
      m_console.m_save_2_vis_buf = true;
    }
    if( m_diff_mode ) m_diff.Do_v();
    else                CV().Do_v();
  }
  void Handle_V()
  {
    if( !m_console.m_get_from_dot_buf )
    {
      m_console.m_vis_buf.setLength( 0 );
      m_console.m_vis_buf.append( 'V' );
      m_console.m_save_2_vis_buf = true;
    }
    if( m_diff_mode ) m_diff.Do_V();
    else                CV().Do_V();
  }
  void Handle_F()
  {
    if( m_diff_mode ) m_diff.PageDown();
    else                CV().PageDown();
  }
  void Handle_B()
  {
    if( m_diff_mode ) m_diff.PageUp();
    else                CV().PageUp();
  }
  void Handle_c()
  {
    m_states.addFirst( m_run_c );
  }
  void run_c()
  {
    if( 0<m_console.KeysIn() )
    {
      m_states.removeFirst();

      final char C = m_console.GetKey();

      if( C == 'w' )
      {
        if( !m_console.m_get_from_dot_buf )
        {
          m_console.m_dot_buf.setLength( 0 );
          m_console.m_dot_buf.append( 'c' );
          m_console.m_dot_buf.append( 'w' );
          m_console.m_save_2_dot_buf = true;
        }
        if( m_diff_mode ) m_diff.Do_cw();
        else                CV().Do_cw();
      }
      else if( C == '$' )
      {
        if( !m_console.m_get_from_dot_buf )
        {
          m_console.m_dot_buf.setLength( 0 );
          m_console.m_dot_buf.append( 'c' );
          m_console.m_dot_buf.append( '$' );
          m_console.m_save_2_dot_buf = true;
        }
        if( m_diff_mode ) { m_diff.Do_D(); m_diff.Do_a(); }
        else              {   CV().Do_D();   CV().Do_a(); }
      }
    }
  }
  void Handle_d()
  {
    m_states.addFirst( m_run_d );
  }
  void run_d()
  {
    if( 0<m_console.KeysIn() )
    {
      m_states.removeFirst();

      final char C = m_console.GetKey();

      if( C == 'd' )
      {
        if( !m_console.m_get_from_dot_buf )
        {
          m_console.m_dot_buf.setLength( 0 );
          m_console.m_dot_buf.append( 'd' );
          m_console.m_dot_buf.append( 'd' );
        }
        if( m_diff_mode ) m_diff.Do_dd();
        else                CV().Do_dd();
      }
      else if( C == 'w' )
      {
        if( !m_console.m_get_from_dot_buf )
        {
          m_console.m_dot_buf.setLength( 0 );
          m_console.m_dot_buf.append( 'd' );
          m_console.m_dot_buf.append( 'w' );
        }
        if( m_diff_mode ) m_diff.Do_dw();
        else                CV().Do_dw();
      }
    }
  }
  void Handle_D()
  {
    if( !m_console.m_get_from_dot_buf )
    {
      m_console.m_dot_buf.setLength( 0 );
      m_console.m_dot_buf.append( 'D' );
    }
    if( m_diff_mode ) m_diff.Do_D();
    else                CV().Do_D();
  }
  void Handle_e()
  {
    if( m_diff_mode ) m_diff.GoToEndOfWord();
    else                CV().GoToEndOfWord();
  }
  void Handle_f()
  {
    if( m_diff_mode ) m_diff.Do_f();
    else                CV().Do_f();
  }
  void Handle_i()
  {
    if( !m_console.m_get_from_dot_buf )
    {
      m_console.m_dot_buf.setLength( 0 );
      m_console.m_dot_buf.append( 'i' );
      m_console.m_save_2_dot_buf = true;
    }
    if( m_diff_mode ) m_diff.Do_i();
    else                CV().Do_i();
  }
  void Handle_a()
  {
    if( !m_console.m_get_from_dot_buf )
    {
      m_console.m_dot_buf.setLength( 0 );
      m_console.m_dot_buf.append( 'a' );
      m_console.m_save_2_dot_buf = true;
    }
    if( m_diff_mode ) m_diff.Do_a();
    else                CV().Do_a();
  }
  void Handle_A()
  {
    if( !m_console.m_get_from_dot_buf )
    {
      m_console.m_dot_buf.setLength( 0 );
      m_console.m_dot_buf.append( 'A' );
      m_console.m_save_2_dot_buf = true;
    }
    if( m_diff_mode ) m_diff.Do_A();
    else                CV().Do_A();
  }
  void Handle_b()
  {
    if( m_diff_mode ) m_diff.GoToPrevWord();
    else                CV().GoToPrevWord();
  }
  void Handle_g()
  {
    m_states.addFirst( m_run_g );
  }
  void run_g()
  {
    if( 0<m_console.KeysIn() )
    {
      m_states.removeFirst();

      final char c2 = m_console.GetKey();

      if( c2 == 'g' )
      {
        if( m_diff_mode ) m_diff.GoToTopOfFile();
        else                CV().GoToTopOfFile();
      }
      else if( c2 == '0' )
      {
        if( m_diff_mode ) m_diff.GoToStartOfRow();
        else                CV().GoToStartOfRow();
      }
      else if( c2 == '$' )
      {
        if( m_diff_mode ) m_diff.GoToEndOfRow();
        else                CV().GoToEndOfRow();
      }
      else if( c2 == 'f' )
      {
        if( !m_diff_mode ) GoToFile();
      }
    }
  }
  void Handle_G()
  {
    if( m_diff_mode ) m_diff.GoToEndOfFile();
    else                CV().GoToEndOfFile();
  }
  void Handle_SemiColon()
  {
    if( 0 <= m_fast_char )
    {
      if( m_diff_mode ) m_diff.Do_semicolon( m_fast_char );
      else                CV().Do_semicolon( m_fast_char );
    }
  }

  void Handle_w()
  {
    if( m_diff_mode ) m_diff.GoToNextWord();
    else                CV().GoToNextWord();
  }
  void Handle_W()
  {
    m_states.addFirst( m_run_W );
  }
  void run_W()
  {
    if( 0<m_console.KeysIn() )
    {
      m_states.removeFirst();

      final char c2 = m_console.GetKey();

      if     ( c2 == 'W' ) GoToNextWindow();
      else if( c2 == 'l' ) GoToNextWindow_l();
      else if( c2 == 'h' ) GoToNextWindow_h();
      else if( c2 == 'j'
            || c2 == 'k' ) GoToNextWindow_jk();
      else if( c2 == 'R' ) FlipWindows();
    }
  }
  void GoToNextWindow()
  {
    if( 1 < m_num_wins )
    {
      final int win_old = m_win;

      m_win = (++m_win) % m_num_wins;

      View pV     = GetView_Win( m_win   );
      View pV_old = GetView_Win( win_old );

      pV_old.Print_Borders();
      pV    .Print_Borders();

      MoveCursor( pV, pV_old ); // Calls m_console.Update();
    }
  }
  void MoveCursor( View pV_new, View pV_old )
  {
    if( m_diff_mode )
    {
      m_diff.Clear_Console_CrsCell( pV_old );
      m_diff.Set_Console_CrsCell( pV_new );
    }
    else {
      pV_old.Clear_Console_CrsCell();
      pV_new.Set_Console_CrsCell();
    }
    m_console.Update();
  }
  void GoToNextWindow_l()
  {
    if( 1 < m_num_wins )
    {
      final int win_old = m_win;

      // If next view to go to was not found, dont do anything, just return
      // If next view to go to is found, m_win will be updated to new value
      if( GoToNextWindow_l_Find() )
      {
        View pV     = GetView_Win( m_win   );
        View pV_old = GetView_Win( win_old );

        pV_old.Print_Borders();
        pV    .Print_Borders();

        m_console.Update();

        MoveCursor( pV, pV_old ); // Calls m_console.Update();
      }
    }
  }
  boolean GoToNextWindow_l_Find()
  {
    boolean found = false; // Found next view to go to
  
    final View     curr_V  = CV();
    final Tile_Pos curr_TP = curr_V.m_tile_pos;
  
    if( curr_TP == Tile_Pos.LEFT_HALF )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.RITE_HALF         == TP
         || Tile_Pos.TOP__RITE_QTR     == TP
         || Tile_Pos.BOT__RITE_QTR     == TP
         || Tile_Pos.RITE_CTR__QTR     == TP
         || Tile_Pos.TOP__RITE_CTR_8TH == TP
         || Tile_Pos.BOT__RITE_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.RITE_HALF )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.LEFT_HALF     == TP
         || Tile_Pos.TOP__LEFT_QTR == TP
         || Tile_Pos.BOT__LEFT_QTR == TP
         || Tile_Pos.LEFT_QTR      == TP
         || Tile_Pos.TOP__LEFT_8TH == TP
         || Tile_Pos.BOT__LEFT_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.TOP__LEFT_QTR )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.RITE_HALF         == TP
         || Tile_Pos.TOP__RITE_QTR     == TP
         || Tile_Pos.RITE_CTR__QTR     == TP
         || Tile_Pos.TOP__RITE_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.TOP__RITE_QTR )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.LEFT_HALF     == TP
         || Tile_Pos.LEFT_QTR      == TP
         || Tile_Pos.TOP__LEFT_QTR == TP
         || Tile_Pos.TOP__LEFT_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.BOT__LEFT_QTR )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.RITE_HALF         == TP
         || Tile_Pos.BOT__RITE_QTR     == TP
         || Tile_Pos.RITE_CTR__QTR     == TP
         || Tile_Pos.BOT__RITE_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.BOT__RITE_QTR )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.LEFT_HALF     == TP
         || Tile_Pos.LEFT_QTR      == TP
         || Tile_Pos.BOT__LEFT_QTR == TP
         || Tile_Pos.BOT__LEFT_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.LEFT_QTR )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.LEFT_CTR__QTR     == TP
         || Tile_Pos.TOP__LEFT_CTR_8TH == TP
         || Tile_Pos.BOT__LEFT_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.RITE_QTR )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.LEFT_HALF     == TP
         || Tile_Pos.LEFT_QTR      == TP
         || Tile_Pos.TOP__LEFT_8TH == TP
         || Tile_Pos.BOT__LEFT_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.LEFT_CTR__QTR )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.RITE_HALF         == TP
         || Tile_Pos.RITE_CTR__QTR     == TP
         || Tile_Pos.TOP__RITE_CTR_8TH == TP
         || Tile_Pos.BOT__RITE_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.RITE_CTR__QTR )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.RITE_QTR      == TP
         || Tile_Pos.TOP__RITE_8TH == TP
         || Tile_Pos.BOT__RITE_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.TOP__LEFT_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.LEFT_CTR__QTR     == TP
         || Tile_Pos.TOP__LEFT_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.BOT__LEFT_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.LEFT_CTR__QTR     == TP
         || Tile_Pos.BOT__LEFT_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.TOP__LEFT_CTR_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.RITE_HALF         == TP
         || Tile_Pos.RITE_CTR__QTR     == TP
         || Tile_Pos.TOP__RITE_QTR     == TP
         || Tile_Pos.TOP__RITE_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.BOT__LEFT_CTR_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.RITE_HALF         == TP
         || Tile_Pos.RITE_CTR__QTR     == TP
         || Tile_Pos.BOT__RITE_QTR     == TP
         || Tile_Pos.BOT__RITE_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.TOP__RITE_CTR_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.RITE_QTR      == TP
         || Tile_Pos.TOP__RITE_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.BOT__RITE_CTR_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.RITE_QTR      == TP
         || Tile_Pos.BOT__RITE_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.TOP__RITE_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.LEFT_HALF     == TP
         || Tile_Pos.LEFT_QTR      == TP
         || Tile_Pos.TOP__LEFT_QTR == TP
         || Tile_Pos.TOP__LEFT_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.BOT__RITE_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.LEFT_HALF     == TP
         || Tile_Pos.LEFT_QTR      == TP
         || Tile_Pos.BOT__LEFT_QTR == TP
         || Tile_Pos.BOT__LEFT_8TH == TP ) { m_win = k; found = true; }
      }
    }
    return found;
  }

  void GoToNextWindow_h()
  {
    if( 1 < m_num_wins )
    {
      final int win_old = m_win;

      // If next view to go to was not found, dont do anything, just return
      // If next view to go to is found, m_win will be updated to new value
      if( GoToNextWindow_h_Find() )
      {
        View pV     = GetView_Win( m_win   );
        View pV_old = GetView_Win( win_old );

        pV_old.Print_Borders();
        pV    .Print_Borders();

        MoveCursor( pV, pV_old ); // Calls m_console.Update();
      }
    }
  }
  boolean GoToNextWindow_h_Find()
  {
    boolean found = false; // Found next view to go to

    final View     curr_V  = CV();
    final Tile_Pos curr_TP = curr_V.m_tile_pos;

    if( curr_TP == Tile_Pos.LEFT_HALF )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.RITE_HALF     == TP
         || Tile_Pos.TOP__RITE_QTR == TP
         || Tile_Pos.BOT__RITE_QTR == TP
         || Tile_Pos.RITE_QTR      == TP
         || Tile_Pos.TOP__RITE_8TH == TP
         || Tile_Pos.BOT__RITE_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.RITE_HALF )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.LEFT_HALF         == TP
         || Tile_Pos.TOP__LEFT_QTR     == TP
         || Tile_Pos.BOT__LEFT_QTR     == TP
         || Tile_Pos.LEFT_CTR__QTR     == TP
         || Tile_Pos.TOP__LEFT_CTR_8TH == TP
         || Tile_Pos.BOT__LEFT_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.TOP__LEFT_QTR )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.RITE_HALF     == TP
         || Tile_Pos.TOP__RITE_QTR == TP
         || Tile_Pos.RITE_QTR      == TP
         || Tile_Pos.TOP__RITE_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.TOP__RITE_QTR )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.LEFT_HALF         == TP
         || Tile_Pos.LEFT_CTR__QTR     == TP
         || Tile_Pos.TOP__LEFT_QTR     == TP
         || Tile_Pos.TOP__LEFT_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.BOT__LEFT_QTR )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.RITE_HALF     == TP
         || Tile_Pos.BOT__RITE_QTR == TP
         || Tile_Pos.RITE_QTR      == TP
         || Tile_Pos.BOT__RITE_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.BOT__RITE_QTR )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.LEFT_HALF         == TP
         || Tile_Pos.LEFT_CTR__QTR     == TP
         || Tile_Pos.BOT__LEFT_QTR     == TP
         || Tile_Pos.BOT__LEFT_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.LEFT_QTR )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.RITE_HALF     == TP
         || Tile_Pos.RITE_QTR      == TP
         || Tile_Pos.TOP__RITE_8TH == TP
         || Tile_Pos.BOT__RITE_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.RITE_QTR )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.RITE_CTR__QTR     == TP
         || Tile_Pos.TOP__RITE_CTR_8TH == TP
         || Tile_Pos.BOT__RITE_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.LEFT_CTR__QTR )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.LEFT_QTR      == TP
         || Tile_Pos.TOP__LEFT_8TH == TP
         || Tile_Pos.BOT__LEFT_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.RITE_CTR__QTR )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.LEFT_HALF         == TP
         || Tile_Pos.LEFT_CTR__QTR     == TP
         || Tile_Pos.TOP__LEFT_QTR     == TP
         || Tile_Pos.BOT__LEFT_QTR     == TP
         || Tile_Pos.TOP__LEFT_CTR_8TH == TP
         || Tile_Pos.BOT__LEFT_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.TOP__LEFT_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.RITE_HALF     == TP
         || Tile_Pos.TOP__RITE_QTR == TP
         || Tile_Pos.RITE_QTR      == TP
         || Tile_Pos.TOP__RITE_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.BOT__LEFT_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.RITE_HALF     == TP
         || Tile_Pos.BOT__RITE_QTR == TP
         || Tile_Pos.RITE_QTR      == TP
         || Tile_Pos.BOT__RITE_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.TOP__LEFT_CTR_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.LEFT_QTR      == TP
         || Tile_Pos.TOP__LEFT_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.BOT__LEFT_CTR_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.LEFT_QTR      == TP
         || Tile_Pos.BOT__LEFT_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.TOP__RITE_CTR_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.LEFT_HALF         == TP
         || Tile_Pos.TOP__LEFT_QTR     == TP
         || Tile_Pos.LEFT_CTR__QTR     == TP
         || Tile_Pos.TOP__LEFT_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.BOT__RITE_CTR_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.LEFT_HALF         == TP
         || Tile_Pos.BOT__LEFT_QTR     == TP
         || Tile_Pos.LEFT_CTR__QTR     == TP
         || Tile_Pos.BOT__LEFT_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.TOP__RITE_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.RITE_CTR__QTR     == TP
         || Tile_Pos.TOP__RITE_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.BOT__RITE_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.RITE_CTR__QTR     == TP
         || Tile_Pos.BOT__RITE_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    return found;
  }

  void GoToNextWindow_jk()
  {
    if( 1 < m_num_wins )
    {
      final int win_old = m_win;
  
      // If next view to go to was not found, dont do anything, just return
      // If next view to go to is found, m_win will be updated to new value
      if( GoToNextWindow_jk_Find() )
      {
        View pV     = GetView_Win( m_win   );
        View pV_old = GetView_Win( win_old );
  
        pV_old.Print_Borders();
        pV    .Print_Borders();

        MoveCursor( pV, pV_old ); // Calls m_console.Update();
      }
    }
  }
  boolean GoToNextWindow_jk_Find()
  {
    boolean found = false; // Found next view to go to
  
    final View     curr_V  = CV();
    final Tile_Pos curr_TP = curr_V.m_tile_pos;
  
    if( curr_TP == Tile_Pos.TOP__HALF )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.BOT__HALF         == TP
         || Tile_Pos.BOT__LEFT_QTR     == TP
         || Tile_Pos.BOT__RITE_QTR     == TP
         || Tile_Pos.BOT__LEFT_8TH     == TP
         || Tile_Pos.BOT__RITE_8TH     == TP
         || Tile_Pos.BOT__LEFT_CTR_8TH == TP
         || Tile_Pos.BOT__RITE_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.BOT__HALF )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.TOP__HALF         == TP
         || Tile_Pos.TOP__LEFT_QTR     == TP
         || Tile_Pos.TOP__RITE_QTR     == TP
         || Tile_Pos.TOP__LEFT_8TH     == TP
         || Tile_Pos.TOP__RITE_8TH     == TP
         || Tile_Pos.TOP__LEFT_CTR_8TH == TP
         || Tile_Pos.TOP__RITE_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.TOP__LEFT_QTR )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.BOT__HALF         == TP
         || Tile_Pos.BOT__LEFT_QTR     == TP
         || Tile_Pos.BOT__LEFT_8TH     == TP
         || Tile_Pos.BOT__LEFT_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.TOP__RITE_QTR )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.BOT__HALF         == TP
         || Tile_Pos.BOT__RITE_QTR     == TP
         || Tile_Pos.BOT__RITE_8TH     == TP
         || Tile_Pos.BOT__RITE_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.BOT__LEFT_QTR )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.TOP__HALF         == TP
         || Tile_Pos.TOP__LEFT_QTR     == TP
         || Tile_Pos.TOP__LEFT_8TH     == TP
         || Tile_Pos.TOP__LEFT_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.BOT__RITE_QTR )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.TOP__HALF         == TP
         || Tile_Pos.TOP__RITE_QTR     == TP
         || Tile_Pos.TOP__RITE_8TH     == TP
         || Tile_Pos.TOP__RITE_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.TOP__LEFT_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.BOT__HALF     == TP
         || Tile_Pos.BOT__LEFT_QTR == TP
         || Tile_Pos.BOT__LEFT_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.TOP__RITE_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.BOT__HALF     == TP
         || Tile_Pos.BOT__RITE_QTR == TP
         || Tile_Pos.BOT__RITE_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.TOP__LEFT_CTR_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.BOT__HALF         == TP
         || Tile_Pos.BOT__LEFT_QTR     == TP
         || Tile_Pos.BOT__LEFT_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.TOP__RITE_CTR_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.BOT__HALF         == TP
         || Tile_Pos.BOT__RITE_QTR     == TP
         || Tile_Pos.BOT__RITE_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.BOT__LEFT_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.TOP__HALF     == TP
         || Tile_Pos.TOP__LEFT_QTR == TP
         || Tile_Pos.TOP__LEFT_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.BOT__RITE_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.TOP__HALF     == TP
         || Tile_Pos.TOP__RITE_QTR == TP
         || Tile_Pos.TOP__RITE_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.BOT__LEFT_CTR_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.TOP__HALF         == TP
         || Tile_Pos.TOP__LEFT_QTR     == TP
         || Tile_Pos.TOP__LEFT_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    else if( curr_TP == Tile_Pos.BOT__RITE_CTR_8TH )
    {
      for( int k=0; !found && k<m_num_wins; k++ )
      {
        final Tile_Pos TP = GetView_Win( k ).m_tile_pos;
        if( Tile_Pos.TOP__HALF         == TP
         || Tile_Pos.TOP__RITE_QTR     == TP
         || Tile_Pos.TOP__RITE_CTR_8TH == TP ) { m_win = k; found = true; }
      }
    }
    return found;
  }

  void FlipWindows()
  {
    if( 1 < m_num_wins )
    {
      // This code only works for MAX_WINS == 2
      View pV1 = GetView_Win( 0 );
      View pV2 = GetView_Win( 1 );
  
      if( pV1 != pV2 )
      {
        // Swap pV1 and pV2 Tile Positions:
        Tile_Pos tp_v1 = pV1.m_tile_pos;
        pV1.SetTilePos( pV2.m_tile_pos );
        pV2.SetTilePos( tp_v1 );
      }
      UpdateViews();
    }
  }

  void Handle_x()
  {
    if( !m_console.m_get_from_dot_buf )
    {
      m_console.m_dot_buf.setLength( 0 );
      m_console.m_dot_buf.append( 'x' );
    //m_console.m_save_2_dot_buf = true;
    }
    if( m_diff_mode ) m_diff.Do_x();
    else                CV().Do_x();
  }

  void Handle_y()
  {
    m_states.addFirst( m_run_y );
  }
  void run_y()
  {
    if( 0<m_console.KeysIn() )
    {
      m_states.removeFirst();

      final char c2 = m_console.GetKey();

      if( c2 == 'y' )
      {
        if( m_diff_mode ) m_diff.Do_yy();
        else                CV().Do_yy();
      }
      else if( c2 == 'w' )
      {
        CV().Do_yw();
      }
    }
  }

  void Handle_z()
  {
    if( m_diff_mode ) m_diff.Do_z();
    else                CV().Do_z();
  }

  void Handle_Colon()
  {
    if( null == m_colon )
    {
      m_colon = new Colon( this );
    }
    m_colon.Run( CV() );
  }
  void Handle_Slash()
  {
    if( m_diff_mode ) m_diff.Clear_Console_CrsCell();
    else                CV().Clear_Console_CrsCell();

    CV().GoToCmdLineClear("/");

    m_states.addFirst( m_run_slash );

    m_sb.setLength( 0 );
  }
  void Handle_Dot()
  {
    if( 0<m_console.m_dot_buf.length() )
    {
      if( m_console.m_save_2_map_buf )
      {
        // Pop '.' off map_buf, because the contents of m_console.m_map_buf
        // will be saved to m_console.m_map_buf.
        StringBuilder map_buf = m_console.m_map_buf;
        if( 0<map_buf.length() ) map_buf.deleteCharAt( map_buf.length()-1 );
      }
      m_console.m_get_from_dot_buf = true;

      m_states.addFirst( m_run_dot );
    }
  }

  void Handle_Return()
  {
    if( m_diff_mode ) m_diff.GoToBegOfNextLine();
    else                CV().GoToBegOfNextLine();
  }

  void run_dot()
  {
    if( m_console.m_get_from_dot_buf )
    {
      final char CC = m_console.GetKey();

      Handle_Cmd( CC );
    }
    else {
      // Done running dot:
      m_states.removeFirst();

      if( m_diff_mode ) {
        // Diff does its own update every time a command is run
      }
      else {
        // Dont update until after all the commands have been executed:
        CV().m_fb.Update();
      }
    }
  }
  void run_map()
  {
    if( m_console.m_get_from_map_buf )
    {
      final char CC = m_console.GetKey();

      Handle_Cmd( CC );
    }
    else {
      // Done running map:
      m_states.removeFirst();

      if( m_diff_mode ) {
        // Diff does its own update every time a command is run
      }
      else {
        // Dont update until after all the commands have been executed:
        CV().m_fb.Update();
      }
    }
  }

  void run_slash()
  {
    if( 0<m_console.KeysIn() )
    {
      final char c = m_console.GetKey();

      if( Utils.IsEndOfLineDelim( c ) )
      {
        m_states.removeFirst();
        Exe_Slash();
      }
      else {
        final int ROW = CV().Cmd__Line_Row();

        if( Console.BS != c && Console.DEL != c )
        {
          m_sb.append( c );
          m_console.Set( ROW, CV().Col_Win_2_GL( m_sb.length()   ), c  , Style.NORMAL );
          m_console.Set( ROW, CV().Col_Win_2_GL( m_sb.length()+1 ), ' ', Style.CURSOR );
        }
        else {  // Backspace or Delete key
          if( 0<m_sb.length() )
          {
            // Replace last typed char with space:
            m_console.Set( ROW, CV().Col_Win_2_GL( m_sb.length()+1 ), ' ', Style.NORMAL );
            // Move back onto new space:
            m_console.Set( ROW, CV().Col_Win_2_GL( m_sb.length() ), ' ', Style.CURSOR );
            m_sb.deleteCharAt( m_sb.length()-1 );
          }
        }
        m_console.Update();
      }
    }
  }

  void Exe_Colon_Cmd()
  {
    m_states.removeFirst(); //< Drop out of m_run_colon
    // Copy m_sb into m_sb2
    m_sb2.ensureCapacity( m_sb.length() );
    m_sb2.setLength( 0 );
    for( int k=0; k<m_sb.length(); k++ ) m_sb2.append( m_sb.charAt( k ) );
    Utils.RemoveSpaces( m_sb );
    Exe_Colon_MapEnd();

    boolean need_2_print_cursor = false;

    if     ( m_sb.length()<1 )                need_2_print_cursor = true;
    else if( m_sb.toString().equals("q") )    Exe_Colon_q();
    else if( m_sb.toString().equals("qa") )   Exe_Colon_qa();
    else if( m_sb.toString().equals("help") ) Exe_Colon_help();
    else if( m_sb.toString().equals("diff") ) Exe_Colon_DoDiff();
    else if( m_sb.toString().equals("nodiff"))Exe_Colon_NoDiff();
    else if( m_sb.toString().equals("hi") )   Exe_Colon_hi();
    else if( m_sb.toString().equals("vsp") )  Exe_Colon_vsp();
    else if( m_sb.toString().equals("sp") )   Exe_Colon_sp();
    else if( m_sb.toString().equals("se") )   Exe_Colon_se();
    else if( m_sb.toString().equals("sh")
          || m_sb.toString().equals("shell")) Exe_Colon_sh();
    else if( m_sb.toString().equals("run") )  Exe_Colon_run();
    else if( m_sb.toString().equals("pwd") )  Exe_Colon_pwd();
    else if( m_sb.toString().equals("map") )  Exe_Colon_MapStart();
    else if( m_sb.toString().equals("showmap") )Exe_Colon_MapShow();
    else if( m_sb.toString().equals("coverkey"))Exe_Colon_CoverKey();
    else if( m_sb.toString().equals("cover") ) Exe_Colon_Cover();
    else if( m_sb.toString().equals("cs1") )   m_console.Set_Color_Scheme_1();
    else if( m_sb.toString().equals("cs2") )   m_console.Set_Color_Scheme_2();
    else if( m_sb.toString().equals("cs3") )   m_console.Set_Color_Scheme_3();
    else if( m_sb.toString().equals("cs4") )   m_console.Set_Color_Scheme_4();
    else if( m_sb.toString().startsWith("cd"))Exe_Colon_cd();
    else if( m_sb.toString().startsWith("syn"))Exe_Colon_Syntax();
    else if( m_sb.charAt(0)=='w' )            Exe_Colon_w();
    else if( m_sb.charAt(0)=='b' )            Exe_Colon_b();
    else if( m_sb.charAt(0)=='n' )            Exe_Colon_n();
    else if( m_sb.charAt(0)=='e' )            Exe_Colon_e();
    else if( '0' <= m_sb.charAt(0)
                 && m_sb.charAt(0) <= '9' )
    {
      final int line_num = Integer.valueOf( m_sb.toString() );
      if( m_diff_mode ) m_diff.GoToLine( line_num );
      else                CV().GoToLine( line_num );
    }
    else {
      need_2_print_cursor = true;
    }
    if( need_2_print_cursor )
    {
      final int ROW = CV().Cmd__Line_Row();
      final int COL = CV().Col_Win_2_GL( m_sb.length()+1 );

      // Remove cursor from command line row:
      m_console.Set( ROW, COL, ' ', Style.NORMAL );
      // Put cursor back in window:
      if( m_diff_mode ) m_diff.PrintCursor();
      else                CV().PrintCursor();
    }
  }
  void Exe_Slash()
  {
    Handle_Slash_GotPattern( m_sb.toString(), true );
  }
  void Handle_Slash_GotPattern( final String pattern
                              , final boolean MOVE_TO_FIRST_PATTERN )
  {
    if( m_slash && pattern.equals( m_star ) )
    {
      CV().PrintCursor();
      return;
    }
    // Un-highlight old star patterns for windows displayed:
    if( 0<m_star.length()  )
    { // Since diff_mode does Console::Update(),
      // no need to print patterns here if in diff_mode
      if( !m_diff_mode ) Do_Star_PrintPatterns( false );
    }
    Do_Star_ClearPatterns();

    m_star = pattern;

    final int ROW = CV().Cmd__Line_Row();

    if( m_star.length()<=0 )
    {
      final int COL = CV().Col_Win_2_GL( 1 );
      // Remove cursor from command line row:
      m_console.Set( ROW, COL, ' ', Style.NORMAL );
      // Put cursor back in window:
      CV().PrintCursor();
    }
    else {
      m_slash = true;

      Do_Star_Update_Search_Editor();
      Do_Star_FindPatterns();

      // Highlight new star patterns for windows displayed:
      if( !m_diff_mode ) Do_Star_PrintPatterns( true );

      if( MOVE_TO_FIRST_PATTERN )
      {
        if( m_diff_mode ) m_diff.Do_n(); // Move to first pattern
        else                CV().Do_n(); // Move to first pattern
      }
      if( m_diff_mode ) m_diff.Update();
      else {
        final int COL = CV().Col_Win_2_GL( m_star.length()+1 );
        // Remove cursor from command line row:
        m_console.Set( ROW, COL, ' ', Style.NORMAL );
        // Print out all the changes:
        m_console.Update();
      }
    }
  }
  void Exe_Colon_q()
  {
    final Tile_Pos TP = CV().m_tile_pos;

    if( m_num_wins <= 1 ) Exe_Colon_qa();
    else {
      if( m_win < m_num_wins-1 )
      {
        Quit_ShiftDown();
      }
      if( 0 < m_win ) m_win--;
      m_num_wins--;

      Quit_JoinTiles( TP );

      UpdateViews();

      CV().PrintCursor();
    }
  }
  void Exe_Colon_qa()
  {
    System.exit( 0 );
  }
  void Quit_ShiftDown()
  {
    // Make copy of m_win's list of views and view history:
    ViewList win_views     = new ViewList( m_views    [m_win] );
     IntList win_view_hist = new  IntList( m_file_hist[m_win] );

    // Shift everything down
    for( int w=m_win+1; w<m_num_wins; w++ )
    {
      m_views    [w-1] = m_views    [w];
      m_file_hist[w-1] = m_file_hist[w];
    }
    // Put m_win's list of m_views at end of m_views:
    // Put m_win's view history at end of view historys:
    m_views    [m_num_wins-1] = win_views;
    m_file_hist[m_num_wins-1] = win_view_hist;
  }
  void Quit_JoinTiles( final Tile_Pos TP )
  {
    // m_win is disappearing, so move its screen space to another view:
    if     ( TP == Tile_Pos.LEFT_HALF )         Quit_JoinTiles_LEFT_HALF();
    else if( TP == Tile_Pos.RITE_HALF )         Quit_JoinTiles_RITE_HALF();
    else if( TP == Tile_Pos.TOP__HALF )         Quit_JoinTiles_TOP__HALF();
    else if( TP == Tile_Pos.BOT__HALF )         Quit_JoinTiles_BOT__HALF();
    else if( TP == Tile_Pos.TOP__LEFT_QTR )     Quit_JoinTiles_TOP__LEFT_QTR();
    else if( TP == Tile_Pos.TOP__RITE_QTR )     Quit_JoinTiles_TOP__RITE_QTR();
    else if( TP == Tile_Pos.BOT__LEFT_QTR )     Quit_JoinTiles_BOT__LEFT_QTR();
    else if( TP == Tile_Pos.BOT__RITE_QTR )     Quit_JoinTiles_BOT__RITE_QTR();
    else if( TP == Tile_Pos.LEFT_QTR )          Quit_JoinTiles_LEFT_QTR();
    else if( TP == Tile_Pos.RITE_QTR )          Quit_JoinTiles_RITE_QTR();
    else if( TP == Tile_Pos.LEFT_CTR__QTR )     Quit_JoinTiles_LEFT_CTR__QTR();
    else if( TP == Tile_Pos.RITE_CTR__QTR )     Quit_JoinTiles_RITE_CTR__QTR();
    else if( TP == Tile_Pos.TOP__LEFT_8TH )     Quit_JoinTiles_TOP__LEFT_8TH();
    else if( TP == Tile_Pos.TOP__RITE_8TH )     Quit_JoinTiles_TOP__RITE_8TH();
    else if( TP == Tile_Pos.TOP__LEFT_CTR_8TH ) Quit_JoinTiles_TOP__LEFT_CTR_8TH();
    else if( TP == Tile_Pos.TOP__RITE_CTR_8TH ) Quit_JoinTiles_TOP__RITE_CTR_8TH();
    else if( TP == Tile_Pos.BOT__LEFT_8TH )     Quit_JoinTiles_BOT__LEFT_8TH();
    else if( TP == Tile_Pos.BOT__RITE_8TH )     Quit_JoinTiles_BOT__RITE_8TH();
    else if( TP == Tile_Pos.BOT__LEFT_CTR_8TH ) Quit_JoinTiles_BOT__LEFT_CTR_8TH();
    else /*( TP == Tile_Pos.BOT__RITE_CTR_8TH*/ Quit_JoinTiles_BOT__RITE_CTR_8TH();
  }
  void Quit_JoinTiles_LEFT_HALF()
  {
    for( int k=0; k<m_num_wins; k++ )
    {
      View v = GetView_Win( k );
      final Tile_Pos TP = v.m_tile_pos;
  
      if     ( TP == Tile_Pos.RITE_HALF         ) { v.SetTilePos( Tile_Pos.FULL ); break; }
      else if( TP == Tile_Pos.TOP__RITE_QTR     ) v.SetTilePos( Tile_Pos.TOP__HALF );
      else if( TP == Tile_Pos.BOT__RITE_QTR     ) v.SetTilePos( Tile_Pos.BOT__HALF );
      else if( TP == Tile_Pos.RITE_QTR          ) v.SetTilePos( Tile_Pos.RITE_HALF );
      else if( TP == Tile_Pos.RITE_CTR__QTR     ) v.SetTilePos( Tile_Pos.LEFT_HALF );
      else if( TP == Tile_Pos.TOP__RITE_8TH     ) v.SetTilePos( Tile_Pos.TOP__RITE_QTR );
      else if( TP == Tile_Pos.TOP__RITE_CTR_8TH ) v.SetTilePos( Tile_Pos.TOP__LEFT_QTR );
      else if( TP == Tile_Pos.BOT__RITE_8TH     ) v.SetTilePos( Tile_Pos.BOT__RITE_QTR );
      else if( TP == Tile_Pos.BOT__RITE_CTR_8TH ) v.SetTilePos( Tile_Pos.BOT__LEFT_QTR );
    }
  }
  void Quit_JoinTiles_RITE_HALF()
  {
    for( int k=0; k<m_num_wins; k++ )
    {
      View v = GetView_Win( k );
      final Tile_Pos TP = v.m_tile_pos;
  
      if     ( TP == Tile_Pos.LEFT_HALF         ) { v.SetTilePos( Tile_Pos.FULL ); break; }
      else if( TP == Tile_Pos.TOP__LEFT_QTR     ) v.SetTilePos( Tile_Pos.TOP__HALF );
      else if( TP == Tile_Pos.BOT__LEFT_QTR     ) v.SetTilePos( Tile_Pos.BOT__HALF );
      else if( TP == Tile_Pos.LEFT_QTR          ) v.SetTilePos( Tile_Pos.LEFT_HALF );
      else if( TP == Tile_Pos.LEFT_CTR__QTR     ) v.SetTilePos( Tile_Pos.RITE_HALF );
      else if( TP == Tile_Pos.TOP__LEFT_8TH     ) v.SetTilePos( Tile_Pos.TOP__LEFT_QTR );
      else if( TP == Tile_Pos.TOP__LEFT_CTR_8TH ) v.SetTilePos( Tile_Pos.TOP__RITE_QTR );
      else if( TP == Tile_Pos.BOT__LEFT_8TH     ) v.SetTilePos( Tile_Pos.BOT__LEFT_QTR );
      else if( TP == Tile_Pos.BOT__LEFT_CTR_8TH ) v.SetTilePos( Tile_Pos.BOT__RITE_QTR );
    }
  }
  void Quit_JoinTiles_TOP__HALF()
  {
    for( int k=0; k<m_num_wins; k++ )
    {
      View v = GetView_Win( k );
      final Tile_Pos TP = v.m_tile_pos;
 
      if     ( TP == Tile_Pos.BOT__HALF         ) { v.SetTilePos( Tile_Pos.FULL ); break; }
      else if( TP == Tile_Pos.BOT__LEFT_QTR     ) v.SetTilePos( Tile_Pos.LEFT_HALF );
      else if( TP == Tile_Pos.BOT__RITE_QTR     ) v.SetTilePos( Tile_Pos.RITE_HALF );
      else if( TP == Tile_Pos.BOT__LEFT_8TH     ) v.SetTilePos( Tile_Pos.LEFT_QTR );
      else if( TP == Tile_Pos.BOT__RITE_8TH     ) v.SetTilePos( Tile_Pos.RITE_QTR );
      else if( TP == Tile_Pos.BOT__LEFT_CTR_8TH ) v.SetTilePos( Tile_Pos.LEFT_CTR__QTR );
      else if( TP == Tile_Pos.BOT__RITE_CTR_8TH ) v.SetTilePos( Tile_Pos.RITE_CTR__QTR );
    }
  }
  void Quit_JoinTiles_BOT__HALF()
  {
    for( int k=0; k<m_num_wins; k++ )
    {
      View v = GetView_Win( k );
      final Tile_Pos TP = v.m_tile_pos;
  
      if     ( TP == Tile_Pos.TOP__HALF         ) { v.SetTilePos( Tile_Pos.FULL ); break; }
      else if( TP == Tile_Pos.TOP__LEFT_QTR     ) v.SetTilePos( Tile_Pos.LEFT_HALF );
      else if( TP == Tile_Pos.TOP__RITE_QTR     ) v.SetTilePos( Tile_Pos.RITE_HALF );
      else if( TP == Tile_Pos.TOP__LEFT_8TH     ) v.SetTilePos( Tile_Pos.LEFT_QTR );
      else if( TP == Tile_Pos.TOP__RITE_8TH     ) v.SetTilePos( Tile_Pos.RITE_QTR );
      else if( TP == Tile_Pos.TOP__LEFT_CTR_8TH ) v.SetTilePos( Tile_Pos.LEFT_CTR__QTR );
      else if( TP == Tile_Pos.TOP__RITE_CTR_8TH ) v.SetTilePos( Tile_Pos.RITE_CTR__QTR );
    }
  }
  void Quit_JoinTiles_TOP__LEFT_QTR()
  {
    if( Have_BOT__HALF() )
    {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if     ( TP == Tile_Pos.TOP__RITE_QTR     ) { v.SetTilePos( Tile_Pos.TOP__HALF ); break; }
        else if( TP == Tile_Pos.TOP__RITE_8TH     ) v.SetTilePos( Tile_Pos.TOP__RITE_QTR );
        else if( TP == Tile_Pos.TOP__RITE_CTR_8TH ) v.SetTilePos( Tile_Pos.TOP__LEFT_QTR );
      }
    }
    else {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if     ( TP == Tile_Pos.BOT__LEFT_QTR     ) { v.SetTilePos( Tile_Pos.LEFT_HALF ); break; }
        else if( TP == Tile_Pos.BOT__LEFT_8TH     ) v.SetTilePos( Tile_Pos.LEFT_QTR );
        else if( TP == Tile_Pos.BOT__LEFT_CTR_8TH ) v.SetTilePos( Tile_Pos.LEFT_CTR__QTR );
      }
    }
  }
  void Quit_JoinTiles_TOP__RITE_QTR()
  {
    if( Have_BOT__HALF() )
    {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if     ( TP == Tile_Pos.TOP__LEFT_QTR     ) { v.SetTilePos( Tile_Pos.TOP__HALF ); break; }
        else if( TP == Tile_Pos.TOP__LEFT_8TH     ) v.SetTilePos( Tile_Pos.TOP__LEFT_QTR );
        else if( TP == Tile_Pos.TOP__LEFT_CTR_8TH ) v.SetTilePos( Tile_Pos.TOP__RITE_QTR );
      }
    }
    else {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if     ( TP == Tile_Pos.BOT__RITE_QTR     ) { v.SetTilePos( Tile_Pos.RITE_HALF ); break; }
        else if( TP == Tile_Pos.BOT__RITE_8TH     ) v.SetTilePos( Tile_Pos.RITE_QTR );
        else if( TP == Tile_Pos.BOT__RITE_CTR_8TH ) v.SetTilePos( Tile_Pos.RITE_CTR__QTR );
      }
    }
  }
  void Quit_JoinTiles_BOT__LEFT_QTR()
  {
    if( Have_TOP__HALF() )
    {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if     ( TP == Tile_Pos.BOT__RITE_QTR     ) { v.SetTilePos( Tile_Pos.BOT__HALF ); break; }
        else if( TP == Tile_Pos.BOT__RITE_8TH     ) v.SetTilePos( Tile_Pos.BOT__RITE_QTR );
        else if( TP == Tile_Pos.BOT__RITE_CTR_8TH ) v.SetTilePos( Tile_Pos.BOT__LEFT_QTR );
      }
    }
    else {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if     ( TP == Tile_Pos.TOP__LEFT_QTR     ) { v.SetTilePos( Tile_Pos.LEFT_HALF ); break; }
        else if( TP == Tile_Pos.TOP__LEFT_8TH     ) v.SetTilePos( Tile_Pos.LEFT_QTR );
        else if( TP == Tile_Pos.TOP__LEFT_CTR_8TH ) v.SetTilePos( Tile_Pos.LEFT_CTR__QTR );
      }
    }
  }
  void Quit_JoinTiles_BOT__RITE_QTR()
  {
    if( Have_TOP__HALF() )
    {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if     ( TP == Tile_Pos.BOT__LEFT_QTR     ) { v.SetTilePos( Tile_Pos.BOT__HALF ); break; }
        else if( TP == Tile_Pos.BOT__LEFT_8TH     ) v.SetTilePos( Tile_Pos.BOT__LEFT_QTR );
        else if( TP == Tile_Pos.BOT__LEFT_CTR_8TH ) v.SetTilePos( Tile_Pos.BOT__RITE_QTR );
      }
    }
    else {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if     ( TP == Tile_Pos.TOP__RITE_QTR     ) { v.SetTilePos( Tile_Pos.RITE_HALF ); break; }
        else if( TP == Tile_Pos.TOP__RITE_8TH     ) v.SetTilePos( Tile_Pos.RITE_QTR );
        else if( TP == Tile_Pos.TOP__RITE_CTR_8TH ) v.SetTilePos( Tile_Pos.RITE_CTR__QTR );
      }
    }
  }
  void Quit_JoinTiles_LEFT_QTR()
  {
    for( int k=0; k<m_num_wins; k++ )
    {
      View v = GetView_Win( k );
      final Tile_Pos TP = v.m_tile_pos;
  
      if     ( TP == Tile_Pos.LEFT_CTR__QTR     ) { v.SetTilePos( Tile_Pos.LEFT_HALF ); break; }
      else if( TP == Tile_Pos.TOP__LEFT_CTR_8TH ) v.SetTilePos( Tile_Pos.TOP__LEFT_QTR );
      else if( TP == Tile_Pos.BOT__LEFT_CTR_8TH ) v.SetTilePos( Tile_Pos.BOT__LEFT_QTR );
    }
  }
  void Quit_JoinTiles_RITE_QTR()
  {
    for( int k=0; k<m_num_wins; k++ )
    {
      View v = GetView_Win( k );
      final Tile_Pos TP = v.m_tile_pos;
  
      if     ( TP == Tile_Pos.RITE_CTR__QTR     ) { v.SetTilePos( Tile_Pos.RITE_HALF ); break; }
      else if( TP == Tile_Pos.TOP__RITE_CTR_8TH ) v.SetTilePos( Tile_Pos.TOP__RITE_QTR );
      else if( TP == Tile_Pos.BOT__RITE_CTR_8TH ) v.SetTilePos( Tile_Pos.BOT__RITE_QTR );
    }
  }
  void Quit_JoinTiles_LEFT_CTR__QTR()
  {
    for( int k=0; k<m_num_wins; k++ )
    {
      View v = GetView_Win( k );
      final Tile_Pos TP = v.m_tile_pos;
  
      if     ( TP == Tile_Pos.LEFT_QTR      ) { v.SetTilePos( Tile_Pos.LEFT_HALF ); break; }
      else if( TP == Tile_Pos.TOP__LEFT_8TH ) v.SetTilePos( Tile_Pos.TOP__LEFT_QTR );
      else if( TP == Tile_Pos.BOT__LEFT_8TH ) v.SetTilePos( Tile_Pos.BOT__LEFT_QTR );
    }
  }
  void Quit_JoinTiles_RITE_CTR__QTR()
  {
    for( int k=0; k<m_num_wins; k++ )
    {
      View v = GetView_Win( k );
      final Tile_Pos TP = v.m_tile_pos;
  
      if     ( TP == Tile_Pos.RITE_QTR      ) { v.SetTilePos( Tile_Pos.RITE_HALF ); break; }
      else if( TP == Tile_Pos.TOP__RITE_8TH ) v.SetTilePos( Tile_Pos.TOP__RITE_QTR );
      else if( TP == Tile_Pos.BOT__RITE_8TH ) v.SetTilePos( Tile_Pos.BOT__RITE_QTR );
    }
  }
  void Quit_JoinTiles_TOP__LEFT_8TH()
  {
    if( Have_BOT__LEFT_QTR() )
    {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if( TP == Tile_Pos.TOP__LEFT_CTR_8TH ) { v.SetTilePos( Tile_Pos.TOP__LEFT_QTR ); break; }
      }
    }
    else {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if( TP == Tile_Pos.BOT__LEFT_8TH ) { v.SetTilePos( Tile_Pos.LEFT_QTR ); break; }
      }
    }
  }
  void Quit_JoinTiles_TOP__RITE_8TH()
  {
    if( Have_BOT__RITE_QTR() )
    {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if( TP == Tile_Pos.TOP__RITE_CTR_8TH ) { v.SetTilePos( Tile_Pos.TOP__RITE_QTR ); break; }
      }
    }
    else {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if( TP == Tile_Pos.BOT__RITE_8TH ) { v.SetTilePos( Tile_Pos.RITE_QTR ); break; }
      }
    }
  }
  void Quit_JoinTiles_TOP__LEFT_CTR_8TH()
  {
    if( Have_BOT__LEFT_QTR() )
    {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if( TP == Tile_Pos.TOP__LEFT_8TH ) { v.SetTilePos( Tile_Pos.TOP__LEFT_QTR ); break; }
      }
    }
    else {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if( TP == Tile_Pos.BOT__LEFT_CTR_8TH ) { v.SetTilePos( Tile_Pos.LEFT_CTR__QTR ); break; }
      }
    }
  }
  void Quit_JoinTiles_TOP__RITE_CTR_8TH()
  {
    if( Have_BOT__RITE_QTR() )
    {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if( TP == Tile_Pos.TOP__RITE_8TH ) { v.SetTilePos( Tile_Pos.TOP__RITE_QTR ); break; }
      }
    }
    else {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if( TP == Tile_Pos.BOT__RITE_CTR_8TH ) { v.SetTilePos( Tile_Pos.RITE_CTR__QTR ); break; }
      }
    }
  }
  void Quit_JoinTiles_BOT__LEFT_8TH()
  {
    if( Have_TOP__LEFT_QTR() )
    {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if( TP == Tile_Pos.BOT__LEFT_CTR_8TH ) { v.SetTilePos( Tile_Pos.BOT__LEFT_QTR ); break; }
      }
    }
    else {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if( TP == Tile_Pos.TOP__LEFT_8TH ) { v.SetTilePos( Tile_Pos.LEFT_QTR ); break; }
      }
    }
  }
  void Quit_JoinTiles_BOT__RITE_8TH()
  {
    if( Have_TOP__RITE_QTR() )
    {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if( TP == Tile_Pos.BOT__RITE_CTR_8TH ) { v.SetTilePos( Tile_Pos.BOT__RITE_QTR ); break; }
      }
    }
    else {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if( TP == Tile_Pos.TOP__RITE_8TH ) { v.SetTilePos( Tile_Pos.RITE_QTR ); break; }
      }
    }
  }
  void Quit_JoinTiles_BOT__LEFT_CTR_8TH()
  {
    if( Have_TOP__LEFT_QTR() )
    {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if( TP == Tile_Pos.BOT__LEFT_8TH ) { v.SetTilePos( Tile_Pos.BOT__LEFT_QTR ); break; }
      }
    }
    else {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if( TP == Tile_Pos.TOP__LEFT_CTR_8TH ) { v.SetTilePos( Tile_Pos.LEFT_CTR__QTR ); break; }
      }
    }
  }
  void Quit_JoinTiles_BOT__RITE_CTR_8TH()
  {
    if( Have_TOP__RITE_QTR() )
    {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if( TP == Tile_Pos.BOT__RITE_8TH ) { v.SetTilePos( Tile_Pos.BOT__RITE_QTR ); break; }
      }
    }
    else {
      for( int k=0; k<m_num_wins; k++ )
      {
        View v = GetView_Win( k );
        final Tile_Pos TP = v.m_tile_pos;
  
        if( TP == Tile_Pos.TOP__RITE_CTR_8TH ) { v.SetTilePos( Tile_Pos.RITE_CTR__QTR ); break; }
      }
    }
  }
  boolean Have_BOT__HALF()
  {
    for( int k=0; k<m_num_wins; k++ )
    {
      View v = GetView_Win( k );
      final Tile_Pos TP = v.m_tile_pos;
  
      if( TP == Tile_Pos.BOT__HALF ) return true;
    }
    return false;
  }
  boolean Have_TOP__HALF()
  {
    for( int k=0; k<m_num_wins; k++ )
    {
      View v = GetView_Win( k );
      final Tile_Pos TP = v.m_tile_pos;
  
      if( TP == Tile_Pos.TOP__HALF ) return true;
    }
    return false;
  }
  boolean Have_BOT__LEFT_QTR()
  {
    for( int k=0; k<m_num_wins; k++ )
    {
      View v = GetView_Win( k );
      final Tile_Pos TP = v.m_tile_pos;
  
      if( TP == Tile_Pos.BOT__LEFT_QTR ) return true;
    }
    return false;
  }
  boolean Have_TOP__LEFT_QTR()
  {
    for( int k=0; k<m_num_wins; k++ )
    {
      View v = GetView_Win( k );
      final Tile_Pos TP = v.m_tile_pos;
  
      if( TP == Tile_Pos.TOP__LEFT_QTR ) return true;
    }
    return false;
  }
  boolean Have_BOT__RITE_QTR()
  {
    for( int k=0; k<m_num_wins; k++ )
    {
      View v = GetView_Win( k );
      final Tile_Pos TP = v.m_tile_pos;
  
      if( TP == Tile_Pos.BOT__RITE_QTR ) return true;
    }
    return false;
  }
  boolean Have_TOP__RITE_QTR()
  {
    for( int k=0; k<m_num_wins; k++ )
    {
      View v = GetView_Win( k );
      final Tile_Pos TP = v.m_tile_pos;
  
      if( TP == Tile_Pos.TOP__RITE_QTR ) return true;
    }
    return false;
  }

  void Exe_Colon_help()
  {
    GoToBuffer( HELP_FILE );
  }

  void Exe_Colon_DoDiff()
  {
    // Must be exactly 2 buffers to do diff:
    if( 2 == m_num_wins )
    {
      View v0 = GetView_Win( 0 );
      View v1 = GetView_Win( 1 );
      FileBuf fb0 = v0.m_fb;
      FileBuf fb1 = v1.m_fb;
 
      // New code in progress:
      boolean ok = true;
      if( !fb0.m_isDir && fb1.m_isDir )
      {
        v1 = DoDiff_FindRegFileView( fb0, fb1, 1, v1 );
      }
      else if( fb0.m_isDir && !fb1.m_isDir )
      {
        v0 = DoDiff_FindRegFileView( fb1, fb0, 0, v0 );
      }
      else {
        if( !Paths.get( fb0.m_fname ).toFile().exists() )
        {
          ok = false;
          Utils.Log("");
          Utils.Log( fb0.m_fname + " does not exist");
        }
        if( !Paths.get( fb1.m_fname ).toFile().exists() )
        {
          ok = false;
          Utils.Log("");
          Utils.Log( fb1.m_fname + " does not exist");
        }
      }
      if( !ok )
      {
        System.exit( 0 );
      }
      else {
        ok = m_diff.Run( v0, v1 );
        if( ok ) {
          m_diff_mode = true;
          if( m_states.peekFirst() != m_run_focus )
          {
            m_diff.Update();
          }
        }
      }
    }
  }
  View DoDiff_FindRegFileView( final FileBuf pfb_reg
                             , final FileBuf pfb_dir
                             , final int     win_idx
                             ,       View    pv )
  {
    StringBuilder possible_fname = new StringBuilder( pfb_dir.m_fname );
    StringBuilder fname_extension = new StringBuilder();

    final int BASE_LEN = possible_fname.length();
    // vit -d dir1/file.cc dir2/
    // Try -d m_cwd/dir1/file.cc m_cwd/dir2/file.cc
    // Dir file name always has DIR_DELIM at end, so no need to add
    // a DIR_DELIM before appending head:
    String split_delim = Utils.DIR_DELIM_STR;
    if( split_delim.equals("\\") ) split_delim = "\\\\";

    String[] path_parts = pfb_reg.m_fname.split( split_delim );

    for( int k=path_parts.length-1; 0<=k; k-- )
    {
      // Revert back to pfb_dir.m_fname:
      possible_fname.setLength( BASE_LEN );

      if( 0<fname_extension.length()
       && fname_extension.charAt(0)!=Utils.DIR_DELIM )
      {
        fname_extension.insert( 0, Utils.DIR_DELIM );
      }
      fname_extension.insert( 0, path_parts[k] );

      possible_fname.append( fname_extension );

    //String pos_fname = Utils.FindFullFileName_Path( m_cwd, possible_fname.toString() );
      String pos_fname = possible_fname.toString();

      View nv = DoDiff_CheckPossibleFile( win_idx, pos_fname );

      if( null != nv ) return nv;
    }
    return pv;
  }
  // If file is found, puts View of file in win_idx window,
  // and returns the View, else returns null
  View DoDiff_CheckPossibleFile( final int win_idx
                               , String pos_fname )
  {
    if( Paths.get( pos_fname ).toFile().exists() )
    {
      // File exists, find or create FileBuf, and set second view to display that file:
      if( !HaveFile( pos_fname, null ) )
      {
        FileBuf fb = new FileBuf( this, pos_fname, true );
        boolean ok = fb.ReadFile();
        if( ok ) Add_FileBuf_2_Lists_Create_Views( fb, pos_fname );
      }
      Ptr_Int file_index = new Ptr_Int( 0 );
      if( HaveFile( pos_fname, file_index ) )
      {
        SetWinToBuffer( win_idx, file_index.val, false );

        return GetView_Win( win_idx );
      }
    }
    return null;
  }

  void Exe_Colon_NoDiff()
  {
    if( true == m_diff_mode )
    {
      m_diff_mode = false;

      UpdateViews();
    }
  }

  void Exe_Colon_hi()
  {
    CV().m_fb.m_hi_touched_line = 0;

    if( m_diff_mode ) m_diff.Update();
    else                CV().Update();
  }

  void Exe_Colon_run()
  {
    if( CMD_FILE == m_file_hist[ m_win ].get( 0 ) )
    {
      if( null == m_shell )
      {
        m_shell = new Shell( this, m_console, CV() );
      }
      m_shell.Run();
    }
  }
  void Exe_Colon_pwd()
  {
    CmdLineMessage( m_cwd );
  }
  void Exe_Colon_cd()
  {
    String[] tokens = m_sb.toString().split("\\s");

    if( tokens.length == 1 ) // ":cd" to location of current file
    {
      m_cwd = CV().m_fb.m_pname;

      CmdLineMessage( m_cwd );
    }
    else if( tokens.length == 2 ) // ":cd path"
    {
      m_cwd = tokens[1];

      CmdLineMessage( m_cwd );
    }
    else {
      CmdLineMessage( m_sb.toString() +" failed" );
    }
  }

  void Exe_Colon_Syntax()
  {
    String[] toks = m_sb.toString().split("=");

    if( toks.length==2 )
    {
      CV().m_fb.Set_File_Type( toks[1] );
    }
  }

  void Exe_Colon_MapStart()
  {
    m_console.m_map_buf.setLength( 0 );
    m_console.m_save_2_map_buf = true;

    if( m_diff_mode ) m_diff.DisplayMapping();
    else                CV().DisplayMapping();
  }
  void Exe_Colon_MapEnd()
  {
    if( m_console.m_save_2_map_buf )
    {
      m_console.m_save_2_map_buf = false;
      // Remove trailing ':' from m_console.map_buf:
      StringBuilder map_buf = m_console.m_map_buf;
      if( 0<map_buf.length() ) map_buf.deleteCharAt( map_buf.length()-1 ); // '\n'
      if( 0<map_buf.length() ) map_buf.deleteCharAt( map_buf.length()-1 ); // ':'
    }
  }
  void Exe_Colon_MapShow()
  {
    final View V = CV();
    V.Clear_Console_CrsCell();
    final int ROW = V.Cmd__Line_Row();
    final int ST  = V.Col_Win_2_GL( 0 );
    final int WC  = V.WorkingCols();
    final int MAP_LEN = m_console.m_map_buf.length();

    // Print :
    m_console.Set( ROW, ST, ':', Style.NORMAL );

    // Print map
    int offset = 1;
    for( int k=0; k<MAP_LEN && offset+k<WC; k++ )
    {
      final char C = m_console.m_map_buf.charAt( k );
      if( C == '\n' )
      {
        m_console.Set( ROW, ST+offset+k, '<', Style.NORMAL ); offset++;
        m_console.Set( ROW, ST+offset+k, 'C', Style.NORMAL ); offset++;
        m_console.Set( ROW, ST+offset+k, 'R', Style.NORMAL ); offset++;
        m_console.Set( ROW, ST+offset+k, '>', Style.NORMAL );
      }
      else if( C == m_console.ESC )
      {
        m_console.Set( ROW, ST+offset+k, '<', Style.NORMAL ); offset++;
        m_console.Set( ROW, ST+offset+k, 'E', Style.NORMAL ); offset++;
        m_console.Set( ROW, ST+offset+k, 'S', Style.NORMAL ); offset++;
        m_console.Set( ROW, ST+offset+k, 'C', Style.NORMAL ); offset++;
        m_console.Set( ROW, ST+offset+k, '>', Style.NORMAL );
      }
      else {
        m_console.Set( ROW, ST+offset+k, C, Style.NORMAL );
      }
    }
    // Print empty space after map
    for( int k=MAP_LEN; offset+k<WC; k++ )
    {
      m_console.Set( ROW, ST+offset+k, ' ', Style.NORMAL );
    }
    if( m_diff_mode ) m_diff.PrintCursor();
    else                   V.PrintCursor();
  }

  void Exe_Colon_CoverKey()
  {
    m_colon.Run_GetCoverKey( CV() );
  }
  void Exe_Colon_Cover()
  {
    View V = CV();
    FileBuf fb = V.m_fb;

    if( fb.m_isDir )
    {
      V.PrintCursor();
    }
    else {
      final int seed = fb.GetSize() % 256;

      Cover.Cover_Array( fb, m_cover_buf, seed, m_cover_key );

      // Fill in m_cover_buf from old file data:
      // Clear old file:
      fb.ClearLines();

      // Read in covered file:
      fb.ReadArray( m_cover_buf );

      // Keep V.m_unsaved_changes in sync with V.m_fb.Changed()
      Update_Change_Statuses();

      // Reset view position:
      V.Clear_Context();

      V.Update();
    }
  }

  void Exe_Colon_w()
  {
    final View V = CV();

    boolean file_written = false;

    if( m_sb.toString().equals("w") ) // :w
    {
      if( V == m_views[ m_win ].get( CMD_FILE ) )
      {
        // Dont allow SHELL_BUFFER to be saved with :w.
        // Require :w filename.
        V.PrintCursor();
      }
      else {
        // If the file gets written, CmdLineMessage will be called,
        // which will put the cursor back in position,
        // else Window_Message will be called
        // which will put the cursor back in the message window
        file_written = V.m_fb.Write();
      }
    }
    else // :w file_name
    {
      // Edit file of supplied file name:
      String fname = new String( m_sb.substring(1) );

      fname = CV().m_fb.Relative_2_FullFname( fname );

      Ptr_Int file_index = new Ptr_Int( 0 );
      if( HaveFile( fname, file_index ) )
      {
        m_files.get( file_index.val ).Write();
      }
      else if( Utils.DIR_DELIM != fname.charAt( fname.length()-1 ) )
      {
        FileBuf fb = new FileBuf( this, fname, CV().m_fb );

        Add_FileBuf_2_Lists_Create_Views( fb, fname );

        file_written = fb.Write();
      }
    }
    if( file_written )
    {
      if( Update_Change_Statuses() )
      {
        if( m_diff_mode ) m_diff.PrintCursor(); // Does m_console.Update()
        else              m_console.Update();
      }
    }
  }
  void Exe_Colon_b()
  {
    if( m_sb.toString().equals("b") ) // :b
    {
      GoToPrevBuffer();
    }
    else {
      // Switch to a different buffer:
      if     ( m_sb.toString().equals("b#") ) GoToPoundBuffer(); // :b#
      else if( m_sb.toString().equals("bc") ) GoToCurrBuffer();  // :bc
      else if( m_sb.toString().equals("be") ) GoToFileBuffer();  // :be
      else if( m_sb.toString().equals("bm") ) GoToMsgBuffer();   // :bm
      else {                                                     // :b<number>
        Ptr_Int buffer_num = new Ptr_Int( 0 );
        if( Utils.String_2_Int( m_sb.substring(1), buffer_num ) )
        {
          GoToBuffer( buffer_num.val );
        }
        else {
          CV().PrintCursor();
        }
      }
    }
  }
  void Exe_Colon_n()
  {
    GoToNextBuffer();
  }
  void GoToNextBuffer()
  {
    final int FILE_HIST_LEN = m_file_hist[ m_win ].size();

    if( FILE_HIST_LEN <= 1 )
    {
      // Nothing to do, so just put cursor back
      CV().PrintCursor();
    }
    else {
      Tile_Pos tp_old = CV().m_tile_pos;

      // Move view index at back to front of m_file_hist
      int view_index_new = m_file_hist[ m_win ].pop();
      m_file_hist[ m_win ].add( 0, view_index_new );

      // Redisplay current window with new view:
      CV().SetTilePos( tp_old );
      CV().Update();
    }
  }
  void Exe_Colon_vsp()
  {
    View cv = CV();
    final Tile_Pos cv_tp = cv.m_tile_pos;
 
    if( m_num_wins < MAX_WINS 
     && ( cv_tp == Tile_Pos.FULL
       || cv_tp == Tile_Pos.TOP__HALF
       || cv_tp == Tile_Pos.BOT__HALF
       || cv_tp == Tile_Pos.LEFT_HALF
       || cv_tp == Tile_Pos.RITE_HALF
       || cv_tp == Tile_Pos.TOP__LEFT_QTR
       || cv_tp == Tile_Pos.BOT__LEFT_QTR
       || cv_tp == Tile_Pos.TOP__RITE_QTR
       || cv_tp == Tile_Pos.BOT__RITE_QTR ) )
    {
      m_file_hist[m_num_wins].copy( m_file_hist[m_win] );
 
      View nv = GetView_Win( m_num_wins );
 
      nv.Set_Context( cv );
 
      m_win = m_num_wins;
      m_num_wins++;
 
      if( cv_tp == Tile_Pos.FULL )
      {
        cv.SetTilePos( Tile_Pos.LEFT_HALF );
        nv.SetTilePos( Tile_Pos.RITE_HALF );
      }
      else if( cv_tp == Tile_Pos.TOP__HALF )
      {
        cv.SetTilePos( Tile_Pos.TOP__LEFT_QTR );
        nv.SetTilePos( Tile_Pos.TOP__RITE_QTR );
      }
      else if( cv_tp == Tile_Pos.BOT__HALF )
      {
        cv.SetTilePos( Tile_Pos.BOT__LEFT_QTR );
        nv.SetTilePos( Tile_Pos.BOT__RITE_QTR );
      }
      else if( cv_tp == Tile_Pos.LEFT_HALF )
      {
        cv.SetTilePos( Tile_Pos.LEFT_QTR );
        nv.SetTilePos( Tile_Pos.LEFT_CTR__QTR );
      }
      else if( cv_tp == Tile_Pos.RITE_HALF )
      {
        cv.SetTilePos( Tile_Pos.RITE_CTR__QTR );
        nv.SetTilePos( Tile_Pos.RITE_QTR );
      }
      else if( cv_tp == Tile_Pos.TOP__LEFT_QTR )
      {
        cv.SetTilePos( Tile_Pos.TOP__LEFT_8TH );
        nv.SetTilePos( Tile_Pos.TOP__LEFT_CTR_8TH );
      }
      else if( cv_tp == Tile_Pos.BOT__LEFT_QTR )
      {
        cv.SetTilePos( Tile_Pos.BOT__LEFT_8TH );
        nv.SetTilePos( Tile_Pos.BOT__LEFT_CTR_8TH );
      }
      else if( cv_tp == Tile_Pos.TOP__RITE_QTR )
      {
        cv.SetTilePos( Tile_Pos.TOP__RITE_CTR_8TH );
        nv.SetTilePos( Tile_Pos.TOP__RITE_8TH );
      }
      else //( cv_tp == Tile_Pos.BOT__RITE_QTR )
      {
        cv.SetTilePos( Tile_Pos.BOT__RITE_CTR_8TH );
        nv.SetTilePos( Tile_Pos.BOT__RITE_8TH );
      }
    }
    UpdateViews();
  }
  void Exe_Colon_sp()
  {
    View cv = CV();
    final Tile_Pos cv_tp = cv.m_tile_pos;

    if( m_num_wins < MAX_WINS 
     && ( cv_tp == Tile_Pos.FULL
       || cv_tp == Tile_Pos.LEFT_HALF
       || cv_tp == Tile_Pos.RITE_HALF
       || cv_tp == Tile_Pos.LEFT_QTR
       || cv_tp == Tile_Pos.RITE_QTR
       || cv_tp == Tile_Pos.LEFT_CTR__QTR
       || cv_tp == Tile_Pos.RITE_CTR__QTR ) )
    {
      m_file_hist[m_num_wins].copy( m_file_hist[m_win] );

      View nv = GetView_Win( m_num_wins );

      nv.Set_Context( cv );

      m_win = m_num_wins;
      m_num_wins++;

      if( cv_tp == Tile_Pos.FULL )
      {
        cv.SetTilePos( Tile_Pos.TOP__HALF );
        nv.SetTilePos( Tile_Pos.BOT__HALF );
      }
      else if( cv_tp == Tile_Pos.LEFT_HALF )
      {
        cv.SetTilePos( Tile_Pos.TOP__LEFT_QTR );
        nv.SetTilePos( Tile_Pos.BOT__LEFT_QTR );
      }
      else if( cv_tp == Tile_Pos.RITE_HALF )
      {
        cv.SetTilePos( Tile_Pos.TOP__RITE_QTR );
        nv.SetTilePos( Tile_Pos.BOT__RITE_QTR );
      }
      else if( cv_tp == Tile_Pos.LEFT_QTR )
      {
        cv.SetTilePos( Tile_Pos.TOP__LEFT_8TH );
        nv.SetTilePos( Tile_Pos.BOT__LEFT_8TH );
      }
      else if( cv_tp == Tile_Pos.RITE_QTR )
      {
        cv.SetTilePos( Tile_Pos.TOP__RITE_8TH );
        nv.SetTilePos( Tile_Pos.BOT__RITE_8TH );
      }
      else if( cv_tp == Tile_Pos.LEFT_CTR__QTR )
      {
        cv.SetTilePos( Tile_Pos.TOP__LEFT_CTR_8TH );
        nv.SetTilePos( Tile_Pos.BOT__LEFT_CTR_8TH );
      }
      else //( cv_tp == Tile_Pos.RITE_CTR__QTR )
      {
        cv.SetTilePos( Tile_Pos.TOP__RITE_CTR_8TH );
        nv.SetTilePos( Tile_Pos.BOT__RITE_CTR_8TH );
      }
    }
    UpdateViews();
  }
  void Exe_Colon_se()
  {
    GoToBuffer( SE_FILE );
  }
  void Exe_Colon_sh()
  {
    GoToBuffer( CMD_FILE );
  }
  void GoToFile()
  {
    String fname = CV().GetFileName_UnderCursor();

    if( null != fname ) GoToBuffer_Fname( fname );
  }
  // Return true if went to buffer indicated by fname, else false
  boolean GoToBuffer_Fname( String fname )
  {
    // 1. Search for fname in buffer list, and if found, go to that buffer:
    Ptr_Int file_index = new Ptr_Int( 0 );
    if( HaveFile( fname, file_index ) )
    {
      GoToBuffer( file_index.val );
      return true;
    }
    // 2. Get file name relative to current view:
    fname = CV().m_fb.Relative_2_FullFname( fname );

    // 3. Search for fname in buffer list, and if found, go to that buffer:
    if( HaveFile( fname, file_index ) )
    {
      GoToBuffer( file_index.val );
      return true;
    }

    // 4. See if file exists, and if so, add a file buffer, and go to that buffer
    boolean exists = Files.exists( Paths.get( fname ) );

    if( exists )
    {
      FileBuf fb = new FileBuf( this, fname, true );
      boolean ok = fb.ReadFile();
      if( ok ) {
        Add_FileBuf_2_Lists_Create_Views( fb, fname );

        GoToBuffer( m_views[m_win].size()-1 );
      }
    }
    else {
      CmdLineMessage( "Could not find file: "+ fname );
      return false;
    }
    return true;
  }
  void GoToBuffer( final int buf_idx )
  {
    if( m_views[ m_win ].size() <= buf_idx )
    {
      CmdLineMessage( "Buffer "+ buf_idx +" does not exist" );
    }
    else {
      if( buf_idx == m_file_hist[ m_win ].get( 0 ) )
      {
        // User asked for view that is currently displayed.
        // Dont do anything, just put cursor back in place.
        CV().PrintCursor();
      }
      else {
        m_file_hist[ m_win ].add( 0, buf_idx );

        // Remove subsequent buf_idx's from m_file_hist[ m_win ]:
        for( int k=1; k<m_file_hist[ m_win ].size(); k++ )
        {
          if( buf_idx == m_file_hist[ m_win ].get( k ) )
          {
            m_file_hist[ m_win ].remove( k );
          }
        }
        View nv = CV(); // New View to display
        if( ! nv.Has_Context() )
        {
          // Look for context for the new view:
          boolean found_context = false;
          for( int w=0; !found_context && w<m_num_wins; w++ )
          {
            View v = m_views[ w ].get( buf_idx );
            if( v.Has_Context() )
            {
              found_context = true;

              nv.Set_Context( v );
            }
          }
        }
        nv.SetTilePos( PV().m_tile_pos );
        nv.Update();
      }
    }
  }

  void SetWinToBuffer( final int     win_idx
                     , final int     buf_idx
                     , final boolean update )
  {
    if( m_views[ win_idx ].size() <= buf_idx )
    {
      CmdLineMessage( "Buffer "+ buf_idx +" does not exist" );
    }
    else {
      if( buf_idx == m_file_hist[ win_idx ].get( 0 ) )
      {
        // User asked for view that is currently displayed in win_idx.
        // Dont do anything.
      }
      else {
        m_file_hist[ win_idx ].add( 0, buf_idx );

        // Remove subsequent buf_idx's from m_file_hist[win_idx]:
        for( int k=1; k<m_file_hist[win_idx].size(); k++ )
        {
          if( buf_idx == m_file_hist[ win_idx ].get( k ) )
          {
            m_file_hist[ win_idx ].remove( k );
          }
        }
        View pV_curr = GetView_WinPrev( win_idx, 0 );
        View pV_prev = GetView_WinPrev( win_idx, 1 );

                     pV_curr.SetTilePos( pV_prev.m_tile_pos );
        if( update ) pV_curr.Update();
      }
    }
  }

  void GoToPoundBuffer()
  {
    if( BE_FILE == m_file_hist[ m_win ].get( 1 ) )
    {
      GoToBuffer( m_file_hist[ m_win ].get( 2 ) );
    }
    else GoToBuffer( m_file_hist[ m_win ].get( 1 ) );
  }

  void GoToCurrBuffer()
  {
    // CVI = Current View Index
    final int CVI = m_file_hist[ m_win ].get( 0 );

    if( CVI == BE_FILE
     || CVI == HELP_FILE
     || CVI == SE_FILE )  
    {
      GoToBuffer( m_file_hist[ m_win ].get( 1 ) );
    }
    else {
      // User asked for view that is currently displayed.
      // Dont do anything, just put cursor back in place.
      CV().PrintCursor();
    }
  }

  //-------------------------
  //| 5 | 4 | 3 | 2 | 1 | 0 |
  //-------------------------
  //:b -> GoToPrevBuffer()
  //-------------------------
  //| 4 | 3 | 2 | 1 | 0 | 5 |
  //-------------------------
  //:b -> GoToPrevBuffer()
  //-------------------------
  //| 3 | 2 | 1 | 0 | 5 | 4 |
  //-------------------------
  //:n -> GoToNextBuffer()
  //-------------------------
  //| 4 | 3 | 2 | 1 | 0 | 5 |
  //-------------------------
  //:n -> GoToNextBuffer()
  //-------------------------
  //| 5 | 4 | 3 | 2 | 1 | 0 |
  //-------------------------
  void GoToPrevBuffer()
  {
    final int FILE_HIST_LEN = m_file_hist[ m_win ].size();

    if( FILE_HIST_LEN <= 1 )
    {
      // Nothing to do, so just put cursor back
      CV().PrintCursor();
    }
    else {
      View     pV_old = CV();
      Tile_Pos tp_old = pV_old.m_tile_pos;

      // Move view index at front to back of m_file_hist
      int view_index_old = m_file_hist[ m_win ].remove( 0 );
      m_file_hist[ m_win ].add( view_index_old );

      // Redisplay current window with new view:
      CV().SetTilePos( tp_old );
      CV().Update();
    }
  }
  void GoToFileBuffer()
  {
    GoToBuffer( BE_FILE );
  }

  void GoToMsgBuffer()
  {
    GoToBuffer( MSG_FILE );
  }

  void Exe_Colon_e()
  {
    View cv = CV();

    if( m_sb.toString().equals("e") ) // :e
    {
      cv.m_fb.ReReadFile();

      for( int w=0; w<m_num_wins; w++ )
      {
        View v = GetView_Win( w );

        if( cv.m_fb == v.m_fb )
        {
          // View is currently displayed, perform needed update:
          v.Update();
        }
      }
    }
    else // :e file_name
    {
      Utils.Trim( m_sb2 ); // Remove leading and trailing white space
      m_sb2.deleteCharAt( 0 ); // Remove initial 'e'
      Utils.Trim_Beg( m_sb2 ); // Remove space after initial 'e'

      // Edit file of supplied file name, which can contain spaces:
      String fname = m_sb2.toString();

      final int FILE_NUM = m_file_hist[ m_win ].get( 0 );

      if( CMD_FILE < FILE_NUM )
      {
        // Get full file name relative to path of current file:
        fname = cv.m_fb.Relative_2_FullFname( fname );
      }
      else {
        // Get full file name relative to CWD:
        fname = Utils.FindFullFileName_Path( m_cwd, fname );
      }
      Ptr_Int file_index = new Ptr_Int( 0 );

      if( HaveFile( fname, file_index ) )
      {
        GoToBuffer( file_index.val );
      }
      else {
        FileBuf new_fb = new FileBuf( this, fname, true );
        boolean ok = new_fb.ReadFile();
        if( ok ) {
          Add_FileBuf_2_Lists_Create_Views( new_fb, fname );

          GoToBuffer( m_views[m_win].size()-1 );
        }
      }
    }
  }

  void UpdateViewsConsoleSize()
  {
    for( int w=0; w<MAX_WINS; w++ )
    {
      for( int f=0; f<m_views[w].size(); f++ )
      {
        View v = m_views[w].get( f );
 
      //v.m_num_rows = m_console.Num_Rows();
      //v.m_num_cols = m_console.Num_Cols();
        v.SetViewPos();
      }
    }
  }
  void UpdateViews()
  {
    if( m_diff_mode )
    {
      m_diff.Update();
    }
    else {
      for( int w=0; w<m_num_wins; w++ )
      {
        GetView_Win( w ).Update();
      }
    }
  }
  boolean Update_Change_Statuses()
  {
    // Update buffer changed status around windows:
    boolean updated_change_sts = false;

    for( int w=0; w<m_num_wins; w++ )
    {
      // pV points to currently displayed view in window w:
      final View rV = GetView_Win( w );

      if( rV.m_unsaved_changes != rV.m_fb.Changed() )
      {
        rV.Print_Borders();

        rV.m_unsaved_changes = rV.m_fb.Changed();

        updated_change_sts = true;
      }
    }
    return updated_change_sts;
  }

  // Print a command line message.
  // Put cursor back in edit window.
  //
  void CmdLineMessage( String msg )
  {
    final int MSG_LEN = msg.length();

    View v = CV();

    final int WC  = v.WorkingCols();
    final int ROW = v.Cmd__Line_Row();
    final int COL = v.Col_Win_2_GL( 0 );

    if( WC < msg.length() )
    {
      // messaged does not fit, so truncate beginning
      m_console.SetS( ROW, COL, msg.substring( MSG_LEN - WC ), Style.NORMAL );
    }
    else {
      // messaged fits, add spaces at end
      m_console.SetS( ROW, COL, msg, Style.NORMAL );

      for( int k=0; k<(WC-MSG_LEN); k++ )
      {
        m_console.Set( ROW, v.Col_Win_2_GL( k+MSG_LEN ), ' ', Style.NORMAL );
      }
    }
    if( m_diff_mode ) m_diff.PrintCursor();
    else                   v.PrintCursor();
  }

  void Window_Message( String msg )
  {
    View v = m_views[0].get( MSG_FILE );
    FileBuf fb = v.m_fb;

  //fb.Clear();
    fb.ClearLines();
    fb.m_views.clear();

     v.Clear_Context();

    fb.ReadString( msg );

    GoToBuffer( MSG_FILE );
  }

  View CV()
  {
    return GetView_WinPrev( m_win, 0 );
  }
  View PV()
  {
    return GetView_WinPrev( m_win, 1 );
  }
  // Get view of window w, currently displayed file
  View GetView_Win( final int w )
  {
    return m_views[w].get( m_file_hist[w].get( 0 ) );
  }
  // Get view of window w, filebuf'th previously displayed file
  View GetView_WinPrev( final int w, final int prev )
  {
    return m_views[w].get( m_file_hist[w].get( prev ) );
  }
  static final int KEY_REPEAT_PERIOD =  10; // ms between key repeats
  static final int KEY_REPEAT_DELAY  = 250; // ms to wait for first key repeat
  static final int BE_FILE   = 0;    // Buffer editor view
  static final int HELP_FILE = 1;    // Help          view
  static final int SE_FILE   = 2;    // Search editor view
  static final int MSG_FILE  = 3;    // Message       view
  static final int CMD_FILE  = 4;    // Command Shell view
  static final int MAX_WINS  = 8;    // Maximum number of sub-windows
  static final String EDIT_BUF_NAME = "BUFFER_EDITOR";
  static final String HELP_BUF_NAME = "VIS_HELP";
  static final String SRCH_BUF_NAME = "SEARCH_EDITOR";
  static final String MSG__BUF_NAME = "MESSAGE_BUFFER";
  static final String CMD__BUF_NAME = "SHELL_BUFFER";
  String[]           m_args;
  Deque<Thread>      m_states     = new ArrayDeque<Thread>();
  Thread             m_run_init   = new Thread() { public void run() { run_init  (); } };
  Thread             m_run_focus  = new Thread() { public void run() { run_focus (); } };
  Thread             m_run_idle   = new Thread() { public void run() { run_idle  (); } };
  Thread             m_run_resize = new Thread() { public void run() { run_resize(); } };
  Thread             m_run_slash  = new Thread() { public void run() { run_slash (); } };
  Thread             m_run_c      = new Thread() { public void run() { run_c     (); } };
  Thread             m_run_d      = new Thread() { public void run() { run_d     (); } };
  Thread             m_run_g      = new Thread() { public void run() { run_g     (); } };
  Thread             m_run_W      = new Thread() { public void run() { run_W     (); } };
  Thread             m_run_y      = new Thread() { public void run() { run_y     (); } };
  Thread             m_run_dot    = new Thread() { public void run() { run_dot   (); } };
  Thread             m_run_map    = new Thread() { public void run() { run_map   (); } };
  Thread             m_run_Q      = new Thread() { public void run() { run_Q     (); } };
  int                m_win;
  int                m_num_wins = 1; // Number of sub-windows currently on screen
  JFrame             m_frame;
  Console            m_console;
  Colon              m_colon;
  boolean            m_initialized;
  boolean            m_received_focus;
  char               m_fast_char;
  boolean            m_diff_mode;
  boolean            m_run_mode;
  Diff               m_diff;
  StringBuilder      m_sb        = new StringBuilder();
  StringBuilder      m_sb2       = new StringBuilder();
  ArrayList<FileBuf> m_files     = new ArrayList<>();
   IntList[]         m_file_hist = new  IntList[ MAX_WINS ];
  ViewList[]         m_views     = new ViewList[ MAX_WINS ];
  String             m_star      = new String();
  boolean            m_slash;
  ArrayList<Line>    m_reg = new ArrayList<>();
  Paste_Mode         m_paste_mode;
  String             m_cwd = Utils.GetCWD();
  String             m_cover_key = new String();
  ArrayList<Byte>    m_cover_buf = new ArrayList<>();
  Shell              m_shell;
}

// Run threads using lambdas.
// The syntax is more concise, but according to my research,
// a new is done every time the lambda is called because the lambda
// captures a method outside the lambda, so dont use for now.
//Thread             m_run_init   = new Thread( ()->run_init  () );
//Thread             m_run_focus  = new Thread( ()->run_focus () );
//Thread             m_run_idle   = new Thread( ()->run_idle  () );
//Thread             m_run_resize = new Thread( ()->run_resize() );
//Thread             m_run_slash  = new Thread( ()->run_slash () );
//Thread             m_run_c      = new Thread( ()->run_c     () );
//Thread             m_run_d      = new Thread( ()->run_d     () );
//Thread             m_run_g      = new Thread( ()->run_g     () );
//Thread             m_run_W      = new Thread( ()->run_W     () );
//Thread             m_run_y      = new Thread( ()->run_y     () );
//Thread             m_run_dot    = new Thread( ()->run_dot   () );
//Thread             m_run_Q      = new Thread( ()->run_Q     () );

