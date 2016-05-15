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

import java.lang.Math;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import javax.swing.JComponent;
import java.util.Queue;
import java.util.ArrayDeque;
import java.util.TreeSet;

class Console extends JComponent
              implements ComponentListener, KeyListener
{
  // ComponentListener interface:
  public void componentResized( ComponentEvent e ) {}
  public void componentHidden ( ComponentEvent e ) {}
  public void componentShown  ( ComponentEvent e ) {}
  public void componentMoved  ( ComponentEvent e ) {}

  // KeyListener interface:
  public void keyPressed( KeyEvent ke )
  {
    final char C = ke.getKeyChar();

    if( C == 65535 ) return; // Ignore SHIFT

    if( m_input.add( C ) ) //< Should always be true
    {
      if( m_save_2_map_buf ) m_map_buf.append( C );
      if( m_save_2_dot_buf ) m_dot_buf.append( C );
      if( m_save_2_vis_buf ) m_vis_buf.append( C );

      if( m_keys.add( C ) ) //< Should always be true
      {
        m_key_time = System.currentTimeMillis();
      }
    }
  }
  public void keyReleased( KeyEvent ke )
  {
    final char C = ke.getKeyChar();

    if( C == 65535 ) return; // Ignore SHIFT

    if( !m_keys.remove( C ) )
    {
      // User released shift key before primary key,
      // so convert C to upper and then remove:
      if( !m_keys.remove( LowerToUpper( C ) ) )
      {
        // Should never get here, but need to remove something or else
        // key repeat will quit working, so remove everything:
        m_keys.clear();
      }
    }
  }
  public void keyTyped( KeyEvent ke )
  {
  }

  Console( Vis vis )
  {
    m_vis      = vis;

    m_num_rows = 0;
    m_num_cols = 0;
    m_siz_rows = 0;
    m_siz_cols = 0;

    m_font_plain = new Font( GetFontName(), Font.PLAIN, GetFontSize() );
    m_font_bold  = new Font( GetFontName(), Font.BOLD , GetFontSize() );

    addComponentListener( this );
    addKeyListener( this );
  }

  private char LowerToUpper( final char C )
  {
    switch( C )
    {
    case '`': return '~';
    case '1': return '!';
    case '2': return '@';
    case '3': return '#';
    case '4': return '$';
    case '5': return '%';
    case '6': return '^';
    case '7': return '&';
    case '8': return '*';
    case '9': return '(';
    case '0': return ')';
    case '-': return '_';
    case '=': return '+';

    case 'q': return 'Q';
    case 'w': return 'W';
    case 'e': return 'E';
    case 'r': return 'R';
    case 't': return 'T';
    case 'y': return 'Y';
    case 'u': return 'U';
    case 'i': return 'I';
    case 'o': return 'O';
    case 'p': return 'P';
    case '[': return '{';
    case ']': return '}';
    case '\\':return '|';

    case 'a': return 'A';
    case 's': return 'S';
    case 'd': return 'D';
    case 'f': return 'F';
    case 'g': return 'G';
    case 'h': return 'H';
    case 'j': return 'J';
    case 'k': return 'K';
    case 'l': return 'L';
    case ';': return ':';
    case '\'':return '"';

    case 'z': return 'Z';
    case 'x': return 'X';
    case 'c': return 'C';
    case 'v': return 'V';
    case 'b': return 'B';
    case 'n': return 'N';
    case 'm': return 'M';
    case ',': return '<';
    case '.': return '>';
    case '/': return '?';
    }
    return C;
  }

  // 
  // -------------------------------------------------
  // | Name        | OSX   | Win32 | Linux | Solaris |
  // -------------------------------------------------
  // | Monospaced  | Yes   | Yes   | No    | No      |
  // -------------------------------------------------
  // | Monospaced  | No    | Yes   | No    | No      |
  // -------------------------------------------------
  // | Courier     | Yes   | Yes   | Yes   | Yes     |
  // -------------------------------------------------
  // | Courier New | Yes   | Yes   | No    | Yes     |
  // -------------------------------------------------
  // | CourierStd  | No    | Yes   | ???   | ???     |
  // -------------------------------------------------
  // | Consolas    | No    | Yes * | ???   | ???     |
  // -------------------------------------------------
  // | AndaleMono  | Yes * | No    | ???   | ???     |
  // -------------------------------------------------
  // 
  String GetFontName()
  {
    String font_name = "Courier";

  //String os = System.getenv("OS");
  //
  //if( null != os )
  //{
  //  if( os.equals("Windows_NT") ) font_name = "Consolas";
  //}
    if( Utils.Get_OS_Type() == OS_Type.Windows )
    {
      font_name = "Courier New";
    }
    return font_name;
  }
  int GetFontSize()
  {
    int font_size = 16;

  //String os = System.getenv("OS");
  //
  //if( null != os )
  //{
  //  if( os.equals("Windows_NT") ) font_size = 17;
  //}
    if( Utils.Get_OS_Type() == OS_Type.Windows )
    {
      font_size = 17;
    }
    return font_size;
  }
  int Num_Rows() { return m_num_rows; }
  int Num_Cols() { return m_num_cols; }

  void Init()
  {
    // Set this to false to keyEvent's get generated for tab, or
    // else tab is used to traverse to another window, and
    // keyEvent's will not get generated for tab.  Another option
    // is to pre-listen to key events using keyEventDispatcher.
    setFocusTraversalKeysEnabled( false );
    setDoubleBuffered( true );

    Init_Graphics();
    Init_FontMetrics();
    Init_RowsCols();
    Init_Clear();
  }
  void Init_Graphics()
  {
    m_image = createImage( getWidth(), getHeight() );
    m_g     = (Graphics2D)m_image.getGraphics();
    m_g.setColor( Color.black );
    m_g.fillRect( 0, 0, getWidth(), getHeight() );
    m_g.setBackground( Color.black );
  //m_g.clearRect( 0, 0, getWidth(), getHeight() );
    m_g.setFont( m_font_plain );
  }
  void Init_FontMetrics()
  {
    FontMetrics fm = m_g.getFontMetrics();

    m_text_W = fm.charWidth(' ');
    m_text_H = fm.getHeight();
    m_text_L = fm.getLeading();
    m_text_A = fm.getAscent();
    m_text_D = fm.getDescent();

    final int MAX_H = m_text_W*2-2;
    if( MAX_H < m_text_H )
    {
      if( 1<m_text_L )
      {
         m_text_L = 1;
         m_text_H = m_text_A + m_text_D + m_text_L;
      }
      if( MAX_H < m_text_H ) m_text_H = MAX_H;
    }
  }
  void Init_RowsCols()
  {
    m_num_rows = getHeight()/m_text_H;
    m_num_cols = getWidth ()/m_text_W;

    if( m_siz_rows < m_num_rows
     || m_siz_cols < m_num_cols )
    {
      // Window got bigger, so re-allocate:
      m_siz_rows = Math.max( m_siz_rows, m_num_rows );
      m_siz_cols = Math.max( m_siz_cols, m_num_cols );
      m_siz_rows = m_num_rows;
      m_siz_cols = m_num_cols;

      m_chars__p    = new char [m_siz_rows][m_siz_cols];
      m_chars__w    = new char [m_siz_rows][m_siz_cols];
      m_styles_p    = new Style[m_siz_rows][m_siz_cols];
      m_styles_w    = new Style[m_siz_rows][m_siz_cols];
      m_min_touched = new int  [m_siz_rows];
      m_max_touched = new int  [m_siz_rows];
    }
  }
  void Init_Clear()
  {
    // Clear everything:
    for( int row=0; row<m_num_rows; row++ )
    {
      m_min_touched[row] = 0;          // Everything
      m_max_touched[row] = m_num_cols; // touched

      for( int col=0; col<m_num_cols; col++ )
      {
        m_chars__p[row][col] = ' ';
        m_chars__w[row][col] = ' ';
        m_styles_p[row][col] = Style.UNKNOWN;
        m_styles_w[row][col] = Style.UNKNOWN;
      }
    }
  }
  boolean Resized()
  {
    final int old_num_rows = m_num_rows;
    final int old_num_cols = m_num_cols;

    m_num_rows = getHeight()/m_text_H;
    m_num_cols = getWidth ()/m_text_W;

    return old_num_rows != m_num_rows
        || old_num_cols != m_num_cols;
  }

//int KeysIn()
//{
//  if( m_get_from_dot_buf ) return m_dot_buf.length();
//  if( m_get_from_map_buf ) return m_vis_buf.length();
//
//  Utils.Sleep( m_vis.KEY_REPEAT_PERIOD );
//
//  Handle_Key_Repeat();
//
//  return m_input.size();
//}
  int KeysIn()
  {
    if( m_get_from_dot_buf )
    {
      // If there is something to process, process it right away,
      // else sleep to avoid hogging CPU:
      final int num_keys_in = m_dot_buf.length();
      if( num_keys_in <= 0 )
      {
        Utils.Sleep( m_vis.KEY_REPEAT_PERIOD );
      }
      return num_keys_in;
    }
    if( m_get_from_map_buf )
    {
      // If there is something to process, process it right away,
      // else sleep to avoid hogging CPU:
      final int num_keys_in = m_map_buf.length();
      if( num_keys_in <= 0 )
      {
        Utils.Sleep( m_vis.KEY_REPEAT_PERIOD );
      }
      return num_keys_in;
    }
    Utils.Sleep( m_vis.KEY_REPEAT_PERIOD );

    Handle_Key_Repeat();

    return m_input.size();
  }
  char GetKey()
  {
    char C = 0;
    if     ( m_get_from_dot_buf ) C = In_DotBuf();
    else if( m_get_from_map_buf ) C = In_MapBuf();
    else                          C = m_input.remove();

  //if( m_save_2_map_buf ) m_map_buf.append( C );
  //if( m_save_2_dot_buf ) m_dot_buf.append( C );
  //if( m_save_2_vis_buf ) m_vis_buf.append( C );

    return C;
  }
  private
  char In_DotBuf()
  {
    final int DOT_BUF_LEN = m_dot_buf.length();

    final char C = m_dot_buf.charAt( m_dot_buf_index++ );

    if( DOT_BUF_LEN <= m_dot_buf_index )
    {
      m_get_from_dot_buf = false;
      m_dot_buf_index    = 0;
    }
    return C;
  }
  private
  char In_MapBuf()
  {
    final int MAP_BUF_LEN = m_map_buf.length();

    final char C = m_map_buf.charAt( m_map_buf_index++ );

    if( MAP_BUF_LEN <= m_map_buf_index )
    {
      m_get_from_map_buf = false;
      m_map_buf_index    = 0;
    }
    return C;
  }
  boolean FirstKeyIs( final char C )
  {
    final Character head = m_input.peek();

    if( null != head )
    {
      if( C == head ) return true;
    }
    return false;
  }
  void Handle_Key_Repeat()
  {
    // Only repeat if there is only one key pressed
    // and there is not pending input:
    if( m_keys.size()==1 && m_input.size()<1 )
    {
      final long elapsed_time = System.currentTimeMillis() - m_key_time;

      if( m_vis.KEY_REPEAT_DELAY < elapsed_time )
      {
        m_input.add( m_keys.first() );

        if( m_save_2_map_buf ) m_map_buf.append( m_keys.first() );
        if( m_save_2_dot_buf ) m_dot_buf.append( m_keys.first() );
        if( m_save_2_vis_buf ) m_vis_buf.append( m_keys.first() );
      }
    }
  }
//void Set( final int ROW, final int COL, final char C, final Style S )
//{
//  if( m_siz_rows <= ROW )
//  {
//  //System.out.println( "Console::Set(): m_siz_rows="+ m_siz_rows +", ROW="+ ROW );
//  //System.exit( 0 );
//    throw new Exception( "Console::Set(): m_siz_rows="+ m_siz_rows +", ROW="+ ROW );
//  }
//  else if( m_siz_cols <= COL )
//  {
//  //System.out.println( "Console::Set(): m_siz_cols="+ m_siz_cols +", COL="+ COL );
//  //System.exit( 0 );
//    throw new Exception( "Console::Set(): m_siz_cols="+ m_siz_cols +", COL="+ COL );
//  }
//  else {
//    m_chars__p[ ROW ][ COL ] = C;
//    m_styles_p[ ROW ][ COL ] = S;
//    m_min_touched[ ROW ] = Math.min( m_min_touched[ ROW ], COL   );
//    m_max_touched[ ROW ] = Math.max( m_max_touched[ ROW ], COL+1 );
//  }
//}
  void Set( final int ROW, final int COL, final char C, final Style S )
  {
    try {
      if( m_siz_rows <= ROW )
      {
        throw new Exception( "Console::Set(): m_siz_rows="+ m_siz_rows +", ROW="+ ROW );
      }
      else if( m_siz_cols <= COL )
      {
        throw new Exception( "Console::Set(): m_siz_cols="+ m_siz_cols +", COL="+ COL );
      }
      else {
        m_chars__p[ ROW ][ COL ] = C;
        m_styles_p[ ROW ][ COL ] = S;
        m_min_touched[ ROW ] = Math.min( m_min_touched[ ROW ], COL   );
        m_max_touched[ ROW ] = Math.max( m_max_touched[ ROW ], COL+1 );
      }
    }
    catch( Exception e )
    {
      e.printStackTrace( System.err );
      System.exit( 0 );
    }
  }
  void SetS( final int ROW, final int COL, final String str, final Style S )
  {
    for( int k=0; k<str.length(); k++ )
    {
      Set( ROW, COL+k, str.charAt(k), S );
    }
  }
  void Set_Crs_Cell( final int ROW, final int COL )
  {
    // Set new position to cursor style;
    Set( ROW, COL
       , m_chars__p[ROW][COL]
       , Style.CURSOR );
  }
//void Set_Crs_Cell_Empty( final int ROW, final int COL )
//{
//  // Set new position to cursor style;
//  Set( ROW, COL
//     , m_chars__p[ROW][COL]
//     , Style.CURSOR_EMPTY );
//}
  public void paint( Graphics g )
  {
    if( null == m_image ) Init();

    g.drawImage( m_image, 0, 0, null );
  }
  private
  void PrintC( final int row, final int col, final char C, final Style S )
  {
    if( row < m_num_rows && col < m_num_cols )
    {
      // Draw background rectangle:
      m_g.setPaint( Style_2_BG( S ) );

      final int x_p_b = col*m_text_W; // X point background
      final int y_p_b = row*m_text_H; // Y point background

      m_g.fillRect( x_p_b, y_p_b, m_text_W, m_text_H );

      // Draw foreground character:
      m_g.setPaint( Style_2_FG( S ) );

      char data[] = { C };

      final int x_p_t = col*m_text_W;                           // X point text
      final int y_p_t = (row+1)*m_text_H - m_text_D - m_text_L; // Y point text

      m_g.drawChars( data, 0, 1, x_p_t, y_p_t );

      repaint( x_p_b, y_p_b, m_text_W, m_text_H );
    }
  }
  void Set_Color_Scheme_1()
  {
    NORMAL_FG       = Color.white  ;  NORMAL_BG       = Color.black  ;
    STATUS_FG       = Color.white  ;  STATUS_BG       = Color.blue   ;
    BORDER_FG       = Color.white  ;  BORDER_BG       = Color.blue   ;
    BORDER_HI_FG    = Color.white  ;  BORDER_HI_BG    = Color.green  ;
    BANNER_FG       = Color.white  ;  BANNER_BG       = Color.red    ;
    STAR_FG         = Color.white  ;  STAR_BG         = Color.red    ;
    COMMENT_FG      = m_comment_fg ;  COMMENT_BG      = Color.black  ;
    DEFINE_FG       = Color.magenta;  DEFINE_BG       = Color.black  ;
    CONST_FG        = Color.cyan   ;  CONST_BG        = Color.black  ;
    CONTROL_FG      = Color.yellow ;  CONTROL_BG      = Color.black  ;
    VARTYPE_FG      = Color.green  ;  VARTYPE_BG      = Color.black  ;
    VISUAL_FG       = Color.white  ;  VISUAL_BG       = Color.red    ;
    NONASCII_FG     = Color.red    ;  NONASCII_BG     = Color.blue   ;
    RV_NORMAL_FG    = Color.black  ;  RV_NORMAL_BG    = Color.white  ;
    RV_STATUS_FG    = Color.blue   ;  RV_STATUS_BG    = Color.blue   ;
    RV_BORDER_FG    = Color.blue   ;  RV_BORDER_BG    = Color.white  ;
    RV_BORDER_HI_FG = Color.green  ;  RV_BORDER_HI_BG = Color.white  ;
    RV_BANNER_FG    = Color.red    ;  RV_BANNER_BG    = Color.white  ;
    RV_STAR_FG      = Color.red    ;  RV_STAR_BG      = Color.white  ;
    RV_COMMENT_FG   = Color.white  ;  RV_COMMENT_BG   = Color.blue   ;
    RV_DEFINE_FG    = Color.white  ;  RV_DEFINE_BG    = Color.magenta;
    RV_CONST_FG     = Color.black  ;  RV_CONST_BG     = Color.cyan   ;
    RV_CONTROL_FG   = Color.black  ;  RV_CONTROL_BG   = Color.yellow ;
    RV_VARTYPE_FG   = Color.white  ;  RV_VARTYPE_BG   = Color.green  ;
    RV_VISUAL_FG    = Color.red    ;  RV_VISUAL_BG    = Color.white  ;
    RV_NONASCII_FG  = Color.blue   ;  RV_NONASCII_BG  = Color.red    ;
    EMPTY_FG        = Color.red    ;  EMPTY_BG        = Color.black  ;
    EOF_FG          = Color.red    ;  EOF_BG          = Color.darkGray;
    DIFF_DEL_FG     = Color.white  ;  DIFF_DEL_BG     = Color.red    ;
    DIFF_NORMAL_FG  = Color.white  ;  DIFF_NORMAL_BG  = Color.blue   ;
    DIFF_STAR_FG    = Color.blue   ;  DIFF_STAR_BG    = Color.red    ;
    DIFF_COMMENT_FG = Color.white  ;  DIFF_COMMENT_BG = Color.blue   ;
    DIFF_DEFINE_FG  = Color.magenta;  DIFF_DEFINE_BG  = Color.blue   ;
    DIFF_CONST_FG   = Color.cyan   ;  DIFF_CONST_BG   = Color.blue   ;
    DIFF_CONTROL_FG = Color.yellow ;  DIFF_CONTROL_BG = Color.blue   ;
    DIFF_VARTYPE_FG = Color.green  ;  DIFF_VARTYPE_BG = Color.blue   ;
    DIFF_VISUAL_FG  = Color.blue   ;  DIFF_VISUAL_BG  = Color.red    ;
    CURSOR_FG       = Color.black  ;  CURSOR_BG       = Color.pink   ;
                                      CURSOR_EMPTY_BG = Color.black  ;
    m_g.setFont( m_font_plain );

    Init_Clear();
    m_vis.UpdateViews();
    m_vis.CV().PrintCursor();  // Does m_console.Update();
  }
  void Set_Color_Scheme_2()
  {
    NORMAL_FG       = Color.white  ;  NORMAL_BG       = Color.black  ;
    STATUS_FG       = Color.white  ;  STATUS_BG       = Color.blue   ;
    BORDER_FG       = Color.white  ;  BORDER_BG       = Color.blue   ;
    BORDER_HI_FG    = Color.white  ;  BORDER_HI_BG    = Color.green  ;
    BANNER_FG       = Color.white  ;  BANNER_BG       = Color.red    ;
    STAR_FG         = Color.white  ;  STAR_BG         = Color.red    ;
    COMMENT_FG      = m_comment_fg ;  COMMENT_BG      = Color.black  ;
    DEFINE_FG       = Color.magenta;  DEFINE_BG       = Color.black  ;
    CONST_FG        = Color.cyan   ;  CONST_BG        = Color.black  ;
    CONTROL_FG      = Color.yellow ;  CONTROL_BG      = Color.black  ;
    VARTYPE_FG      = Color.green  ;  VARTYPE_BG      = Color.black  ;
    VISUAL_FG       = Color.white  ;  VISUAL_BG       = Color.red    ;
    NONASCII_FG     = Color.red    ;  NONASCII_BG     = Color.blue   ;
    RV_NORMAL_FG    = Color.black  ;  RV_NORMAL_BG    = Color.white  ;
    RV_STATUS_FG    = Color.blue   ;  RV_STATUS_BG    = Color.blue   ;
    RV_BORDER_FG    = Color.blue   ;  RV_BORDER_BG    = Color.white  ;
    RV_BORDER_HI_FG = Color.green  ;  RV_BORDER_HI_BG = Color.white  ;
    RV_BANNER_FG    = Color.red    ;  RV_BANNER_BG    = Color.white  ;
    RV_STAR_FG      = Color.red    ;  RV_STAR_BG      = Color.white  ;
    RV_COMMENT_FG   = Color.white  ;  RV_COMMENT_BG   = Color.blue   ;
    RV_DEFINE_FG    = Color.white  ;  RV_DEFINE_BG    = Color.magenta;
    RV_CONST_FG     = Color.black  ;  RV_CONST_BG     = Color.cyan   ;
    RV_CONTROL_FG   = Color.black  ;  RV_CONTROL_BG   = Color.yellow ;
    RV_VARTYPE_FG   = Color.white  ;  RV_VARTYPE_BG   = Color.green  ;
    RV_VISUAL_FG    = Color.red    ;  RV_VISUAL_BG    = Color.white  ;
    RV_NONASCII_FG  = Color.blue   ;  RV_NONASCII_BG  = Color.red    ;
    EMPTY_FG        = Color.red    ;  EMPTY_BG        = Color.darkGray;
    EOF_FG          = Color.red    ;  EOF_BG          = Color.darkGray;
    DIFF_DEL_FG     = Color.white  ;  DIFF_DEL_BG     = Color.red    ;
    DIFF_NORMAL_FG  = Color.white  ;  DIFF_NORMAL_BG  = Color.blue   ;
    DIFF_STAR_FG    = Color.blue   ;  DIFF_STAR_BG    = Color.red    ;
    DIFF_COMMENT_FG = Color.white  ;  DIFF_COMMENT_BG = Color.blue   ;
    DIFF_DEFINE_FG  = Color.magenta;  DIFF_DEFINE_BG  = Color.blue   ;
    DIFF_CONST_FG   = Color.cyan   ;  DIFF_CONST_BG   = Color.blue   ;
    DIFF_CONTROL_FG = Color.yellow ;  DIFF_CONTROL_BG = Color.blue   ;
    DIFF_VARTYPE_FG = Color.green  ;  DIFF_VARTYPE_BG = Color.blue   ;
    DIFF_VISUAL_FG  = Color.blue   ;  DIFF_VISUAL_BG  = Color.red    ;
    CURSOR_FG       = Color.black  ;  CURSOR_BG       = Color.pink   ;
                                      CURSOR_EMPTY_BG = Color.black  ;
    m_g.setFont( m_font_plain );

    Init_Clear();
    m_vis.UpdateViews();
    m_vis.CV().PrintCursor();  // Does m_console.Update();
  }
  void Set_Color_Scheme_3()
  {
    NORMAL_FG       = Color.black    ;  NORMAL_BG       = Color.white  ;
    STATUS_FG       = Color.white    ;  STATUS_BG       = Color.blue   ;
    BORDER_FG       = Color.white    ;  BORDER_BG       = Color.blue   ;
    BORDER_HI_FG    = Color.white    ;  BORDER_HI_BG    = Color.green  ;
    BANNER_FG       = Color.white    ;  BANNER_BG       = Color.red    ;
    STAR_FG         = Color.white    ;  STAR_BG         = Color.red    ;
    COMMENT_FG      = m_d_blue       ;  COMMENT_BG      = Color.white  ;
    DEFINE_FG       = m_d_magenta    ;  DEFINE_BG       = Color.white  ;
    CONST_FG        = m_d_cyan       ;  CONST_BG        = Color.white  ;
    CONTROL_FG      = m_d_yellow     ;  CONTROL_BG      = Color.white  ;
    VARTYPE_FG      = m_d_green      ;  VARTYPE_BG      = Color.white  ;
    VISUAL_FG       = Color.red      ;  VISUAL_BG       = Color.white  ;
    NONASCII_FG     = Color.yellow   ;  NONASCII_BG     = Color.cyan   ;
    RV_NORMAL_FG    = Color.white    ;  RV_NORMAL_BG    = Color.black  ;
    RV_STATUS_FG    = Color.blue     ;  RV_STATUS_BG    = Color.white  ;
    RV_BORDER_FG    = Color.blue     ;  RV_BORDER_BG    = Color.white  ;
    RV_BORDER_HI_FG = Color.green    ;  RV_BORDER_HI_BG = Color.white  ;
    RV_BANNER_FG    = Color.white    ;  RV_BANNER_BG    = Color.red    ;
    RV_STAR_FG      = Color.red      ;  RV_STAR_BG      = Color.white  ;
    RV_COMMENT_FG   = Color.white    ;  RV_COMMENT_BG   = Color.blue   ;
    RV_DEFINE_FG    = Color.magenta  ;  RV_DEFINE_BG    = Color.white  ;
    RV_CONST_FG     = Color.cyan     ;  RV_CONST_BG     = Color.black  ;
    RV_CONTROL_FG   = Color.black    ;  RV_CONTROL_BG   = Color.yellow ;
    RV_VARTYPE_FG   = Color.green    ;  RV_VARTYPE_BG   = Color.white  ;
    RV_VISUAL_FG    = Color.white    ;  RV_VISUAL_BG    = Color.red    ;
    RV_NONASCII_FG  = Color.cyan     ;  RV_NONASCII_BG  = Color.yellow ;
    EMPTY_FG        = Color.red      ;  EMPTY_BG        = Color.white  ;
    EOF_FG          = Color.red      ;  EOF_BG          = Color.darkGray;
    DIFF_DEL_FG     = Color.white    ;  DIFF_DEL_BG     = Color.red    ;
    DIFF_NORMAL_FG  = Color.white    ;  DIFF_NORMAL_BG  = Color.blue   ;
    DIFF_STAR_FG    = Color.blue     ;  DIFF_STAR_BG    = Color.red    ;
    DIFF_COMMENT_FG = Color.white    ;  DIFF_COMMENT_BG = Color.blue   ;
    DIFF_DEFINE_FG  = Color.magenta  ;  DIFF_DEFINE_BG  = Color.blue   ;
    DIFF_CONST_FG   = Color.cyan     ;  DIFF_CONST_BG   = Color.blue   ;
    DIFF_CONTROL_FG = Color.yellow   ;  DIFF_CONTROL_BG = Color.blue   ;
    DIFF_VARTYPE_FG = Color.green    ;  DIFF_VARTYPE_BG = Color.blue   ;
    DIFF_VISUAL_FG  = Color.blue     ;  DIFF_VISUAL_BG  = Color.red    ;
    CURSOR_FG       = Color.black    ;  CURSOR_BG       = m_d_pink     ;
                                        CURSOR_EMPTY_BG = Color.black  ;
    m_g.setFont( m_font_bold );

    Init_Clear();
    m_vis.UpdateViews();
    m_vis.CV().PrintCursor();  // Does m_console.Update();
  }
  void Set_Color_Scheme_4()
  {
    NORMAL_FG       = Color.black    ;  NORMAL_BG       = Color.white  ;
    STATUS_FG       = Color.white    ;  STATUS_BG       = Color.blue   ;
    BORDER_FG       = Color.white    ;  BORDER_BG       = Color.blue   ;
    BORDER_HI_FG    = Color.white    ;  BORDER_HI_BG    = Color.green  ;
    BANNER_FG       = Color.white    ;  BANNER_BG       = Color.red    ;
    STAR_FG         = Color.white    ;  STAR_BG         = Color.red    ;
    COMMENT_FG      = m_d_blue       ;  COMMENT_BG      = Color.white  ;
    DEFINE_FG       = m_d_magenta    ;  DEFINE_BG       = Color.white  ;
    CONST_FG        = m_d_cyan       ;  CONST_BG        = Color.white  ;
    CONTROL_FG      = m_d_yellow     ;  CONTROL_BG      = Color.white  ;
    VARTYPE_FG      = m_d_green      ;  VARTYPE_BG      = Color.white  ;
    VISUAL_FG       = Color.red      ;  VISUAL_BG       = Color.white  ;
    NONASCII_FG     = Color.yellow   ;  NONASCII_BG     = Color.cyan   ;
    RV_NORMAL_FG    = Color.white    ;  RV_NORMAL_BG    = Color.black  ;
    RV_STATUS_FG    = Color.blue     ;  RV_STATUS_BG    = Color.white  ;
    RV_BORDER_FG    = Color.blue     ;  RV_BORDER_BG    = Color.white  ;
    RV_BORDER_HI_FG = Color.green    ;  RV_BORDER_HI_BG = Color.white  ;
    RV_BANNER_FG    = Color.white    ;  RV_BANNER_BG    = Color.red    ;
    RV_STAR_FG      = Color.red      ;  RV_STAR_BG      = Color.white  ;
    RV_COMMENT_FG   = Color.white    ;  RV_COMMENT_BG   = Color.blue   ;
    RV_DEFINE_FG    = Color.magenta  ;  RV_DEFINE_BG    = Color.white  ;
    RV_CONST_FG     = Color.cyan     ;  RV_CONST_BG     = Color.black  ;
    RV_CONTROL_FG   = Color.black    ;  RV_CONTROL_BG   = Color.yellow ;
    RV_VARTYPE_FG   = Color.green    ;  RV_VARTYPE_BG   = Color.white  ;
    RV_VISUAL_FG    = Color.white    ;  RV_VISUAL_BG    = Color.red    ;
    RV_NONASCII_FG  = Color.cyan     ;  RV_NONASCII_BG  = Color.yellow ;
    EMPTY_FG        = Color.red      ;  EMPTY_BG        = Color.darkGray;
    EOF_FG          = Color.red      ;  EOF_BG          = Color.darkGray;
    DIFF_DEL_FG     = Color.white    ;  DIFF_DEL_BG     = Color.red    ;
    DIFF_NORMAL_FG  = Color.white    ;  DIFF_NORMAL_BG  = Color.blue   ;
    DIFF_STAR_FG    = Color.blue     ;  DIFF_STAR_BG    = Color.red    ;
    DIFF_COMMENT_FG = Color.white    ;  DIFF_COMMENT_BG = Color.blue   ;
    DIFF_DEFINE_FG  = Color.magenta  ;  DIFF_DEFINE_BG  = Color.blue   ;
    DIFF_CONST_FG   = Color.cyan     ;  DIFF_CONST_BG   = Color.blue   ;
    DIFF_CONTROL_FG = Color.yellow   ;  DIFF_CONTROL_BG = Color.blue   ;
    DIFF_VARTYPE_FG = Color.green    ;  DIFF_VARTYPE_BG = Color.blue   ;
    DIFF_VISUAL_FG  = Color.blue     ;  DIFF_VISUAL_BG  = Color.red    ;
    CURSOR_FG       = Color.black    ;  CURSOR_BG       = m_d_pink     ;
                                        CURSOR_EMPTY_BG = Color.black  ;
    m_g.setFont( m_font_bold );

    Init_Clear();
    m_vis.UpdateViews();
    m_vis.CV().PrintCursor();  // Does m_console.Update();
  }

//private Color Style_2_BG( final Style S )
//{
//  Color c =  Color.black; // Default
//  switch( S )
//  {
//  case NORMAL      : c = Color.black  ; break;
//  case STATUS      : c = Color.blue   ; break;
//  case BORDER      : c = Color.blue   ; break;
//  case BORDER_HI   : c = Color.green  ; break;
//  case BANNER      : c = Color.red    ; break;
//  case STAR        : c = Color.red    ; break;
//  case COMMENT     : c = Color.black  ; break;
//  case DEFINE      : c = Color.black  ; break;
//  case CONST       : c = Color.black  ; break;
//  case CONTROL     : c = Color.black  ; break;
//  case VARTYPE     : c = Color.black  ; break;
//  case VISUAL      : c = Color.red    ; break;
//  case NONASCII    : c = Color.blue   ; break;
//  case RV_NORMAL   : c = Color.white  ; break;
//  case RV_STATUS   : c = Color.blue   ; break;
//  case RV_BORDER   : c = Color.white  ; break;
//  case RV_BORDER_HI: c = Color.white  ; break;
//  case RV_BANNER   : c = Color.white  ; break;
//  case RV_STAR     : c = Color.white  ; break;
//  case RV_COMMENT  : c = Color.blue   ; break;
//  case RV_DEFINE   : c = Color.magenta; break;
//  case RV_CONST    : c = Color.cyan   ; break;
//  case RV_CONTROL  : c = Color.yellow ; break;
//  case RV_VARTYPE  : c = Color.green  ; break;
//  case RV_VISUAL   : c = Color.white  ; break;
//  case RV_NONASCII : c = Color.red    ; break;
////case EMPTY       : c = Color.black  ; break;
//  case EMPTY       : c = Color.darkGray; break;
//  case DIFF_DEL    : c = Color.red    ; break;
//  case DIFF_NORMAL : c = Color.blue   ; break;
//  case DIFF_STAR   : c = Color.red    ; break;
//  case DIFF_COMMENT: c = Color.blue   ; break;
//  case DIFF_DEFINE : c = Color.blue   ; break;
//  case DIFF_CONST  : c = Color.blue   ; break;
//  case DIFF_CONTROL: c = Color.blue   ; break;
//  case DIFF_VARTYPE: c = Color.blue   ; break;
//  case DIFF_VISUAL : c = Color.red    ; break;
////case CURSOR      : c = Color.white  ; break;
//  case CURSOR      : c = Color.pink   ; break;
//  case CURSOR_EMPTY: c = Color.black  ; break;
//  }
//  return c;
//}
  private Color Style_2_BG( final Style S )
  {
    Color C =  Color.black; // Default
    switch( S )
    {
    case NORMAL      : C = NORMAL_BG      ; break;
    case STATUS      : C = STATUS_BG      ; break;
    case BORDER      : C = BORDER_BG      ; break;
    case BORDER_HI   : C = BORDER_HI_BG   ; break;
    case BANNER      : C = BANNER_BG      ; break;
    case STAR        : C = STAR_BG        ; break;
    case COMMENT     : C = COMMENT_BG     ; break;
    case DEFINE      : C = DEFINE_BG      ; break;
    case CONST       : C = CONST_BG       ; break;
    case CONTROL     : C = CONTROL_BG     ; break;
    case VARTYPE     : C = VARTYPE_BG     ; break;
    case VISUAL      : C = VISUAL_BG      ; break;
    case NONASCII    : C = NONASCII_BG    ; break;
    case RV_NORMAL   : C = RV_NORMAL_BG   ; break;
    case RV_STATUS   : C = RV_STATUS_BG   ; break;
    case RV_BORDER   : C = RV_BORDER_BG   ; break;
    case RV_BORDER_HI: C = RV_BORDER_HI_BG; break;
    case RV_BANNER   : C = RV_BANNER_BG   ; break;
    case RV_STAR     : C = RV_STAR_BG     ; break;
    case RV_COMMENT  : C = RV_COMMENT_BG  ; break;
    case RV_DEFINE   : C = RV_DEFINE_BG   ; break;
    case RV_CONST    : C = RV_CONST_BG    ; break;
    case RV_CONTROL  : C = RV_CONTROL_BG  ; break;
    case RV_VARTYPE  : C = RV_VARTYPE_BG  ; break;
    case RV_VISUAL   : C = RV_VISUAL_BG   ; break;
    case RV_NONASCII : C = RV_NONASCII_BG ; break;
  //case EMPTY       : C = EMPTY_BG       ; break;
    case EMPTY       : C = EMPTY_BG       ; break;
    case EOF         : C = EOF_BG         ; break;
    case DIFF_DEL    : C = DIFF_DEL_BG    ; break;
    case DIFF_NORMAL : C = DIFF_NORMAL_BG ; break;
    case DIFF_STAR   : C = DIFF_STAR_BG   ; break;
    case DIFF_COMMENT: C = DIFF_COMMENT_BG; break;
    case DIFF_DEFINE : C = DIFF_DEFINE_BG ; break;
    case DIFF_CONST  : C = DIFF_CONST_BG  ; break;
    case DIFF_CONTROL: C = DIFF_CONTROL_BG; break;
    case DIFF_VARTYPE: C = DIFF_VARTYPE_BG; break;
    case DIFF_VISUAL : C = DIFF_VISUAL_BG ; break;
  //case CURSOR      : C = CURSOR_BG      ; break;
    case CURSOR      : C = CURSOR_BG      ; break;
    case CURSOR_EMPTY: C = CURSOR_EMPTY_BG; break;
    }
    return C;
  }
//private Color Style_2_FG( final Style S )
//{
//  Color c =  Color.white; // Default
//  switch( S )
//  {
//  case NORMAL      : c = Color.white  ; break;
//  case STATUS      : c = Color.white  ; break;
//  case BORDER      : c = Color.white  ; break;
//  case BORDER_HI   : c = Color.white  ; break;
//  case BANNER      : c = Color.white  ; break;
//  case STAR        : c = Color.white  ; break;
////case COMMENT     : c = Color.blue   ; break;
//  case COMMENT     : c = m_comment_fg ; break;
//  case DEFINE      : c = Color.magenta; break;
//  case CONST       : c = Color.cyan   ; break;
//  case CONTROL     : c = Color.yellow ; break;
//  case VARTYPE     : c = Color.green  ; break;
//  case VISUAL      : c = Color.white  ; break;
//  case NONASCII    : c = Color.red    ; break;
//  case RV_NORMAL   : c = Color.black  ; break;
//  case RV_STATUS   : c = Color.blue   ; break;
//  case RV_BORDER   : c = Color.blue   ; break;
//  case RV_BORDER_HI: c = Color.green  ; break;
//  case RV_BANNER   : c = Color.red    ; break;
//  case RV_STAR     : c = Color.red    ; break;
//  case RV_COMMENT  : c = Color.white  ; break;
//  case RV_DEFINE   : c = Color.white  ; break;
//  case RV_CONST    : c = Color.black  ; break;
//  case RV_CONTROL  : c = Color.black  ; break;
//  case RV_VARTYPE  : c = Color.white  ; break;
//  case RV_VISUAL   : c = Color.red    ; break;
//  case RV_NONASCII : c = Color.blue   ; break;
//  case EMPTY       : c = Color.red    ; break;
//  case DIFF_DEL    : c = Color.white  ; break;
//  case DIFF_NORMAL : c = Color.white  ; break;
//  case DIFF_STAR   : c = Color.blue   ; break;
//  case DIFF_COMMENT: c = Color.white  ; break;
//  case DIFF_DEFINE : c = Color.magenta; break;
//  case DIFF_CONST  : c = Color.cyan   ; break;
//  case DIFF_CONTROL: c = Color.yellow ; break;
//  case DIFF_VARTYPE: c = Color.green  ; break;
//  case DIFF_VISUAL : c = Color.blue   ; break;
////case CURSOR      : c = Color.red    ; break;
//  case CURSOR      : c = Color.black  ; break;
//  }
//  return c;
//}
  private Color Style_2_FG( final Style S )
  {
    Color C =  Color.white; // Default
    switch( S )
    {
    case NORMAL      : C = NORMAL_FG      ; break;
    case STATUS      : C = STATUS_FG      ; break;
    case BORDER      : C = BORDER_FG      ; break;
    case BORDER_HI   : C = BORDER_HI_FG   ; break;
    case BANNER      : C = BANNER_FG      ; break;
    case STAR        : C = STAR_FG        ; break;
  //case COMMENT     : C = COMMENT_FG     ; break;
    case COMMENT     : C = COMMENT_FG     ; break;
    case DEFINE      : C = DEFINE_FG      ; break;
    case CONST       : C = CONST_FG       ; break;
    case CONTROL     : C = CONTROL_FG     ; break;
    case VARTYPE     : C = VARTYPE_FG     ; break;
    case VISUAL      : C = VISUAL_FG      ; break;
    case NONASCII    : C = NONASCII_FG    ; break;
    case RV_NORMAL   : C = RV_NORMAL_FG   ; break;
    case RV_STATUS   : C = RV_STATUS_FG   ; break;
    case RV_BORDER   : C = RV_BORDER_FG   ; break;
    case RV_BORDER_HI: C = RV_BORDER_HI_FG; break;
    case RV_BANNER   : C = RV_BANNER_FG   ; break;
    case RV_STAR     : C = RV_STAR_FG     ; break;
    case RV_COMMENT  : C = RV_COMMENT_FG  ; break;
    case RV_DEFINE   : C = RV_DEFINE_FG   ; break;
    case RV_CONST    : C = RV_CONST_FG    ; break;
    case RV_CONTROL  : C = RV_CONTROL_FG  ; break;
    case RV_VARTYPE  : C = RV_VARTYPE_FG  ; break;
    case RV_VISUAL   : C = RV_VISUAL_FG   ; break;
    case RV_NONASCII : C = RV_NONASCII_FG ; break;
    case EMPTY       : C = EMPTY_FG       ; break;
    case EOF         : C = EOF_FG         ; break;
    case DIFF_DEL    : C = DIFF_DEL_FG    ; break;
    case DIFF_NORMAL : C = DIFF_NORMAL_FG ; break;
    case DIFF_STAR   : C = DIFF_STAR_FG   ; break;
    case DIFF_COMMENT: C = DIFF_COMMENT_FG; break;
    case DIFF_DEFINE : C = DIFF_DEFINE_FG ; break;
    case DIFF_CONST  : C = DIFF_CONST_FG  ; break;
    case DIFF_CONTROL: C = DIFF_CONTROL_FG; break;
    case DIFF_VARTYPE: C = DIFF_VARTYPE_FG; break;
    case DIFF_VISUAL : C = DIFF_VISUAL_FG ; break;
  //case CURSOR      : C = CURSOR_FG      ; break;
    case CURSOR      : C = CURSOR_FG      ; break;
    }
    return C;
  }
  boolean Update()
  {
    boolean output_something = false;

    if( !m_get_from_dot_buf )
    {
      for( int row=0; row<m_num_rows; row++ )
      {
        final int col_st = m_min_touched[ row ];
        final int col_fn = m_max_touched[ row ];

        for( int col=col_st; col<col_fn; col++ )
        {
          final char  c_p = m_chars__p[row][col]; // char pending
          final char  c_w = m_chars__w[row][col]; // char written
          final Style s_p = m_styles_p[row][col]; // style pending
          final Style s_w = m_styles_w[row][col]; // style written

          if( c_p != c_w || s_p != s_w || s_w == Style.UNKNOWN )
          {
            PrintC( row, col, c_p, s_p );

            m_chars__w[row][col] = c_p;
            m_styles_w[row][col] = s_p;

            output_something = true;
          }
        }
        m_min_touched[ row ] = m_num_cols; // Nothing
        m_max_touched[ row ] = 0;          // touched
      }
    }
    return output_something;
  }
  final int FONT_SIZE = 17;

  static final char BS  =   8; // Backspace
  static final char ESC =  27; // Escape
  static final char DEL = 127; // Delete
  static final char CTRL_C = 3;

  Vis              m_vis;
  Image            m_image;
  Graphics2D       m_g;
  Font             m_font_plain;
  Font             m_font_bold;
  int              m_text_W;
  int              m_text_H;
  int              m_text_L;
  int              m_text_A;
  int              m_text_D;
  int              m_num_rows; // Current num rows
  int              m_num_cols; // Current num rows
  int              m_siz_rows; // Allocated num rows
  int              m_siz_cols; // Allocated num rows
  char[][]         m_chars__p; // char pending
  char[][]         m_chars__w; // char written
  Style[][]        m_styles_p; // style pending
  Style[][]        m_styles_w; // style written
  int[]            m_min_touched;
  int[]            m_max_touched;

  long               m_key_time;
  TreeSet<Character> m_keys = new TreeSet<>();

  boolean          m_save_2_dot_buf;
  boolean          m_save_2_vis_buf;
  boolean          m_save_2_map_buf;
  boolean          m_get_from_dot_buf;
  boolean          m_get_from_map_buf;
  Queue<Character> m_input   = new ArrayDeque<Character>();
  StringBuilder    m_dot_buf = new StringBuilder();
  StringBuilder    m_vis_buf = new StringBuilder();
  StringBuilder    m_map_buf = new StringBuilder();
  private int      m_dot_buf_index;
  private int      m_map_buf_index;
  private Color    m_comment_fg = new Color( 0.3f, 0.3f, 1.0f );
  private Color    m_d_blue     = new Color( 0.0f, 0.0f, 1.0f );
  private Color    m_d_green    = new Color( 0.0f, 1.0f, 0.0f );
  private Color    m_d_yellow   = new Color( 0.9f, 0.9f, 0.0f );
  private Color    m_d_magenta  = new Color( 1.0f, 0.0f, 1.0f );
  private Color    m_d_cyan     = new Color( 0.0f, 0.9f, 0.9f );
  private Color    m_d_white    = new Color( 0.9f, 0.9f, 0.9f );
  private Color    m_d_pink     = new Color( 1.0f, 0.5f, 0.5f );

  private Color NORMAL_FG       = Color.white  ;
  private Color STATUS_FG       = Color.white  ;
  private Color BORDER_FG       = Color.white  ;
  private Color BORDER_HI_FG    = Color.white  ;
  private Color BANNER_FG       = Color.white  ;
  private Color STAR_FG         = Color.white  ;
  private Color COMMENT_FG      = m_comment_fg ;
  private Color DEFINE_FG       = Color.magenta;
  private Color CONST_FG        = Color.cyan   ;
  private Color CONTROL_FG      = Color.yellow ;
  private Color VARTYPE_FG      = Color.green  ;
  private Color VISUAL_FG       = Color.white  ;
  private Color NONASCII_FG     = Color.red    ;
  private Color RV_NORMAL_FG    = Color.black  ;
  private Color RV_STATUS_FG    = Color.blue   ;
  private Color RV_BORDER_FG    = Color.blue   ;
  private Color RV_BORDER_HI_FG = Color.green  ;
  private Color RV_BANNER_FG    = Color.red    ;
  private Color RV_STAR_FG      = Color.red    ;
  private Color RV_COMMENT_FG   = Color.white  ;
  private Color RV_DEFINE_FG    = Color.white  ;
  private Color RV_CONST_FG     = Color.black  ;
  private Color RV_CONTROL_FG   = Color.black  ;
  private Color RV_VARTYPE_FG   = Color.white  ;
  private Color RV_VISUAL_FG    = Color.red    ;
  private Color RV_NONASCII_FG  = Color.blue   ;
  private Color EMPTY_FG        = Color.red    ;
  private Color EOF_FG          = Color.red    ;
  private Color DIFF_DEL_FG     = Color.white  ;
  private Color DIFF_NORMAL_FG  = Color.white  ;
  private Color DIFF_STAR_FG    = Color.blue   ;
  private Color DIFF_COMMENT_FG = Color.white  ;
  private Color DIFF_DEFINE_FG  = Color.magenta;
  private Color DIFF_CONST_FG   = Color.cyan   ;
  private Color DIFF_CONTROL_FG = Color.yellow ;
  private Color DIFF_VARTYPE_FG = Color.green  ;
  private Color DIFF_VISUAL_FG  = Color.blue   ;
  private Color CURSOR_FG       = Color.black  ;

  private Color NORMAL_BG       = Color.black  ; 
  private Color STATUS_BG       = Color.blue   ; 
  private Color BORDER_BG       = Color.blue   ; 
  private Color BORDER_HI_BG    = Color.green  ; 
  private Color BANNER_BG       = Color.red    ; 
  private Color STAR_BG         = Color.red    ; 
  private Color COMMENT_BG      = Color.black  ; 
  private Color DEFINE_BG       = Color.black  ; 
  private Color CONST_BG        = Color.black  ; 
  private Color CONTROL_BG      = Color.black  ; 
  private Color VARTYPE_BG      = Color.black  ; 
  private Color VISUAL_BG       = Color.red    ; 
  private Color NONASCII_BG     = Color.blue   ; 
  private Color RV_NORMAL_BG    = Color.white  ; 
  private Color RV_STATUS_BG    = Color.blue   ; 
  private Color RV_BORDER_BG    = Color.white  ; 
  private Color RV_BORDER_HI_BG = Color.white  ; 
  private Color RV_BANNER_BG    = Color.white  ; 
  private Color RV_STAR_BG      = Color.white  ; 
  private Color RV_COMMENT_BG   = Color.blue   ; 
  private Color RV_DEFINE_BG    = Color.magenta; 
  private Color RV_CONST_BG     = Color.cyan   ; 
  private Color RV_CONTROL_BG   = Color.yellow ; 
  private Color RV_VARTYPE_BG   = Color.green  ; 
  private Color RV_VISUAL_BG    = Color.white  ; 
  private Color RV_NONASCII_BG  = Color.red    ; 
  private Color EMPTY_BG        = Color.black  ;
  private Color EOF_BG          = Color.darkGray;
  private Color DIFF_DEL_BG     = Color.red    ; 
  private Color DIFF_NORMAL_BG  = Color.blue   ; 
  private Color DIFF_STAR_BG    = Color.red    ; 
  private Color DIFF_COMMENT_BG = Color.blue   ; 
  private Color DIFF_DEFINE_BG  = Color.blue   ; 
  private Color DIFF_CONST_BG   = Color.blue   ; 
  private Color DIFF_CONTROL_BG = Color.blue   ; 
  private Color DIFF_VARTYPE_BG = Color.blue   ; 
  private Color DIFF_VISUAL_BG  = Color.red    ; 
  private Color CURSOR_BG       = Color.pink   ; 
  private Color CURSOR_EMPTY_BG = Color.black  ; 
}                                

