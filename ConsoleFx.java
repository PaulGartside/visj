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
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.geometry.VPos;

import javafx.scene.text.Text;
import javafx.geometry.Bounds;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

class ConsoleFx extends Canvas
             implements ConsoleIF
{
  ConsoleFx( VisFx vis, int width, int height )
  {
    super( width, height );

    m_vis      = vis;
    m_gc       = getGraphicsContext2D();
    m_num_rows = 0;
    m_num_cols = 0;
    m_siz_rows = 0;
    m_siz_cols = 0;

    m_font_name  = GetDefaultFontName();
    m_font_plain = Font.font( m_font_name, FontWeight.NORMAL, m_font_size );
    m_font_bold  = Font.font( m_font_name, FontWeight.BOLD  , m_font_size );

    Init_TextChars();
    Init_FontMetrics();
    Init_Graphics();
    Init_RowsCols();
    Init_Clear();
    Init_Color_Scheme_1();
  }

  void Key_Pressed( KeyEvent ke )
  {
    if( Add_Key_Char( ke ) )
    {
      if( m_input.add( m_C ) ) //< Should always be true
      {
        if( m_save_2_map_buf   ) m_map_buf.append( m_C );
        if( m_save_2_dot_buf_n ) m_dot_buf_n.append( m_C );
        if( m_save_2_dot_buf_l ) m_dot_buf_l.append( m_C );
        if( m_save_2_vis_buf   ) m_vis_buf.append( m_C );
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

  String GetDefaultFontName()
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
    if( null == m_x_p_o )
    {
      m_x_p_o = new Integer[Character.MAX_VALUE+1];
    }
    Ptr_Double max_w = new Ptr_Double();
    Ptr_Double max_h = new Ptr_Double();

    Find_Font_Bounds( m_font_plain, max_w, max_h );

  //m_text_W = (int)(FONT_SIZE*0.6 + 0.5);
  //m_text_H = FONT_SIZE;

    final int avg_W = (int)(m_avg_W/m_cnt_W + 0.5);

    // Works better if m_text_W and m_text_H are integers
  //m_text_W = (int)(max_w.val + 0.8);
    m_text_W = (int)(avg_W*1.1 + 0.8);
    m_text_H = (int)(max_h.val + 0.8);

//Utils.Log("m_text_W ="+ m_text_W+", avg_W="+avg_W);
//Utils.Log("m_text_H ="+ m_text_H);

    for( int k=0; k<Character.MAX_VALUE+1; k++ )
    {
      if( null != m_x_p_o[ k ] )
      {
        Calc_X_Point_Offset( (char)k );
      }
    }
  }
  void Find_Font_Bounds( Font f, Ptr_Double max_w, Ptr_Double max_h )
  {
    m_avg_W = 0;
    m_cnt_W = 0;

    Get_Bounds("A", f, max_w, max_h );
    Get_Bounds("B", f, max_w, max_h );
    Get_Bounds("C", f, max_w, max_h );
    Get_Bounds("D", f, max_w, max_h );
    Get_Bounds("E", f, max_w, max_h );
    Get_Bounds("F", f, max_w, max_h );
    Get_Bounds("G", f, max_w, max_h );
    Get_Bounds("H", f, max_w, max_h );
    Get_Bounds("I", f, max_w, max_h );
    Get_Bounds("J", f, max_w, max_h );
    Get_Bounds("K", f, max_w, max_h );
    Get_Bounds("L", f, max_w, max_h );
    Get_Bounds("M", f, max_w, max_h );
    Get_Bounds("N", f, max_w, max_h );
    Get_Bounds("O", f, max_w, max_h );
    Get_Bounds("P", f, max_w, max_h );
    Get_Bounds("Q", f, max_w, max_h );
    Get_Bounds("R", f, max_w, max_h );
    Get_Bounds("S", f, max_w, max_h );
    Get_Bounds("T", f, max_w, max_h );
    Get_Bounds("U", f, max_w, max_h );
    Get_Bounds("V", f, max_w, max_h );
    Get_Bounds("W", f, max_w, max_h );
    Get_Bounds("X", f, max_w, max_h );
    Get_Bounds("Y", f, max_w, max_h );
    Get_Bounds("Z", f, max_w, max_h );

    Get_Bounds("a", f, max_w, max_h );
    Get_Bounds("b", f, max_w, max_h );
    Get_Bounds("c", f, max_w, max_h );
    Get_Bounds("d", f, max_w, max_h );
    Get_Bounds("e", f, max_w, max_h );
    Get_Bounds("f", f, max_w, max_h );
    Get_Bounds("g", f, max_w, max_h );
    Get_Bounds("h", f, max_w, max_h );
    Get_Bounds("i", f, max_w, max_h );
    Get_Bounds("j", f, max_w, max_h );
    Get_Bounds("k", f, max_w, max_h );
    Get_Bounds("l", f, max_w, max_h );
    Get_Bounds("m", f, max_w, max_h );
    Get_Bounds("n", f, max_w, max_h );
    Get_Bounds("o", f, max_w, max_h );
    Get_Bounds("p", f, max_w, max_h );
    Get_Bounds("q", f, max_w, max_h );
    Get_Bounds("r", f, max_w, max_h );
    Get_Bounds("s", f, max_w, max_h );
    Get_Bounds("t", f, max_w, max_h );
    Get_Bounds("u", f, max_w, max_h );
    Get_Bounds("v", f, max_w, max_h );
    Get_Bounds("w", f, max_w, max_h );
    Get_Bounds("x", f, max_w, max_h );
    Get_Bounds("y", f, max_w, max_h );
    Get_Bounds("z", f, max_w, max_h );

    Get_Bounds("`", f, max_w, max_h );
    Get_Bounds("1", f, max_w, max_h );
    Get_Bounds("2", f, max_w, max_h );
    Get_Bounds("3", f, max_w, max_h );
    Get_Bounds("4", f, max_w, max_h );
    Get_Bounds("5", f, max_w, max_h );
    Get_Bounds("6", f, max_w, max_h );
    Get_Bounds("7", f, max_w, max_h );
    Get_Bounds("8", f, max_w, max_h );
    Get_Bounds("9", f, max_w, max_h );
    Get_Bounds("0", f, max_w, max_h );
    Get_Bounds("-", f, max_w, max_h );
    Get_Bounds("=", f, max_w, max_h );
    Get_Bounds("[", f, max_w, max_h );
    Get_Bounds("]", f, max_w, max_h );
    Get_Bounds("\\",f, max_w, max_h );
    Get_Bounds(";", f, max_w, max_h );
    Get_Bounds("'", f, max_w, max_h );
    Get_Bounds(",", f, max_w, max_h );
    Get_Bounds(".", f, max_w, max_h );
    Get_Bounds("/", f, max_w, max_h );
    Get_Bounds(" ", f, max_w, max_h );

    Get_Bounds("~", f, max_w, max_h );
    Get_Bounds("@", f, max_w, max_h );
    Get_Bounds("#", f, max_w, max_h );
    Get_Bounds("$", f, max_w, max_h );
    Get_Bounds("%", f, max_w, max_h );
    Get_Bounds("^", f, max_w, max_h );
    Get_Bounds("&", f, max_w, max_h );
    Get_Bounds("*", f, max_w, max_h );
    Get_Bounds("(", f, max_w, max_h );
    Get_Bounds(")", f, max_w, max_h );
    Get_Bounds("_", f, max_w, max_h );
    Get_Bounds("+", f, max_w, max_h );
    Get_Bounds("{", f, max_w, max_h );
    Get_Bounds("}", f, max_w, max_h );
    Get_Bounds("|", f, max_w, max_h );
    Get_Bounds(":", f, max_w, max_h );
    Get_Bounds("\"",f, max_w, max_h );
    Get_Bounds("<", f, max_w, max_h );
    Get_Bounds(">", f, max_w, max_h );
    Get_Bounds("?", f, max_w, max_h );
  }

  void Get_Bounds( String C
                 , Font f
                 , Ptr_Double max_w
                 , Ptr_Double max_h )
  {
    m_text.setFont( f );
    m_text.setText( C );
    Bounds b = m_text.getLayoutBounds();
    max_w.val = Math.max( b.getWidth(), max_w.val );
    max_h.val = Math.max( b.getHeight(), max_h.val );

    m_avg_W += b.getWidth();
    m_cnt_W ++;

  //int b_w = (int)(b.getWidth ()*10 + 0.5);
  //int b_h = (int)(b.getHeight()*10 + 0.5);
  //int m_w = (int)(max_w.val*10 + 0.5);
  //Utils.Log( C+": w*10="+b_w+", h*10="+b_h+", max_w*10="+m_w);
  }

  void Calc_X_Point_Offset( final char C )
  {
    if( null == m_x_p_o[C] )
    {
      m_x_p_o[C] = new Integer(0);
    }
    m_text.setText( m_text_chars[C] );
    Bounds b = m_text.getLayoutBounds();
    final double d_w = b.getWidth();
    // X point offset within cell:
    m_x_p_o[C] = d_w < m_text_W
                 ? (int)(0.5*(m_text_W - d_w) + 0.5)
                 : 0;
  }

  public int Num_Rows() { return m_num_rows; }
  public int Num_Cols() { return m_num_cols; }

  void Init_Graphics()
  {
    m_gc.setFill( Color.BLACK );
    m_gc.fillRect( 0, 0, getWidth(), getHeight() );
    m_gc.setFont( m_font_plain );
    m_text.setFont( m_font_plain );
    m_gc.setTextBaseline( VPos.TOP );
  //m_gc.setFontSmoothingType( FontSmoothingType.LCD );
  }
  void Init_TextChars()
  {
    m_text_chars = new String[0x10000];
  //m_text_chars = new String[Character.MAX_VALUE+1];

    for( char C=0; C<0xffff; C++ )
  //for( char C=0; C<Character.MAX_VALUE; C++ )
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

      m_chars__w    = new char [m_siz_rows][m_siz_cols];
      m_styles_w    = new Style[m_siz_rows][m_siz_cols];
    }
  }
  void Init_Clear()
  {
    // Clear everything:
    for( int row=0; row<m_num_rows; row++ )
    {
      for( int col=0; col<m_num_cols; col++ )
      {
        m_chars__w[row][col] = ' ';
        m_styles_w[row][col] = Style.UNKNOWN;
      }
    }
  }
  void Invalidate_Text( final int row_st
                      , final int row_fn
                      , final int col_st
                      , final int col_fn )
  {
    for( int row=row_st; row<row_fn; row++ )
    {
      for( int col=col_st; col<col_fn; col++ )
      {
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
                   || old_num_cols != m_num_cols
                   || m_font_changed;
    if( resized )
    {
      setWidth ( width  );
      setHeight( height );

      m_font_changed = false;
    }
    return resized;
  }
  void Inc_Font()
  {
    Change_Font_Size( 1 );
  }
  void Dec_Font()
  {
    Change_Font_Size( -1 );
  }
  void Change_Font_Size( final int size_change )
  {
    if( 0 != size_change )
    {
      int new_font_size = m_font_size + size_change;

      if( new_font_size < MIN_FONT_SIZE ) new_font_size = MIN_FONT_SIZE;

      if( new_font_size != m_font_size )
      {
        Change_Font( new_font_size, m_font_name );
      }
    }
  }
  void Change_Font( final int new_font_size, String font_name )
  {
    m_font_size = new_font_size;

    m_font_name  = font_name;
    m_font_plain = Font.font( m_font_name, FontWeight.NORMAL, m_font_size );
    m_font_bold  = Font.font( m_font_name, FontWeight.BOLD  , m_font_size );
    m_font_changed = true;

    m_gc.setFont( m_font_plain );
    m_text.setFont( m_font_plain );

    Init_FontMetrics();
  }

  public int KeysIn()
  {
    int num_keys_in = 0;

    if     ( m_get_from_dot_buf_n ) num_keys_in = m_dot_buf_n.length();
    else if( m_get_from_dot_buf_l ) num_keys_in = m_dot_buf_l.length();
    else if( m_get_from_map_buf   ) num_keys_in = m_map_buf.length();
    else                            num_keys_in = m_input.size();

    if( num_keys_in <= 0 )
    {
      final int KEY_IN_SLEEP_PERIOD = 10;

      // If there is nothing to process, sleep to avoid hogging CPU:
      Utils.Sleep( KEY_IN_SLEEP_PERIOD );
    }
    return num_keys_in;
  }
  public char GetKey()
  {
    char C = 0;
    if     ( m_get_from_dot_buf_n ) C = In_DotBuf_n();
    else if( m_get_from_dot_buf_l ) C = In_DotBuf_l();
    else if( m_get_from_map_buf   ) C = In_MapBuf();
    else                            C = m_input.remove();

    return C;
  }
  private
  char In_DotBuf_n()
  {
    final int DOT_BUF_LEN = m_dot_buf_n.length();

    final char C = m_dot_buf_n.charAt( m_dot_buf_index++ );

    if( DOT_BUF_LEN <= m_dot_buf_index )
    {
      m_get_from_dot_buf_n = false;
      m_dot_buf_index      = 0;
    }
    return C;
  }
  private
  char In_DotBuf_l()
  {
    final int DOT_BUF_LEN = m_dot_buf_l.length();

    final char C = m_dot_buf_l.charAt( m_dot_buf_index++ );

    if( DOT_BUF_LEN <= m_dot_buf_index )
    {
      m_get_from_dot_buf_l = false;
      m_dot_buf_index      = 0;
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
    if( 0 <= ROW && ROW < m_num_rows
     && 0 <= COL && COL < m_num_cols )
    {
      final char  C_w = m_chars__w[ROW][COL];
      final Style S_w = m_styles_w[ROW][COL];

      if( C != C_w || S != S_w )
      {
        PrintC( ROW, COL, C, S );

        m_chars__w[ ROW ][ COL ] = C;
        m_styles_w[ ROW ][ COL ] = S;
      }
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
  void Move_Crs_Cell( final int ROW, final int COL )
  {
    if( 0 <= m_crs_row && m_crs_row < m_num_rows
     && 0 <= m_crs_col && m_crs_col < m_num_cols )
    {
      if( null != m_crs_pos_style_saved )
      {
        // Remove the old cursor:
        PrintC( m_crs_row
              , m_crs_col
              , m_chars__w[m_crs_row][m_crs_col]
              , m_crs_pos_style_saved );

        m_styles_w[m_crs_row][m_crs_col] = m_crs_pos_style_saved;
      }
      m_crs_row = ROW;
      m_crs_col = COL;

      // Save the style at new cursor position:
      m_crs_pos_style_saved = m_styles_w[m_crs_row][m_crs_col];

      // Print the new cursor:
      PrintC( m_crs_row
            , m_crs_col
            , m_chars__w[m_crs_row][m_crs_col]
            , Style.CURSOR );

      m_styles_w[m_crs_row][m_crs_col] = Style.CURSOR;
    }
  }
  void Remove_Crs_Cell()
  {
    if( 0 <= m_crs_row && m_crs_row < m_num_rows
     && 0 <= m_crs_col && m_crs_col < m_num_cols )
    {
      if( null != m_crs_pos_style_saved )
      {
        // Remove the old cursor:
        PrintC( m_crs_row
              , m_crs_col
              , m_chars__w[m_crs_row][m_crs_col]
              , m_crs_pos_style_saved );

        m_styles_w[m_crs_row][m_crs_col] = m_crs_pos_style_saved;

        m_crs_pos_style_saved = null;
      }
    }
  }
  public
  void Add_Crs_Cell( final int ROW, final int COL )
  {
    if( 0 <= m_crs_row && m_crs_row < m_num_rows
     && 0 <= m_crs_col && m_crs_col < m_num_cols )
    {
      m_crs_row = ROW;
      m_crs_col = COL;

      // Save the style at new cursor position:
      m_crs_pos_style_saved = m_styles_w[m_crs_row][m_crs_col];

      // Print the new cursor:
      PrintC( m_crs_row
            , m_crs_col
            , m_chars__w[m_crs_row][m_crs_col]
            , Style.CURSOR );

      m_styles_w[m_crs_row][m_crs_col] = Style.CURSOR;
    }
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

        if( null == m_x_p_o[ C ] )
        {
          Calc_X_Point_Offset( C );
        }
        final int x_p_t = col*m_text_W + m_x_p_o[ C ]; // X point text
        final int y_p_t = row*m_text_H; // Y point text

        // Draw char at x_p_t, y_p_t, clip to m_test_W pixels wide
        m_gc.fillText( m_text_chars[C], x_p_t, y_p_t, m_text_W );
      }
    }
  }

//void DrawImage( Image I, View V, int sx, int sy )
//{
//  final int row = V.Y()+1;
//  final int col = V.X()+1;
//  final int w_row = V.WorkingRows();
//  final int w_col = V.WorkingCols();
//
//  final double sw = Math.min( w_col*m_text_W, I.getWidth() );
//  final double sh = Math.min( w_row*m_text_H, I.getHeight() );
//  final double dx = col*m_text_W;
//  final double dy = row*m_text_H;
//  final double dw = sw;
//  final double dh = sh;
//
//  // Fill in background with gray:
//  m_gc.setFill( m_d_gray );
//  m_gc.fillRect( dx, dy, w_col*m_text_W, w_row*m_text_H );
//
//  m_gc.drawImage( I, sx, sy, sw, sh, dx, dy, dw, dh );
//}
  void DrawImage( Image I, View V, int sx, int sy, double zoom )
  {
    final int row = V.Y()+1;
    final int col = V.X()+1;
    final int w_row = V.WorkingRows();
    final int w_col = V.WorkingCols();

    final double dw = Math.min( w_col*m_text_W, I.getWidth()*zoom );
    final double dh = Math.min( w_row*m_text_H, I.getHeight()*zoom );
    final double sw = dw/zoom;
    final double sh = dh/zoom;
    final double dx = col*m_text_W;
    final double dy = row*m_text_H;

    // Fill in background with gray:
    m_gc.setFill( m_d_gray );
    m_gc.fillRect( dx, dy, w_col*m_text_W, w_row*m_text_H );

    m_gc.drawImage( I, sx, sy, sw, sh, dx, dy, dw, dh );
  }

  void Set_Color_Scheme_1()
  {
    Init_Color_Scheme_1();

    m_gc.setFont( m_font_plain );
    m_text.setFont( m_font_plain );

    Init_Clear();
    m_vis.UpdateViews( false );
    m_vis.CV().PrintCursor();  // Does m_console.Update();
  }
  void Init_Color_Scheme_1()
  {
    NORMAL_FG       = Color.WHITE  ;  NORMAL_BG       = Color.BLACK  ;
    STATUS_FG       = Color.WHITE  ;  STATUS_BG       = Color.BLUE   ;
    BORDER_FG       = Color.WHITE  ;  BORDER_BG       = Color.BLUE   ;
    BORDER_HI_FG    = Color.BLUE   ;  BORDER_HI_BG    = Color.LIME   ; // FX calls green LIME
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
  }

  void Set_Color_Scheme_2()
  {
    Init_Color_Scheme_2();

    m_gc.setFont( m_font_plain );
    m_text.setFont( m_font_plain );

    Init_Clear();
    m_vis.UpdateViews( false );
    m_vis.CV().PrintCursor();  // Does m_console.Update();
  }
  void Init_Color_Scheme_2()
  {
    NORMAL_FG       = Color.WHITE  ;  NORMAL_BG       = Color.BLACK  ;
    STATUS_FG       = Color.WHITE  ;  STATUS_BG       = Color.BLUE   ;
    BORDER_FG       = Color.WHITE  ;  BORDER_BG       = Color.BLUE   ;
    BORDER_HI_FG    = Color.BLUE   ;  BORDER_HI_BG    = Color.LIME   ; // FX calls green LIME
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
  }

  void Set_Color_Scheme_3()
  {
    Init_Color_Scheme_3();

    m_gc.setFont( m_font_bold );
    m_text.setFont( m_font_bold );

    Init_Clear();
    m_vis.UpdateViews( false );
    m_vis.CV().PrintCursor();  // Does m_console.Update();
  }
  void Init_Color_Scheme_3()
  {
    NORMAL_FG       = Color.BLACK    ;  NORMAL_BG       = Color.WHITE  ;
    STATUS_FG       = Color.WHITE    ;  STATUS_BG       = Color.BLUE   ;
    BORDER_FG       = Color.WHITE    ;  BORDER_BG       = Color.BLUE   ;
    BORDER_HI_FG    = Color.BLUE     ;  BORDER_HI_BG    = Color.LIME   ; // FX calls green LIME
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
  }

  void Set_Color_Scheme_4()
  {
    Init_Color_Scheme_4();

    m_gc.setFont( m_font_bold );
    m_text.setFont( m_font_bold );

    Init_Clear();
    m_vis.UpdateViews( false );
    m_vis.CV().PrintCursor();  // Does m_console.Update();
  }
  void Init_Color_Scheme_4()
  {
    NORMAL_FG       = Color.BLACK    ;  NORMAL_BG       = Color.WHITE  ;
    STATUS_FG       = Color.WHITE    ;  STATUS_BG       = Color.BLUE   ;
    BORDER_FG       = Color.WHITE    ;  BORDER_BG       = Color.BLUE   ;
    BORDER_HI_FG    = Color.BLUE     ;  BORDER_HI_BG    = Color.LIME   ; // FX calls green LIME
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

  boolean Init_Font_List()
  {
    boolean just_initialized_fonts = false;

    if( null == m_all_fonts )
    {
      m_all_fonts = Font.getFontNames();

      Collections.sort( m_all_fonts );

      just_initialized_fonts = true;;
    }
    return just_initialized_fonts;
  }
  void Next_Font()
  {
    if( !Init_Font_List() )
    {
      m_all_fonts_index = (m_all_fonts_index<m_all_fonts.size()-1)
                        ? m_all_fonts_index+1 : 0;
    }
    String font_name = m_all_fonts.get( m_all_fonts_index );

    Change_Font( m_font_size, font_name );
  }
  void Prev_Font()
  {
    if( !Init_Font_List() )
    {
      m_all_fonts_index = (0 < m_all_fonts_index) ? m_all_fonts_index-1
                                                  : m_all_fonts.size()-1;
    }
    String font_name = m_all_fonts.get( m_all_fonts_index );

    Change_Font( m_font_size, font_name );
  }

  void Set_Font( String font_name )
  {
    Init_Font_List();

    String new_font_name = GetDefaultFontName();

    boolean matched = false;

    String font_name_lc = font_name.toLowerCase();

    for( int k=0; !matched && k<m_all_fonts.size(); k++ )
    {
      if( m_all_fonts.get( k ).toLowerCase().startsWith( font_name_lc ) )
      {
        matched = true;

        new_font_name = m_all_fonts.get( k );

        m_all_fonts_index = k;
      }
    }
    Change_Font( m_font_size, new_font_name );
  }

  public void set_save_2_vis_buf( final boolean save )
  {
    m_save_2_vis_buf = save;
  }
  public boolean get_from_dot_buf()
  {
    return m_get_from_dot_buf_n || m_get_from_dot_buf_l;
  }
  public boolean get_from_dot_buf_n()
  {
    return m_get_from_dot_buf_n;
  }
  public boolean get_from_dot_buf_l()
  {
    return m_get_from_dot_buf_l;
  }
  public void copy_vis_buf_2_dot_buf()
  {
    // setLength( 0 ) followed by append() accomplishes copy:
    m_dot_buf_n.setLength( 0 );
    m_dot_buf_n.append( m_vis_buf );
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
  private Color m_comment_fg = Color.color( 0.3f, 0.3f, 1.0f );
  private Color m_d_blue     = Color.color( 0.0f, 0.0f, 1.0f );
  private Color m_d_green    = Color.color( 0.0f, 1.0f, 0.0f );
  private Color m_d_yellow   = Color.color( 0.9f, 0.9f, 0.0f );
  private Color m_d_magenta  = Color.color( 1.0f, 0.0f, 1.0f );
  private Color m_d_cyan     = Color.color( 0.0f, 0.9f, 0.9f );
  private Color m_d_white    = Color.color( 0.9f, 0.9f, 0.9f );
  private Color m_d_pink     = Color.color( 1.0f, 0.5f, 0.5f );
  private Color m_d_gray     = Color.color( 0.2f, 0.2f, 0.2f );

  private Color NORMAL_FG      ;  private Color NORMAL_BG      ;
  private Color STATUS_FG      ;  private Color STATUS_BG      ;
  private Color BORDER_FG      ;  private Color BORDER_BG      ;
  private Color BORDER_HI_FG   ;  private Color BORDER_HI_BG   ;
  private Color BANNER_FG      ;  private Color BANNER_BG      ;
  private Color STAR_FG        ;  private Color STAR_BG        ;
  private Color COMMENT_FG     ;  private Color COMMENT_BG     ;
  private Color DEFINE_FG      ;  private Color DEFINE_BG      ;
  private Color CONST_FG       ;  private Color CONST_BG       ;
  private Color CONTROL_FG     ;  private Color CONTROL_BG     ;
  private Color VARTYPE_FG     ;  private Color VARTYPE_BG     ;
  private Color VISUAL_FG      ;  private Color VISUAL_BG      ;
  private Color NONASCII_FG    ;  private Color NONASCII_BG    ;
  private Color RV_NORMAL_FG   ;  private Color RV_NORMAL_BG   ;
  private Color RV_STATUS_FG   ;  private Color RV_STATUS_BG   ;
  private Color RV_BORDER_FG   ;  private Color RV_BORDER_BG   ;
  private Color RV_BORDER_HI_FG;  private Color RV_BORDER_HI_BG;
  private Color RV_BANNER_FG   ;  private Color RV_BANNER_BG   ;
  private Color RV_STAR_FG     ;  private Color RV_STAR_BG     ;
  private Color RV_COMMENT_FG  ;  private Color RV_COMMENT_BG  ;
  private Color RV_DEFINE_FG   ;  private Color RV_DEFINE_BG   ;
  private Color RV_CONST_FG    ;  private Color RV_CONST_BG    ;
  private Color RV_CONTROL_FG  ;  private Color RV_CONTROL_BG  ;
  private Color RV_VARTYPE_FG  ;  private Color RV_VARTYPE_BG  ;
  private Color RV_VISUAL_FG   ;  private Color RV_VISUAL_BG   ;
  private Color RV_NONASCII_FG ;  private Color RV_NONASCII_BG ;
  private Color EMPTY_FG       ;  private Color EMPTY_BG       ;
  private Color EOF_FG         ;  private Color EOF_BG         ;
  private Color DIFF_DEL_FG    ;  private Color DIFF_DEL_BG    ;
  private Color DIFF_NORMAL_FG ;  private Color DIFF_NORMAL_BG ;
  private Color DIFF_STAR_FG   ;  private Color DIFF_STAR_BG   ;
  private Color DIFF_COMMENT_FG;  private Color DIFF_COMMENT_BG;
  private Color DIFF_DEFINE_FG ;  private Color DIFF_DEFINE_BG ;
  private Color DIFF_CONST_FG  ;  private Color DIFF_CONST_BG  ;
  private Color DIFF_CONTROL_FG;  private Color DIFF_CONTROL_BG;
  private Color DIFF_VARTYPE_FG;  private Color DIFF_VARTYPE_BG;
  private Color DIFF_VISUAL_FG ;  private Color DIFF_VISUAL_BG ;
  private Color CURSOR_FG      ;  private Color CURSOR_BG      ;
                                  private Color CURSOR_EMPTY_BG;
  VisFx            m_vis;
  GraphicsContext  m_gc;
  Font             m_font_plain;
  Font             m_font_bold;
  String           m_font_name;
  boolean          m_font_changed;
  Text             m_text = new Text();
  int              m_font_size = FONT_SIZE;
  int              m_text_W;
  int              m_text_H;
  double           m_avg_W;
  double           m_cnt_W;
  Integer[]        m_x_p_o;    // X point offsets within cell
  int              m_num_rows; // Current num rows
  int              m_num_cols; // Current num rows
  int              m_siz_rows; // Allocated num rows
  int              m_siz_cols; // Allocated num rows
  char[][]         m_chars__w; // char written
  Style[][]        m_styles_w; // style written

  boolean          m_save_2_dot_buf_n; // Normal view
  boolean          m_save_2_dot_buf_l; // Line view
  boolean          m_save_2_vis_buf;
  boolean          m_save_2_map_buf;
  boolean          m_get_from_dot_buf_n; // Normal view
  boolean          m_get_from_dot_buf_l; // Line view
  boolean          m_get_from_map_buf;
  String[]         m_text_chars;
  char             m_C;
  boolean          m_caps_on;
  Queue<Character> m_input   = new ArrayDeque<Character>();

  StringBuilder    m_dot_buf_n = new StringBuilder(); // Dot buf for normal view
  StringBuilder    m_dot_buf_l = new StringBuilder(); // Dot buf for line view
  StringBuilder    m_vis_buf = new StringBuilder();
  StringBuilder    m_map_buf = new StringBuilder();
  private int      m_dot_buf_index;
  private int      m_map_buf_index;
  private int      m_crs_row;
  private int      m_crs_col;
  private Style    m_crs_pos_style_saved;
  StringBuilder    m_sb = new StringBuilder();
  Clipboard        m_cb;
  ClipboardContent m_cbc;

  List<String> m_all_fonts;
  int          m_all_fonts_index;
}

