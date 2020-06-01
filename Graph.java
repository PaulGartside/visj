//
// Get_Data()
//   |-m_Title
//   |
//   |-Get_Titles()
//   |   |-m_Title
//   |   |-m_X_Title
//   |   |-m_Y_Title
//   |   |-m_L1_Title
//   |   |-m_L2_Title
//   |   |-m_L3_Title
//   |   \-m_L4_Title
//   |
//   \-Get_X_Y_Values()
//       |-m_pX
//       |-m_pY1
//       |-m_pY2
//       |-m_pY3
//       \-m_pY4
//
// Det_MinMax_Data()
//   |-m_x_min_d-m_x_min_r
//   |-m_x_max_d-m_x_max_r
//   |-m_y_min_d-m_y_min_r
//   \-m_y_max_d-m_y_max_r
//
// Det_Scale()
//   |-m_tick_x
//   \-m_tick_y
//
// Draw()
//   |-m_gc
//   |-m_view
//   |-m_font_name
//   |
//   |-Draw_Clear()
//   |   |-m_x
//   |   |-m_y
//   |   |-m_win_width
//   |   \-m_win_height
//   |
//   |-Det_Scale()
//   |   |-m_tick_x
//   |   \-m_tick_y
//   |
//   \-Draw_All()
//       |
//       |-Draw_The_Title()
//       |   \-m_y_box_st
//       |
//       |-Draw_Line_Legends()
//       |   |-m_line_legend_h
//       |   \-Draw_A_Legend
//       |       \-m_line_legend_h
//       |
//       |-Draw_Box_Graticules()
//       |   |-m_x_legend_h
//       |   |-Draw_the_horizontal_graticles()
//       |   |   |-Find_Max_Y_Number_Width()
//       |   |   |-Find_Y_Title_Width()
//       |   |   |-m_y_legend_w
//       |   |   |-Data_2_Line()
//       |   |   \-Draw_Y_Title()
//       |   |
//       |   |-Draw_the_vertical_graticles()
//       |   |   \-Data_2_Line()
//       |   |
//       |   \-Draw_the_enclosing_box()
//       |
//       |-Draw_The_Points()
//       \-Draw_The_Lines()
//           \-Data_2_Line()
//               |-Data_2_Pnt_X()
//               \-Data_2_Pnt_Y()
//
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javafx.geometry.Bounds;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

class Graph
{
  Graph( FileBuf fb )
  {
    m_fb = fb;

    Get_Data();
    Det_MinMax_Data();
    Det_Scale();
  }
  boolean isError()
  {
    return ! m_okay;
  }
  void Get_Data()
  {
    m_Title = m_fb.m_fname;

    for( int k=0; k<m_fb.NumLines(); k++ )
    {
      Line fb_l_k = m_fb.GetLine( k );

      int line_num = k+1;
      String line = fb_l_k.toString();

      Matcher m = m_pat_comment.matcher( line );

      if( m.matches() ) // Whole comment line
      {
        Get_Titles( line );
      }
      else {
        line = Trim_Comments_WSpace( line );

        m_okay = Get_X_Y_Values( line, line_num );
      }
    }
  }
  void Get_Titles( String line )
  {
    Matcher tt = m_pat_title .matcher( line );
    Matcher tx = m_pat_xtitle.matcher( line );
    Matcher ty = m_pat_ytitle.matcher( line );
    Matcher l1 = m_pat_line1 .matcher( line );
    Matcher l2 = m_pat_line2 .matcher( line );
    Matcher l3 = m_pat_line3 .matcher( line );
    Matcher l4 = m_pat_line4 .matcher( line );

    if     ( tt.matches() ) m_Title    = tt.group(1);
    else if( tx.matches() ) m_X_Title  = tx.group(1);
    else if( ty.matches() ) m_Y_Title  = ty.group(1);
    else if( l1.matches() ) m_L1_Title = l1.group(1);
    else if( l2.matches() ) m_L2_Title = l2.group(1);
    else if( l3.matches() ) m_L3_Title = l3.group(1);
    else if( l4.matches() ) m_L4_Title = l4.group(1);
  }
  String Trim_Comments_WSpace( String line )
  {
    final int comment_index = line.indexOf('#');

    if( comment_index != -1 )
    {
      line = line.substring( 0, comment_index );
    }
    line = line.trim();

    return line;
  }
  boolean Get_X_Y_Values( String line, int line_num )
  {
    boolean ok = true;
    String[] tokens = line.split("(\\s|,)+");
    int non_zero_toks = 0;
    for( int k=0; k<tokens.length; k++ )
    {
      if( 0 < tokens[k].length() ) non_zero_toks++;
    }
    if     ( non_zero_toks == 0 ) ; // Empty line
    else if( 5 < non_zero_toks )
    {
      m_msg = "'"+ m_fb.m_fname +"' has too many parameters on line: "+ line_num;
      ok = false;
    }
    else if( non_zero_toks == 1 ) // Single y-value, x-value implicit
    {
      double x  = m_pX.size();                 m_pX .add( x  );
      double y1 = Double.valueOf( tokens[0] ); m_pY1.add( y1 );
    }
    else {
      double x  = Double.valueOf( tokens[0] ); m_pX .add( x  );
      double y1 = Double.valueOf( tokens[1] ); m_pY1.add( y1 );
      if( 2 < non_zero_toks )
      {
        double y2 = Double.valueOf( tokens[2] ); m_pY2.add( y2 );
        if( 3 < non_zero_toks )
        {
          double y3 = Double.valueOf( tokens[3] ); m_pY3.add( y3 );
          if( 4 < non_zero_toks )
          {
            double y4 = Double.valueOf( tokens[4] ); m_pY4.add( y4 );
          }
        }
      }
    }
    return ok;
  }
  void Det_MinMax_Data()
  {
    m_x_min_d = m_pX .get( 0 );
    m_x_max_d = m_pX .get( 0 );
    m_y_min_d = m_pY1.get( 0 );
    m_y_max_d = m_pY1.get( 0 );

    for( int k=0; k<m_pX.size(); k+=1 )
    {
      m_x_min_d = Math.min( m_x_min_d, m_pX .get( k ) );
      m_x_max_d = Math.max( m_x_max_d, m_pX .get( k ) );
      m_y_min_d = Math.min( m_y_min_d, m_pY1.get( k ) );
      m_y_max_d = Math.max( m_y_max_d, m_pY1.get( k ) );

      if( 0<m_pY2.size() )
      {
        m_y_min_d = Math.min( m_y_min_d, m_pY2.get( k ) );
        m_y_max_d = Math.max( m_y_max_d, m_pY2.get( k ) );

        if( 0<m_pY3.size() )
        {
          m_y_min_d = Math.min( m_y_min_d, m_pY3.get( k ) );
          m_y_max_d = Math.max( m_y_max_d, m_pY3.get( k ) );

          if( 0<m_pY4.size() )
          {
            m_y_min_d = Math.min( m_y_min_d, m_pY4.get( k ) );
            m_y_max_d = Math.max( m_y_max_d, m_pY4.get( k ) );
          }
        }
      }
    }
    m_x_min_r = m_x_min_d;
    m_x_max_r = m_x_max_d;
    m_y_min_r = m_y_min_d;
    m_y_max_r = m_y_max_d;
  }
  void Det_Scale()
  {
    Ptr_Double p_sz_x = new Ptr_Double( range_x() );
    Ptr_Double p_sz_y = new Ptr_Double( range_y() );

    int pow_x = Det_Power( p_sz_x );
    int pow_y = Det_Power( p_sz_y );

    if     (                    p_sz_x.val < 2 ) m_tick_x =  2*Math.pow( 10, pow_x-1 );
    else if( 2 <= p_sz_x.val && p_sz_x.val < 5 ) m_tick_x =  5*Math.pow( 10, pow_x-1 );
    else if( 5 <= p_sz_x.val && p_sz_x.val <=10) m_tick_x = 10*Math.pow( 10, pow_x-1 );
    else Utils.Assert( false, "Det_Scale X Error, p_sz_x = "+ p_sz_x.val );

    if     (                    p_sz_y.val < 2 ) m_tick_y =  2*Math.pow( 10, pow_y-1 );
    else if( 2 <= p_sz_y.val && p_sz_y.val < 5 ) m_tick_y =  5*Math.pow( 10, pow_y-1 );
    else if( 5 <= p_sz_y.val && p_sz_y.val <=10) m_tick_y = 10*Math.pow( 10, pow_y-1 );
    else Utils.Assert( false, "Det_Scale Y Error, p_sz_y = "+ p_sz_y.val );
  }
  int Det_Power( Ptr_Double axis_range )
  {
    int power = 0;

    if( 0<axis_range.val )
    {
      while( 10 < axis_range.val ) { power += 1; axis_range.val *= 0.1; }
      while( axis_range.val <  1 ) { power -= 1; axis_range.val *= 10 ; }
    }
    return power;
  }

  double range_x()
  {
    Utils.Assert( m_x_min_r <= m_x_max_r, "m_x_max_r < m_x_min_r");

    if( m_x_max_r == m_x_min_r ) { m_x_max_r += 1; m_x_min_r -= 1; }
    return m_x_max_r - m_x_min_r;
  }
  double range_y()
  {
    Utils.Assert( m_y_min_r <= m_y_max_r, "m_y_max_r < m_y_min_r");

    if( m_y_max_r == m_y_min_r ) { m_y_max_r += 1; m_y_min_r -= 1; }
    return m_y_max_r - m_y_min_r;
  }
  void Clear()
  {
       m_Title = "No Title";
     m_X_Title = "";
     m_Y_Title = "";
    m_L1_Title = "";
    m_L2_Title = "";
    m_L3_Title = "";
    m_L4_Title = "";

    m_pX .clear();
    m_pY1.clear();
    m_pY2.clear();
    m_pY3.clear();
    m_pY4.clear();

    m_x_min_d = 0;
    m_x_max_d = 0;
    m_y_min_d = 0;
    m_y_max_d = 0;

    m_x_min_r = 0;
    m_x_max_r = 0;
    m_y_min_r = 0;
    m_y_max_r = 0;

    m_tick_x = 0;
    m_tick_y = 0;

    m_x = 0;
    m_y = 0;
    m_win_width  = 0;
    m_win_height = 0;

    m_line_legend_h = 0;
       m_x_legend_h = 0;
       m_y_legend_w = 0;
  }
  void Draw( GraphicsContext gc
           , View V
           , String font_name
           , final int console_text_W
           , final int console_text_H )
  {
    m_gc = gc;
    m_view = V;
    m_font_name = font_name;

    Draw_Clear( console_text_W, console_text_H );
    Det_Scale();
    Draw_All();
  }
  void Draw_Clear( final int console_text_W
                 , final int console_text_H )
  {
    final int row = m_view.Y()+1;
    final int col = m_view.X()+1;

    m_x = col*console_text_W;
    m_y = row*console_text_H;

    m_win_width  = m_view.WorkingCols()*console_text_W;
    m_win_height = m_view.WorkingRows()*console_text_H;

    // Fill in background with gray:
    m_gc.setFill( Color.BLACK );
    m_gc.fillRect( m_x, m_y, m_win_width, m_win_height );
  }
  // Line graph
  void Draw_All()
  {
    // Draw the box
    Draw_The_Title     ();
    Draw_Line_Legends  ();
    Draw_Box_Graticules();

    if( m_view.m_graph_bar ) Draw_All_Bar();
    else                     Draw_All_Line();
  }
  // Bar graph
  void Draw_All_Bar()
  {
    // Draw the box
    Draw_The_Title     ();
    Draw_Line_Legends  ();
    Draw_Box_Graticules();

    Draw_The_Bars( m_pY1 );
  }
  // Line graph
  void Draw_All_Line()
  {
    m_gc.setStroke( Color.LIME );
    Draw_The_Points( m_pY1 );
    Draw_The_Lines ( m_pY1 );

    if( 0<m_pY2.size() )
    {
      m_gc.setStroke( Color.CYAN );
      Draw_The_Points( m_pY2 );
      Draw_The_Lines ( m_pY2 );

      if( 0<m_pY3.size() )
      {
        m_gc.setStroke( Color.RED );
        Draw_The_Points( m_pY3 );
        Draw_The_Lines ( m_pY3 );

        if( 0<m_pY4.size() )
        {
          m_gc.setStroke( Color.GRAY );
          Draw_The_Points( m_pY4 );
          Draw_The_Lines ( m_pY4 );
        }
      }
    }
  }
  double Get_FontStr_Width( String S )
  {
    return Get_FontStr_Width( m_gc.getFont(), S );
  }
  double Get_FontStr_Width( Font font, String S )
  {
    Text text = new Text();
    text.setFont( font );
    text.setText( S );

    Bounds bounds = text.getLayoutBounds();

    return bounds.getWidth();
  }
  double Get_FontStr_Height( String S )
  {
    return Get_FontStr_Height( m_gc.getFont(), S );
  }
  double Get_FontStr_Height( Font font, String S )
  {
    Text text = new Text();
    text.setFont( font );
    text.setText( S );

    Bounds bounds = text.getLayoutBounds();

    return bounds.getHeight();
  }
  int m_x_box_st() // Left side of graph box
  {
    return m_x + m_y_legend_w;
  }
  int m_x_box_fn() // Right side of graph box
  {
    return m_x + (int)(0.95*m_win_width);
  }
  int m_y_box_fn() // Bottom of graph box
  {
    return m_y + m_win_height - m_x_legend_h - m_line_legend_h;
  }

  void Draw_The_Title()
  {
    int font_size_title = (int)(10 + 0.01*m_win_width
                                   + 0.01*m_win_height
                                   + 0.5);
    Font font_title = Font.font( m_font_name
                               , FontWeight.NORMAL
                               , font_size_title );
    m_gc.setFont( font_title );

    double t_w = Get_FontStr_Width( font_title, m_Title );
    double t_h = Get_FontStr_Height( font_title, m_Title );

    double xtp = m_x + 0.5*( m_win_width - t_w );
    double ytp = m_y + 0.01*m_win_height;

    m_gc.setFill( Color.WHITE );
    m_gc.fillText( m_Title, xtp, ytp );

    m_y_box_st = (int)(ytp + t_h + 0.5);
  }
  void Draw_Line_Legends()
  {
    double font_size = 8 + 0.002*m_win_width
                         + 0.002*m_win_height;

    Font font_legend = Font.font( m_font_name, FontWeight.NORMAL, (int)font_size );
    Text text = new Text();
    text.setFont( font_legend );
    text.setText( "ABC" );
    Bounds b = text.getLayoutBounds();

    m_line_legend_h = (int)(b.getHeight() + 0.5);

    if( 0<m_pY1.size() )
    {
      if( 0<m_pY2.size() )
      {
        if( 0<m_pY3.size() )
        {
          if( 0<m_pY4.size() )
          {
            if( 0<m_L4_Title.length() ) Draw_A_Legend( m_L4_Title, Color.GRAY );
          }
          if( 0<m_L3_Title.length() ) Draw_A_Legend( m_L3_Title, Color.RED );
        }
        if( 0<m_L2_Title.length() ) Draw_A_Legend( m_L2_Title, Color.CYAN );
      }
      if( 0<m_L1_Title.length() ) Draw_A_Legend( m_L1_Title, Color.LIME );
    }
  }
  void Draw_A_Legend( String text, Color C )
  {
  //double t_h = m_win.GetTextHeight( text );
    double t_h = m_line_legend_h;

    double xtp = m_x + 0.4*m_win_width;
    double ytp = m_y +     m_win_height - m_line_legend_h + 0.5*t_h;

    m_gc.setFill( C );
    m_gc.fillText( text, xtp, ytp );

    final int ix      = (int)(xtp+0.5) - 10;
    final int iy      = (int)(ytp - t_h/2 + 0.5);
    final int x_start = (int)(0.1*m_win_width + 0.5);

    for( int x=ix; x>x_start; x-=20 )
    {
      m_gc.fillRect( x-2, iy-2, 5, 5 );

      if( x != ix ) m_gc.strokeLine( x, iy, x+21, iy );
    }
    m_line_legend_h += t_h;
  }
  void Draw_Box_Graticules()
  {
    int font_size_grats = (int)(5 + 0.01*m_win_width
                                  + 0.01*m_win_height
                                  + 0.5);
    Font font_grats = Font.font( m_font_name
                               , FontWeight.NORMAL
                               , font_size_grats );
    m_gc.setFont( font_grats );

    double t_h = Get_FontStr_Height( font_grats, "0123456789" );

    m_x_legend_h = (int)( t_h + 0.5 );

    if( 0<m_X_Title.length() ) m_x_legend_h *= 2;

    m_gc.setFill( Color.WHITE );
    m_gc.setStroke( Color.WHITE );

    Draw_the_horizontal_graticles();
    Draw_the_vertical_graticles();
    Draw_the_enclosing_box();
  }
  void Draw_the_horizontal_graticles()
  {
    // Start the graticle, specified by y, on a multiple of m_tick_y
    final int iy = (int)(m_y_min_r/m_tick_y);
    double y = iy*m_tick_y;
    if( y < m_y_min_r ) y += m_tick_y;

    final double max_Y_num_w = Find_Max_Y_Number_Width( y );
    final double y_title_w   = Find_Y_Title_Width();

    // m_y_legend_w must be set before calling Data_2_Line
    //   to draw the horizontal graticles
    final double space_factor = 1.2;
    m_y_legend_w = (int)(space_factor*( max_Y_num_w + y_title_w )+0.5);

    // xtp_e = X-text-position-end:
    double xtp_e = m_x + space_factor*y_title_w + max_Y_num_w;

    // Draw the numbers:
    for( ; y<=m_y_max_r; y+=m_tick_y )
    {
      Data_2_Line( m_x_min_r, y, m_x_max_r, y );

      Draw_horizontal_graticle_number( y, xtp_e );
    }
    Draw_horizontal_graticle_number( m_y_min_r, xtp_e );
    Draw_horizontal_graticle_number( m_y_max_r, xtp_e );

    Draw_Y_Title();
  }
  void Draw_horizontal_graticle_number( double y, final double xtp_e )
  {
    final double space_factor = 1.2;

    // If y is some tiny value like 0.00123E-10, set it to zero
    //   so that y.str does not distort max_Y_min_w
    if( Math.abs(y) < 0.01*range_y() ) y = 0;

    String y_str = Double_2_String( y );
    double t_w = Get_FontStr_Width( y_str );
    double t_h = Get_FontStr_Height( y_str );

    // xtp_e = X-text-position-end:
    double xtp = xtp_e - t_w;
    double ytp = Data_2_Pnt_Y( y ) - 0.5*t_h;

    m_gc.fillText( y_str, xtp, ytp );
  }
  void Draw_the_vertical_graticles()
  {
    // Start the graticle, specified by x, on a multiple of m_tick_x
    int   ix = (int)(m_x_min_r/m_tick_x);
    double x = ix*m_tick_x;
    if( x < m_x_min_r ) x += m_tick_x;

    // Only draw the m_x_min_r graticle number if it is more than
    // half m_tick_x less than the next graticle number:
    if( 0.4*m_tick_x < (x - m_x_min_r) )
    {
      Draw_vertical_graticle_number( m_x_min_r );
    }
    for( ; x<=m_x_max_r; x+=m_tick_x )
    {
      Data_2_Line( x, m_y_min_r, x, m_y_max_r );

      Draw_vertical_graticle_number( x );
    }
    x-=m_tick_x;
    // Only draw the m_x_max_r graticle number if it is more than
    // half m_tick_x greater than the previous graticle number:
    if( 0.4*m_tick_x < (m_x_max_r - x) )
    {
      Draw_vertical_graticle_number( m_x_max_r );
    }
    Draw_X_Title();
  }
  void Draw_vertical_graticle_number( double x )
  {
    // If x is some tiny value like 0.00123E-10, set it to zero
    if( Math.abs(x) < 0.01*range_x() ) x = 0;

    String x_str = Double_2_String( x );
    if( 2<x_str.length() && x_str.endsWith(".0") )
    {
      x_str = x_str.substring( 0, x_str.length()-2 );
    }
    double t_w = Get_FontStr_Width( x_str );
    double t_h = Get_FontStr_Height( x_str );

    double xtp = Data_2_Pnt_X( x ) - 0.5*t_w;
    double ytp = m_y_box_fn() + 0.2*t_h;

    m_gc.fillText( x_str, xtp, ytp );
  }
  void Draw_X_Title()
  {
    if( 0<m_X_Title.length() )
    {
      double t_w = Get_FontStr_Width( m_X_Title );
      double t_h = Get_FontStr_Height( m_X_Title );

      double xtp = m_x_box_st() + 0.5*( m_x_box_fn() - m_x_box_st() - t_w );
      double ytp = m_y_box_fn() + 2*t_h;

      m_gc.fillText( m_X_Title, xtp, ytp );
    }
  }
  void Draw_the_enclosing_box()
  {
    int w = m_win_width;
    int h = m_win_height;

    int x1 = m_x_box_st();
    int y1 = m_y_box_st;
    int x2 = m_x_box_fn();
    int y2 = m_y_box_fn();

    m_gc.strokeLine( x1, y1, x1, y2 );
    m_gc.strokeLine( x1, y1, x2, y1 );
    m_gc.strokeLine( x1, y2, x2, y2 );
    m_gc.strokeLine( x2, y1, x2, y2 );
  }
  double Find_Max_Y_Number_Width( double y )
  {
    double max_Y_num_w = 0; // Max y number width
    for( ; y<=m_y_max_r; y+=m_tick_y )
    {
      // If y is some tiny value like 0.00123E-10, set it to zero
      //   so that y.str does not distort max_Y_min_w
      if( Math.abs(y) < 0.01*range_y() ) y = 0;

      double t_w = Get_FontStr_Width( Double_2_String( y ) );
      max_Y_num_w = Math.max( t_w, max_Y_num_w );
    }
    return max_Y_num_w;
  }
  double Find_Y_Title_Width()
  {
    double y_title_w = 0;
    if( 0<m_Y_Title.length() )
    {
      y_title_w = Get_FontStr_Height( String.valueOf( m_Y_Title ) );
    }
    // Give the Y numbers some padding in case there is no m_Y_Title
    y_title_w = Math.max( y_title_w, 0.02*m_win_width );
    return y_title_w;
  }
  void Draw_Y_Title()
  {
    if( 0<m_Y_Title.length() )
    {
      double t_w = Get_FontStr_Width( m_Y_Title );
      double t_h = Get_FontStr_Height( m_Y_Title );
      double t_a = t_h;

      double xtp = t_a/2;
      double ytp = m_y_box_st + 0.5*( m_y_box_fn() - m_y_box_st - t_w );

      m_gc.rotate( 90 );
      m_gc.fillText( m_Y_Title, ytp, -xtp );
      m_gc.rotate( -90 );
    }
  }
//int Data_2_Pnt_X( double dx )
//{
//  double x_box_width = m_x_box_fn() - m_x_box_st();
//
//  double p_x = m_x_box_st()
//             + x_box_width*( (dx-m_x_min_r)/range_x() );
//
//  return (int)(p_x+0.5);
//}
//int Data_2_Pnt_Y( double dy )
//{
//  double y_box_height = m_y_box_fn() - m_y_box_st;
//
//  double p_y = m_y_box_st
//             + y_box_height*( (m_y_max_r-dy)/range_y() );
//
//  return (int)(p_y+0.5);
//}
  double Data_2_Pnt_X( double dx )
  {
    double x_box_width = m_x_box_fn() - m_x_box_st();

    double p_x = m_x_box_st()
               + x_box_width*( (dx-m_x_min_r)/range_x() );
    return p_x;
  }
  double Data_2_Pnt_Y( double dy )
  {
    double y_box_height = m_y_box_fn() - m_y_box_st;

    double p_y = m_y_box_st
               + y_box_height*( (m_y_max_r-dy)/range_y() );
    return p_y;
  }
//void Data_2_Line( double x1
//                , double y1
//                , double x2
//                , double y2 )
//{
//  int ix1 = Data_2_Pnt_X( x1 );
//  int ix2 = Data_2_Pnt_X( x2 );
//  int iy1 = Data_2_Pnt_Y( y1 );
//  int iy2 = Data_2_Pnt_Y( y2 );
//
//  m_gc.strokeLine( ix1, iy1, ix2, iy2 );
//}
  void Data_2_Line( double x1
                  , double y1
                  , double x2
                  , double y2 )
  {
    // Graph coordinates to window coordinates:
    double sx1 = Data_2_Pnt_X( x1 );
    double sx2 = Data_2_Pnt_X( x2 );
    double sy1 = Data_2_Pnt_Y( y1 );
    double sy2 = Data_2_Pnt_Y( y2 );

    m_gc.strokeLine( sx1, sy1, sx2, sy2 );
  }
//String Double_2_String( final double X )
//{
//  String S = String.valueOf( X );
//
////if( -1 < X && X < 1 )
//  {
//    final int LEN_1 = X < 0 ? 6 : 5;
//    final int LEN_2 = X < 0 ? 4 : 3;
//
//    // Change values like  0.010000000000002 to  0.01
//    // Change values like -0.010000000000002 to -0.01
//    if( LEN_1 < S.length() )
//    {
//      if( S.endsWith("001")
//       || S.endsWith("002")
//       || S.endsWith("003")
//       || S.endsWith("004")
//       || S.endsWith("005")
//       || S.endsWith("006")
//       || S.endsWith("007")
//       || S.endsWith("008")
//       || S.endsWith("009") )
//      {
//        S = S.substring( 0, S.length()-3 );
//
//        while( LEN_2 < S.length() && S.endsWith("0") )
//        {
//          S = S.substring( 0, S.length()-1 );
//        }
//      }
//    }
//  }
//  return S;
//}

//String Double_2_String( double X )
//{
//  final double scale_num   =    10;
//  final double lower_bound =  1000;
//  final double upper_bound = 10000;
//
//  if( 0 < X )
//  {
//    // Scale X to between lower_bound and upper_bound, round, and then scale back:
//    if( X < lower_bound )
//    {
//      // Scale up:
//      int num = 0;
//      for( ; X < lower_bound; num++ ) X *= scale_num;
//      // Round:
//      X = (int)(X + 0.5);
//      // Scale down:
//      for( ; 0 < num; num-- ) X /= scale_num;
//    }
//    else if( upper_bound < X )
//    {
//      // Scale down:
//      int num = 0;
//      for( ; upper_bound < X; num++ ) X /= scale_num;
//      // Round:
//      X = (int)(X + 0.5);
//      // Scale up:
//      for( ; 0 < num; num-- ) X *= scale_num;
//    }
//  }
//  else if( X < 0 )
//  {
//    // Scale X to between -lower_bound and -upper_bound, round, and then scale back:
//    if( X < -upper_bound )
//    {
//      // Scale down:
//      int num = 0;
//      for( ; X < -upper_bound; num++ ) X /= scale_num;
//      // Round:
//      X = (int)(X - 0.5);
//      // Scale up:
//      for( ; 0 < num; num-- ) X *= scale_num;
//    }
//    else if( -lower_bound < X )
//    {
//      // Scale up:
//      int num = 0;
//      for( ; -lower_bound < X; num++ ) X *= scale_num;
//      // Round:
//      X = (int)(X - 0.5);
//      // Scale down:
//      for( ; 0 < num; num-- ) X /= scale_num;
//    }
//  }
//  String S = String.valueOf( X );
//
//  return S;
//}

  String Double_2_String( double X )
  {
    String S = String.valueOf( X );

    if( S.indexOf('.') != -1
     && S.indexOf('e') == -1
     && S.indexOf('E') == -1 )
    {
      // S has decimal point but no exponent.
      // Trim long fraction:
      while( 5 < S.length() )
      {
        S = S.substring( 0, S.length()-1 );
      }
    }
    return S;
  }

  void Draw_The_Points( ArrayList<Double> pY )
  {
    for( int k=0; k<m_pX.size(); k++ )
    {
      double dx = m_pX.get( k );
      double dy =   pY.get( k );

      if( m_x_min_r <= dx && dx <= m_x_max_r
       && m_y_min_r <= dy && dy <= m_y_max_r )
      {
        // Graph coordinates to window coordinates:
        double sx = Data_2_Pnt_X( dx );
        double sy = Data_2_Pnt_Y( dy );

        m_gc.strokeRect( sx-2, sy-2, 5, 5 );
      }
    }
  }
  void Draw_The_Lines( ArrayList<Double> pY )
  {
    for( int k=1; k<m_pX.size(); k++ )
    {
      double x1 = m_pX.get( k-1 );
      double y1 =   pY.get( k-1 );
      double x2 = m_pX.get( k   );
      double y2 =   pY.get( k   );

      boolean p1_in_bounds = (m_x_min_r <= x1 && x1 <= m_x_max_r)
                          && (m_y_min_r <= y1 && y1 <= m_y_max_r);
      boolean p2_in_bounds = (m_x_min_r <= x2 && x2 <= m_x_max_r)
                          && (m_y_min_r <= y2 && y2 <= m_y_max_r);

      if( p1_in_bounds || p2_in_bounds )
      {
        Data_2_Line( x1, y1, x2, y2 );
      }
    }
  }
////Utils.Log("xl="+xl+", yt="+yt+", w="+w+", h="+h);
//void Draw_The_Bars( ArrayList<Double> pY )
//{
//  final int X_size    = m_pX.size();
//  final int X_size_m1 = X_size-1;
//
//  for( int k=0; k<X_size; k++ )
//  {
//    double y = pY.get( k );
//
//    if( 0 != y )
//    {
//      // Graph coordinates:
//      double x_k = m_pX.get( k );
//
//      if( (m_x_min_r <= x_k && x_k <= m_x_max_r) )
//      {
//        double xm1 = (0<k        ) ? m_pX.get( k-1 ) : x_k;
//        double xp1 = (k<X_size_m1) ? m_pX.get( k+1 ) : x_k;
//
//        double x_st = 0.45*xm1 + 0.55*x_k;
//        double x_fn = 0.45*xp1 + 0.55*x_k;
//
//        double yt = (0<y) ? Data_2_Pnt_Y( y ) : Data_2_Pnt_Y( 0 );
//        double yb = (0<y) ? Data_2_Pnt_Y( 0 ) : Data_2_Pnt_Y( y );
//
//        // Window coorinates of bar:
//        double xl = Data_2_Pnt_X( x_st );
//        double xr = Data_2_Pnt_X( x_fn );
//
//        // Window width and height of bar:
//        double w = xr - xl;
//        double h = yb - yt;
//
//        m_gc.setFill( (0<y) ? Color.LIME : Color.RED );
//
//        // (x,y) of top left corner, width, height
//        m_gc.fillRect( xl, yt, w, h );
//      }
//    }
//  }
//}
  //Utils.Log("xl="+xl+", yt="+yt+", w="+w+", h="+h);
  void Draw_The_Bars( ArrayList<Double> pY )
  {
    final int X_size    = m_pX.size();
    final int X_size_m1 = X_size-1;

    for( int k=0; k<X_size; k++ )
    {
      // Graph coordinates:
      double y   =   pY.get( k );
      double x_k = m_pX.get( k );
      boolean x_k_in_bounds = (m_x_min_r <= x_k && x_k <= m_x_max_r);

      if( 0 != y && x_k_in_bounds )
      {
        double xm1 = (0<k        ) ? m_pX.get( k-1 ) : x_k;
        double xp1 = (k<X_size_m1) ? m_pX.get( k+1 ) : x_k;

        double x_st = 0.45*xm1 + 0.55*x_k;
        double x_fn = 0.45*xp1 + 0.55*x_k;

        double yt = (0<y) ? Data_2_Pnt_Y( y ) : Data_2_Pnt_Y( 0 );
        double yb = (0<y) ? Data_2_Pnt_Y( 0 ) : Data_2_Pnt_Y( y );

        // Window coorinates of bar:
        double xl = Data_2_Pnt_X( x_st );
        double xr = Data_2_Pnt_X( x_fn );

        // Window width and height of bar:
        double w = xr - xl;
        double h = yb - yt;

        m_gc.setFill( (0<y) ? Color.LIME : Color.RED );

        // (x,y) of top left corner, width, height
        m_gc.fillRect( xl, yt, w, h );
      }
    }
  }

  void GoLeft()
  {
    final double r_x = range_x();

    if( m_x_min_r <= m_x_min_d )
    {
      m_x_min_r = m_x_min_d;
      m_x_max_r -= 0.1*r_x;
    }
    else
    {
      if( (0.1*r_x) < (m_x_min_r - m_x_min_d) )
      {
        m_x_min_r -= 0.1*r_x;
        m_x_max_r -= 0.1*r_x;
      }
      else {
        m_x_min_r = m_x_min_d;
        m_x_max_r = m_x_min_d + r_x;
      }
    }
  }

  void GoRight()
  {
    final double r_x = range_x();

    if( m_x_max_d <= m_x_max_r )
    {
      m_x_max_r = m_x_max_d;
      m_x_min_r += 0.1*r_x;
    }
    else
    {
      if( (0.1*r_x) < (m_x_max_d - m_x_max_r) )
      {
        m_x_min_r += 0.1*r_x;
        m_x_max_r += 0.1*r_x;
      }
      else {
        m_x_max_r = m_x_max_d;
        m_x_min_r = m_x_max_d - r_x;
      }
    }
  }

  void GoDown()
  {
    final double r_y = range_y();

    if( m_y_min_r <= m_y_min_d )
    {
      m_y_min_r = m_y_min_d;
      m_y_max_r -= 0.1*r_y;
    }
    else
    {
      if( (0.1*r_y) < (m_y_min_r - m_y_min_d) )
      {
        m_y_min_r -= 0.1*r_y;
        m_y_max_r -= 0.1*r_y;
      }
      else {
        m_y_min_r = m_y_min_d;
        m_y_max_r = m_y_min_d + r_y;
      }
    }
  }
  void GoUp()
  {
    final double r_y = range_y();

    if( m_y_max_d <= m_y_max_r )
    {
      m_y_max_r = m_y_max_d;
      m_y_min_r += 0.1*r_y;
    }
    else
    {
      if( (0.1*r_y) < (m_y_max_d - m_y_max_r) )
      {
        m_y_min_r += 0.1*r_y;
        m_y_max_r += 0.1*r_y;
      }
      else {
        m_y_max_r = m_y_max_d;
        m_y_min_r = m_y_max_d - r_y;
      }
    }
  }

  void GoToMin_X()
  {
    // Keep range the same but slide all the way to the left:
    final double x_range = m_x_max_r - m_x_min_r;

    m_x_min_r = m_x_min_d;
    m_x_max_r = m_x_min_r + x_range;
  }
  void GoToMax_X()
  {
    // Keep range the same but slide all the way to the right:
    final double x_range = m_x_max_r - m_x_min_r;

    m_x_max_r = m_x_max_d;
    m_x_min_r = m_x_max_r - x_range;
  }
  void GoToMiddle_X()
  {
    // Keep range the same but slide all middle:
    final double x_half_range = 0.5*(m_x_max_r - m_x_min_r);
    final double x_middle = 0.5*(m_x_max_d - m_x_min_d);

    m_x_max_r = x_middle + x_half_range;
    m_x_min_r = x_middle - x_half_range;
  }
  void GoToMin_Y()
  {
    // Keep range the same but slide all the way to the bottom:
    final double y_range = m_y_max_r - m_y_min_r;

    m_y_min_r = m_y_min_d;
    m_y_max_r = m_y_min_r + y_range;
  }
  void GoToMax_Y()
  {
    // Keep range the same but slide all the way to the right:
    final double y_range = m_y_max_r - m_y_min_r;

    m_y_max_r = m_y_max_d;
    m_y_min_r = m_y_max_r - y_range;
  }
  void ZoomIn()
  {
    final double r_x = range_x();
    final double r_y = range_y();

    m_x_min_r += r_x*0.1;
    m_x_max_r -= r_x*0.1;
    m_y_min_r += r_y*0.1;
    m_y_max_r -= r_y*0.1;

  //m_x_min_r = Math.max( m_x_min_r, m_x_min_d );
  //m_x_max_r = Math.min( m_x_max_r, m_x_max_d );
  //m_y_min_r = Math.max( m_y_min_r, m_y_min_d );
  //m_y_max_r = Math.min( m_y_max_r, m_y_max_d );
  }
  void ZoomOut()
  {
    final double r_x = range_x();
    final double r_y = range_y();

    m_x_min_r -= r_x*0.1;
    m_x_max_r += r_x*0.1;
    m_y_min_r -= r_y*0.1;
    m_y_max_r += r_y*0.1;

    m_x_min_r = Math.max( m_x_min_r, m_x_min_d );
    m_x_max_r = Math.min( m_x_max_r, m_x_max_d );
    m_y_min_r = Math.max( m_y_min_r, m_y_min_d );
    m_y_max_r = Math.min( m_y_max_r, m_y_max_d );
  }
  // Optional white space, #, anything else
  final Pattern m_pat_comment = Pattern.compile("^\\s*#.*$");

  // beg = Optional white space, #, optional white space
  // end = Optional white space, name allowing spaces, optional white space
  final String m_beg = "^\\s*#\\s*";
  final String m_end = "\\s*(\\S+(\\s*\\S+)*)\\s*$";
  final Pattern m_pat_title  = Pattern.compile( m_beg+"Title:"  +m_end );
  final Pattern m_pat_xtitle = Pattern.compile( m_beg+"X-Title:"+m_end );
  final Pattern m_pat_ytitle = Pattern.compile( m_beg+"Y-Title:"+m_end );
  final Pattern m_pat_line1  = Pattern.compile( m_beg+"Line-1:" +m_end );
  final Pattern m_pat_line2  = Pattern.compile( m_beg+"Line-2:" +m_end );
  final Pattern m_pat_line3  = Pattern.compile( m_beg+"Line-3:" +m_end );
  final Pattern m_pat_line4  = Pattern.compile( m_beg+"Line-4:" +m_end );

  FileBuf         m_fb;
  View            m_view;
  GraphicsContext m_gc;
  String          m_font_name;
  boolean         m_okay = true;
  String          m_msg;

  // These parameters are read in from the input file:
  String    m_Title = "No Title";
  String  m_X_Title = "";
  String  m_Y_Title = "";
  String m_L1_Title = "";
  String m_L2_Title = "";
  String m_L3_Title = "";
  String m_L4_Title = "";

  ArrayList<Double> m_pX  = new ArrayList<>();
  ArrayList<Double> m_pY1 = new ArrayList<>();
  ArrayList<Double> m_pY2 = new ArrayList<>();
  ArrayList<Double> m_pY3 = new ArrayList<>();
  ArrayList<Double> m_pY4 = new ArrayList<>();

  double m_x_min_d;   // Min x data value in m_pX
  double m_x_max_d;   // Max x data value in m_pX
  double m_y_min_d;   // Min y data value in m_pY1,2,3,4
  double m_y_max_d;   // Max y data value in m_pY1,2,3,4

  double m_x_min_r;   // Min x range value in m_pX displayed
  double m_x_max_r;   // Max x range value in m_pX displayed
  double m_y_min_r;   // Min y range value in m_pY1,2,3,4 displayed
  double m_y_max_r;   // Max y range value in m_pY1,2,3,4 displayed

  double m_tick_x;    // Difference between X-graticles
  double m_tick_y;    // Difference between Y-graticles

  // These parameters change every time the window is resized:
  int m_x;  // Top left x-pixel of buffer view in parent window
  int m_y;  // Top left y-pixel of buffer view in parent window
  int m_win_width ; // Window pane width in pixels
  int m_win_height; // Window pane height in pixels

  int m_y_box_st; // Top of the graph box

  int m_line_legend_h;
  int    m_x_legend_h; // Graticle legend and X-Title height
  int    m_y_legend_w; // Graticle legend and Y-Title width
}

