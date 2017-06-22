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

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.geometry.VPos;

import javafx.scene.text.Text;
import javafx.geometry.Bounds;

import java.util.Queue;
import java.util.ArrayDeque;
import java.util.ArrayList;

class ConsoleFx extends Canvas
             implements ConsoleIF
{
  void Key_Pressed( KeyEvent ke )
  {
    final KeyCode CODE  = ke.getCode();
    if( Add_Key_Char( ke ) )
    {
      if( m_input.add( m_C ) ) //< Should always be true
      {
        if( m_save_2_map_buf ) m_map_buf.append( m_C );
        if( m_save_2_dot_buf ) m_dot_buf.append( m_C );
        if( m_save_2_vis_buf ) m_vis_buf.append( m_C );
      }
    }
  }
  void Key_Released( KeyEvent ke )
  {
  }
  void Key_Typed( KeyEvent ke )
  {
  }

  boolean Add_Key_Char( KeyEvent ke )
  {
    final KeyCode CODE  = ke.getCode();
    final boolean caps_pressed = CODE == KeyCode.CAPS;
    m_caps_on = caps_pressed ? !m_caps_on : m_caps_on;
    final boolean SHIFT =  ke.isShiftDown() && !m_caps_on
                       || !ke.isShiftDown() &&  m_caps_on;
    boolean add = true;

    switch( CODE )
    {
    // Function row:
    case ESCAPE          : m_C = ESC; break;
    // Number row:
    case BACK_QUOTE      : m_C = SHIFT ? '~' : '`'; break;
    case DIGIT1          : m_C = SHIFT ? '!' : '1'; break;
    case DIGIT2          : m_C = SHIFT ? '@' : '2'; break;
    case DIGIT3          : m_C = SHIFT ? '#' : '3'; break;
    case DIGIT4          : m_C = SHIFT ? '$' : '4'; break;
    case DIGIT5          : m_C = SHIFT ? '%' : '5'; break;
    case DIGIT6          : m_C = SHIFT ? '^' : '6'; break;
    case DIGIT7          : m_C = SHIFT ? '&' : '7'; break;
    case DIGIT8          : m_C = SHIFT ? '*' : '8'; break;
    case DIGIT9          : m_C = SHIFT ? '(' : '9'; break;
    case DIGIT0          : m_C = SHIFT ? ')' : '0'; break;
    case MINUS           : m_C = SHIFT ? '_' : '-'; break;
    case EQUALS          : m_C = SHIFT ? '+' : '='; break;
    case BACK_SPACE      : m_C = SHIFT ? BS  : BS ; break;
    // Top letter row:
    case TAB             : m_C = SHIFT ? '\t':'\t'; break;
    case Q               : m_C = SHIFT ? 'Q' : 'q'; break;
    case W               : m_C = SHIFT ? 'W' : 'w'; break;
    case E               : m_C = SHIFT ? 'E' : 'e'; break;
    case R               : m_C = SHIFT ? 'R' : 'r'; break;
    case T               : m_C = SHIFT ? 'T' : 't'; break;
    case Y               : m_C = SHIFT ? 'Y' : 'y'; break;
    case U               : m_C = SHIFT ? 'U' : 'u'; break;
    case I               : m_C = SHIFT ? 'I' : 'i'; break;
    case O               : m_C = SHIFT ? 'O' : 'o'; break;
    case P               : m_C = SHIFT ? 'P' : 'p'; break;
    case OPEN_BRACKET    : m_C = SHIFT ? '{' : '['; break;
    case CLOSE_BRACKET   : m_C = SHIFT ? '}' : ']'; break;
    case BACK_SLASH      : m_C = SHIFT ? '|' :'\\'; break;
    // Middle letter row:
    case A               : m_C = SHIFT ? 'A' : 'a'; break;
    case S               : m_C = SHIFT ? 'S' : 's'; break;
    case D               : m_C = SHIFT ? 'D' : 'd'; break;
    case F               : m_C = SHIFT ? 'F' : 'f'; break;
    case G               : m_C = SHIFT ? 'G' : 'g'; break;
    case H               : m_C = SHIFT ? 'H' : 'h'; break;
    case J               : m_C = SHIFT ? 'J' : 'j'; break;
    case K               : m_C = SHIFT ? 'K' : 'k'; break;
    case L               : m_C = SHIFT ? 'L' : 'l'; break;
    case SEMICOLON       : m_C = SHIFT ? ':' : ';'; break;
    case QUOTE           : m_C = SHIFT ? '"' :'\''; break;
    case ENTER           : m_C = SHIFT ? '\n':'\n'; break;
    // Botom letter row:
    case Z               : m_C = SHIFT ? 'Z' : 'z'; break;
    case X               : m_C = SHIFT ? 'X' : 'x'; break;
    case C               : m_C = SHIFT ? 'C' : 'c'; break;
    case V               : m_C = SHIFT ? 'V' : 'v'; break;
    case B               : m_C = SHIFT ? 'B' : 'b'; break;
    case N               : m_C = SHIFT ? 'N' : 'n'; break;
    case M               : m_C = SHIFT ? 'M' : 'm'; break;
    case COMMA           : m_C = SHIFT ? '<' : ','; break;
    case PERIOD          : m_C = SHIFT ? '>' : '.'; break;
    case SLASH           : m_C = SHIFT ? '?' : '/'; break;
    // Spacebar row:
    case SPACE           : m_C = SHIFT ? ' ' : ' '; break;
    default: add = false;
    }
    return add;
  }

  ConsoleFx( VisFx vis, int width, int height )
  {
    super( width, height );

    m_vis      = vis;
    m_gc       = getGraphicsContext2D();
    m_num_rows = 0;
    m_num_cols = 0;
    m_siz_rows = 0;
    m_siz_cols = 0;

    m_font_plain = Font.font( GetFontName(), FontWeight.NORMAL, m_font_size );
    m_font_bold  = Font.font( GetFontName(), FontWeight.BOLD  , m_font_size );

    Init_FontMetrics();
    Init_Graphics();
    Init_TextChars();
    Init_RowsCols();
    Init_Clear();
  }

  String GetFontName()
  {
    String font_name = "Courier";
  //String font_name = "Courier New";

    if( Utils.Get_OS_Type() == OS_Type.Windows )
    {
      font_name = "Lucida Console";
    }
    else if( Utils.Get_OS_Type() == OS_Type.OSX )
    {
      font_name = "Menlo";  // Very Nice
    //font_name = "Monaco"; // Very Nice
    }
    return font_name;
  }
  void Init_FontMetrics()
  {
    Ptr_Double max_w = new Ptr_Double();
    Ptr_Double max_h = new Ptr_Double();

    Find_Font_Bounds( m_font_plain, max_w, max_h );

  //m_text_W = (int)(FONT_SIZE*0.6 + 0.5);
  //m_text_H = FONT_SIZE;

    // Works better if m_text_W and m_text_H are integers
    m_text_W = (int)(max_w.val + 0.8);
    m_text_H = (int)(max_h.val + 0.8);

//Utils.Log("m_text_W ="+ m_text_W);
//Utils.Log("m_text_H ="+ m_text_H);
  }
  void Find_Font_Bounds( Font f, Ptr_Double max_w, Ptr_Double max_h )
  {
    Text t = new Text();

    Get_Bounds("A", f, t, max_w, max_h );
    Get_Bounds("B", f, t, max_w, max_h );
    Get_Bounds("C", f, t, max_w, max_h );
    Get_Bounds("D", f, t, max_w, max_h );
    Get_Bounds("E", f, t, max_w, max_h );
    Get_Bounds("F", f, t, max_w, max_h );
    Get_Bounds("G", f, t, max_w, max_h );
    Get_Bounds("H", f, t, max_w, max_h );
    Get_Bounds("I", f, t, max_w, max_h );
    Get_Bounds("J", f, t, max_w, max_h );
    Get_Bounds("K", f, t, max_w, max_h );
    Get_Bounds("L", f, t, max_w, max_h );
    Get_Bounds("M", f, t, max_w, max_h );
    Get_Bounds("N", f, t, max_w, max_h );
    Get_Bounds("O", f, t, max_w, max_h );
    Get_Bounds("P", f, t, max_w, max_h );
    Get_Bounds("Q", f, t, max_w, max_h );
    Get_Bounds("R", f, t, max_w, max_h );
    Get_Bounds("S", f, t, max_w, max_h );
    Get_Bounds("T", f, t, max_w, max_h );
    Get_Bounds("U", f, t, max_w, max_h );
    Get_Bounds("V", f, t, max_w, max_h );
    Get_Bounds("W", f, t, max_w, max_h );
    Get_Bounds("X", f, t, max_w, max_h );
    Get_Bounds("Y", f, t, max_w, max_h );
    Get_Bounds("Z", f, t, max_w, max_h );

    Get_Bounds("a", f, t, max_w, max_h );
    Get_Bounds("b", f, t, max_w, max_h );
    Get_Bounds("c", f, t, max_w, max_h );
    Get_Bounds("d", f, t, max_w, max_h );
    Get_Bounds("e", f, t, max_w, max_h );
    Get_Bounds("f", f, t, max_w, max_h );
    Get_Bounds("g", f, t, max_w, max_h );
    Get_Bounds("h", f, t, max_w, max_h );
    Get_Bounds("i", f, t, max_w, max_h );
    Get_Bounds("j", f, t, max_w, max_h );
    Get_Bounds("k", f, t, max_w, max_h );
    Get_Bounds("l", f, t, max_w, max_h );
    Get_Bounds("m", f, t, max_w, max_h );
    Get_Bounds("n", f, t, max_w, max_h );
    Get_Bounds("o", f, t, max_w, max_h );
    Get_Bounds("p", f, t, max_w, max_h );
    Get_Bounds("q", f, t, max_w, max_h );
    Get_Bounds("r", f, t, max_w, max_h );
    Get_Bounds("s", f, t, max_w, max_h );
    Get_Bounds("t", f, t, max_w, max_h );
    Get_Bounds("u", f, t, max_w, max_h );
    Get_Bounds("v", f, t, max_w, max_h );
    Get_Bounds("w", f, t, max_w, max_h );
    Get_Bounds("x", f, t, max_w, max_h );
    Get_Bounds("y", f, t, max_w, max_h );
    Get_Bounds("z", f, t, max_w, max_h );

    Get_Bounds("`", f, t, max_w, max_h );
    Get_Bounds("1", f, t, max_w, max_h );
    Get_Bounds("2", f, t, max_w, max_h );
    Get_Bounds("3", f, t, max_w, max_h );
    Get_Bounds("4", f, t, max_w, max_h );
    Get_Bounds("5", f, t, max_w, max_h );
    Get_Bounds("6", f, t, max_w, max_h );
    Get_Bounds("7", f, t, max_w, max_h );
    Get_Bounds("8", f, t, max_w, max_h );
    Get_Bounds("9", f, t, max_w, max_h );
    Get_Bounds("0", f, t, max_w, max_h );
    Get_Bounds("-", f, t, max_w, max_h );
    Get_Bounds("=", f, t, max_w, max_h );
    Get_Bounds("[", f, t, max_w, max_h );
    Get_Bounds("]", f, t, max_w, max_h );
    Get_Bounds("\\",f, t, max_w, max_h );
    Get_Bounds(";", f, t, max_w, max_h );
    Get_Bounds("'", f, t, max_w, max_h );
    Get_Bounds(",", f, t, max_w, max_h );
    Get_Bounds(".", f, t, max_w, max_h );
    Get_Bounds("/", f, t, max_w, max_h );
    Get_Bounds(" ", f, t, max_w, max_h );

    Get_Bounds("~", f, t, max_w, max_h );
    Get_Bounds("@", f, t, max_w, max_h );
    Get_Bounds("#", f, t, max_w, max_h );
    Get_Bounds("$", f, t, max_w, max_h );
    Get_Bounds("%", f, t, max_w, max_h );
    Get_Bounds("^", f, t, max_w, max_h );
    Get_Bounds("&", f, t, max_w, max_h );
    Get_Bounds("*", f, t, max_w, max_h );
    Get_Bounds("(", f, t, max_w, max_h );
    Get_Bounds(")", f, t, max_w, max_h );
    Get_Bounds("_", f, t, max_w, max_h );
    Get_Bounds("+", f, t, max_w, max_h );
    Get_Bounds("{", f, t, max_w, max_h );
    Get_Bounds("}", f, t, max_w, max_h );
    Get_Bounds("|", f, t, max_w, max_h );
    Get_Bounds(":", f, t, max_w, max_h );
    Get_Bounds("\"",f, t, max_w, max_h );
    Get_Bounds("<", f, t, max_w, max_h );
    Get_Bounds(">", f, t, max_w, max_h );
    Get_Bounds("?", f, t, max_w, max_h );
  }

  void Get_Bounds( String C
                 , Font f
                 , Text t
                 , Ptr_Double max_w
                 , Ptr_Double max_h )
  {
    t.setFont( f );
    t.setText(C);
    Bounds b = t.getLayoutBounds();
    max_w.val = Math.max( b.getWidth(), max_w.val );
    max_h.val = Math.max( b.getHeight(), max_h.val );
  }
  public int Num_Rows() { return m_num_rows; }
  public int Num_Cols() { return m_num_cols; }

  void Init_Graphics()
  {
    m_gc.setFill( Color.BLACK );
    m_gc.fillRect( 0, 0, getWidth(), getHeight() );
    m_gc.setFont( m_font_plain );
    m_gc.setTextBaseline( VPos.TOP );
  //m_gc.setFontSmoothingType( FontSmoothingType.LCD );
  }
  void Init_TextChars()
  {
    m_text_chars = new String[0x10000];

    for( char C=0; C<0xffff; C++ )
    {
      char data[] = { C };

      m_text_chars[C] = new String(data);
    }
    char data[] = { 0 };

    m_text_chars[0xffff] = new String(data);
  }
  void Init_RowsCols()
  {
    m_num_rows = (int)(getHeight()/m_text_H+0.5);
    m_num_cols = (int)(getWidth ()/m_text_W+0.5);

    if( m_siz_rows < m_num_rows
     || m_siz_cols < m_num_cols )
    {
      // Window got bigger, so re-allocate:
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

    final double height = m_vis.m_scene.getHeight();
    final double width  = m_vis.m_scene.getWidth();

    m_num_rows = (int)(height/m_text_H+0.5);
    m_num_cols = (int)(width /m_text_W+0.5);

    boolean resized = old_num_rows != m_num_rows
                   || old_num_cols != m_num_cols;
    if( resized )
    {
      setWidth ( width  );
      setHeight( height );
    }
    return resized;
  }
  void Change_Font_Size( final int size_change )
  {
    if( 0 != size_change )
    {
      int new_font_size = m_font_size + size_change;

      if( new_font_size < MIN_FONT_SIZE ) new_font_size = MIN_FONT_SIZE;

      if( new_font_size != m_font_size )
      {
        m_font_size = new_font_size;

        m_font_plain = Font.font( GetFontName(), FontWeight.NORMAL, m_font_size );
        m_font_bold  = Font.font( GetFontName(), FontWeight.BOLD  , m_font_size );

        m_gc.setFont( m_font_plain );

        Init_FontMetrics();
      }
    }
  }
  public int KeysIn()
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

    return m_input.size();
  }
  public char GetKey()
  {
    char C = 0;
    if     ( m_get_from_dot_buf ) C = In_DotBuf();
    else if( m_get_from_map_buf ) C = In_MapBuf();
    else                          C = m_input.remove();

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
  public
  void Set( final int ROW, final int COL, final char C, final Style S )
  {
    try {
      if( m_num_rows <= ROW )
      {
        throw new Exception( "ConsoleFx::Set(): m_num_rows="+ m_num_rows +", ROW="+ ROW );
      }
      else if( m_num_cols <= COL )
      {
        throw new Exception( "ConsoleFx::Set(): m_num_cols="+ m_num_cols +", COL="+ COL );
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
  public
  void SetS( final int ROW, final int COL, final String str, final Style S )
  {
    for( int k=0; k<str.length(); k++ )
    {
      Set( ROW, COL+k, str.charAt(k), S );
    }
  }
  public
  void Set_Crs_Cell( final int ROW, final int COL )
  {
    m_crs_row = ROW;
    m_crs_col = COL;
  }
  private
  void PrintC( final int row, final int col, final char C, final Style S )
  {
    if( row < m_num_rows && col < m_num_cols )
    {
      // Draw background rectangle:
      m_gc.setFill( Style_2_BG( S ) );

      final int x_p_b = col*m_text_W; // X point background
      final int y_p_b = row*m_text_H; // Y point background

      m_gc.fillRect( x_p_b, y_p_b, m_text_W, m_text_H );

      if( C != ' ' && C != '\t' && C != '\r' )
      {
        // Draw foreground character:
        m_gc.setFill( Style_2_FG( S ) );

        final int x_p_t = col*m_text_W; // X point text
        final int y_p_t = row*m_text_H; // Y point text

        m_gc.fillText( m_text_chars[C], x_p_t, y_p_t );
      }
    }
  }

  void Set_Color_Scheme_1()
  {
    NORMAL_FG       = Color.WHITE  ;  NORMAL_BG       = Color.BLACK  ;
    STATUS_FG       = Color.WHITE  ;  STATUS_BG       = Color.BLUE   ;
    BORDER_FG       = Color.WHITE  ;  BORDER_BG       = Color.BLUE   ;
    BORDER_HI_FG    = Color.WHITE  ;  BORDER_HI_BG    = Color.LIME   ;
    BANNER_FG       = Color.WHITE  ;  BANNER_BG       = Color.RED    ;
    STAR_FG         = Color.WHITE  ;  STAR_BG         = Color.RED    ;
    COMMENT_FG      = m_comment_fg ;  COMMENT_BG      = Color.BLACK  ;
    DEFINE_FG       = Color.MAGENTA;  DEFINE_BG       = Color.BLACK  ;
    CONST_FG        = Color.CYAN   ;  CONST_BG        = Color.BLACK  ;
    CONTROL_FG      = Color.YELLOW ;  CONTROL_BG      = Color.BLACK  ;
    VARTYPE_FG      = Color.LIME   ;  VARTYPE_BG      = Color.BLACK  ;
    VISUAL_FG       = Color.WHITE  ;  VISUAL_BG       = Color.RED    ;
    NONASCII_FG     = Color.RED    ;  NONASCII_BG     = Color.BLUE   ;
    RV_NORMAL_FG    = Color.BLACK  ;  RV_NORMAL_BG    = Color.WHITE  ;
    RV_STATUS_FG    = Color.BLUE   ;  RV_STATUS_BG    = Color.BLUE   ;
    RV_BORDER_FG    = Color.BLUE   ;  RV_BORDER_BG    = Color.WHITE  ;
    RV_BORDER_HI_FG = Color.LIME   ;  RV_BORDER_HI_BG = Color.WHITE  ;
    RV_BANNER_FG    = Color.RED    ;  RV_BANNER_BG    = Color.WHITE  ;
    RV_STAR_FG      = Color.RED    ;  RV_STAR_BG      = Color.WHITE  ;
    RV_COMMENT_FG   = Color.WHITE  ;  RV_COMMENT_BG   = Color.BLUE   ;
    RV_DEFINE_FG    = Color.WHITE  ;  RV_DEFINE_BG    = Color.MAGENTA;
    RV_CONST_FG     = Color.BLACK  ;  RV_CONST_BG     = Color.CYAN   ;
    RV_CONTROL_FG   = Color.BLACK  ;  RV_CONTROL_BG   = Color.YELLOW ;
    RV_VARTYPE_FG   = Color.WHITE  ;  RV_VARTYPE_BG   = Color.LIME   ;
    RV_VISUAL_FG    = Color.RED    ;  RV_VISUAL_BG    = Color.WHITE  ;
    RV_NONASCII_FG  = Color.BLUE   ;  RV_NONASCII_BG  = Color.RED    ;
    EMPTY_FG        = Color.RED    ;  EMPTY_BG        = Color.BLACK  ;
    EOF_FG          = Color.RED    ;  EOF_BG          = m_d_gray     ;
    DIFF_DEL_FG     = Color.WHITE  ;  DIFF_DEL_BG     = Color.RED    ;
    DIFF_NORMAL_FG  = Color.WHITE  ;  DIFF_NORMAL_BG  = Color.BLUE   ;
    DIFF_STAR_FG    = Color.BLUE   ;  DIFF_STAR_BG    = Color.RED    ;
    DIFF_COMMENT_FG = Color.WHITE  ;  DIFF_COMMENT_BG = Color.BLUE   ;
    DIFF_DEFINE_FG  = Color.MAGENTA;  DIFF_DEFINE_BG  = Color.BLUE   ;
    DIFF_CONST_FG   = Color.CYAN   ;  DIFF_CONST_BG   = Color.BLUE   ;
    DIFF_CONTROL_FG = Color.YELLOW ;  DIFF_CONTROL_BG = Color.BLUE   ;
    DIFF_VARTYPE_FG = Color.LIME   ;  DIFF_VARTYPE_BG = Color.BLUE   ;
    DIFF_VISUAL_FG  = Color.BLUE   ;  DIFF_VISUAL_BG  = Color.RED    ;
    CURSOR_FG       = Color.BLACK  ;  CURSOR_BG       = Color.PINK   ;
                                      CURSOR_EMPTY_BG = Color.BLACK  ;
    m_gc.setFont( m_font_plain );

    Init_Clear();
    m_vis.UpdateViews();
    m_vis.CV().PrintCursor();  // Does m_console.Update();
  }
  void Set_Color_Scheme_2()
  {
    NORMAL_FG       = Color.WHITE  ;  NORMAL_BG       = Color.BLACK  ;
    STATUS_FG       = Color.WHITE  ;  STATUS_BG       = Color.BLUE   ;
    BORDER_FG       = Color.WHITE  ;  BORDER_BG       = Color.BLUE   ;
    BORDER_HI_FG    = Color.WHITE  ;  BORDER_HI_BG    = Color.LIME   ;
    BANNER_FG       = Color.WHITE  ;  BANNER_BG       = Color.RED    ;
    STAR_FG         = Color.WHITE  ;  STAR_BG         = Color.RED    ;
    COMMENT_FG      = m_comment_fg ;  COMMENT_BG      = Color.BLACK  ;
    DEFINE_FG       = Color.MAGENTA;  DEFINE_BG       = Color.BLACK  ;
    CONST_FG        = Color.CYAN   ;  CONST_BG        = Color.BLACK  ;
    CONTROL_FG      = Color.YELLOW ;  CONTROL_BG      = Color.BLACK  ;
    VARTYPE_FG      = Color.LIME   ;  VARTYPE_BG      = Color.BLACK  ;
    VISUAL_FG       = Color.WHITE  ;  VISUAL_BG       = Color.RED    ;
    NONASCII_FG     = Color.RED    ;  NONASCII_BG     = Color.BLUE   ;
    RV_NORMAL_FG    = Color.BLACK  ;  RV_NORMAL_BG    = Color.WHITE  ;
    RV_STATUS_FG    = Color.BLUE   ;  RV_STATUS_BG    = Color.BLUE   ;
    RV_BORDER_FG    = Color.BLUE   ;  RV_BORDER_BG    = Color.WHITE  ;
    RV_BORDER_HI_FG = Color.LIME   ;  RV_BORDER_HI_BG = Color.WHITE  ;
    RV_BANNER_FG    = Color.RED    ;  RV_BANNER_BG    = Color.WHITE  ;
    RV_STAR_FG      = Color.RED    ;  RV_STAR_BG      = Color.WHITE  ;
    RV_COMMENT_FG   = Color.WHITE  ;  RV_COMMENT_BG   = Color.BLUE   ;
    RV_DEFINE_FG    = Color.WHITE  ;  RV_DEFINE_BG    = Color.MAGENTA;
    RV_CONST_FG     = Color.BLACK  ;  RV_CONST_BG     = Color.CYAN   ;
    RV_CONTROL_FG   = Color.BLACK  ;  RV_CONTROL_BG   = Color.YELLOW ;
    RV_VARTYPE_FG   = Color.WHITE  ;  RV_VARTYPE_BG   = Color.LIME   ;
    RV_VISUAL_FG    = Color.RED    ;  RV_VISUAL_BG    = Color.WHITE  ;
    RV_NONASCII_FG  = Color.BLUE   ;  RV_NONASCII_BG  = Color.RED    ;
    EMPTY_FG        = Color.RED    ;  EMPTY_BG        = m_d_gray     ;
    EOF_FG          = Color.RED    ;  EOF_BG          = m_d_gray     ;
    DIFF_DEL_FG     = Color.WHITE  ;  DIFF_DEL_BG     = Color.RED    ;
    DIFF_NORMAL_FG  = Color.WHITE  ;  DIFF_NORMAL_BG  = Color.BLUE   ;
    DIFF_STAR_FG    = Color.BLUE   ;  DIFF_STAR_BG    = Color.RED    ;
    DIFF_COMMENT_FG = Color.WHITE  ;  DIFF_COMMENT_BG = Color.BLUE   ;
    DIFF_DEFINE_FG  = Color.MAGENTA;  DIFF_DEFINE_BG  = Color.BLUE   ;
    DIFF_CONST_FG   = Color.CYAN   ;  DIFF_CONST_BG   = Color.BLUE   ;
    DIFF_CONTROL_FG = Color.YELLOW ;  DIFF_CONTROL_BG = Color.BLUE   ;
    DIFF_VARTYPE_FG = Color.LIME   ;  DIFF_VARTYPE_BG = Color.BLUE   ;
    DIFF_VISUAL_FG  = Color.BLUE   ;  DIFF_VISUAL_BG  = Color.RED    ;
    CURSOR_FG       = Color.BLACK  ;  CURSOR_BG       = Color.PINK   ;
                                      CURSOR_EMPTY_BG = Color.BLACK  ;
    m_gc.setFont( m_font_plain );

    Init_Clear();
    m_vis.UpdateViews();
    m_vis.CV().PrintCursor();  // Does m_console.Update();
  }
  void Set_Color_Scheme_3()
  {
    NORMAL_FG       = Color.BLACK    ;  NORMAL_BG       = Color.WHITE  ;
    STATUS_FG       = Color.WHITE    ;  STATUS_BG       = Color.BLUE   ;
    BORDER_FG       = Color.WHITE    ;  BORDER_BG       = Color.BLUE   ;
    BORDER_HI_FG    = Color.WHITE    ;  BORDER_HI_BG    = Color.LIME   ;
    BANNER_FG       = Color.WHITE    ;  BANNER_BG       = Color.RED    ;
    STAR_FG         = Color.WHITE    ;  STAR_BG         = Color.RED    ;
    COMMENT_FG      = m_d_blue       ;  COMMENT_BG      = Color.WHITE  ;
    DEFINE_FG       = m_d_magenta    ;  DEFINE_BG       = Color.WHITE  ;
    CONST_FG        = m_d_cyan       ;  CONST_BG        = Color.WHITE  ;
    CONTROL_FG      = m_d_yellow     ;  CONTROL_BG      = Color.WHITE  ;
    VARTYPE_FG      = m_d_green      ;  VARTYPE_BG      = Color.WHITE  ;
    VISUAL_FG       = Color.RED      ;  VISUAL_BG       = Color.WHITE  ;
    NONASCII_FG     = Color.YELLOW   ;  NONASCII_BG     = Color.CYAN   ;
    RV_NORMAL_FG    = Color.WHITE    ;  RV_NORMAL_BG    = Color.BLACK  ;
    RV_STATUS_FG    = Color.BLUE     ;  RV_STATUS_BG    = Color.WHITE  ;
    RV_BORDER_FG    = Color.BLUE     ;  RV_BORDER_BG    = Color.WHITE  ;
    RV_BORDER_HI_FG = Color.LIME     ;  RV_BORDER_HI_BG = Color.WHITE  ;
    RV_BANNER_FG    = Color.WHITE    ;  RV_BANNER_BG    = Color.RED    ;
    RV_STAR_FG      = Color.RED      ;  RV_STAR_BG      = Color.WHITE  ;
    RV_COMMENT_FG   = Color.WHITE    ;  RV_COMMENT_BG   = Color.BLUE   ;
    RV_DEFINE_FG    = Color.MAGENTA  ;  RV_DEFINE_BG    = Color.WHITE  ;
    RV_CONST_FG     = Color.CYAN     ;  RV_CONST_BG     = Color.BLACK  ;
    RV_CONTROL_FG   = Color.BLACK    ;  RV_CONTROL_BG   = Color.YELLOW ;
    RV_VARTYPE_FG   = Color.LIME     ;  RV_VARTYPE_BG   = Color.WHITE  ;
    RV_VISUAL_FG    = Color.WHITE    ;  RV_VISUAL_BG    = Color.RED    ;
    RV_NONASCII_FG  = Color.CYAN     ;  RV_NONASCII_BG  = Color.YELLOW ;
    EMPTY_FG        = Color.RED      ;  EMPTY_BG        = Color.WHITE  ;
    EOF_FG          = Color.RED      ;  EOF_BG          = m_d_gray     ;
    DIFF_DEL_FG     = Color.WHITE    ;  DIFF_DEL_BG     = Color.RED    ;
    DIFF_NORMAL_FG  = Color.WHITE    ;  DIFF_NORMAL_BG  = Color.BLUE   ;
    DIFF_STAR_FG    = Color.BLUE     ;  DIFF_STAR_BG    = Color.RED    ;
    DIFF_COMMENT_FG = Color.WHITE    ;  DIFF_COMMENT_BG = Color.BLUE   ;
    DIFF_DEFINE_FG  = Color.MAGENTA  ;  DIFF_DEFINE_BG  = Color.BLUE   ;
    DIFF_CONST_FG   = Color.CYAN     ;  DIFF_CONST_BG   = Color.BLUE   ;
    DIFF_CONTROL_FG = Color.YELLOW   ;  DIFF_CONTROL_BG = Color.BLUE   ;
    DIFF_VARTYPE_FG = Color.LIME     ;  DIFF_VARTYPE_BG = Color.BLUE   ;
    DIFF_VISUAL_FG  = Color.BLUE     ;  DIFF_VISUAL_BG  = Color.RED    ;
    CURSOR_FG       = Color.BLACK    ;  CURSOR_BG       = m_d_pink     ;
                                        CURSOR_EMPTY_BG = Color.BLACK  ;
    m_gc.setFont( m_font_bold );

    Init_Clear();
    m_vis.UpdateViews();
    m_vis.CV().PrintCursor();  // Does m_console.Update();
  }
  void Set_Color_Scheme_4()
  {
    NORMAL_FG       = Color.BLACK    ;  NORMAL_BG       = Color.WHITE  ;
    STATUS_FG       = Color.WHITE    ;  STATUS_BG       = Color.BLUE   ;
    BORDER_FG       = Color.WHITE    ;  BORDER_BG       = Color.BLUE   ;
    BORDER_HI_FG    = Color.WHITE    ;  BORDER_HI_BG    = Color.LIME   ;
    BANNER_FG       = Color.WHITE    ;  BANNER_BG       = Color.RED    ;
    STAR_FG         = Color.WHITE    ;  STAR_BG         = Color.RED    ;
    COMMENT_FG      = m_d_blue       ;  COMMENT_BG      = Color.WHITE  ;
    DEFINE_FG       = m_d_magenta    ;  DEFINE_BG       = Color.WHITE  ;
    CONST_FG        = m_d_cyan       ;  CONST_BG        = Color.WHITE  ;
    CONTROL_FG      = m_d_yellow     ;  CONTROL_BG      = Color.WHITE  ;
    VARTYPE_FG      = m_d_green      ;  VARTYPE_BG      = Color.WHITE  ;
    VISUAL_FG       = Color.RED      ;  VISUAL_BG       = Color.WHITE  ;
    NONASCII_FG     = Color.YELLOW   ;  NONASCII_BG     = Color.CYAN   ;
    RV_NORMAL_FG    = Color.WHITE    ;  RV_NORMAL_BG    = Color.BLACK  ;
    RV_STATUS_FG    = Color.BLUE     ;  RV_STATUS_BG    = Color.WHITE  ;
    RV_BORDER_FG    = Color.BLUE     ;  RV_BORDER_BG    = Color.WHITE  ;
    RV_BORDER_HI_FG = Color.LIME     ;  RV_BORDER_HI_BG = Color.WHITE  ;
    RV_BANNER_FG    = Color.WHITE    ;  RV_BANNER_BG    = Color.RED    ;
    RV_STAR_FG      = Color.RED      ;  RV_STAR_BG      = Color.WHITE  ;
    RV_COMMENT_FG   = Color.WHITE    ;  RV_COMMENT_BG   = Color.BLUE   ;
    RV_DEFINE_FG    = Color.MAGENTA  ;  RV_DEFINE_BG    = Color.WHITE  ;
    RV_CONST_FG     = Color.CYAN     ;  RV_CONST_BG     = Color.BLACK  ;
    RV_CONTROL_FG   = Color.BLACK    ;  RV_CONTROL_BG   = Color.YELLOW ;
    RV_VARTYPE_FG   = Color.LIME     ;  RV_VARTYPE_BG   = Color.WHITE  ;
    RV_VISUAL_FG    = Color.WHITE    ;  RV_VISUAL_BG    = Color.RED    ;
    RV_NONASCII_FG  = Color.CYAN     ;  RV_NONASCII_BG  = Color.YELLOW ;
    EMPTY_FG        = Color.RED      ;  EMPTY_BG        = m_d_gray     ;
    EOF_FG          = Color.RED      ;  EOF_BG          = m_d_gray     ;
    DIFF_DEL_FG     = Color.WHITE    ;  DIFF_DEL_BG     = Color.RED    ;
    DIFF_NORMAL_FG  = Color.WHITE    ;  DIFF_NORMAL_BG  = Color.BLUE   ;
    DIFF_STAR_FG    = Color.BLUE     ;  DIFF_STAR_BG    = Color.RED    ;
    DIFF_COMMENT_FG = Color.WHITE    ;  DIFF_COMMENT_BG = Color.BLUE   ;
    DIFF_DEFINE_FG  = Color.MAGENTA  ;  DIFF_DEFINE_BG  = Color.BLUE   ;
    DIFF_CONST_FG   = Color.CYAN     ;  DIFF_CONST_BG   = Color.BLUE   ;
    DIFF_CONTROL_FG = Color.YELLOW   ;  DIFF_CONTROL_BG = Color.BLUE   ;
    DIFF_VARTYPE_FG = Color.LIME     ;  DIFF_VARTYPE_BG = Color.BLUE   ;
    DIFF_VISUAL_FG  = Color.BLUE     ;  DIFF_VISUAL_BG  = Color.RED    ;
    CURSOR_FG       = Color.BLACK    ;  CURSOR_BG       = m_d_pink     ;
                                        CURSOR_EMPTY_BG = Color.BLACK  ;
    m_gc.setFont( m_font_bold );

    Init_Clear();
    m_vis.UpdateViews();
    m_vis.CV().PrintCursor();  // Does m_console.Update();
  }

  private Color Style_2_BG( final Style S )
  {
    Color C =  Color.BLACK; // Default
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
    case CURSOR      : C = CURSOR_BG      ; break;
    case CURSOR_EMPTY: C = CURSOR_EMPTY_BG; break;
    }
    return C;
  }
  private Color Style_2_FG( final Style S )
  {
    Color C =  Color.WHITE; // Default
    switch( S )
    {
    case NORMAL      : C = NORMAL_FG      ; break;
    case STATUS      : C = STATUS_FG      ; break;
    case BORDER      : C = BORDER_FG      ; break;
    case BORDER_HI   : C = BORDER_HI_FG   ; break;
    case BANNER      : C = BANNER_FG      ; break;
    case STAR        : C = STAR_FG        ; break;
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
    case CURSOR      : C = CURSOR_FG      ; break;
    }
    return C;
  }
  public boolean Update()
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
      Print_Cursor();
    }
    return output_something;
  }
  void Print_Cursor()
  {
    // Print the cursor:
    PrintC( m_crs_row
          , m_crs_col
          , m_chars__w[m_crs_row][m_crs_col]
          , Style.CURSOR );

    m_min_touched[ m_crs_row ] = m_crs_col;   // Cursor
    m_max_touched[ m_crs_row ] = m_crs_col+1; // touched

    // This will cause the old cursor cell to be put back to
    // its highlighed value on the next call of Update():
    m_styles_w[m_crs_row][m_crs_col] = Style.CURSOR;
  }
  public void set_save_2_vis_buf( final boolean save )
  {
    m_save_2_vis_buf = save;
  }
  public StringBuilder get_dot_buf()
  {
    return m_dot_buf;
  }
  public boolean get_from_dot_buf()
  {
    return m_get_from_dot_buf;
  }
  public void copy_vis_buf_2_dot_buf()
  {
    // setLength( 0 ) followed by append() accomplishes copy:
    m_dot_buf.setLength( 0 );
    m_dot_buf.append( m_vis_buf );
  }
  public void copy_paste_buf_2_system_clipboard()
  {
    if( null == m_cb )
    {
      m_cb = Clipboard.getSystemClipboard();
    }
    if( null == m_cbc )
    {
      m_cbc = new ClipboardContent();
    }

    m_sb.setLength( 0 );
    ArrayList<Line> reg = m_vis.get_reg();

    for( int k=0; k<reg.size(); k++ )
    {
      if( 0<k ) m_sb.append("\n");
      m_sb.append( reg.get(k).toString() ); 
    }
    m_cbc.putString( m_sb.toString() ); 

    m_cb.setContent( m_cbc );

    if( 0<m_sb.length() )
    {
      m_vis.CmdLineMessage("Copied "
                          + reg.size()    +" lines, "
                          + m_sb.length() +" chars to system clipboard");
    }
    else {
      m_vis.CmdLineMessage("Cleared system clipboard");
    }
  }
  public void copy_system_clipboard_2_paste_buf()
  {
    if( null == m_cb )
    {
      m_cb = Clipboard.getSystemClipboard();
    }

    ArrayList<Line> reg = m_vis.get_reg();
    reg.clear();
    String cb_str = m_cb.getString();

    if( null == cb_str )
    {
      m_vis.CmdLineMessage("Cleared paste buffer");
    }
    else {
      String[] cb_lines = cb_str.split("\n");

      for( int k=0; k<cb_lines.length; k++ )
      {
        Line line = new Line();
        line.append_s( cb_lines[k] );
        reg.add( line );
      }
      m_vis.CmdLineMessage("Copied "
                          + cb_lines.length +" lines, "
                          + cb_str.length() +" chars to paste buffer");
    }
  }
  private Color    m_comment_fg = Color.color( 0.3f, 0.3f, 1.0f );
  private Color    m_d_blue     = Color.color( 0.0f, 0.0f, 1.0f );
  private Color    m_d_green    = Color.color( 0.0f, 1.0f, 0.0f );
  private Color    m_d_yellow   = Color.color( 0.9f, 0.9f, 0.0f );
  private Color    m_d_magenta  = Color.color( 1.0f, 0.0f, 1.0f );
  private Color    m_d_cyan     = Color.color( 0.0f, 0.9f, 0.9f );
  private Color    m_d_white    = Color.color( 0.9f, 0.9f, 0.9f );
  private Color    m_d_pink     = Color.color( 1.0f, 0.5f, 0.5f );
  private Color    m_d_gray     = Color.color( 0.2f, 0.2f, 0.2f );

  private Color NORMAL_FG       = Color.WHITE  ;
  private Color STATUS_FG       = Color.WHITE  ;
  private Color BORDER_FG       = Color.WHITE  ;
  private Color BORDER_HI_FG    = Color.WHITE  ;
  private Color BANNER_FG       = Color.WHITE  ;
  private Color STAR_FG         = Color.WHITE  ;
  private Color COMMENT_FG      = m_comment_fg ;
  private Color DEFINE_FG       = Color.MAGENTA;
  private Color CONST_FG        = Color.CYAN   ;
  private Color CONTROL_FG      = Color.YELLOW ;
  private Color VARTYPE_FG      = Color.LIME   ; // FX calls green LIME
  private Color VISUAL_FG       = Color.WHITE  ;
  private Color NONASCII_FG     = Color.RED    ;
  private Color RV_NORMAL_FG    = Color.BLACK  ;
  private Color RV_STATUS_FG    = Color.BLUE   ;
  private Color RV_BORDER_FG    = Color.BLUE   ;
  private Color RV_BORDER_HI_FG = Color.LIME   ;
  private Color RV_BANNER_FG    = Color.RED    ;
  private Color RV_STAR_FG      = Color.RED    ;
  private Color RV_COMMENT_FG   = Color.WHITE  ;
  private Color RV_DEFINE_FG    = Color.WHITE  ;
  private Color RV_CONST_FG     = Color.BLACK  ;
  private Color RV_CONTROL_FG   = Color.BLACK  ;
  private Color RV_VARTYPE_FG   = Color.WHITE  ;
  private Color RV_VISUAL_FG    = Color.RED    ;
  private Color RV_NONASCII_FG  = Color.BLUE   ;
  private Color EMPTY_FG        = Color.RED    ;
  private Color EOF_FG          = Color.RED    ;
  private Color DIFF_DEL_FG     = Color.WHITE  ;
  private Color DIFF_NORMAL_FG  = Color.WHITE  ;
  private Color DIFF_STAR_FG    = Color.BLUE   ;
  private Color DIFF_COMMENT_FG = Color.WHITE  ;
  private Color DIFF_DEFINE_FG  = Color.MAGENTA;
  private Color DIFF_CONST_FG   = Color.CYAN   ;
  private Color DIFF_CONTROL_FG = Color.YELLOW ;
  private Color DIFF_VARTYPE_FG = Color.LIME   ;
  private Color DIFF_VISUAL_FG  = Color.BLUE   ;
  private Color CURSOR_FG       = Color.BLACK  ;

  private Color NORMAL_BG       = Color.BLACK  ; 
  private Color STATUS_BG       = Color.BLUE   ; 
  private Color BORDER_BG       = Color.BLUE   ; 
  private Color BORDER_HI_BG    = Color.LIME   ; 
  private Color BANNER_BG       = Color.RED    ; 
  private Color STAR_BG         = Color.RED    ; 
  private Color COMMENT_BG      = Color.BLACK  ; 
  private Color DEFINE_BG       = Color.BLACK  ; 
  private Color CONST_BG        = Color.BLACK  ; 
  private Color CONTROL_BG      = Color.BLACK  ; 
  private Color VARTYPE_BG      = Color.BLACK  ; 
  private Color VISUAL_BG       = Color.RED    ; 
  private Color NONASCII_BG     = Color.BLUE   ; 
  private Color RV_NORMAL_BG    = Color.WHITE  ; 
  private Color RV_STATUS_BG    = Color.BLUE   ; 
  private Color RV_BORDER_BG    = Color.WHITE  ; 
  private Color RV_BORDER_HI_BG = Color.WHITE  ; 
  private Color RV_BANNER_BG    = Color.WHITE  ; 
  private Color RV_STAR_BG      = Color.WHITE  ; 
  private Color RV_COMMENT_BG   = Color.BLUE   ; 
  private Color RV_DEFINE_BG    = Color.MAGENTA; 
  private Color RV_CONST_BG     = Color.CYAN   ; 
  private Color RV_CONTROL_BG   = Color.YELLOW ; 
  private Color RV_VARTYPE_BG   = Color.LIME   ; 
  private Color RV_VISUAL_BG    = Color.WHITE  ; 
  private Color RV_NONASCII_BG  = Color.RED    ; 
  private Color EMPTY_BG        = Color.BLACK  ;
  private Color EOF_BG          = m_d_gray     ;
  private Color DIFF_DEL_BG     = Color.RED    ; 
  private Color DIFF_NORMAL_BG  = Color.BLUE   ; 
  private Color DIFF_STAR_BG    = Color.RED    ; 
  private Color DIFF_COMMENT_BG = Color.BLUE   ; 
  private Color DIFF_DEFINE_BG  = Color.BLUE   ; 
  private Color DIFF_CONST_BG   = Color.BLUE   ; 
  private Color DIFF_CONTROL_BG = Color.BLUE   ; 
  private Color DIFF_VARTYPE_BG = Color.BLUE   ; 
  private Color DIFF_VISUAL_BG  = Color.RED    ; 
  private Color CURSOR_BG       = Color.PINK   ; 
  private Color CURSOR_EMPTY_BG = Color.BLACK  ; 

  VisFx            m_vis;
  GraphicsContext  m_gc;
  Font             m_font_plain;
  Font             m_font_bold;
  int              m_font_size = FONT_SIZE;
  int              m_text_W;
  int              m_text_H;
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

  boolean          m_save_2_dot_buf;
  boolean          m_save_2_vis_buf;
  boolean          m_save_2_map_buf;
  boolean          m_get_from_dot_buf;
  boolean          m_get_from_map_buf;
  String[]         m_text_chars;
  char             m_C;
  boolean          m_caps_on;
  Queue<Character> m_input   = new ArrayDeque<Character>();

  StringBuilder    m_dot_buf = new StringBuilder();
  StringBuilder    m_vis_buf = new StringBuilder();
  StringBuilder    m_map_buf = new StringBuilder();
  private int      m_dot_buf_index;
  private int      m_map_buf_index;
  private int      m_crs_row;
  private int      m_crs_col;
  StringBuilder    m_sb = new StringBuilder();
  Clipboard        m_cb;
  ClipboardContent m_cbc;
}

