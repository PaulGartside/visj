////////////////////////////////////////////////////////////////////////////////
// VI-Simplified (vis) Java Implementation                                    //
// Copyright (c) 11 Feb 2017 Paul J. Gartside                                 //
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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.Cursor;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class VisFx extends Application
                implements VisIF
{
  public static void main( String[] args )
  {
    launch( args );
  }

  @Override
  public void start( Stage stage )
  {
    try {
      m_stage = stage;
      m_args  = getParameters().getRaw();

      Initialize();

      m_states.add( m_run_init );  // First  state
      m_states.add( m_run_idle );  // Second state

      m_stage.setOnCloseRequest( (WindowEvent we) -> Die("") );
      m_stage.show();

      m_scheduler.start();
    }
    catch( Exception e )
    {
      Handle_Exception( e );
    }
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

  void Initialize()
  {
    Rectangle2D screen_sz = Screen.getPrimary().getVisualBounds();

    m_width  = (int)(screen_sz.getWidth() * 0.8 + 0.5 );
    m_height = (int)(screen_sz.getHeight()* 0.8 + 0.5 );

    m_console = new ConsoleFx( this, m_width, m_height );
    m_diff    = new Diff( this, m_console );
    m_group   = new Group( m_console );
    m_scene   = new Scene( m_group, m_width, m_height );

    m_scene.setOnKeyPressed ( ke -> Key_Pressed ( ke ) );
    m_scene.setOnKeyReleased( ke -> Key_Released( ke ) );
    m_scene.setOnKeyTyped   ( ke -> Key_Typed   ( ke ) );

    m_scene.setOnMouseMoved( me -> Cursor_Default() );

    m_scene.widthProperty ().addListener( (o) -> Scene_Width_CB() );
    m_scene.heightProperty().addListener( (o) -> Scene_Height_CB() );

    m_stage.setTitle("visj");
    m_stage.setScene( m_scene );
  }
  void Key_Pressed( KeyEvent ke )
  {
    Cursor_None();
    m_console.Key_Pressed( ke );
  }
  void Key_Released( KeyEvent ke )
  {
    Cursor_None();
    m_console.Key_Released( ke );
  }
  void Key_Typed( KeyEvent ke )
  {
    Cursor_None();
    m_console.Key_Typed( ke );
  }
  void Cursor_None()
  {
    m_scene.setCursor( Cursor.NONE );
  }
  void Cursor_Default()
  {
    m_scene.setCursor( Cursor.DEFAULT );
  }
  void Scheduler()
  {
    try {
      while( 0<m_states.size() )
      {
        // Take:
        m_run_sem.acquire();

        Platform.runLater( m_states.peekFirst() );
      }
    }
    catch( Exception e )
    {
      Handle_Exception( e );
    }
  }
  public void Give()
  {
    m_run_sem.release();
  }
  void Scene_Width_CB()
  {
    m_width = (int)(m_scene.getWidth() + 0.5);
  }
  void Scene_Height_CB()
  {
    m_height = (int)(m_scene.getHeight() + 0.5);
  }

  void run_init()
  {
    run_init_files();
    UpdateViews();

    m_console.Update();

    m_states.removeFirst();
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
    InitMsgBuffer();
    InitShellBuffer();
    InitColonBuffer();
    InitSlashBuffer();
    boolean run_diff = InitUserFiles() && ((USER_FILE+2) == m_files.size());
    InitFileHistory();

    if( run_diff )
    {
      // User supplied: "-d file1 file2", so run diff:
      m_diff_mode = true;
      m_num_wins = 2;
      m_file_hist[ 0 ].set( 0, USER_FILE );
      m_file_hist[ 1 ].set( 0, USER_FILE+1 );
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
  void InitMsgBuffer()
  {
    // Message buffer, 3
    FileBuf fb = new FileBuf( this, MSG__BUF_NAME, false );
    fb.PushLine(); // Add an empty line

    Add_FileBuf_2_Lists_Create_Views( fb, MSG__BUF_NAME );
  }
  void InitShellBuffer()
  {
    // Command buffer, SHELL_FILE(4)
    FileBuf fb = new FileBuf( this, SHELL_BUF_NAME, false );

    // Add ######################################
    String divider = "######################################";
    fb.PushLine( divider );

    // Add an empty line
    fb.PushLine();

    Add_FileBuf_2_Lists_Create_Views( fb, SHELL_BUF_NAME );

    // Put cursor on empty line below # line
    for( int w=0; w<MAX_WINS; w++ )
    {
      View pV_shell = m_views[w].get( SHELL_FILE );
      pV_shell.SetCrsRow( 1 );
    }
  }
  void InitColonBuffer()
  {
    // Search editor buffer, 2
    m_colon_file = new FileBuf( this, COLON_BUF_NAME, true );

    Add_FileBuf_2_Lists_Create_Views( m_colon_file, COLON_BUF_NAME );

    m_colon_view = new LineView( this, m_colon_file, m_console, ':' );

    m_colon_file.m_line_view = m_colon_view;
  }
  void InitSlashBuffer()
  {
    // Search editor buffer, 2
    m_slash_file = new FileBuf( this, SLASH_BUF_NAME, true );

    // Add an empty line
    m_slash_file.PushLine();

    Add_FileBuf_2_Lists_Create_Views( m_slash_file, SLASH_BUF_NAME );

    m_slash_view = new LineView( this, m_slash_file, m_console, '/' );

    m_slash_file.m_line_view = m_slash_view;
  }
  boolean InitUserFiles()
  {
    boolean run_diff = false;

    if( 0==m_args.size() )
    {
      // If user does not supply arguments, open current directory:
      InitUserFiles_AddFile(".");
    }
    else {
      // User file buffers, 5, 6, ...
      for( int k=0; k<m_args.size(); k++ )
      {
        if( m_args.get(k).equals("-d") )
        {
          run_diff = true;
        }
        else {
          InitUserFiles_AddFile( m_args.get(k) );
        }
      }
    }
    return run_diff;
  }
  void InitUserFiles_AddFile( String relative_name )
  {
    String full_name = Utils.FindFullFileName_Process( relative_name );

    if( !HaveFile( full_name, null ) )
    {
      FileBuf fb = new FileBuf( this, full_name, true );

      boolean ok = fb.ReadFile();

      if( ok ) Add_FileBuf_2_Lists_Create_Views( fb, full_name );
    }
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

      if( USER_FILE < m_views[w].size() )
      {
        m_file_hist[w].add( 0, USER_FILE );

        for( int f=m_views[w].size()-1; (USER_FILE+1)<=f; f-- )
        {
          m_file_hist[w].add( f );
        }
      }
    }
  }
  public
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
  public
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
  public
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
  public
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
      m_console.Init_Graphics();
      m_console.Init_RowsCols();
      m_console.Init_Clear();
      UpdateViewsPositions();
      UpdateViews();
      m_console.Update();

      m_states.removeFirst();
    }
  }
  void Handle_Cmd( final char c1 )
  {
    if( m_colon_mode || m_slash_mode )
    {
      Handle_Line_Cmd( c1 );
    }
    else {
      Handle_View_Cmd( c1 );
    }
  }
  void Handle_Line_Cmd( final char c1 )
  {
    switch( c1 )
    {
    case 'a': L_Handle_a();         break;
    case 'A': L_Handle_A();         break;
    case 'b': L_Handle_b();         break;
    case 'c': L_Handle_c();         break;
    case 'd': L_Handle_d();         break;
    case 'D': L_Handle_D();         break;
    case 'e': L_Handle_e();         break;
    case 'f': L_Handle_f();         break;
    case 'g': L_Handle_g();         break;
    case 'G': L_Handle_G();         break;
    case 'h': L_Handle_h();         break;
    case 'i': L_Handle_i();         break;
    case 'j': L_Handle_j();         break;
    case 'J': L_Handle_J();         break;
    case 'k': L_Handle_k();         break;
    case 'l': L_Handle_l();         break;
    case 'n': L_Handle_n();         break;
    case 'N': L_Handle_N();         break;
    case 'o': L_Handle_o();         break;
    case 'p': L_Handle_p();         break;
    case 'P': L_Handle_P();         break;
    case 'R': L_Handle_R();         break;
    case 's': L_Handle_s();         break;
  //case 'u': L_Handle_u();         break;
  //case 'U': L_Handle_U();         break;
    case 'v': L_Handle_v();         break;
    case 'w': L_Handle_w();         break;
    case 'x': L_Handle_x();         break;
    case 'y': L_Handle_y();         break;
    case '0': L_Handle_0();         break;
    case '$': L_Handle_Dollar();    break;
    case '%': L_Handle_Percent();   break;
    case '~': L_Handle_Tilda();     break;
    case ';': L_Handle_SemiColon(); break;
    case ':': L_Handle_Colon();     break;
    case '/': L_Handle_Slash();     break;
    case '\n':L_Handle_Return();    break;
    case ESC: L_Handle_Escape();    break;
    }
  }
  void Handle_View_Cmd( final char c1 )
  {
    switch( c1 )
    {
    case 'a': Handle_a();         break;
    case 'A': Handle_A();         break;
    case 'b': Handle_b();         break;
    case 'B': Handle_B();         break;
    case 'c': Handle_c();         break;
    case 'd': Handle_d();         break;
    case 'C': Handle_C();         break;
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
  void L_Handle_j()
  {
    int num = 1;
    while( m_console.FirstKeyIs('j') )
    {
      m_console.GetKey();
      num++;
    }
    if     ( m_colon_mode ) m_colon_view.GoDown( num );
    else if( m_slash_mode ) m_slash_view.GoDown( num );
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
  void L_Handle_k()
  {
    int num = 1;
    while( m_console.FirstKeyIs('k') )
    {
      m_console.GetKey();
      num++;
    }
    if     ( m_colon_mode ) m_colon_view.GoUp( num );
    else if( m_slash_mode ) m_slash_view.GoUp( num );
  }

  void Handle_h()
  {
    int num = 1;
    while( m_console.FirstKeyIs('h') )
    {
      m_console.GetKey();
      num++;
    }
    if( m_diff_mode ) m_diff.GoLeft( num );
    else                CV().GoLeft( num );
  }
  void L_Handle_h()
  {
    int num = 1;
    while( m_console.FirstKeyIs('h') )
    {
      m_console.GetKey();
      num++;
    }
    if     ( m_colon_mode ) m_colon_view.GoLeft( num );
    else if( m_slash_mode ) m_slash_view.GoLeft( num );
  }

  void Handle_l()
  {
    int num = 1;
    while( m_console.FirstKeyIs('l') )
    {
      m_console.GetKey();
      num++;
    }
    if( m_diff_mode ) m_diff.GoRight(num);
    else                CV().GoRight(num);
  }
  void L_Handle_l()
  {
    int num = 1;
    while( m_console.FirstKeyIs('l') )
    {
      m_console.GetKey();
      num++;
    }
    if     ( m_colon_mode ) m_colon_view.GoRight(num);
    else if( m_slash_mode ) m_slash_view.GoRight(num);
  }

  void Handle_0()
  {
    if( m_diff_mode )  m_diff.GoToBegOfLine();
    else                 CV().GoToBegOfLine();
  }
  void L_Handle_0()
  {
    if     ( m_colon_mode ) m_colon_view.GoToBegOfLine();
    else if( m_slash_mode ) m_slash_view.GoToBegOfLine();
  }

  void Handle_Dollar()
  {
    if( m_diff_mode )  m_diff.GoToEndOfLine();
    else                 CV().GoToEndOfLine();
  }
  void L_Handle_Dollar()
  {
    if     ( m_colon_mode ) m_colon_view.GoToEndOfLine();
    else if( m_slash_mode ) m_slash_view.GoToEndOfLine();
  }

  void Handle_Percent()
  {
    if( m_diff_mode ) m_diff.GoToOppositeBracket();
    else                CV().GoToOppositeBracket();
  }
  void L_Handle_Percent()
  {
    if     ( m_colon_mode ) m_colon_view.GoToOppositeBracket();
    else if( m_slash_mode ) m_slash_view.GoToOppositeBracket();
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
    String pattern = m_diff_mode ? m_diff.Do_Star_GetNewPattern()
                                 :   CV().Do_Star_GetNewPattern();
    if( ! pattern.equals( m_regex ) )
    {
      m_regex = pattern;
 
      if( 0<m_regex.length() )
      {
        Do_Star_Update_Search_Editor();
      }
      if( m_diff_mode ) m_diff.Update();
      else {
        // Show new star patterns for all windows currently displayed,
        // but update current window last, so that the cursor ends up
        // in the current window.
        for( int w=0; w<m_num_wins; w++ )
        {
          View pV = GetView_Win( w );
 
          if( pV != CV() ) pV.Update();
        }
        CV().Update();
      }
    }
  }
  void Do_Star_PrintPatterns( final boolean HIGHLIGHT )
  {
    for( int w=0; w<m_num_wins; w++ )
    {
      GetView_Win( w ).PrintPatterns( HIGHLIGHT );
    }
  }

  // 1. Search for regex pattern in search editor.
  // 2. If regex pattern is found in search editor,
  //         move pattern to end of search editor
  //    else add regex pattern to end of search editor
  // 3. If search editor is displayed, update search editor window
  //
  void Do_Star_Update_Search_Editor()
  {
    final FileBuf fb = m_slash_file;

    // Remove last line if it is blank:
    int NUM_SE_LINES = fb.NumLines(); // Number of search editor lines
    if( 0<NUM_SE_LINES && 0 == fb.LineLen( NUM_SE_LINES-1 ) )
    {
      fb.RemoveLine( NUM_SE_LINES-1 );
      NUM_SE_LINES = fb.NumLines();
    }
    // 1. Search for regex pattern in search editor.
    boolean found_pattern_in_search_editor = false;
    int line_in_search_editor = 0;

    for( int ln=0; !found_pattern_in_search_editor && ln<NUM_SE_LINES; ln++ )
    {
      if( fb.GetLine( ln ).toString().equals( m_regex ) )
      {
        found_pattern_in_search_editor = true;
        line_in_search_editor = ln;
      }
    }
    // 2. If regex pattern is found in search editor,
    //         move pattern to end of search editor
    //    else add regex pattern to end of search editor
    if( found_pattern_in_search_editor )
    {
      // Move pattern to end of search editor, so newest searches are at bottom of file
      if( line_in_search_editor < NUM_SE_LINES-1 )
      {
        Line lp = fb.RemoveLine( line_in_search_editor );
        fb.PushLine( lp );
      }
    }
    else {
      // Push regex onto search editor buffer
      fb.PushLine( m_regex );
    }
    // Push an emtpy line onto slash buffer to leave empty / prompt:
    fb.PushLine();

    // 3. If search editor is displayed, update search editor window
    View cv = CV();
    m_slash_view.SetContext( cv.WinCols(), cv.X(), cv.Cmd__Line_Row() );
    m_slash_view.GoToCrsPos_NoWrite( fb.NumLines()-1, 0 );
    fb.Update();
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
  void L_Handle_Tilda()
  {
    if     ( m_colon_mode ) m_colon_view.Do_Tilda();
    else if( m_slash_mode ) m_slash_view.Do_Tilda();
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
  void L_Handle_J()
  {
    if     ( m_colon_mode ) m_colon_view.Do_J();
    else if( m_slash_mode ) m_slash_view.Do_J();
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
  void L_Handle_n()
  {
    if     ( m_colon_mode ) m_colon_view.Do_n();
    else if( m_slash_mode ) m_slash_view.Do_n();
  }

  void Handle_N()
  {
    if( m_diff_mode ) m_diff.Do_N();
    else                CV().Do_N();
  }
  void L_Handle_N()
  {
    if     ( m_colon_mode ) m_colon_view.Do_N();
    else if( m_slash_mode ) m_slash_view.Do_N();
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

  void L_Handle_o()
  {
    m_states.addFirst( m_run_L_Ha_i );

    if     ( m_colon_mode ) m_colon_view.Do_o();
    else if( m_slash_mode ) m_slash_view.Do_o();
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

  void L_Handle_p()
  {
    if     ( m_colon_mode ) m_colon_view.Do_p();
    else if( m_slash_mode ) m_slash_view.Do_p();
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
  void L_Handle_P()
  {
    if     ( m_colon_mode ) m_colon_view.Do_P();
    else if( m_slash_mode ) m_slash_view.Do_P();
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
  void L_Handle_R()
  {
    m_states.addFirst( m_run_L_Ha_i );

    if     ( m_colon_mode ) m_colon_view.Do_R();
    else if( m_slash_mode ) m_slash_view.Do_R();
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
  void L_Handle_s()
  {
    if     ( m_colon_mode ) m_colon_view.Do_s();
    else if( m_slash_mode ) m_slash_view.Do_s();
  }

  void Handle_u()
  {
    if( m_diff_mode ) m_diff.Do_u();
    else                CV().Do_u();
  }
  void Handle_U()
  {
    if( m_diff_mode ) m_diff.Do_U();
    else                CV().Do_U();
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
  void L_Handle_v()
  {
    if     ( m_colon_mode ) m_colon_view.Do_v();
    else if( m_slash_mode ) m_slash_view.Do_v();
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

  void L_Handle_c()
  {
    m_states.addFirst( m_run_L_c );
  }
  void run_L_c()
  {
    if( 0<m_console.KeysIn() )
    {
      m_states.removeFirst();

      final char C = m_console.GetKey();

      if( C == 'w' )
      {
        if     ( m_colon_mode ) m_colon_view.Do_cw();
        else if( m_slash_mode ) m_slash_view.Do_cw();
      }
      else if( C == '$' )
      {
        if( m_colon_mode )
        {
          m_colon_view.Do_D();
          m_colon_view.Do_a();
        }
        else if( m_slash_mode )
        {
          m_slash_view.Do_D();
          m_slash_view.Do_a();
        }
      }
    }
  }

  void Handle_C()
  {
    m_states.addFirst( m_run_C );
  }
  void run_C()
  {
    if( 0<m_console.KeysIn() )
    {
      m_states.removeFirst();

      final char c2 = m_console.GetKey();

      if( c2 == 'C' )
      {
        m_console.copy_paste_buf_2_system_clipboard();
      }
      else if( c2 == 'P' )
      {
        m_console.copy_system_clipboard_2_paste_buf();
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

  void L_Handle_d()
  {
    m_states.addFirst( m_run_L_d );
  }
  void run_L_d()
  {
    if( 0<m_console.KeysIn() )
    {
      m_states.removeFirst();

      final char C = m_console.GetKey();

      if( C == 'd' )
      {
        if     ( m_colon_mode ) m_colon_view.Do_dd();
        else if( m_slash_mode ) m_slash_view.Do_dd();
      }
      else if( C == 'w' )
      {
        if     ( m_colon_mode ) m_colon_view.Do_dw();
        else if( m_slash_mode ) m_slash_view.Do_dw();
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
  void L_Handle_D()
  {
    if     ( m_colon_mode ) m_colon_view.Do_D();
    else if( m_slash_mode ) m_slash_view.Do_D();
  }

  void Handle_e()
  {
    if( m_diff_mode ) m_diff.GoToEndOfWord();
    else                CV().GoToEndOfWord();
  }
  void L_Handle_e()
  {
    if     ( m_colon_mode ) m_colon_view.GoToEndOfWord();
    else if( m_slash_mode ) m_slash_view.GoToEndOfWord();
  }

  void Handle_f()
  {
    if( m_diff_mode ) m_diff.Do_f();
    else                CV().Do_f();
  }
  void L_Handle_f()
  {
    if     ( m_colon_mode ) m_colon_view.Do_f();
    else if( m_slash_mode ) m_slash_view.Do_f();
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

  void L_Handle_i()
  {
    m_states.addFirst( m_run_L_Ha_i );

    if     ( m_colon_mode ) m_colon_view.Do_i();
    else if( m_slash_mode ) m_slash_view.Do_i();
  }

  void run_L_Ha_i()
  {
    if( m_colon_mode )
    {
      if( m_colon_view.m_i_EOL_delim )
      {
        m_colon_mode = false;
 
        Exe_Colon_Cmd();
      }
      else {
        m_states.removeFirst();
      }
    }
    else if( m_slash_mode )
    {
      if( m_slash_view.m_i_EOL_delim )
      {
        m_slash_mode = false;
 
        Exe_Slash();
      }
      else {
        m_states.removeFirst();
      }
    }
    else {
      // Should never get here, but remove this state just in case
      // so we dont get stuck doing nothing
      m_states.removeFirst();
    }
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

  void L_Handle_a()
  {
    m_states.addFirst( m_run_L_Ha_i );

    if     ( m_colon_mode ) m_colon_view.Do_a();
    else if( m_slash_mode ) m_slash_view.Do_a();
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
  void L_Handle_A()
  {
    m_states.addFirst( m_run_L_Ha_i );

    if     ( m_colon_mode ) m_colon_view.Do_A();
    else if( m_slash_mode ) m_slash_view.Do_A();
  }

  void Handle_b()
  {
    if( m_diff_mode ) m_diff.GoToPrevWord();
    else                CV().GoToPrevWord();
  }
  void L_Handle_b()
  {
    if     ( m_colon_mode ) m_colon_view.GoToPrevWord();
    else if( m_slash_mode ) m_slash_view.GoToPrevWord();
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
        if( m_diff_mode ) m_diff.GoToFile();
        else                CV().GoToFile();
      }
    }
  }

  void L_Handle_g()
  {
    m_states.addFirst( m_run_L_g );
  }
  void run_L_g()
  {
    if( 0<m_console.KeysIn() )
    {
      m_states.removeFirst();

      final char c2 = m_console.GetKey();

      if( c2 == 'g' )
      {
        if     ( m_colon_mode ) m_colon_view.GoToTopOfFile();
        else if( m_slash_mode ) m_slash_view.GoToTopOfFile();
      }
      else if( c2 == '0' )
      {
        if     ( m_colon_mode ) m_colon_view.GoToStartOfRow();
        else if( m_slash_mode ) m_slash_view.GoToStartOfRow();
      }
      else if( c2 == '$' )
      {
        if     ( m_colon_mode ) m_colon_view.GoToEndOfRow();
        else if( m_slash_mode ) m_slash_view.GoToEndOfRow();
      }
    }
  }

  void Handle_G()
  {
    if( m_diff_mode ) m_diff.GoToEndOfFile();
    else                CV().GoToEndOfFile();
  }
  void L_Handle_G()
  {
    if     ( m_colon_mode ) m_colon_view.GoToEndOfFile();
    else if( m_slash_mode ) m_slash_view.GoToEndOfFile();
  }

  public void Handle_SemiColon()
  {
    if( 0 <= m_fast_char )
    {
      if( m_diff_mode ) m_diff.Do_semicolon( m_fast_char );
      else                CV().Do_semicolon( m_fast_char );
    }
  }
  void L_Handle_SemiColon()
  {
    if( 0 <= m_fast_char )
    {
      if     ( m_colon_mode ) m_colon_view.Do_semicolon( m_fast_char );
      else if( m_slash_mode ) m_slash_view.Do_semicolon( m_fast_char );
    }
  }

  void Handle_w()
  {
    if( m_diff_mode ) m_diff.GoToNextWord();
    else                CV().GoToNextWord();
  }
  void L_Handle_w()
  {
    if     ( m_colon_mode ) m_colon_view.GoToNextWord();
    else if( m_slash_mode ) m_slash_view.GoToNextWord();
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
      m_diff.Set_Console_CrsCell( pV_new );
    }
    else {
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
    }
    if( m_diff_mode ) m_diff.Do_x();
    else                CV().Do_x();
  }
  void L_Handle_x()
  {
    if     ( m_colon_mode ) m_colon_view.Do_x();
    else if( m_slash_mode ) m_slash_view.Do_x();
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
        if( m_diff_mode ) m_diff.Do_yw();
        else                CV().Do_yw();
      }
    }
  }

  void L_Handle_y()
  {
    m_states.addFirst( m_run_L_y );
  }
  void run_L_y()
  {
    if( 0<m_console.KeysIn() )
    {
      m_states.removeFirst();

      final char c2 = m_console.GetKey();

      if( c2 == 'y' )
      {
        if     ( m_colon_mode ) m_colon_view.Do_yy();
        else if( m_slash_mode ) m_slash_view.Do_yy();
      }
      else if( c2 == 'w' )
      {
        if     ( m_colon_mode ) m_colon_view.Do_yw();
        else if( m_slash_mode ) m_slash_view.Do_yw();
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
    if( 0 == m_colon_file.NumLines() )
    {
      m_colon_file.PushLine();
    }
    View cv = CV();
    final int NUM_COLS = cv.WinCols();
    final int X        = cv.X();
    final int Y        = cv.Cmd__Line_Row();

    m_colon_view.SetContext( NUM_COLS, X, Y );
    m_colon_mode = true;
 
    final int CL = m_colon_view.CrsLine();
    final int LL = m_colon_file.LineLen( CL );
 
    if( 0<LL )
    {
      // Something on current line, so goto command line in escape mode
      m_colon_view.Update();
    }
    else {
      // Nothing on current line, so goto command line in insert mode
      L_Handle_i();
    }
  }

  void L_Handle_Colon()
  {
    m_colon_mode = false;

    if( m_diff_mode ) m_diff.PrintCursor();
    else                CV().PrintCursor();
  }

  void L_Handle_Escape()
  {
    if( m_colon_mode )
    {
      L_Handle_Colon();
    }
    else if( m_slash_mode )
    {
      L_Handle_Slash();
    }
  }

  void L_Handle_Return()
  {
    if( m_colon_mode )
    {
      m_colon_mode = false;

      m_colon_view.HandleReturn();

      Exe_Colon_Cmd();
    }
    else if( m_slash_mode )
    {
      m_slash_mode = false;

      m_slash_view.HandleReturn();

      Handle_Slash_GotPattern( m_sb.toString(), true );
    }
  }

  void Handle_Slash()
  {
    if( 0 == m_slash_file.NumLines() )
    {
      m_slash_file.PushLine();
    }
    final View cv = CV();
    final int NUM_COLS = cv.WinCols();
    final int X        = cv.X();
    final int Y        = cv.Cmd__Line_Row();

    m_slash_view.SetContext( NUM_COLS, X, Y );
    m_slash_mode = true;

    final int CL = m_slash_view.CrsLine();
    final int LL = m_slash_file.LineLen( CL );

    if( 0<LL )
    {
      // Something on current line, so goto command line in escape mode
      m_slash_view.Update();
    }
    else {
      // Nothing on current line, so goto command line in insert mode
      L_Handle_i();
    }
  }
  void L_Handle_Slash()
  {
    m_slash_mode = false;

    if( m_diff_mode ) m_diff.PrintCursor();
    else                CV().PrintCursor();
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

  void Exe_Colon_Cmd()
  {
    Exe_Colon_Begin();
    boolean need_2_print_cursor = false;
    if     ( m_sb.length()<1 )                need_2_print_cursor = true;
    else if( m_sb.toString().equals("q") )    Exe_Colon_q();
    else if( m_sb.toString().equals("qa") )   Exe_Colon_qa();
    else if( m_sb.toString().equals("help") ) Exe_Colon_help();
    else if( m_sb.toString().equals("diff") ) Exe_Colon_DoDiff();
    else if( m_sb.toString().equals("nodiff"))Exe_Colon_NoDiff();
    else if( m_sb.toString().equals("rediff"))Exe_Colon_ReDiff();
    else if( m_sb.toString().equals("hi") )   Exe_Colon_hi();
    else if( m_sb.toString().equals("vsp") )  Exe_Colon_vsp();
    else if( m_sb.toString().equals("sp") )   Exe_Colon_sp();
    else if( m_sb.toString().equals("se") )   Exe_Colon_se();
    else if( m_sb.toString().equals("sh")
          || m_sb.toString().equals("shell")) Exe_Colon_shell();
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
    else if( m_sb.toString().equals("dos2unix")) Exe_dos2unix();
    else if( m_sb.toString().equals("unix2dos")) Exe_unix2dos();
    else if( m_sb.toString().startsWith("cd"))Exe_Colon_cd();
    else if( m_sb.toString().startsWith("syn="))Exe_Colon_Syntax();
    else if( m_sb.toString().startsWith("detab="))Exe_Colon_Detab();
    else if( m_sb.charAt(0)=='w' )            Exe_Colon_w();
    else if( m_sb.charAt(0)=='b' )            Exe_Colon_b();
    else if( m_sb.charAt(0)=='n' )            Exe_Colon_n();
    else if( m_sb.charAt(0)=='e' )            Exe_Colon_e();
    else if( m_sb.charAt(0)=='+' )            Exe_Colon_Font();
    else if( m_sb.charAt(0)=='_' )            Exe_Colon_Font();
    else if( '0' <= m_sb.charAt(0)
                 && m_sb.charAt(0) <= '9' ) Exe_Colon_GoToLine();
    else {
      need_2_print_cursor = true;
    }
    if( need_2_print_cursor ) Exe_Colon_PrintCursor();
  }
  void Exe_Colon_Begin()
  {
    if( 1<m_states.size() ) m_states.removeFirst(); //< Drop out of m_run_colon
    // Copy m_sb into m_sb2
    m_sb2.ensureCapacity( m_sb.length() );
    m_sb2.setLength( 0 );
    for( int k=0; k<m_sb.length(); k++ ) m_sb2.append( m_sb.charAt( k ) );
    Utils.RemoveSpaces( m_sb );
    Exe_Colon_MapEnd();
  }
  void Exe_Colon_PrintCursor()
  {
    final int ROW = CV().Cmd__Line_Row();
    final int COL = CV().Col_Win_2_GL( m_sb.length()+1 );

    // Remove cursor from command line row:
    m_console.Set( ROW, COL, ' ', Style.NORMAL );
    // Put cursor back in window:
    if( m_diff_mode ) m_diff.PrintCursor();
    else                CV().PrintCursor();
  }

  void Exe_Slash()
  {
    Handle_Slash_GotPattern( m_sb.toString(), true );
  }
  public
  void Handle_Slash_GotPattern( final String  pattern
                              , final boolean MOVE_TO_FIRST_PATTERN )
  {
    m_regex = pattern;

    if( 0<m_regex.length() )
    {
      Do_Star_Update_Search_Editor();
    }
    if( MOVE_TO_FIRST_PATTERN )
    {
      if( m_diff_mode ) m_diff.Do_n();
      else                CV().Do_n();
    }
    if( m_diff_mode ) m_diff.Update();
    else {
      // Show new slash patterns for all windows currently displayed,
      // but update current window last, so that the cursor ends up
      // in the current window.
      for( int w=0; w<m_num_wins; w++ )
      {
        final View pV = GetView_Win( 0 );

        if( pV != CV() ) pV.Update();
      }
      CV().Update();
    }
  }

  void Exe_Colon_q()
  {
    final Tile_Pos TP = CV().m_tile_pos;

    if( m_num_wins <= 1 ) Exe_Colon_qa();
    else {
      m_diff_mode = false;

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
        if( !fb0.m_hname.equals(SHELL_BUF_NAME)
         && !Paths.get( fb0.m_fname ).toFile().exists() )
        {
          ok = false;
          Window_Message("\n"+ fb0.m_fname + " does not exist\n\n");
        }
        if( !fb1.m_hname.equals(SHELL_BUF_NAME)
         && !Paths.get( fb1.m_fname ).toFile().exists() )
        {
          ok = false;
          Window_Message("\n"+ fb1.m_fname + " does not exist\n\n");
        }
      }
      if( ok )
      {
        ok = m_diff.Run( v0, v1 );
        if( ok ) {
          m_diff_mode = true;
          m_diff.Update();
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

      View pvS = m_diff.m_vS;
      View pvL = m_diff.m_vL;

      // Set the view contexts to similar values as the diff contexts:
      if( null != pvS )
      {
        pvS.SetTopLine ( m_diff.GetTopLine ( pvS ) );
        pvS.SetLeftChar( m_diff.GetLeftChar() );
        pvS.SetCrsRow  ( m_diff.GetCrsRow  () );
        pvS.SetCrsCol  ( m_diff.GetCrsCol  () );
      }
      if( null != pvL )
      {
        pvL.SetTopLine ( m_diff.GetTopLine ( pvL ) );
        pvL.SetLeftChar( m_diff.GetLeftChar() );
        pvL.SetCrsRow  ( m_diff.GetCrsRow  () );
        pvL.SetCrsCol  ( m_diff.GetCrsCol  () );
      }
      UpdateViews();
    }
  }

  void Exe_Colon_ReDiff()
  {
    if( true == m_diff_mode )
    {
      m_diff.ReDiff();
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
    if( SHELL_FILE == m_file_hist[ m_win ].get( 0 ) )
    {
      if( null == m_shell )
      {
        m_shell = new Shell( this, m_console, m_sb );
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

  void Exe_Colon_Detab()
  {
    String[] toks = m_sb.toString().split("=");

    if( toks.length==2 )
    {
      Ptr_Int ptr_int = new Ptr_Int( 0 );

      if( Utils.String_2_Int( toks[1], ptr_int ) )
      {
        final int tab_sz = ptr_int.val;

        if( 0 < tab_sz && tab_sz <= 32 )
        {
          CV().m_fb.RemoveTabs_SpacesAtEOLs( tab_sz );
        }
      }
    }
  }

  void Exe_dos2unix()
  {
    CV().m_fb.dos2unix();
  }

  void Exe_unix2dos()
  {
    CV().m_fb.unix2dos();
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
      else if( C == ESC )
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
    m_colon_view.CoverKey();
  }
  void Exe_Colon_Cover()
  {
    Exe_Colon_NoDiff();

    m_colon_view.Do_Cover();
  }

//void Exe_Colon_Full()
//{
//  m_old_loc = m_frame.getLocationOnScreen();
//  m_old_sz  = m_frame.getSize();
//  m_frame.setLocation( 0, 0 );
//  m_frame.setSize( m_screen_sz.width, m_screen_sz.height );
//}
//void Exe_Colon_NoFull()
//{
//  if( null == m_old_loc || null == m_old_sz )
//  {
//    m_frame.setSize( (int)(m_screen_sz.width*0.9)
//                   , (int)(m_screen_sz.height*0.9) );
//  }
//  else {
//    m_frame.setLocation( m_old_loc );
//    m_frame.setSize( m_old_sz  );
//  }
//}
//void Exe_Colon_Border()
//{
//  m_frame.dispose();
//  m_frame.setUndecorated( false );
//  m_frame.setVisible( true );
//}
//void Exe_Colon_NoBorder()
//{
//  m_frame.dispose();
//  m_frame.setUndecorated( true );
//  m_frame.setVisible( true );
//}
  void Exe_Colon_Font()
  {
    int size_change = 0;

    boolean done = false;
    for( int k=0; !done && k<m_sb.length(); k++ )
    {
      if     ( m_sb.charAt(k)=='+' ) size_change++;
      else if( m_sb.charAt(k)=='_' ) size_change--;
      else                           done = true;
    }
    m_console.Change_Font_Size( size_change );
  }

  void Exe_Colon_w()
  {
    final View V = CV();

    boolean file_written = false;

    if( m_sb.toString().equals("w")   // :w
     || m_sb.toString().equals("wq")) // :wq
    {
      if( V == m_views[ m_win ].get( SHELL_FILE ) )
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
      if( m_sb.toString().equals("wq") )
      {
        Exe_Colon_q();
      }
    }
    else // :w file_name
    {
      // Write file of supplied file name:
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
        Ptr_Int ptr_int = new Ptr_Int( 0 );
        if( Utils.String_2_Int( m_sb.substring(1), ptr_int ) )
        {
          GoToBuffer( ptr_int.val );
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
      Exe_Colon_NoDiff();

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
    Exe_Colon_NoDiff();

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
    Exe_Colon_NoDiff();

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
    GoToBuffer( SLASH_FILE );
  }
  void Exe_Colon_shell()
  {
    GoToBuffer( SHELL_FILE );
  }

  // Return true if went to buffer indicated by fname, else false
  public
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
        Exe_Colon_NoDiff();

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
    Exe_Colon_NoDiff();

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
     || CVI == SLASH_FILE )  
    {
      Exe_Colon_NoDiff();

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
      Exe_Colon_NoDiff();

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

      if( SHELL_FILE < FILE_NUM )
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

  void Exe_Colon_GoToLine()
  {
    Ptr_Int ptr_int = new Ptr_Int( 0 );

    if( Utils.String_2_Int( m_sb.toString(), ptr_int ) )
    {
      final int line_num = ptr_int.val;

      if( m_diff_mode ) m_diff.GoToLine( line_num );
      else                CV().GoToLine( line_num );
    }
  }

  void UpdateViewsPositions()
  {
    for( int w=0; w<MAX_WINS; w++ )
    {
      for( int f=0; f<m_views[w].size(); f++ )
      {
        View v = m_views[w].get( f );
 
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
        GetView_Win( w ).Update_DoNot_PrintCursor();
      }
      CV().PrintCursor();
    }
  }
  // This ensures that proper change status is displayed around each window:
  // '+++' for unsaved changes, and
  // '   ' for no unsaved changes
  public boolean Update_Change_Statuses()
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
  public void CmdLineMessage( String msg )
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

  public void Window_Message( String msg )
  {
    View v = m_views[0].get( MSG_FILE );
    FileBuf fb = v.m_fb;

    fb.ClearLines();
    fb.m_views.clear();

     v.Clear_Context();

    fb.ReadString( msg );

    GoToBuffer( MSG_FILE );
  }

  public View CV()
  {
    return GetView_WinPrev( m_win, 0 );
  }
  public View PV()
  {
    return GetView_WinPrev( m_win, 1 );
  }
  public int Curr_FileNum()
  {
    return m_file_hist[ m_win ].get( 0 );
  }
  // Get view of window w, currently displayed file
  public View GetView_Win( final int w )
  {
    return m_views[w].get( m_file_hist[w].get( 0 ) );
  }
  // Get view of window w, filebuf'th previously displayed file
  View GetView_WinPrev( final int w, final int prev )
  {
    return m_views[w].get( m_file_hist[w].get( prev ) );
  }
  public ConsoleIF get_Console()
  {
    return m_console;
  }
  public FileBuf get_FileBuf( final int file_num )
  {
    return m_files.get( file_num );
  }
  public String get_regex()
  {
    return m_regex;
  }
  public Deque<Thread> get_states()
  {
    return m_states;
  }
  public char get_fast_char()
  {
    return m_fast_char;
  }
  public void set_fast_char( final char C )
  {
    m_fast_char = C;
  }
  public ArrayList<Line> get_reg()
  {
    return m_reg;
  }
  public Paste_Mode get_paste_mode()
  {
    return m_paste_mode;
  }
  public void set_paste_mode( final Paste_Mode paste_mode )
  {
    m_paste_mode = paste_mode;
  }
  public int get_num_wins()
  {
    return m_num_wins;
  }
  public boolean get_run_mode()
  {
    return m_run_mode;
  }
  public void set_run_mode( final boolean mode )
  {
    m_run_mode = mode;
  }
  public boolean Is_BE_FILE( final FileBuf fb )
  {
    return fb == m_views[0].get( BE_FILE ).m_fb;
  }
  public String get_cwd()
  {
    return m_cwd;
  }
  public void set_cmd( String cmd )
  {
    m_sb.setLength( 0 );
    m_sb.append( cmd );
  }
  public boolean get_diff_mode()
  {
    return m_diff_mode;
  } 
  public Diff get_diff()
  {
    return m_diff;
  }

  List<String>    m_args;
  Stage           m_stage;
  ConsoleFx       m_console;
  Group           m_group;
  Scene           m_scene;
  int             m_width;
  int             m_height;

  Deque<Thread>      m_states     = new ArrayDeque<Thread>();
  Thread             m_scheduler  = new Thread() { public void run() { Scheduler (); } };
  Thread             m_run_init   = new Thread() { public void run() { run_init  (); Give(); } };
  Thread             m_run_idle   = new Thread() { public void run() { run_idle  (); Give(); } };
  Thread             m_run_resize = new Thread() { public void run() { run_resize(); Give(); } };
  Thread             m_run_c      = new Thread() { public void run() { run_c     (); Give(); } };
  Thread             m_run_L_c    = new Thread() { public void run() { run_L_c   (); Give(); } };
  Thread             m_run_C      = new Thread() { public void run() { run_C     (); Give(); } };
  Thread             m_run_d      = new Thread() { public void run() { run_d     (); Give(); } };
  Thread             m_run_L_d    = new Thread() { public void run() { run_L_d   (); Give(); } };
  Thread             m_run_g      = new Thread() { public void run() { run_g     (); Give(); } };
  Thread             m_run_L_g    = new Thread() { public void run() { run_L_g   (); Give(); } };
  Thread             m_run_W      = new Thread() { public void run() { run_W     (); Give(); } };
  Thread             m_run_y      = new Thread() { public void run() { run_y     (); Give(); } };
  Thread             m_run_L_y    = new Thread() { public void run() { run_L_y   (); Give(); } };
  Thread             m_run_dot    = new Thread() { public void run() { run_dot   (); Give(); } };
  Thread             m_run_map    = new Thread() { public void run() { run_map   (); Give(); } };
  Thread             m_run_Q      = new Thread() { public void run() { run_Q     (); Give(); } };
  Thread             m_run_L_Ha_i = new Thread() { public void run() { run_L_Ha_i(); Give(); } };
  int                m_win;
  int                m_num_wins = 1; // Number of sub-windows currently on screen
  boolean            m_initialized;
  char               m_fast_char;
  boolean            m_diff_mode;
  boolean            m_run_mode; // True if running shell command
  private
  Semaphore          m_run_sem = new Semaphore( 1 );
  Diff               m_diff;
  StringBuilder      m_sb        = new StringBuilder();
  StringBuilder      m_sb2       = new StringBuilder();
  ArrayList<FileBuf> m_files     = new ArrayList<>();
   IntList[]         m_file_hist = new  IntList[ MAX_WINS ];
  ViewList[]         m_views     = new ViewList[ MAX_WINS ];
  FileBuf            m_colon_file;  // Buffer for colon commands
  LineView           m_colon_view;  // View   of  colon commands
  FileBuf            m_slash_file;  // Buffer for slash commands
  LineView           m_slash_view;  // View   of  slash commands
  String             m_regex     = new String();
  boolean            m_colon_mode;
  boolean            m_slash_mode;
  ArrayList<Line>    m_reg = new ArrayList<>();
  Paste_Mode         m_paste_mode;
  String             m_cwd = Utils.GetCWD();
  Shell              m_shell;
}

