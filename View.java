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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

class View
{
  View( VisIF vis, FileBuf fb, ConsoleIF console )
  {
    m_vis      = vis;
    m_fb       = fb;
    m_console  = console;
    m_num_rows = m_console.Num_Rows();
    m_num_cols = m_console.Num_Cols();
  }
  int X()           { return m_x; }
  int Y()           { return m_y; }
  int WorkingRows() { return 5 < m_num_rows ? m_num_rows-5 : 0; }
  int WorkingCols() { return 2 < m_num_cols ? m_num_cols-2 : 0; }
  int CrsLine()     { return m_topLine  + m_crsRow; }
  int CrsChar()     { return m_leftChar + m_crsCol; }
  int RightChar()   { return m_leftChar + WorkingCols()-1; }
  int BotLine()     { return m_topLine  + WorkingRows()-1; }
  int TopLine()     { return m_topLine; }
  int LeftChar()    { return m_leftChar; }
  int CrsRow()      { return m_crsRow; }
  int CrsCol()      { return m_crsCol; }
  int WinCols()     { return m_num_cols; }

  void SetTopLine ( final int val )
  {
    m_topLine = val;
  }
  void SetLeftChar( final int val )
  {
    m_leftChar = val;
  }
  void SetCrsRow( int val )
  {
    final int NUM_LINES = m_fb.NumLines();

    // Do some limit checks to make sure we dont crash on the next Update
    if( 0 < NUM_LINES )
    {
      if( WorkingRows() < val )
      {
        val = WorkingRows()-1;
      }
      if( NUM_LINES < val )
      {
        val = NUM_LINES-1;
      }
      if( NUM_LINES <= m_topLine + val )
      {
        m_topLine = NUM_LINES - val - 1;
      }
      m_crsRow = val;
    }
  }
  void SetCrsCol( final int val )
  {
    m_crsCol = val;
  }

  // Translates zero based working view row to zero based global row
  int Row_Win_2_GL( final int win_row )
  {
    return m_y + 1 + win_row;
  }
  // Translates zero based working view column to zero based global column
  int Col_Win_2_GL( final int win_col )
  {
    return m_x + 1 + win_col;
  }
  int Sts__Line_Row()
  {
    return Row_Win_2_GL( WorkingRows() );
  }
  int File_Line_Row()
  {
    return Row_Win_2_GL( WorkingRows() + 1 );
  }
  int Cmd__Line_Row()
  {
    return Row_Win_2_GL( WorkingRows() + 2 );
  }

  // Translates zero based file line number to zero based global row
  int Line_2_GL( final int file_line )
  {
    return m_y + 1 + file_line - m_topLine;
  }
  // Translates zero based file line char position to zero based global column
  int Char_2_GL( final int line_char )
  {
    return m_x + 1 + line_char - m_leftChar;
  }
//void Set_crsRow( final int row )
//{
//  Clear_Console_CrsCell();
//
//  m_crsRow = row;
//
//  Set_Console_CrsCell();
//}
  void Set_crsCol( final int col )
  {
    m_crsCol = col;

    Set_Console_CrsCell();
  }
  void Set_crsRowCol( final int row, final int col )
  {
    m_crsRow = row;
    m_crsCol = col;

    Set_Console_CrsCell();
  }
  void Set_Console_CrsCell()
  {
    m_console.Set_Crs_Cell( Row_Win_2_GL( m_crsRow )
                          , Col_Win_2_GL( m_crsCol ) );
  }

  void Update()
  {
    if( !m_console.get_from_dot_buf() )
    {
      RepositionView();

      m_fb.Find_Styles( m_topLine + WorkingRows() );
      m_fb.Find_Regexs( m_topLine, WorkingRows() );

      Print_Borders();
      PrintWorkingView();
      PrintStsLine();
      PrintFileLine();
      PrintCmdLine();

      PrintCursor(); // Does m_console.Update()
    }
  }
  void Update_DoNot_PrintCursor()
  {
    if( !m_console.get_from_dot_buf() )
    {
      RepositionView();

      m_fb.Find_Styles( m_topLine + WorkingRows() );
      m_fb.Find_Regexs( m_topLine, WorkingRows() );

      Print_Borders();
      PrintWorkingView();
      PrintStsLine();
      PrintFileLine();
      PrintCmdLine();

      m_console.Update();
    }
  }
  void RepositionView()
  {
    // If a window re-size has taken place, and the window has gotten
    // smaller, change top line and left char if needed, so that the
    // cursor is in the window boundaries when it is re-drawn,
    // and cursor position is preserved:
    final int y_adjust = m_crsRow - (WorkingRows()-1);
    final int x_adjust = m_crsCol - (WorkingCols()-1);

    if( 0 < y_adjust )
    {
      m_topLine += y_adjust;
      m_crsRow  -= y_adjust;
    }
    if( 0 < x_adjust )
    {
      m_leftChar += x_adjust;
      m_crsCol   -= x_adjust;
    }
  }
  void Print_Borders()
  {
    final boolean HIGHLIGHT = 1 < m_vis.get_num_wins() && this == m_vis.CV();

    final Style S = HIGHLIGHT ? Style.BORDER_HI
                              : Style.BORDER;
    Print_Borders_Top   ( S );
    Print_Borders_Right ( S );
    Print_Borders_Left  ( S );
    Print_Borders_Bottom( S );
  }
  void Print_Borders_Top( final Style S )
  {
    final char BORDER_CHAR_1 = Border_Char_1();
    final char BORDER_CHAR_2 = Border_Char_2();

    final int ROW_G = m_y;

    for( int k=0; k<m_num_cols; k++ )
    {
      final int COL_G = m_x + k;
      final boolean odd = 0<k%2 ? true : false;

      if( odd ) m_console.Set( ROW_G, COL_G, BORDER_CHAR_2, S );
      else      m_console.Set( ROW_G, COL_G, BORDER_CHAR_1, S );
    }
  }
  void Print_Borders_Bottom( final Style S )
  {
    final char BORDER_CHAR_1 = Border_Char_1();
    final char BORDER_CHAR_2 = Border_Char_2();

    final int ROW_G = m_y + m_num_rows - 1;

    for( int k=0; k<m_num_cols; k++ )
    {
      final int COL_G = m_x + k;
      final boolean odd = 0<k%2 ? true : false;

      if( odd ) m_console.Set( ROW_G, COL_G, BORDER_CHAR_2, S );
      else      m_console.Set( ROW_G, COL_G, BORDER_CHAR_1, S );
    }
  }
  void Print_Borders_Right( final Style S )
  {
    final char BORDER_CHAR_1 = Border_Char_1();
    final char BORDER_CHAR_2 = Border_Char_2();

    final int COL_G = m_x + m_num_cols - 1;

    for( int k=0; k<m_num_rows-1; k++ )
    {
      final int ROW_G = m_y + k;
      final boolean odd = 0<k%2 ? true : false;

      if( odd ) m_console.Set( ROW_G, COL_G, BORDER_CHAR_2, S );
      else      m_console.Set( ROW_G, COL_G, BORDER_CHAR_1, S );
    }
  }
  void Print_Borders_Left( final Style S )
  {
    final char BORDER_CHAR_1 = Border_Char_1();
    final char BORDER_CHAR_2 = Border_Char_2();

    final int COL_G = m_x;

    for( int k=0; k<m_num_rows; k++ )
    {
      final int ROW_G = m_y + k;
      final boolean odd = 0<k%2 ? true : false;

      if( odd ) m_console.Set( ROW_G, COL_G, BORDER_CHAR_2, S );
      else      m_console.Set( ROW_G, COL_G, BORDER_CHAR_1, S );
    }
  }
  char Border_Char_1()
  {
    char border_char = ' ';

    if( m_fb.Changed() )
    {
      border_char = '+';
    }
    else if( m_fb.m_changed_externally )
    {
      border_char = Utils.DIR_DELIM;
    }
    return border_char;
  }
  char Border_Char_2()
  {
    char border_char = ' ';

    if( m_fb.m_changed_externally )
    {
      border_char = Utils.DIR_DELIM;
    }
    else if( m_fb.Changed() )
    {
      border_char = '+';
    }
    return border_char;
  }

  private void PrintWorkingView()
  {
    final int NUM_LINES = m_fb.NumLines();
    final int WR        = WorkingRows();
    final int WC        = WorkingCols();

    int row = 0;
    for( int k=m_topLine; k<NUM_LINES && row<WR; k++, row++ )
    {
      // Dont allow line wrap:
      final int LL = m_fb.LineLen( k );
      final int G_ROW = Row_Win_2_GL( row );

      int col=0;
      for( int i=m_leftChar; i<LL && col<WC; i++, col++ )
      {
        final Style s = Get_Style( k, i );
        final char  C = m_fb.Get( k, i );

        PrintWorkingView_Set( LL, G_ROW, col, i, C, s );
      }
      for( ; col<WC; col++ )
      {
        m_console.Set( G_ROW, Col_Win_2_GL( col ), ' ', Style.EMPTY );
      }
    }
    // Not enough lines to display, fill in with ~
    for( ; row < WR; row++ )
    {
      final int G_ROW = Row_Win_2_GL( row );

      m_console.Set( G_ROW, Col_Win_2_GL( 0 ), '~', Style.EOF );

      for( int col=1; col<WC; col++ )
      {
        m_console.Set( G_ROW, Col_Win_2_GL( col ), ' ', Style.EOF );
      }
    }
  }
  void PrintWorkingView_Set( final int   LL
                           , final int   G_ROW
                           , final int   col
                           , final int   i
                           , final char  C
                           , final Style s )
  {
    if( '\r' == C && i==(LL-1) )
    {
      // For readability, display carriage return at end of line as a space
      m_console.Set( G_ROW, Col_Win_2_GL( col ), ' ', Style.NORMAL );
    }
    else {
      m_console.Set( G_ROW, Col_Win_2_GL( col ), C, s );
    }
  }
  void PrintStsLine()
  {
    final int CL = CrsLine(); // Line position
    final int CC = CrsChar(); // Char position
    final int LL = m_fb.LineLen( CL );
    final int WC = WorkingCols();
    if( 0 < WC )
    {
      String str = "";
      // When inserting text at the end of a line, CrsChar() == LL
      if( 0<LL && CC < LL ) // Print current char info:
      {
        final int C = m_fb.Get( CL, CC );

        if     (  9 == C ) str = String.valueOf( C ) +",\\t";
        else if( 13 == C ) str = String.valueOf( C ) +",\\r";
        else               str = String.valueOf( C ) +","+ (char)C;
      }
      final int fileSize = m_fb.GetSize();
      final int  crsByte = m_fb.GetCursorByte( CL, CC );
      int percent = (char)(100*(double)crsByte/(double)fileSize + 0.5);
      // Screen width so far
      m_sb.setLength( 0 );
      m_sb.append( "Pos=("+(CL+1)+","+(CC+1)+")"
                 + "  ("+percent+"%, "+crsByte+"/"+fileSize+")"
                 + "  Char=("+str+")  ");

      final int SW = m_sb.length(); // Screen width so far

      if     ( SW < WC ) { for( int k=SW; k<WC; k++ ) m_sb.append(' '); }
      else if( WC < SW ) { m_sb.setLength( WC ); } //< Truncate extra part

      m_console.SetS( Sts__Line_Row()
                    , Col_Win_2_GL( 0 )
                    , m_sb.toString()
                    , Style.STATUS );
    }
  }
  void PrintFileLine()
  {
    StringBuilder buf = new StringBuilder( m_fb.m_pname );

    final int WC = WorkingCols();
    final int FILE_NAME_LEN = buf.length();

    while( 0 < buf.length()
        && WC < buf.length() ) buf.deleteCharAt( 0 );
    while( buf.length() < WC ) buf.append(' ');

    m_console.SetS( File_Line_Row(), Col_Win_2_GL( 0 ), buf.toString(), Style.STATUS );
  }
  void PrintCmdLine()
  {
    final int CMD_LINE_ROW = Cmd__Line_Row();
    final int WC = WorkingCols();
    int col=0;
    // Draw insert banner if needed
    if( m_inInsertMode )
    {
      col=10; // Strlen of "--INSERT--"
      m_console.SetS( CMD_LINE_ROW, Col_Win_2_GL( 0 ), "--INSERT--", Style.BANNER );
    }
    else if( m_inReplaceMode )
    {
      col=11; // Strlen of "--REPLACE--"
      m_console.SetS( CMD_LINE_ROW, Col_Win_2_GL( 0 ), "--REPLACE--", Style.BANNER );
    }
    else if( m_vis.get_run_mode() && m_vis.CV() == this )
    {
      col=11; // Strlen of "--RUNNING--"
      m_console.SetS( CMD_LINE_ROW, Col_Win_2_GL( 0 ), "--RUNNING--", Style.BANNER );
    }
    else if( 0 < m_cmd_line_sb.length() )
    {
      col = m_cmd_line_sb.length();
      for( int k=0; k<col && k<WC; k++ )
      {
        final char C = m_cmd_line_sb.charAt(k);
        m_console.Set( CMD_LINE_ROW, Col_Win_2_GL( k ), C, Style.NORMAL );
      }
    }

    for( ; col<WC; col++ )
    {
      m_console.Set( CMD_LINE_ROW, Col_Win_2_GL( col ), ' ', Style.NORMAL );
    }
  }
  void PrintCursor()
  {
    if( m_vis.CV() == this )
    {
      Set_Console_CrsCell();
    }
    m_console.Update();
  }

  void GoDown( final int num )
  {
    final int NUM_LINES = m_fb.NumLines();
    final int OCL       = CrsLine(); // Old cursor line

    if( 0<NUM_LINES && OCL<NUM_LINES-1 )
    {
      final int NCL = Math.min( NUM_LINES-1, OCL+num ); // New cursor line
      final int OCP = CrsChar(); // Old cursor position

      GoToCrsPos_Write( NCL, OCP );
    }
  }
  void GoUp( final int num )
  {
    final int NUM_LINES = m_fb.NumLines();
    final int OCL       = CrsLine(); // Old cursor line

    if( 0<NUM_LINES && 0<OCL )
    {
      final int NCL = Math.max( 0, OCL-num ); // New cursor line
      final int OCP = CrsChar(); // Old cursor position

      GoToCrsPos_Write( NCL, OCP );
    }
  }

  void GoLeft( final int num )
  {
    final int OCP = CrsChar(); // Old cursor position

    if( 0<m_fb.NumLines() && 0<OCP )
    {
      final int NCP = Math.max( 0, OCP-num ); // New cursor position
      final int CL  = CrsLine(); // Cursor line

      GoToCrsPos_Write( CL, NCP );
    }
  }
  void GoRight( final int num )
  {
    if( 0<m_fb.NumLines() )
    {
      final int CL  = CrsLine(); // Cursor line
      final int LL  = m_fb.LineLen( CL );
      final int OCP = CrsChar(); // Old cursor position

      if( 0<LL && OCP<LL-1 )
      {
        final int NCP = Math.min( LL-1, OCP+num ); // New cursor position

        GoToCrsPos_Write( CL, NCP );
      }
    }
  }

  void GoToBegOfLine()
  {
    if( 0==m_fb.NumLines() ) return;

    final int OCL = CrsLine(); // Old cursor line

    GoToCrsPos_Write( OCL, 0 );
  }
  void GoToEndOfLine()
  {
    if( 0==m_fb.NumLines() ) return;

    final int LL = m_fb.LineLen( CrsLine() );

    final int OCL = CrsLine(); // Old cursor line

    if( m_inVisualMode && m_inVisualBlock )
    {
      // In Visual Block, $ puts cursor at the position
      // of the end of the longest line in the block
      int max_LL = LL;

      for( int L=v_st_line; L<=v_fn_line; L++ )
      {
        max_LL = Math.max( max_LL, m_fb.LineLen( L ) );
      }
      GoToCrsPos_Write( OCL, 0<max_LL ? max_LL-1 : 0 );
    }
    else {
      GoToCrsPos_Write( OCL, 0<LL ? LL-1 : 0 );
    }
  }

  void GoToBegOfNextLine()
  {
    final int NUM_LINES = m_fb.NumLines();

    if( 0<NUM_LINES )
    {
      final int OCL = CrsLine(); // Old cursor line

      if( OCL < (NUM_LINES-1) )
      {
        // Before last line, so can go down
        GoToCrsPos_Write( OCL+1, 0 );
      }
    }
  }

  void GoToLine( final int user_line_num )
  {
    // Internal line number is 1 less than user line number:
    final int NCL = user_line_num - 1; // New cursor line number

    if( NCL == CrsLine() || m_fb.NumLines() <= NCL )
    {
      // Cant move to NCL so just put cursor back where is was
      PrintCursor(); // Does m_console.Update()
    }
    else {
      GoToCrsPos_Write( NCL, 0 );
    }
  }
  void GoToTopLineInView()
  {
    GoToCrsPos_Write( m_topLine, 0 );
  }
  void GoToBotLineInView()
  {
    final int NUM_LINES = m_fb.NumLines();

    int bottom_line_in_view = m_topLine + WorkingRows()-1;

    bottom_line_in_view = Math.min( NUM_LINES-1, bottom_line_in_view );

    GoToCrsPos_Write( bottom_line_in_view, 0 );
  }
  void GoToMidLineInView()
  {
    final int NUM_LINES = m_fb.NumLines();

    // Default: Last line in file is not in view
    int NCL = m_topLine + WorkingRows()/2; // New cursor line

    if( NUM_LINES-1 < BotLine() )
    {
      // Last line in file above bottom of view
      NCL = m_topLine + (NUM_LINES-1 - m_topLine)/2;
    }
    GoToCrsPos_Write( NCL, 0 );
  }
  void GoToTopOfFile()
  {
    GoToCrsPos_Write( 0, 0 );
  }
  void GoToEndOfFile()
  {
    final int NUM_LINES = m_fb.NumLines();

    GoToCrsPos_Write( NUM_LINES-1, 0 );
  }
  void GoToStartOfRow()
  {
    if( 0<m_fb.NumLines() )
    {
      final int OCL = CrsLine(); // Old cursor line

      GoToCrsPos_Write( OCL, m_leftChar );
    }
  }
  void GoToEndOfRow()
  {
    if( 0 < m_fb.NumLines() )
    {
      final int OCL = CrsLine(); // Old cursor line

      final int LL = m_fb.LineLen( OCL );
      if( 0 < LL )
      {
        final int NCP = Math.min( LL-1, m_leftChar + WorkingCols() - 1 );

        GoToCrsPos_Write( OCL, NCP );
      }
    }
  }
  void PageDown()
  {
    final int NUM_LINES = m_fb.NumLines();
    if( 0<NUM_LINES )
    {
      final int newTopLine = m_topLine + WorkingRows() - 1;
      // Subtracting 1 above leaves one line in common between the 2 pages.

      if( newTopLine < NUM_LINES )
      {
        m_crsCol = 0;
        m_topLine = newTopLine;
        // Dont let cursor go past the end of the file:
        if( NUM_LINES <= m_topLine + m_crsRow )
        {
          // This line places the cursor at the top of the screen, which I prefer:
          m_crsRow = 0;
        }
        Set_Console_CrsCell();

        Update();
      }
    }
  }
  void PageUp()
  {
    final int OCL = CrsLine(); // Old cursor line
    final int OCP = CrsChar(); // Old cursor position

    // Dont scroll if we are at the top of the file:
    if( 0<m_topLine )
    {
      //Leave m_crsRow unchanged.
      m_crsCol = 0;

      // Dont scroll past the top of the file:
      if( m_topLine < WorkingRows() - 1 )
      {
        m_topLine = 0;
      }
      else {
        m_topLine -= WorkingRows() - 1;
      }
      Set_Console_CrsCell();

      Update();
    }
  }

  void DisplayBanner()
  {
    if( m_console.get_from_dot_buf() ) return;

    // Command line row in window:
    final int WIN_ROW = WorkingRows() + 2;
    final int WIN_COL = 0;

    final int G_ROW = Row_Win_2_GL( WIN_ROW );
    final int G_COL = Col_Win_2_GL( WIN_COL );

    if( m_inInsertMode )
    {
      m_console.SetS( G_ROW, G_COL, "--INSERT --", Style.BANNER );
    }
    else if( m_inReplaceMode )
    {
      m_console.SetS( G_ROW, G_COL, "--REPLACE--", Style.BANNER );
    }
    else if( m_inVisualMode )
    {
      m_console.SetS( G_ROW, G_COL, "--VISUAL --", Style.BANNER );
    }
    PrintCursor(); // Does m_console.Update()
  }
  void Remove_Banner()
  {
    if( m_console.get_from_dot_buf() ) return;

    final int WC = WorkingCols();
    final int N = Math.min( WC, 11 );

    // Command line row in window:
    final int WIN_ROW = WorkingRows() + 2;

    // Clear command line:
    for( int k=0; k<N; k++ )
    {
      m_console.Set( Row_Win_2_GL( WIN_ROW )
                   , Col_Win_2_GL( k )
                   , ' '
                   , Style.NORMAL );
    }
    PrintCursor(); // Does m_console.Update()
  }

  void DisplayMapping()
  {
    if( m_console.get_from_dot_buf() ) return;

    final String mapping = "--MAPPING--";
    final int    mapping_len = mapping.length();

    // Command line row in window:
    final int WIN_ROW = WorkingRows() + 2;

    final int G_ROW = Row_Win_2_GL( WIN_ROW );
    final int G_COL = Col_Win_2_GL( m_num_cols-2-mapping_len );

    m_console.SetS( G_ROW, G_COL, mapping, Style.BANNER );

    PrintCursor(); // Does m_console.Update()
  }

  void GoToCrsPos_Write( int ncp_crsLine
                       , int ncp_crsChar )
  {
    // Limit range of ncp_crsLine to [ 0 to m_fb.NumLines()-1 ]
    ncp_crsLine = Math.max( ncp_crsLine, 0 );
    ncp_crsLine = Math.min( ncp_crsLine, m_fb.NumLines()-1 );

    final int OCL = CrsLine();
    final int OCP = CrsChar();
    final int NCL = ncp_crsLine;
    final int NCP = ncp_crsChar;

    if( OCL == NCL && OCP == NCP )
    {
      // Not moving to new cursor position so do an update
      m_console.Update();
    }
    else {
      if( m_inVisualMode )
      {
        v_fn_line = NCL;
        v_fn_char = NCP;
      }
      // These moves refer to View of buffer:
      final boolean MOVE_DOWN  = BotLine()   < NCL;
      final boolean MOVE_RIGHT = RightChar() < NCP;
      final boolean MOVE_UP    = NCL < m_topLine;
      final boolean MOVE_LEFT  = NCP < m_leftChar;

      final boolean redraw = MOVE_DOWN || MOVE_RIGHT || MOVE_UP || MOVE_LEFT;

      if( redraw )
      {
        if     ( MOVE_DOWN ) m_topLine = NCL - WorkingRows() + 1;
        else if( MOVE_UP   ) m_topLine = NCL;

        if     ( MOVE_RIGHT ) m_leftChar = NCP - WorkingCols() + 1;
        else if( MOVE_LEFT  ) m_leftChar = NCP;

        Set_crsRowCol( NCL - m_topLine, NCP - m_leftChar );

        Update();
      }
      else {
        if( m_inVisualMode )
        {
          if( m_inVisualBlock ) GoToCrsPos_Write_VisualBlock( OCL, OCP, NCL, NCP );
          else                  GoToCrsPos_Write_Visual     ( OCL, OCP, NCL, NCP );
        }
        else {
          // m_crsRow and m_crsCol must be set to new values before calling PrintCursor
          Set_crsRowCol( NCL - m_topLine, NCP - m_leftChar );
        }
        PrintStsLine();
        PrintCmdLine();
        PrintCursor();  // Does m_console.Update();
      }
    }
  }
  void GoToCrsPos_NoWrite( final int ncp_crsLine
                         , final int ncp_crsChar )
  {
    // These moves refer to View of buffer:
    final boolean MOVE_DOWN  = BotLine()   < ncp_crsLine;
    final boolean MOVE_RIGHT = RightChar() < ncp_crsChar;
    final boolean MOVE_UP    = ncp_crsLine < m_topLine;
    final boolean MOVE_LEFT  = ncp_crsChar < m_leftChar;

    if     ( MOVE_DOWN ) m_topLine = ncp_crsLine - WorkingRows() + 1;
    else if( MOVE_UP   ) m_topLine = ncp_crsLine;
    m_crsRow  = ncp_crsLine - m_topLine;

    if     ( MOVE_RIGHT ) m_leftChar = ncp_crsChar - WorkingCols() + 1;
    else if( MOVE_LEFT  ) m_leftChar = ncp_crsChar;
    m_crsCol   = ncp_crsChar - m_leftChar;

    Set_Console_CrsCell();
  }

  Style Get_Style( final int line, final int pos )
  {
    Style S = Style.EMPTY;

    if( pos < m_fb.LineLen( line ) )
    {
      S = Style.NORMAL;

      if( InVisualArea( line, pos ) )
      {
        S = Style.RV_NORMAL;

        if     ( InStar    ( line, pos ) ) S = Style.RV_STAR;
        else if( InDefine  ( line, pos ) ) S = Style.RV_DEFINE;
        else if( InComment ( line, pos ) ) S = Style.RV_COMMENT;
        else if( InConst   ( line, pos ) ) S = Style.RV_CONST;
        else if( InControl ( line, pos ) ) S = Style.RV_CONTROL;
        else if( InVarType ( line, pos ) ) S = Style.RV_VARTYPE;
        else if( InNonAscii( line, pos ) ) S = Style.RV_NONASCII;
      }
      else if( InStar    ( line, pos ) ) S = Style.STAR;
      else if( InDefine  ( line, pos ) ) S = Style.DEFINE;
      else if( InComment ( line, pos ) ) S = Style.COMMENT;
      else if( InConst   ( line, pos ) ) S = Style.CONST;
      else if( InControl ( line, pos ) ) S = Style.CONTROL;
      else if( InVarType ( line, pos ) ) S = Style.VARTYPE;
      else if( InNonAscii( line, pos ) ) S = Style.NONASCII;
    }
    return S;
  }
  boolean RV_Style( final Style S )
  {
    return S == Style.RV_NORMAL
        || S == Style.RV_STAR
        || S == Style.RV_DEFINE
        || S == Style.RV_COMMENT
        || S == Style.RV_CONST
        || S == Style.RV_CONTROL
        || S == Style.RV_VARTYPE;
  }
  Style RV_Style_2_NonRV( final Style RVS )
  {
    Style S = Style.NORMAL;

    if     ( RVS == Style.RV_STAR    ) S = Style.STAR   ;
    else if( RVS == Style.RV_DEFINE  ) S = Style.DEFINE ;
    else if( RVS == Style.RV_COMMENT ) S = Style.COMMENT;
    else if( RVS == Style.RV_CONST   ) S = Style.CONST  ;
    else if( RVS == Style.RV_CONTROL ) S = Style.CONTROL;
    else if( RVS == Style.RV_VARTYPE ) S = Style.VARTYPE;

    return S;
  }

  void GoToCrsPos_Write_Visual( final int OCL, final int OCP
                              , final int NCL, final int NCP )
  {
    // (old cursor pos) < (new cursor pos)
    final boolean OCP_LT_NCP = OCL < NCL || (OCL == NCL && OCP < NCP);

    if( OCP_LT_NCP ) // Cursor moved forward
    {
      GoToCrsPos_WV_Forward( OCL, OCP, NCL, NCP );
    }
    else // NCP_LT_OCP // Cursor moved backward
    {
      GoToCrsPos_WV_Backward( OCL, OCP, NCL, NCP );
    }
    Set_crsRowCol( NCL - m_topLine, NCP - m_leftChar );

    PrintCursor(); // Does m_console.Update()
  }
  // Cursor is moving forward
  // Write out from (OCL,OCP) up to but not including (NCL,NCP)
  void GoToCrsPos_WV_Forward( final int OCL, final int OCP
                            , final int NCL, final int NCP )
  {
    if( OCL == NCL ) // Only one line:
    {
      for( int k=OCP; k<NCP; k++ )
      {
        final char C = m_fb.Get( OCL, k );
        m_console.Set( Line_2_GL( OCL ), Char_2_GL( k ), C, Get_Style(OCL,k) );
      }
    }
    else { // Multiple lines
      // Write out first line:
      final int OCLL = m_fb.LineLen( OCL ); // Old cursor line length
      final int END_FIRST_LINE = Math.min( RightChar()+1, OCLL );
      for( int k=OCP; k<END_FIRST_LINE; k++ )
      {
        char C = m_fb.Get( OCL, k );
        m_console.Set( Line_2_GL( OCL ), Char_2_GL( k ), C, Get_Style(OCL,k) );
      }
      // Write out intermediate lines:
      for( int l=OCL+1; l<NCL; l++ )
      {
        final int LL = m_fb.LineLen( l ); // Line length
        final int END_OF_LINE = Math.min( RightChar()+1, LL );
        for( int k=m_leftChar; k<END_OF_LINE; k++ )
        {
          char C = m_fb.Get( l, k );
          m_console.Set( Line_2_GL( l ), Char_2_GL( k ), C, Get_Style(l,k) );
        }
      }
      // Write out last line:
      // Print from beginning of next line to new cursor position:
      final int NCLL = m_fb.LineLen( NCL ); // Line length
      final int END_LAST_LINE = Math.min( NCLL, NCP );
      for( int k=m_leftChar; k<END_LAST_LINE; k++ )
      {
        char C = m_fb.Get( NCL, k );
        m_console.Set( Line_2_GL( NCL ), Char_2_GL( k ), C, Get_Style(NCL,k)  );
      }
    }
  }

  // Cursor is moving backwards
  // Write out from (OCL,OCP) back to but not including (NCL,NCP)
  void GoToCrsPos_WV_Backward( final int OCL, final int OCP
                             , final int NCL, final int NCP )
  {
    if( OCL == NCL ) // Only one line:
    {
      final int LL = m_fb.LineLen( OCL ); // Line length
      if( 0<LL ) {
        final int START = Math.min( OCP, LL-1 );
        for( int k=START; NCP<k; k-- )
        {
          char C = m_fb.Get( OCL, k );
          m_console.Set( Line_2_GL( OCL ), Char_2_GL( k ), C, Get_Style(OCL,k) );
        }
      }
    }
    else { // Multiple lines
      // Write out first line:
      final int OCLL = m_fb.LineLen( OCL ); // Old cursor line length
      if( 0<OCLL ) {
        for( int k=Math.min(OCP,OCLL-1); m_leftChar<=k; k-- )
        {
          char C = m_fb.Get( OCL, k );
          m_console.Set( Line_2_GL( OCL ), Char_2_GL( k ), C, Get_Style(OCL,k) );
        }
      }
      // Write out intermediate lines:
      for( int l=OCL-1; NCL<l; l-- )
      {
        final int LL = m_fb.LineLen( l ); // Line length
        if( 0<LL ) {
          final int END_OF_LINE = Math.min( RightChar(), LL-1 );
          for( int k=END_OF_LINE; m_leftChar<=k; k-- )
          {
            char C = m_fb.Get( l, k );
            m_console.Set( Line_2_GL( l ), Char_2_GL( k ), C, Get_Style(l,k) );
          }
        }
      }
      // Write out last line:
      // Go down to beginning of last line:
      final int NCLL = m_fb.LineLen( NCL ); // New cursor line length
      if( 0<NCLL ) {
        final int END_LAST_LINE = Math.min( RightChar(), NCLL-1 );

        // Print from beginning of next line to new cursor position:
        for( int k=END_LAST_LINE; NCP<=k; k-- )
        {
          char C = m_fb.Get( NCL, k );
          m_console.Set( Line_2_GL( NCL ), Char_2_GL( k ), C, Get_Style(NCL,k) );
        }
      }
    }
  }

  void GoToCrsPos_Write_VisualBlock( final int OCL
                                   , final int OCP
                                   , final int NCL
                                   , final int NCP )
  {
    // v_fn_line == NCL && v_fn_char == NCP, so dont need to include
    // v_fn_line       and v_fn_char in Min and Max calls below:
    final int vis_box_left = Math.min( v_st_char, Math.min( OCP, NCP ) );
    final int vis_box_rite = Math.max( v_st_char, Math.max( OCP, NCP ) );
    final int vis_box_top  = Math.min( v_st_line, Math.min( OCL, NCL ) );
    final int vis_box_bot  = Math.max( v_st_line, Math.max( OCL, NCL ) );

    final int draw_box_left = Math.max( m_leftChar , vis_box_left );
    final int draw_box_rite = Math.min( RightChar(), vis_box_rite );
    final int draw_box_top  = Math.max( m_topLine  , vis_box_top  );
    final int draw_box_bot  = Math.min( BotLine()  , vis_box_bot  );

    for( int l=draw_box_top; l<=draw_box_bot; l++ )
    {
      final int LL = m_fb.LineLen( l );

      for( int k=draw_box_left; k<LL && k<=draw_box_rite; k++ )
      {
        // On some terminals, the cursor on reverse video on white space does not
        // show up, so to prevent that, do not reverse video the cursor position:
        final char  C     = m_fb.Get ( l, k );
        final Style style = Get_Style( l, k );

        if( NCL==l && NCP==k )
        {
          if( RV_Style( style ) )
          {
            final Style NonRV_style = RV_Style_2_NonRV( style );

            m_console.Set( Line_2_GL( l ), Char_2_GL( k ), C, NonRV_style );
          }
        }
        else {
          m_console.Set( Line_2_GL( l ), Char_2_GL( k ), C, style );
        }
      }
    }
    Set_crsRowCol( NCL - m_topLine, NCP - m_leftChar );

    PrintCursor(); // Does m_console.Update()
  }

  void GoToCmdLineClear( final String S )
  {
    final int ROW = Cmd__Line_Row();

    // Clear command line:
    for( int k=0; k<WorkingCols(); k++ )
    {
      m_console.Set( ROW, Col_Win_2_GL( k ), ' ', Style.NORMAL );
    }
    final int S_LEN = S.length();
    final int ST    = Col_Win_2_GL( 0 );

    for( int k=0; k<S_LEN; k++ )
    {
      m_console.Set( ROW, ST+k, S.charAt(k), Style.NORMAL );
    }
    m_console.Set_Crs_Cell( ROW, Col_Win_2_GL( S_LEN ) );
    m_console.Update();
  }
  boolean InComment( final int line, final int pos )
  {
    return m_fb.HasStyle( line, pos, Highlight_Type.COMMENT.val );
  }
  boolean InDefine( final int line, final int pos )
  {
    return m_fb.HasStyle( line, pos, Highlight_Type.DEFINE.val );
  }
  boolean InConst( final int line, final int pos )
  {
    return m_fb.HasStyle( line, pos, Highlight_Type.CONST.val );
  }
  boolean InControl( final int line, final int pos )
  {
    return m_fb.HasStyle( line, pos, Highlight_Type.CONTROL.val );
  }
  boolean InVarType( final int line, final int pos )
  {
    return m_fb.HasStyle( line, pos, Highlight_Type.VARTYPE.val );
  }
  boolean InStar( final int line, final int pos )
  {
    return m_fb.HasStyle( line, pos, Highlight_Type.STAR.val );
  }
  boolean InNonAscii( final int line, final int pos )
  {
    return m_fb.HasStyle( line, pos, Highlight_Type.NONASCII.val );
  }
  boolean InVisualArea( final int line, final int pos )
  {
    if( m_inVisualMode )
    {
      if( m_inVisualBlock ) return InVisualBlock( line, pos );
      else                  return InVisualStFn ( line, pos );
    }
    return false;
  }
  boolean InVisualBlock( final int line, final int pos )
  {
    return ( v_st_line <= line && line <= v_fn_line && v_st_char <= pos  && pos  <= v_fn_char ) // bot rite
        || ( v_st_line <= line && line <= v_fn_line && v_fn_char <= pos  && pos  <= v_st_char ) // bot left
        || ( v_fn_line <= line && line <= v_st_line && v_st_char <= pos  && pos  <= v_fn_char ) // top rite
        || ( v_fn_line <= line && line <= v_st_line && v_fn_char <= pos  && pos  <= v_st_char );// top left
  }
  boolean InVisualStFn( final int line, final int pos )
  {
    if( v_st_line == line && line == v_fn_line )
    {
      return (v_st_char <= pos && pos <= v_fn_char)
          || (v_fn_char <= pos && pos <= v_st_char);
    }
    else if( (v_st_line < line && line < v_fn_line)
          || (v_fn_line < line && line < v_st_line) )
    {
      return true;
    }
    else if( v_st_line == line && line < v_fn_line )
    {
      return v_st_char <= pos;
    }
    else if( v_fn_line == line && line < v_st_line )
    {
      return v_fn_char <= pos;
    }
    else if( v_st_line < line && line == v_fn_line )
    {
      return pos <= v_fn_char;
    }
    else if( v_fn_line < line && line == v_st_line )
    {
      return pos <= v_st_char;
    }
    return false;
  }
  void SetTilePos( final Tile_Pos tp )
  {
    m_tile_pos = tp;

    SetViewPos();
  }
  void SetViewPos()
  {
    TilePos_2_x();
    TilePos_2_y();
    TilePos_2_nRows();
    TilePos_2_nCols();
  }

  int Cols_Left_Half()
  {
    final int CON_COLS = m_console.Num_Cols();

    return ( 0 != CON_COLS%2 )
           ? CON_COLS/2+1 //< Left side gets extra column
           : CON_COLS/2;  //< Both sides get equal
  }

  int Cols_Rite_Half()
  {
    final int CON_COLS = m_console.Num_Cols();

    return CON_COLS - Cols_Left_Half();
  }

  int Cols_Left_Far_Qtr()
  {
    final int COLS_LEFT_HALF = Cols_Left_Half();

    return ( 0 != COLS_LEFT_HALF%2 )
           ? COLS_LEFT_HALF/2  //< Left ctr qtr gets extra column
           : COLS_LEFT_HALF/2; //< Both qtrs get equal
  }

  int Cols_Left_Ctr_Qtr()
  {
    return Cols_Left_Half() - Cols_Left_Far_Qtr();
  }

  int Cols_Rite_Far_Qtr()
  {
    final int COLS_RITE_HALF = Cols_Rite_Half();

    return ( 0 != COLS_RITE_HALF%2 )
           ? COLS_RITE_HALF/2  //< Rite ctr qtr gets extra column
           : COLS_RITE_HALF/2; //< Both sides get equal
  }

  int Cols_Rite_Ctr_Qtr()
  {
    return Cols_Rite_Half() - Cols_Rite_Far_Qtr();
  }

  int Cols_Left_Third()
  {
    final int CON_COLS = m_console.Num_Cols();

    return ( 0 != CON_COLS%3 )
           ? ( (CON_COLS%3==1)
             ? CON_COLS/3     // Ctr third gets extra column
             : CON_COLS/3+1 ) // Ctr and left get extra column
           : CON_COLS/3; // All thirds equal
  }

  int Cols_Ctr__Third()
  {
    final int CON_COLS = m_console.Num_Cols();

    return ( 0 != CON_COLS%3 )
           ? ( (CON_COLS%3==1)
             ? CON_COLS/3+1   // Ctr third gets extra column
             : CON_COLS/3+1 ) // Ctr and left get extra column
           : CON_COLS/3; // All thirds equal
  }

  int Cols_Rite_Third()
  {
    final int CON_COLS = m_console.Num_Cols();

    return CON_COLS - Cols_Left_Third() - Cols_Ctr__Third();
  }

  void TilePos_2_x()
  {
    // FULL     , BOT__HALF    , LEFT_QTR
    // LEFT_HALF, TOP__LEFT_QTR, TOP__LEFT_8TH
    // TOP__HALF, BOT__LEFT_QTR, BOT__LEFT_8TH
    // TP_LEFT_THIRD, TP_LEFT_TWO_THIRDS
    m_x = 0;

    if( Tile_Pos.RITE_HALF         == m_tile_pos
     || Tile_Pos.TOP__RITE_QTR     == m_tile_pos
     || Tile_Pos.BOT__RITE_QTR     == m_tile_pos
     || Tile_Pos.RITE_CTR__QTR     == m_tile_pos
     || Tile_Pos.TOP__RITE_CTR_8TH == m_tile_pos
     || Tile_Pos.BOT__RITE_CTR_8TH == m_tile_pos )
    {
      m_x = Cols_Left_Half();
    }
    else if( Tile_Pos.LEFT_CTR__QTR     == m_tile_pos
          || Tile_Pos.TOP__LEFT_CTR_8TH == m_tile_pos
          || Tile_Pos.BOT__LEFT_CTR_8TH == m_tile_pos )
    {
      m_x = Cols_Left_Far_Qtr();
    }
    else if( Tile_Pos.RITE_QTR      == m_tile_pos
          || Tile_Pos.TOP__RITE_8TH == m_tile_pos
          || Tile_Pos.BOT__RITE_8TH == m_tile_pos )
    {
      m_x = Cols_Left_Half() + Cols_Rite_Ctr_Qtr();
    }
    else if( Tile_Pos.CTR__THIRD      == m_tile_pos
          || Tile_Pos.RITE_TWO_THIRDS == m_tile_pos )
    {
      m_x = Cols_Left_Third();
    }
    else if( Tile_Pos.RITE_THIRD == m_tile_pos )
    {
      m_x = Cols_Left_Third() + Cols_Ctr__Third();
    }
  }

  void TilePos_2_y()
  {
    final int CON_ROWS = m_console.Num_Rows();

    // FULL         , LEFT_CTR__QTR
    // LEFT_HALF    , RITE_CTR__QTR
    // RITE_HALF    , RITE_QTR
    // TOP__HALF    , TOP__LEFT_8TH
    // TOP__LEFT_QTR, TOP__LEFT_CTR_8TH
    // TOP__RITE_QTR, TOP__RITE_CTR_8TH
    // LEFT_QTR     , TOP__RITE_8TH
    // LEFT_THIRD   , CTR__THIRD, RITE_THIRD
    // LEFT_TWO_THIRDS, RITE_TWO_THIRDS
    m_y = 0;

    if( Tile_Pos.BOT__HALF         == m_tile_pos
     || Tile_Pos.BOT__LEFT_QTR     == m_tile_pos
     || Tile_Pos.BOT__RITE_QTR     == m_tile_pos
     || Tile_Pos.BOT__LEFT_8TH     == m_tile_pos
     || Tile_Pos.BOT__LEFT_CTR_8TH == m_tile_pos
     || Tile_Pos.BOT__RITE_CTR_8TH == m_tile_pos
     || Tile_Pos.BOT__RITE_8TH     == m_tile_pos )
    {
      m_y = CON_ROWS/2;
    }
  }

  void TilePos_2_nRows()
  {
    final int     CON_ROWS = m_console.Num_Rows();
    final boolean ODD_ROWS = 0 != CON_ROWS%2;

    // TOP__HALF        , BOT__HALF        ,
    // TOP__LEFT_QTR    , BOT__LEFT_QTR    ,
    // TOP__RITE_QTR    , BOT__RITE_QTR    ,
    // TOP__LEFT_8TH    , BOT__LEFT_8TH    ,
    // TOP__LEFT_CTR_8TH, BOT__LEFT_CTR_8TH,
    // TOP__RITE_CTR_8TH, BOT__RITE_CTR_8TH,
    // TOP__RITE_8TH    , BOT__RITE_8TH    ,
    m_num_rows = CON_ROWS/2;

    if( Tile_Pos.FULL            == m_tile_pos
     || Tile_Pos.LEFT_HALF       == m_tile_pos
     || Tile_Pos.RITE_HALF       == m_tile_pos
     || Tile_Pos.LEFT_QTR        == m_tile_pos
     || Tile_Pos.LEFT_CTR__QTR   == m_tile_pos
     || Tile_Pos.RITE_CTR__QTR   == m_tile_pos
     || Tile_Pos.RITE_QTR        == m_tile_pos
     || Tile_Pos.LEFT_THIRD      == m_tile_pos
     || Tile_Pos.CTR__THIRD      == m_tile_pos
     || Tile_Pos.RITE_THIRD      == m_tile_pos
     || Tile_Pos.LEFT_TWO_THIRDS == m_tile_pos
     || Tile_Pos.RITE_TWO_THIRDS == m_tile_pos )
    {
      m_num_rows = CON_ROWS;
    }
    if( ODD_ROWS && ( Tile_Pos.BOT__HALF         == m_tile_pos
                   || Tile_Pos.BOT__LEFT_QTR     == m_tile_pos
                   || Tile_Pos.BOT__RITE_QTR     == m_tile_pos
                   || Tile_Pos.BOT__LEFT_8TH     == m_tile_pos
                   || Tile_Pos.BOT__LEFT_CTR_8TH == m_tile_pos
                   || Tile_Pos.BOT__RITE_CTR_8TH == m_tile_pos
                   || Tile_Pos.BOT__RITE_8TH     == m_tile_pos ) )
    {
      m_num_rows++;
    }
  }

  void TilePos_2_nCols()
  {
    if( Tile_Pos.FULL      == m_tile_pos
     || Tile_Pos.TOP__HALF == m_tile_pos
     || Tile_Pos.BOT__HALF == m_tile_pos )
    {
      m_num_cols = m_console.Num_Cols();
    }
    else if( Tile_Pos.LEFT_HALF     == m_tile_pos
          || Tile_Pos.TOP__LEFT_QTR == m_tile_pos
          || Tile_Pos.BOT__LEFT_QTR == m_tile_pos )
    {
      m_num_cols = Cols_Left_Half();
    }
    else if( Tile_Pos.RITE_HALF     == m_tile_pos
          || Tile_Pos.TOP__RITE_QTR == m_tile_pos
          || Tile_Pos.BOT__RITE_QTR == m_tile_pos )
    {
      m_num_cols = Cols_Rite_Half();
    }
    else if( Tile_Pos.LEFT_QTR      == m_tile_pos
          || Tile_Pos.TOP__LEFT_8TH == m_tile_pos
          || Tile_Pos.BOT__LEFT_8TH == m_tile_pos )
    {
      m_num_cols = Cols_Left_Far_Qtr();
    }
    else if( Tile_Pos.LEFT_CTR__QTR     == m_tile_pos
          || Tile_Pos.TOP__LEFT_CTR_8TH == m_tile_pos
          || Tile_Pos.BOT__LEFT_CTR_8TH == m_tile_pos )
    {
      m_num_cols = Cols_Left_Ctr_Qtr();
    }
    else if( Tile_Pos.RITE_CTR__QTR     == m_tile_pos
          || Tile_Pos.TOP__RITE_CTR_8TH == m_tile_pos
          || Tile_Pos.BOT__RITE_CTR_8TH == m_tile_pos )
    {
      m_num_cols = Cols_Rite_Ctr_Qtr();
    }
    else if( Tile_Pos.RITE_QTR      == m_tile_pos
          || Tile_Pos.TOP__RITE_8TH == m_tile_pos
          || Tile_Pos.BOT__RITE_8TH == m_tile_pos )
    {
      m_num_cols = Cols_Rite_Far_Qtr();
    }
    else if( Tile_Pos.LEFT_THIRD == m_tile_pos )
    {
      m_num_cols = Cols_Left_Third();
    }
    else if( Tile_Pos.CTR__THIRD == m_tile_pos )
    {
      m_num_cols = Cols_Ctr__Third();
    }
    else if( Tile_Pos.RITE_THIRD == m_tile_pos )
    {
      m_num_cols = Cols_Rite_Third();
    }
    else if( Tile_Pos.LEFT_TWO_THIRDS == m_tile_pos )
    {
      m_num_cols = Cols_Left_Third() + Cols_Ctr__Third();
    }
    else if( Tile_Pos.RITE_TWO_THIRDS == m_tile_pos )
    {
      m_num_cols = Cols_Ctr__Third() + Cols_Rite_Third();
    }
  }

//void GoToFile()
//{
//  String fname = GetFileName_UnderCursor();
//
//  if( null != fname ) m_vis.GoToBuffer_Fname( fname );
//}

  void GoToFile()
  {
    String fname = m_vis.Is_BE_FILE( m_fb )
                 ? GetFileName_UnderCursor_WholeLine()
                 : GetFileName_UnderCursor_PartialLine();

    if( null != fname ) m_vis.GoToBuffer_Fname( fname );
  }
  String GetFileName_UnderCursor_PartialLine()
  {
    StringBuilder fname = null;
    final int CL = CrsLine();
    final int LL = m_fb.LineLen( CL );
    if( 0<LL ) {
      MoveInBounds_Line();
      final int CP = CrsChar();
      char C = m_fb.Get( CL, CP );

      if( Utils.IsFileNameChar( C ) )
      {
        // Get the file name:
        fname = new StringBuilder();
        fname.append( C );

        // Search backwards, until non-filename char found:
        for( int k=CP-1; -1<k; k-- )
        {
          C = m_fb.Get( CL, k );
          if( !Utils.IsFileNameChar( C ) ) break;
          else fname.insert( 0, C );
        }
        // Search forwards, until non-filename char found:
        for( int k=CP+1; k<LL; k++ )
        {
          C = m_fb.Get( CL, k );
          if( !Utils.IsFileNameChar( C ) ) break;
          else fname.append( C );
        }
        // Trim white space off beginning and ending of fname:
        Utils.Trim( fname );
        // Replace environment variables with values:
        fname = Utils.EnvKeys2Vals( fname );
      }
    }
    return null != fname ? fname.toString() : null;
  }
  String GetFileName_UnderCursor_WholeLine()
  {
    String fname = null;

    final int CL = CrsLine();
    final int LL = m_fb.LineLen( CL );

    if( 0<LL )
    {
      fname = m_fb.GetLine( CL ).toString();
    }
    return fname;
  }

  // If past end of file, move back to last line.
  // If past end of line, move back to end of line.
  //
  void MoveInBounds_File()
  {
    final int NUM_LINES = m_fb.NumLines();

    if( 0 < NUM_LINES )
    {
      if( NUM_LINES <= CrsLine() )
      {
        final int NCL = NUM_LINES-1;
        final int LL  = m_fb.LineLen( NCL );
        final int EOL = 0<LL ? LL-1 : 0;

        // Since cursor is now allowed past EOL, it may need to be moved back:
        final int NCC = Math.min( CrsChar(), EOL );

        GoToCrsPos_NoWrite( NCL, NCC );
      }
      else
      {
        MoveInBounds_Line();
      }
    }
  }
  // If past end of line, move back to end of line.
  //
  void MoveInBounds_Line()
  {
    final int CL  = CrsLine();
    final int LL  = m_fb.LineLen( CL );
    final int EOL = 0<LL ? LL-1 : 0;

    // Since cursor is now allowed past EOL, it may need to be moved back:
    if( EOL < CrsChar() ) GoToCrsPos_NoWrite( CL, EOL );
  }

  void Do_A()
  {
    GoToEndOfLine();
    Do_a();
  }

  void Do_a()
  {
    if( 0<m_fb.NumLines() )
    {
      final int CL = CrsLine();
      final int CC = CrsChar();
      final int LL = m_fb.LineLen( CL );

      if( LL < CC )
      {
        GoToCrsPos_NoWrite( CL, LL );
        m_fb.Update();
      }
      else if( CC < LL )
      {
        GoToCrsPos_NoWrite( CL, CC+1 );
        m_fb.Update();
      }
    }
    Do_i();
  }

  void Do_i()
  {
    m_vis.get_states().addFirst( m_run_i_end );
    m_vis.get_states().addFirst( m_run_i_mid );
    m_vis.get_states().addFirst( m_run_i_beg );
  }
  void run_i_beg()
  {
    m_inInsertMode = true;
    DisplayBanner();

    if( 0 == m_fb.NumLines() ) m_fb.PushLine();

    final int LL = m_fb.LineLen( CrsLine() );  // Line length

    if( LL < CrsChar() ) // Since cursor is now allowed past EOL,
    {                    // it may need to be moved back:
      // For user friendlyness, move cursor to new position immediately:
      GoToCrsPos_Write( CrsLine(), LL );
    }
    m_i_count = 0;

    m_vis.get_states().removeFirst();
  }
  void run_i_mid()
  {
    if( 0<m_console.KeysIn() )
    {
      final char C = m_console.GetKey();

      if( C == ESC )
      {
        m_vis.get_states().removeFirst(); // Done
      }
      else if( BS == C || DEL == C )
      {
        if( 0<m_i_count )
        {
          InsertBackspace();
          m_i_count--;
        }
      }
      else if( Utils.IsEndOfLineDelim( C ) )
      {
        InsertAddReturn();
        m_i_count++;
      }
      else {
        InsertAddChar( C );
        m_i_count++;
      }
    }
  }
  void run_i_end()
  {
    Remove_Banner();
    m_inInsertMode = false;
    m_inVisualMode = false;

    // Move cursor back one space:
    if( 0<m_crsCol )
    {
      Set_crsCol( m_crsCol-1 );
      m_fb.Update();
    }
    m_vis.get_states().removeFirst();
  }
  void Do_R()
  {
    m_vis.get_states().addFirst( m_run_R_end );
    m_vis.get_states().addFirst( m_run_R_mid );
    m_vis.get_states().addFirst( m_run_R_beg );
  }
  void run_R_beg()
  {
    m_inReplaceMode = true;
    DisplayBanner();

    if( 0 == m_fb.NumLines() ) m_fb.PushLine();

    m_vis.get_states().removeFirst();
  }
  void run_R_mid()
  {
    if( 0<m_console.KeysIn() )
    {
      final char C = m_console.GetKey();

      if( C == ESC )
      {
        m_vis.get_states().removeFirst();
      }
      else if( BS == C || DEL == C )
      {
        m_fb.Undo( this );
      }
      else if( Utils.IsEndOfLineDelim( C ) )
      {
        ReplaceAddReturn();
      }
      else {
        ReplaceAddChars( C );
      }
    }
  }
  void run_R_end()
  {
    Remove_Banner();
    m_inReplaceMode = false;

    // Move cursor back one space:
    if( 0<m_crsCol )
    {
      Set_crsCol( m_crsCol-1 );
    }
    m_fb.Update();

    m_vis.get_states().removeFirst();
  }
  void ReplaceAddReturn()
  {
    // The lines in fb do not end with '\n's.
    // When the file is written, '\n's are added to the ends of the lines.
    Line new_line = new Line();

    final int OLL = m_fb.LineLen( CrsLine() );
    final int OCP = CrsChar();

    for( int k=OCP; k<OLL; k++ )
    {
      final char C = m_fb.RemoveChar( CrsLine(), OCP );
      new_line.append_c( C );
    }
    // Truncate the rest of the old line:
    // Add the new line:
    final int new_line_num = CrsLine()+1;
    m_fb.InsertLine( new_line_num, new_line );

    GoToCrsPos_NoWrite( new_line_num, 0 );

    m_fb.Update();
  }
  void ReplaceAddChars( final char C )
  {
    if( m_fb.NumLines()==0 ) m_fb.PushLine();

    final int CL = CrsLine();
    final int CP = CrsChar();
    final int LL = m_fb.LineLen( CL );
    final int EOL = 0<LL ? LL-1 : 0;

    if( EOL < CP )
    {
      // Extend line out to where cursor is:
      for( int k=LL; k<CP; k++ ) m_fb.PushChar( CL, ' ' );
    }
    // Put char back in file buffer
    final boolean continue_last_update = false;
    if( CP < LL ) m_fb.Set( CL, CP, C, continue_last_update );
    else {
      m_fb.PushChar( CL, C );
    }
    if( m_crsCol < WorkingCols()-1 )
    {
      m_crsCol++;
    }
    else {
      m_leftChar++;
    }
    m_fb.Update();
  }

  void InsertAddReturn()
  {
    // The lines in fb do not end with '\n's.
    // When the file is written, '\n's are added to the ends of the lines.
    Line new_line = new Line();
    final int OLL = m_fb.LineLen( CrsLine() );  // Old line length
    final int OCP = CrsChar();                  // Old cursor position

    for( int k=OCP; k<OLL; k++ )
    {
      final char C = m_fb.RemoveChar( CrsLine(), OCP );
      new_line.append_c( C );
    }
    // Truncate the rest of the old line:
    // Add the new line:
    final int NCL = CrsLine()+1; // New cursor line
    m_fb.InsertLine( NCL, new_line );

    GoToCrsPos_NoWrite( NCL, 0 );

    m_fb.Update();
  }
  void InsertBackspace()
  {
    // If no lines in buffer, no backspacing to be done
    if( 0<m_fb.NumLines() )
    {
      final int OCL = CrsLine();  // Old cursor line
      final int OCP = CrsChar();  // Old cursor position

      if( 0<OCP ) InsertBackspace_RmC ( OCL, OCP );
      else        InsertBackspace_RmNL( OCL );
    }
  }
  void InsertAddChar( final char C )
  {
    if( m_fb.NumLines()<=0 ) m_fb.PushLine();

    m_fb.InsertChar( CrsLine(), CrsChar(), C );

    if( WorkingCols() <= m_crsCol+1 )
    {
      // On last working column, need to scroll right:
      m_leftChar++;
    }
    else {
      m_crsCol += 1;
    }
    m_fb.Update();
  }

  void InsertBackspace_RmC( final int OCL
                          , final int OCP )
  {
    m_fb.RemoveChar( OCL, OCP-1 );

    if( 0 < m_crsCol ) m_crsCol -= 1;
    else               m_leftChar -= 1;

    m_fb.Update();
  }
  void InsertBackspace_RmNL( final int OCL )
  {
    // Cursor Line Position is zero, so:
    // 1. Save previous line, end of line + 1 position
    CrsPos ncp = new CrsPos( OCL-1, m_fb.LineLen( OCL-1 ) );

    // 2. Remove the line
    Line lp = m_fb.RemoveLine( OCL );

    // 3. Append rest of line to previous line
    m_fb.AppendLineToLine( OCL-1, lp );

    // 4. Put cursor at the old previous line end of line + 1 position
    final boolean MOVE_UP    = ncp.crsLine < m_topLine;
    final boolean MOVE_RIGHT = RightChar() < ncp.crsChar;

    if( MOVE_UP ) m_topLine = ncp.crsLine;
                  m_crsRow  = ncp.crsLine - m_topLine;

    if( MOVE_RIGHT ) m_leftChar = ncp.crsChar - WorkingCols() + 1;
                     m_crsCol   = ncp.crsChar - m_leftChar;

    // 5. Removed a line, so update to re-draw window view
    m_fb.Update();
  }

  void Do_x()
  {
    // If there is nothing to 'x', just return:
    if( 0<m_fb.NumLines() )
    {
      final int CL = CrsLine();
      final int LL = m_fb.LineLen( CL );

      // If nothing on line, just return:
      if( 0 < LL )
      {
        // If past end of line, move to end of line:
        if( LL-1 < CrsChar() )
        {
          GoToCrsPos_Write( CL, LL-1 );
        }
        final char C = m_fb.RemoveChar( CL, CrsChar() );

        // Put char x'ed into register:
        Line nlr = new Line();
        nlr.append_c( C );
        m_vis.get_reg().clear();
        m_vis.get_reg().add( nlr );
        m_vis.set_paste_mode( Paste_Mode.ST_FN );

        final int NLL = m_fb.LineLen( CL ); // New line length

        // Reposition the cursor:
        if( NLL <= m_leftChar+m_crsCol )
        {
          // The char x'ed is the last char on the line, so move the cursor
          //   back one space.  Above, a char was removed from the line,
          //   but m_crsCol has not changed, so the last char is now NLL.
          // If cursor is not at beginning of line, move it back one more space.
          if( 0 < m_crsCol ) m_crsCol--;
        }
        m_fb.Update();
      }
    }
  }
  void Do_o()
  {
    final int ONL = m_fb.NumLines();
    final int OCL = CrsLine();

    // Add the new line:
    final int NCL = ( 0 < ONL ) ? OCL+1 : 0;
    m_fb.InsertLine( NCL );

    GoToCrsPos_NoWrite( NCL, 0 );

    m_fb.Update();

    Do_i();
  }
  void Do_O()
  {
    final int OCL = CrsLine();

    // Add the new line:
    m_fb.InsertLine( OCL );

    GoToCrsPos_NoWrite( OCL, 0 );

    m_fb.Update();

    Do_i();
  }
  void GoToNextWord()
  {
    CrsPos ncp = GoToNextWord_GetPosition();

    if( null != ncp )
    {
      GoToCrsPos_Write( ncp.crsLine, ncp.crsChar );
    }
  }
  // Returns new cursor position if next word found, else null
  //
  CrsPos GoToNextWord_GetPosition()
  {
    final int NUM_LINES = m_fb.NumLines();
    if( 0==NUM_LINES ) return null;

    int ncp_crsLine = 0;
    int ncp_crsChar = 0;

    boolean found_space = false;
    boolean found_word  = false;
    final int OCL = CrsLine(); // Old cursor line
    final int OCP = CrsChar(); // Old cursor position

    boolean ident = true; // Looking for identifier

    // Find white space, and then find non-white space
    for( int l=OCL; (!found_space || !found_word) && l<NUM_LINES; l++ )
    {
      final int LL = m_fb.LineLen( l );
      if( LL<=0 || OCL<l )
      {
        found_space = true;
        // Once we have encountered a space, word is anything non-space.
        // An empty line is considered to be a space.
        ident = false;
      }
      final int START_C = OCL==l ? OCP : 0;

      for( int p=START_C; (!found_space || !found_word) && p<LL; p++ )
      {
        ncp_crsLine = l;
        ncp_crsChar = p;

        final char C = m_fb.Get( l, p );

        if( found_space  )
        {
          if( IsWord( C, ident ) ) found_word = true;
        }
        else {
          if( !IsWord( C, ident ) ) found_space = true;
        }
        // Once we have encountered a space, word is anything non-space
        if( Utils.IsSpace( C ) ) ident = false;
      }
    }
    return ( found_space && found_word )
         ? new CrsPos( ncp_crsLine, ncp_crsChar )
         : null;
  }
  void GoToPrevWord()
  {
    CrsPos ncp = GoToPrevWord_GetPosition();

    if( null != ncp )
    {
      GoToCrsPos_Write( ncp.crsLine, ncp.crsChar );
    }
  }
  // Returns new cursor position if prev word found, else null
  //
  CrsPos GoToPrevWord_GetPosition()
  {
    final int NUM_LINES = m_fb.NumLines();
    if( NUM_LINES<=0 ) return null;

    final int OCL = CrsLine(); // Old cursor line
    final int LL  = m_fb.LineLen( OCL );

    if( LL < CrsChar() ) // Since cursor is now allowed past EOL,
    {                    // it may need to be moved back:
      if( 0<LL && !Utils.IsSpace( m_fb.Get( OCL, LL-1 ) ) )
      {
        // Backed up to non-white space, which is previous word, so return true
        return new CrsPos( OCL, LL-1 );
      }
      else {
        GoToCrsPos_NoWrite( OCL, 0<LL ? LL-1 : 0 );
      }
    }
    int ncp_crsLine = 0;
    int ncp_crsChar = 0;

    boolean found_space = false;
    boolean found_word  = false;
    final int OCP = CrsChar(); // Old cursor position

    boolean ident = false; // Looking for identifier

    // Find word to non-word transition
    for( int l=OCL; (!found_space || !found_word) && -1<l; l-- )
    {
      final int LL2 = m_fb.LineLen( l );
      if( LL2<=0 || l<OCL )
      {
        // Once we have encountered a space, word is anything non-space.
        // An empty line is considered to be a space.
        ident = false;
      }
      final int START_C = OCL==l ? OCP-1 : LL2-1;

      for( int p=START_C; (!found_space || !found_word) && -1<p; p-- )
      {
        ncp_crsLine = l;
        ncp_crsChar = p;

        final char C = m_fb.Get( l, p );

        if( found_word  )
        {
          if( !IsWord( C, ident ) || p==0 ) found_space = true;
        }
        else {
          if( IsWord( C, ident ) ) {
            found_word = true;
            if( 0==p ) found_space = true;
          }
        }
        // Once we have encountered a space, word is anything non-space
        if( Utils.IsIdent( C ) ) ident = true;
      }
      if( found_space && found_word )
      {
        if( 0<ncp_crsChar && ncp_crsChar < LL2-1 ) ncp_crsChar++;
      }
    }
    return ( found_space && found_word )
         ? new CrsPos( ncp_crsLine, ncp_crsChar )
         : null;
  }
  boolean IsWord( final char C, final boolean ident )
  {
    if( ident ) return Utils.IsWord_Ident( C );
                return Utils.NotSpace( C );
  }
  void GoToEndOfWord()
  {
    CrsPos ncp = GoToEndOfWord_GetPosition();

    if( null != ncp )
    {
      GoToCrsPos_Write( ncp.crsLine, ncp.crsChar );
    }
  }
  // Returns new cursor position if found end of word, else null
  // 1. If at end of word, or end of non-word, move to next char
  // 2. If on white space, skip past white space
  // 3. If on word, go to end of word
  // 4. If on non-white-non-word, go to end of non-white-non-word
  CrsPos GoToEndOfWord_GetPosition()
  {
    CrsPos ncp = new CrsPos( 0, 0 );

    final int NUM_LINES = m_fb.NumLines();
    if( NUM_LINES<=0 ) return null;

    final int CL = CrsLine(); // Cursor line
    final int LL = m_fb.LineLen( CL );
          int CP = CrsChar(); // Cursor position

    // At end of line, or line too short:
    if( (LL-1) <= CP || LL < 2 ) return null;

          char CC = m_fb.Get( CL, CP );   // Current char
    final char NC = m_fb.Get( CL, CP+1 ); // Next char

    // 1. If at end of word, or end of non-word, move to next char
    if( (Utils.IsWord_Ident   ( CC ) && !Utils.IsWord_Ident   ( NC ))
     || (Utils.IsWord_NonIdent( CC ) && !Utils.IsWord_NonIdent( NC )) ) CP++;

    // 2. If on white space, skip past white space
    if( Utils.IsSpace( m_fb.Get(CL, CP) ) )
    {
      for( ; CP<LL && Utils.IsSpace( m_fb.Get(CL, CP) ); CP++ ) ;
      if( LL <= CP ) return null; // Did not find non-white space
    }
    // At this point (CL,CP) should be non-white space
    CC = m_fb.Get( CL, CP );  // Current char

    ncp.crsLine = CL;

    if( Utils.IsWord_Ident( CC ) ) // On identity
    {
      // 3. If on word space, go to end of word space
      for( ; CP<LL && Utils.IsWord_Ident( m_fb.Get(CL, CP) ); CP++ )
      {
        ncp.crsChar = CP;
      }
    }
    else if( Utils.IsWord_NonIdent( CC ) )// On Non-identity, non-white space
    {
      // 4. If on non-white-non-word, go to end of non-white-non-word
      for( ; CP<LL && Utils.IsWord_NonIdent( m_fb.Get(CL, CP) ); CP++ )
      {
        ncp.crsChar = CP;
      }
    }
    else { // Should never get here:
      return null;
    }
    return ncp;
  }
  void Do_cw()
  {
    final int result = Do_dw();

    if     ( result == 1 ) Do_i();
    else if( result == 2 ) Do_a();
  }
  void Do_dd()
  {
    final int ONL = m_fb.NumLines(); // Old number of lines

    // If there is nothing to 'dd', just return:
    if( 1 < ONL )
    {
    //if( m_fb == m_vis.m_views[0].get( m_vis.BE_FILE ).m_fb )
      if( m_vis.Is_BE_FILE( m_fb ) )
      {
        Do_dd_BufferEditor( ONL );
      }
      else {
        Do_dd_Normal( ONL );
      }
    }
  }
  void Do_dd_BufferEditor( final int ONL )
  {
    final int OCL = CrsLine(); // Old cursor line

    // Can only delete user files out of buffer editor
    if( m_vis.USER_FILE <= OCL )
    {
      Line lr = m_fb.GetLine( OCL );

      String fname = lr.toString();

      if( !m_vis.File_Is_Displayed( fname ) )
      {
        m_vis.ReleaseFileName( fname );

        Do_dd_Normal( ONL );
      }
    }
  }
  void Do_dd_Normal( final int ONL )
  {
    final int OCL = CrsLine();           // Old cursor line
    final int OCP = CrsChar();           // Old cursor position
    final int OLL = m_fb.LineLen( OCL ); // Old line length

    final boolean DELETING_LAST_LINE = OCL == ONL-1;

    final int NCL = DELETING_LAST_LINE ? OCL-1 : OCL; // New cursor line
    final int NLL = DELETING_LAST_LINE ? m_fb.LineLen( NCL )
                                       : m_fb.LineLen( NCL + 1 );
    final int NCP = Math.min( OCP, 0<NLL ? NLL-1 : 0 );

    // Remove line from FileBuf and save in paste register:
    Line lr = m_fb.RemoveLine( OCL );

    // m_vis.m_reg will own lp
    m_vis.get_reg().clear();
    m_vis.get_reg().add( lr );
    m_vis.set_paste_mode( Paste_Mode.LINE );

    GoToCrsPos_NoWrite( NCL, NCP );

    m_fb.Update();
  }
  // If nothing was deleted, return 0.
  // If last char on line was deleted, return 2,
  // Else return 1.
  int Do_dw()
  {
    final int NUM_LINES = m_fb.NumLines();

    if( 0< NUM_LINES )
    {
      final int st_line = CrsLine();
      final int st_char = CrsChar();

      final int LL = m_fb.LineLen( st_line );

      // If past end of line, nothing to do
      if( st_char < LL )
      {
        CrsPos ncp = Do_dw_get_fn( st_line, st_char );

        if( null != ncp )
        {
          Do_x_range( st_line, st_char, ncp.crsLine, ncp.crsChar );

          boolean deleted_last_char = ncp.crsChar == LL-1;

          return deleted_last_char ? 2 : 1;
        }
      }
    }
    return 0;
  }
  CrsPos Do_dw_get_fn( final int st_line, final int st_char )
  {
    final int  LL = m_fb.LineLen( st_line );
    final char C  = m_fb.Get( st_line, st_char );

    if( Utils.IsSpace( C )         // On white space
     || ( st_char < Utils.LLM1(LL) // On non-white space before white space
       && Utils.IsSpace( m_fb.Get( st_line, st_char+1 ) ) ) )
    {
      // w:
      CrsPos ncp_w = GoToNextWord_GetPosition();
      if( null != ncp_w && 0 < ncp_w.crsChar ) ncp_w.crsChar--;
      if( null != ncp_w && st_line == ncp_w.crsLine
                        && st_char <= ncp_w.crsChar )
      {
        return ncp_w;
      }
    }
    // if not on white space, and
    // not on non-white space before white space,
    // or fell through, try e:
    CrsPos ncp_e = GoToEndOfWord_GetPosition();
    if( null != ncp_e && st_line == ncp_e.crsLine
                      && st_char <= ncp_e.crsChar )
    {
      return ncp_e;
    }
    return null;
  }
  void Do_x_range( int st_line, int st_char
                 , int fn_line, int fn_char )
  {
    Ptr_Int p_st_line = new Ptr_Int( st_line );
    Ptr_Int p_st_char = new Ptr_Int( st_char );
    Ptr_Int p_fn_line = new Ptr_Int( fn_line );
    Ptr_Int p_fn_char = new Ptr_Int( fn_char );

    Do_x_range_pre( p_st_line, p_st_char, p_fn_line, p_fn_char );

    if( p_st_line.val == p_fn_line.val )
    {
      Do_x_range_single( p_st_line.val, p_st_char.val, p_fn_char.val );
    }
    else {
      Do_x_range_multiple( p_st_line.val, p_st_char.val, p_fn_line.val, p_fn_char.val );
    }
    Do_x_range_post( p_st_line.val, p_st_char.val );
  }
  void Do_x_range_pre( Ptr_Int p_st_line, Ptr_Int p_st_char
                     , Ptr_Int p_fn_line, Ptr_Int p_fn_char )
  {
    if( m_inVisualBlock )
    {
      if( p_fn_line.val < p_st_line.val ) Utils.Swap( p_st_line, p_fn_line );
      if( p_fn_char.val < p_st_char.val ) Utils.Swap( p_st_char, p_fn_char );
    }
    else {
      if( p_fn_line.val < p_st_line.val
       || (p_fn_line.val == p_st_line.val && p_fn_char.val < p_st_char.val) )
      {
        Utils.Swap( p_st_line, p_fn_line );
        Utils.Swap( p_st_char, p_fn_char );
      }
    }
    m_vis.get_reg().clear();
  }
  void Do_x_range_post( final int st_line, final int st_char )
  {
    if( m_inVisualBlock ) m_vis.set_paste_mode( Paste_Mode.BLOCK );
    else                  m_vis.set_paste_mode( Paste_Mode.ST_FN );

    // Try to put cursor at (st_line, st_char), but
    // make sure the cursor is in bounds after the deletion:
    final int NUM_LINES = m_fb.NumLines();
    int ncl = st_line;
    if( NUM_LINES <= ncl ) ncl = NUM_LINES-1;
    final int NLL = m_fb.LineLen( ncl );
    int ncc = 0;
    if( 0<NLL ) ncc = NLL <= st_char ? NLL-1 : st_char;

    GoToCrsPos_NoWrite( ncl, ncc );

    m_inVisualMode = false;

    m_fb.Update(); //<- No need to Undo_v() or Remove_Banner() because of this
  }

  void Do_x_range_single( final int L
                        , final int st_char
                        , final int fn_char )
  {
    final int OLL = m_fb.LineLen( L ); // Original line length

    if( 0<OLL )
    {
      Line nlr = new Line();

      final int P_st = Math.min( st_char, OLL-1 );
      final int P_fn = Math.min( fn_char, OLL-1 );

      int LL = OLL;

      // Dont remove a single line, or else Q wont work right
      for( int P = st_char; st_char < LL && P <= fn_char; P++ )
      {
        nlr.append_c( m_fb.RemoveChar( L, st_char ) );

        LL = m_fb.LineLen( L ); // Removed a char, so re-set LL
      }
      m_vis.get_reg().add( nlr );
    }
  }
  void Do_x_range_multiple( final int st_line
                          , final int st_char
                          , final int fn_line
                          , final int fn_char )
  {
    boolean started_in_middle = false;
    boolean ended___in_middle = false;

    int n_fn_line = fn_line; // New finish line

    for( int L = st_line; L<=n_fn_line; )
    {
      Line nlr = new Line();

      final int OLL = m_fb.LineLen( L ); // Original line length

      int P_st = (L==  st_line) ? Math.min(st_char, OLL-1) : 0;
      int P_fn = (L==n_fn_line) ? Math.min(fn_char, OLL-1) : OLL-1;
      P_st = Math.max( P_st, 0 ); // Make sure P_st and P_fn are
      P_fn = Math.max( P_fn, 0 ); // greater than or equal to zero

      if(   st_line == L && 0    < P_st  ) started_in_middle = true;
      if( n_fn_line == L && P_fn < OLL-1 ) ended___in_middle = true;

      int LL = OLL;

      for( int P = P_st; P_st < LL && P <= P_fn; P++ )
      {
        nlr.append_c( m_fb.RemoveChar( L, P_st ) );

        LL = m_fb.LineLen( L ); // Removed a char, so re-calculate LL
      }
      if( 0 == P_st && OLL-1 == P_fn ) // Removed entire line
      {
        m_fb.RemoveLine( L );
        n_fn_line--;
      }
      else L++;

      m_vis.get_reg().add( nlr );
    }
    if( started_in_middle && ended___in_middle )
    {
      Line lr = m_fb.RemoveLine( st_line+1 );
      m_fb.AppendLineToLine( st_line, lr );
    }
  }
  void Do_D()
  {
    final int NUM_LINES = m_fb.NumLines();
    final int OCL = CrsLine();  // Old cursor line
    final int OCP = CrsChar();  // Old cursor position
    final int OLL = m_fb.LineLen( OCL );  // Old line length

    // If there is nothing to 'D', just return:
    if( 0<NUM_LINES && 0<OLL && OCP<OLL )
    {
      Line lrd = new Line();

      for( int k=OCP; k<OLL; k++ )
      {
        char C = m_fb.RemoveChar( OCL, OCP );
        lrd.append_c( C );
      }
      m_vis.get_reg().clear();
      m_vis.get_reg().add( lrd );
      m_vis.set_paste_mode( Paste_Mode.ST_FN );

      // If cursor is not at beginning of line, move it back one space.
      if( 0<m_crsCol ) m_crsCol--;

      m_fb.Update();
    }
  }
  void Do_J()
  {
    final int NUM_LINES = m_fb.NumLines(); // Number of lines
    final int CL        = CrsLine();       // Cursor line

    final boolean ON_LAST_LINE = ( CL == NUM_LINES-1 );

    if( ON_LAST_LINE || NUM_LINES < 2 ) return;

    GoToEndOfLine();

    Line lr = m_fb.RemoveLine( CL+1 );
    m_fb.AppendLineToLine( CL, lr );

    // Update() is less efficient than only updating part of the screen,
    //   but it makes the code simpler.
    m_fb.Update();
  }
  void Do_p()
  {
    final Paste_Mode PM = m_vis.get_paste_mode();

    if     ( Paste_Mode.ST_FN == PM ) Do_p_or_P_st_fn( Paste_Pos.After );
    else if( Paste_Mode.BLOCK == PM ) Do_p_block();
    else /*( Paste_Mode.LINE  == PM*/ Do_p_line();
  }
  void Do_P()
  {
    final Paste_Mode PM = m_vis.get_paste_mode();

    if     ( Paste_Mode.ST_FN == PM ) Do_p_or_P_st_fn( Paste_Pos.Before );
    else if( Paste_Mode.BLOCK == PM ) Do_P_block();
    else /*( Paste_Mode.LINE  == PM*/ Do_P_line();
  }
  void Do_p_or_P_st_fn( Paste_Pos paste_pos )
  {
    final int N_REG_LINES = m_vis.get_reg().size();

    for( int k=0; k<N_REG_LINES; k++ )
    {
      final int NLL = m_vis.get_reg().get(k).length();  // New line length
      final int OCL = CrsLine();                        // Old cursor line

      if( 0 == k ) // Add to current line
      {
        MoveInBounds_Line();
        final int OLL = m_fb.LineLen( OCL );
        final int OCP = CrsChar();               // Old cursor position

        // If line we are pasting to is zero length, dont paste a space forward
        final int forward = 0<OLL ? ( paste_pos==Paste_Pos.After ? 1 : 0 ) : 0;

        for( int i=0; i<NLL; i++ )
        {
          char C = m_vis.get_reg().get(k).charAt(i);

          m_fb.InsertChar( OCL, OCP+i+forward, C );
        }
        if( 1 < N_REG_LINES && OCP+forward < OLL ) // Move rest of first line onto new line below
        {
          m_fb.InsertLine( OCL+1 );
          for( int i=0; i<(OLL-OCP-forward); i++ )
          {
            char C = m_fb.RemoveChar( OCL, OCP + NLL+forward );
            m_fb.PushChar( OCL+1, C );
          }
        }
      }
      else if( N_REG_LINES-1 == k )
      {
        // Insert a new line if at end of file:
        if( m_fb.NumLines() == OCL+k ) m_fb.InsertLine( OCL+k );

        for( int i=0; i<NLL; i++ )
        {
          char C = m_vis.get_reg().get(k).charAt(i);

          m_fb.InsertChar( OCL+k, i, C );
        }
      }
      else {
        // Put m_reg on line below:
        m_fb.InsertLine( OCL+k, new Line( m_vis.get_reg().get(k) ) );
      }
    }
    // Update current view after other views, so that the cursor will be put back in place
    m_fb.Update();
  }
  void Do_p_block()
  {
    final int OCL = CrsLine();           // Old cursor line
    final int OCP = CrsChar();           // Old cursor position
    final int OLL = m_fb.LineLen( OCL ); // Old line length
    final int ISP = 0<OCP ? OCP+1        // Insert position
                  : ( 0<OLL ? 1:0 );     // If at beginning of line,
                                         // and LL is zero insert at 0,
                                         // else insert at 1
    final int N_REG_LINES = m_vis.get_reg().size();

    for( int k=0; k<N_REG_LINES; k++ )
    {
      if( m_fb.NumLines()<=OCL+k ) m_fb.InsertLine( OCL+k );

      final int LL = m_fb.LineLen( OCL+k );

      if( LL < ISP )
      {
        // Fill in line with white space up to ISP:
        for( int i=0; i<(ISP-LL); i++ )
        {
          // Insert at end of line so undo will be atomic:
          final int NLL = m_fb.LineLen( OCL+k ); // New line length
          m_fb.InsertChar( OCL+k, NLL, ' ' );
        }
      }
      Line reg_line = m_vis.get_reg().get(k);
      final int RLL = reg_line.length();

      for( int i=0; i<RLL; i++ )
      {
        char C = reg_line.charAt(i);

        m_fb.InsertChar( OCL+k, ISP+i, C );
      }
    }
    m_fb.Update();
  }
  void Do_p_line()
  {
    final int OCL = CrsLine();  // Old cursor line

    final int NUM_LINES = m_vis.get_reg().size();

    for( int k=0; k<NUM_LINES; k++ )
    {
      // Put m_reg on line below:
      m_fb.InsertLine( OCL+k+1, new Line( m_vis.get_reg().get(k) ) );
    }
    // Update current view after other views,
    // so that the cursor will be put back in place
    m_fb.Update();
  }
  void Do_P_block()
  {
    final int OCL = CrsLine();  // Old cursor line
    final int OCP = CrsChar();  // Old cursor position

    final int N_REG_LINES = m_vis.get_reg().size();

    for( int k=0; k<N_REG_LINES; k++ )
    {
      if( m_fb.NumLines()<=OCL+k ) m_fb.InsertLine( OCL+k );

      final int LL = m_fb.LineLen( OCL+k );

      if( LL < OCP )
      {
        // Fill in line with white space up to OCP:
        for( int i=0; i<(OCP-LL); i++ ) m_fb.InsertChar( OCL+k, LL, ' ' );
      }
      Line reg_line = m_vis.get_reg().get(k);
      final int RLL = reg_line.length();

      for( int i=0; i<RLL; i++ )
      {
        char C = reg_line.charAt(i);

        m_fb.InsertChar( OCL+k, OCP+i, C );
      }
    }
    m_fb.Update();
  }
  void Do_P_line()
  {
    final int OCL = CrsLine();  // Old cursor line

    final int NUM_LINES = m_vis.get_reg().size();

    for( int k=0; k<NUM_LINES; k++ )
    {
      // Put m_reg on line above:
      m_fb.InsertLine( OCL+k, new Line( m_vis.get_reg().get(k) ) );
    }
    m_fb.Update();
  }
  void Do_s()
  {
    final int CL  = CrsLine();
    final int LL  = m_fb.LineLen( CL );
    final int EOL = 0<LL ? LL-1 : 0;
    final int CP  = CrsChar();

    if( CP < EOL )
    {
      Do_x();
      Do_i();
    }
    else // EOL <= CP
    {
      Do_x();
      Do_a();
    }
  }
  void Do_u()
  {
    m_fb.Undo( this );
  }
  void Do_U()
  {
    m_fb.UndoAll( this );
  }
  void Do_yy()
  {
    // If there is nothing to 'yy', just return:
    if( 0<m_fb.NumLines() )
    {
      // Get a copy of CrsLine() line:
      Line l = new Line( m_fb.GetLine( CrsLine() ) );

      m_vis.get_reg().clear();
      m_vis.get_reg().add( l );

      m_vis.set_paste_mode( Paste_Mode.LINE );
    }
  }

  void Do_yw()
  {
    // If there is nothing to 'yw', just return:
    if( 0<m_fb.NumLines() )
    {
      final int st_line = CrsLine();
      final int st_char = CrsChar();

      // Determine yank up to position:
      CrsPos ncp = Do_dw_get_fn( st_line, st_char );

      if( null != ncp )
      {
        m_vis.get_reg().clear();
        Line l = new Line();

        // st_line and fn_line should be the same
        for( int k=st_char; k<=ncp.crsChar; k++ )
        {
          l.append_c( m_fb.Get( st_line, k ) );
        }
        m_vis.get_reg().add( l );
        m_vis.set_paste_mode( Paste_Mode.ST_FN );
      }
    }
  }

  void MoveCurrLineToTop()
  {
    if( 0<m_crsRow )
    {
      // Make changes manually:
      m_topLine += m_crsRow;
      m_crsRow = 0;
      Set_Console_CrsCell();

      Update();
    }
  }
  void MoveCurrLineCenter()
  {
    final int center = (int)( 0.5*WorkingRows() + 0.5 );

    final int OCL = CrsLine(); // Old cursor line

    if( 0 < OCL && OCL < center && 0 < m_topLine )
    {
      // Cursor line cannot be moved to center, but can be moved closer to center
      // CrsLine() does not change:
      m_crsRow += m_topLine;
      m_topLine = 0;
      Set_Console_CrsCell();

      Update();
    }
    else if( center <= OCL
          && center != m_crsRow )
    {
      m_topLine += m_crsRow - center;
      m_crsRow = center;
      Set_Console_CrsCell();

      Update();
    }
  }
  void MoveCurrLineToBottom()
  {
    if( 0 < m_topLine )
    {
      final int WR  = WorkingRows();
      final int OCL = CrsLine(); // Old cursor line

      if( WR-1 <= OCL )
      {
        m_topLine -= WR - m_crsRow - 1;
        m_crsRow = WR-1;
        Set_Console_CrsCell();

        Update();
      }
      else {
        // Cursor line cannot be moved to bottom, but can be moved closer to bottom
        // CrsLine() does not change:
        m_crsRow += m_topLine;
        m_topLine = 0;
        Set_Console_CrsCell();

        Update();
      }
    }
  }
  void Do_Tilda()
  {
    if( 0==m_fb.NumLines() ) return;

    final int CL = CrsLine(); // Old cursor line
    final int CP = CrsChar(); // Old cursor position
    final int LL  = m_fb.LineLen( CL );

    if( 0<LL && CP<LL )
    {
      char C = m_fb.Get( CL, CP );
      boolean changed = false;
      if     ( Character.isUpperCase( C ) ) { C = Character.toLowerCase( C ); changed = true; }
      else if( Character.isLowerCase( C ) ) { C = Character.toUpperCase( C ); changed = true; }

      final boolean CONT_LAST_UPDATE = true;
      if( m_crsCol < Math.min( LL-1, WorkingCols()-1 ) )
      {
        if( changed ) m_fb.Set( CL, CP, C, CONT_LAST_UPDATE );

        // Need to move cursor right:
        m_crsCol++;
      }
      else if( RightChar() < LL-1 )
      {
        // Need to scroll window right:
        if( changed ) m_fb.Set( CL, CP, C, CONT_LAST_UPDATE );

        m_leftChar++;
      }
      else // RightChar() == LL-1
      {
        // At end of line so cant move or scroll right:
        if( changed ) m_fb.Set( CL, CP, C, CONT_LAST_UPDATE );
      }
      m_fb.Update();
    }
  }
  void GoToOppositeBracket()
  {
    MoveInBounds_Line();

    final int NUM_LINES = m_fb.NumLines();
    final int CL        = CrsLine();
    final int CC        = CrsChar();
    final int LL        = m_fb.LineLen( CL );

    if( 0==NUM_LINES || 0==LL ) return;

    final char C = m_fb.Get( CL, CC );

    if( C=='{' || C=='[' || C=='(' )
    {
      char finish_char = 0;
      if     ( C=='{' ) finish_char = '}';
      else if( C=='[' ) finish_char = ']';
      else if( C=='(' ) finish_char = ')';

      if( 0 != finish_char ) GoToOppositeBracket_Forward( C, finish_char );
    }
    else if( C=='}' || C==']' || C==')' )
    {
      char finish_char = 0;
      if     ( C=='}' ) finish_char = '{';
      else if( C==']' ) finish_char = '[';
      else if( C==')' ) finish_char = '(';

      if( 0 != finish_char ) GoToOppositeBracket_Backward( C, finish_char );
    }
  }
  void GoToOppositeBracket_Forward( final char ST_C, final char FN_C )
  {
    final int NUM_LINES = m_fb.NumLines();
    final int CL = CrsLine();
    final int CC = CrsChar();

    // Search forward
    int     level = 0;
    boolean found = false;

    for( int l=CL; !found && l<NUM_LINES; l++ )
    {
      final int LL = m_fb.LineLen( l );

      for( int p=(CL==l)?(CC+1):0; !found && p<LL; p++ )
      {
        final char C = m_fb.Get( l, p );

        if     ( C==ST_C ) level++;
        else if( C==FN_C )
        {
          if( 0 < level ) level--;
          else {
            found = true;

            GoToCrsPos_Write( l, p );
          }
        }
      }
    }
  }
  void GoToOppositeBracket_Backward( final char ST_C, final char FN_C )
  {
    final int CL = CrsLine();
    final int CC = CrsChar();

    // Search forward
    int     level = 0;
    boolean found = false;

    for( int l=CL; !found && 0<=l; l-- )
    {
      final int LL = m_fb.LineLen( l );

      for( int p=(CL==l)?(CC-1):(LL-1); !found && 0<=p; p-- )
      {
        final char C = m_fb.Get( l, p );

        if     ( C==ST_C ) level++;
        else if( C==FN_C )
        {
          if( 0 < level ) level--;
          else {
            found = true;

            GoToCrsPos_Write( l, p );
          }
        }
      }
    }
  }
  void GoToLeftSquigglyBracket()
  {
    MoveInBounds_Line();

    final char  start_char = '}';
    final char finish_char = '{';

    GoToOppositeBracket_Backward( start_char, finish_char );
  }
  void GoToRightSquigglyBracket()
  {
    MoveInBounds_Line();

    final char  start_char = '{';
    final char finish_char = '}';

    GoToOppositeBracket_Forward( start_char, finish_char );
  }

  String Do_Star_GetNewPattern()
  {
    if( m_fb.NumLines() == 0 ) return "";

    final int CL = CrsLine();
    final int LL = m_fb.LineLen( CL );

    StringBuilder pattern = null;

    if( 0<LL )
    {
      pattern = new StringBuilder();

      MoveInBounds_Line();
      final int  CC = CrsChar();
      final char C  = m_fb.Get( CL,  CC );

      if( Utils.IsIdent( C ) )
      {
        pattern.append( C );

        // Search forward:
        for( int k=CC+1; k<LL; k++ )
        {
          final char c1 = m_fb.Get( CL, k );
          if( Utils.IsIdent( c1 ) ) pattern.append( c1 );
          else                      break;
        }
        // Search backward:
        for( int k=CC-1; 0<=k; k-- )
        {
          final char c2 = m_fb.Get( CL, k );
          if( Utils.IsIdent( c2 ) ) pattern.insert( 0, c2 );
          else                      break;
        }
      }
      else {
        if( !Utils.IsSpace( C ) ) pattern.append( C );
      }
      if( 0<pattern.length() )
      {
        pattern.insert( 0, "\\b" );
        pattern.append(    "\\b" );
      }
    }
    return null != pattern ? pattern.toString() : "";
  }

  void PrintPatterns( final boolean HIGHLIGHT )
  {
    final int NUM_LINES = m_fb.NumLines();
    final int END_LINE  = Math.min( m_topLine+WorkingRows(), NUM_LINES );

    for( int l=m_topLine; l<END_LINE; l++ )
    {
      final int LL      = m_fb.LineLen( l );
      final int END_POS = Math.min( m_leftChar+WorkingCols(), LL );

      for( int p=m_leftChar; p<END_POS; p++ )
      {
        if( InStar( l, p ) )
        {
          Style s = Style.STAR;

          if( !HIGHLIGHT )
          {
            s = Style.NORMAL;
            if     ( InVisualArea(l,p) ) s = Style.VISUAL;
            else if( InDefine    (l,p) ) s = Style.DEFINE;
            else if( InConst     (l,p) ) s = Style.CONST;
            else if( InControl   (l,p) ) s = Style.CONTROL;
            else if( InVarType   (l,p) ) s = Style.VARTYPE;
            else if( InComment   (l,p) ) s = Style.COMMENT;
            else if( InNonAscii  (l,p) ) s = Style.NONASCII;
          }
          final char C = m_fb.Get( l, p );
          m_console.Set( Line_2_GL( l ), Char_2_GL( p ), C, s );
        }
      }
    }
  }
  // Go to next pattern
  void Do_n()
  {
    if( 0 < m_vis.get_regex().length() )
    {
      Do_n_Pattern();
    }
    else if( m_vis.Is_BE_FILE( m_fb ) )
    {
      Do_n_NextDir();
    }
  }
  void Do_n_Pattern()
  {
    if( 0 < m_fb.NumLines() )
    {
      Set_Cmd_Line_Msg( '/' + m_vis.get_regex() );

      // Next cursor position
      CrsPos ncp = Do_n_FindNextPattern();

      if( null != ncp )
      {
        GoToCrsPos_Write( ncp.crsLine, ncp.crsChar );
      }
      else {
        // Pattern not found, so put cursor back in view:
        PrintCursor();
      }
    }
  }
  CrsPos Do_n_FindNextPattern()
  {
    CrsPos ncp = new CrsPos( 0, 0 );

    final int NUM_LINES = m_fb.NumLines();

    final int OCL = CrsLine(); int st_l = OCL;
    final int OCC = CrsChar(); int st_c = OCC;

    boolean found_next_star = false;

    final int LL = m_fb.LineLen( OCL );

    m_fb.Check_4_New_Regex();
    m_fb.Find_Regexs_4_Line( OCL );

    // Move past current pattern:
    for( ; st_c<LL && InStar(OCL,st_c); st_c++ ) ;

    // If at end of current line, go down to next line:
    if( LL <= st_c ) { st_c=0; st_l++; }

    // Search for first pattern position past current position
    for( int l=st_l; !found_next_star && l<NUM_LINES; l++ )
    {
      m_fb.Find_Regexs_4_Line( l );

      final int LL2 = m_fb.LineLen( l );

      for( int p=st_c; !found_next_star && p<LL2; p++ )
      {
        if( InStar(l,p) )
        {
          found_next_star = true;
          ncp.crsLine = l;
          ncp.crsChar = p;
        }
      }
      // After first line, always start at beginning of line
      st_c = 0;
    }
    // Near end of file and did not find any patterns, so go to first pattern in file
    if( !found_next_star )
    {
      for( int l=0; !found_next_star && l<=OCL; l++ )
      {
        m_fb.Find_Regexs_4_Line( l );

        final int LL3 = m_fb.LineLen( l );
        final int END_C = (OCL==l) ? Math.min( OCC, LL3 ) : LL3;

        for( int p=0; !found_next_star && p<END_C; p++ )
        {
          if( InStar(l,p) )
          {
            found_next_star = true;
            ncp.crsLine = l;
            ncp.crsChar = p;
          }
        }
      }
    }
    return found_next_star ? ncp : null;
  }
  void Do_n_NextDir()
  {
    if( 1 < m_fb.NumLines() )
    {
      Set_Cmd_Line_Msg("Searching down for dir");
      Ptr_Int dl = new Ptr_Int( CrsLine() ); // Dir line, changed by search methods below

      boolean found_line = true;

      if( Do_n_NextDir_cursor_on_dir( CrsLine() ) )
      {
        // If currently on a dir, go to next line before searching for dir
        found_line = Do_n_NextDir_Next_Line( dl );
      }
      if( found_line )
      {
        boolean found_dir = Do_n_NextDir_Search_for_Dir( dl );

        if( found_dir )
        {
          final int NCL = dl.val;
          final int NCP = Utils.LLM1( m_fb.LineLen( NCL ) );

          GoToCrsPos_Write( NCL, NCP );
        }
      }
    }
  }
  boolean Do_n_NextDir_cursor_on_dir( final int CL )
  {
    boolean cursor_on_dir = false;

    String fname = m_fb.GetLine( CL ).toString();

    FileBuf fb = m_vis.get_FileBuf( fname );

    if( null != fb && fb.m_isDir )
    {
      cursor_on_dir = true;
    }
    return cursor_on_dir;
  }
//boolean Do_n_NextDir_Search_for_File( Ptr_Int dl )
//{
//  final int NUM_LINES = m_fb.NumLines();
//  final int dl_st = dl.val;
//
//  // Search forward for file line
//  boolean found = false;
//
//  if( 1 < NUM_LINES )
//  {
//    while( !found && dl.val<NUM_LINES )
//    {
//      if( ! Do_n_NextDir_cursor_on_dir( dl.val ) )
//      {
//        found = true;
//      }
//      else dl.val++;
//    }
//    if( !found )
//    {
//      // Wrap around back to top and search again:
//      dl.val = 0;
//      while( !found && dl.val<dl_st )
//      {
//        if( ! Do_n_NextDir_cursor_on_dir( dl.val ) )
//        {
//          found = true;
//        }
//        else dl.val++;
//      }
//    }
//  }
//  return found;
//}
  boolean Do_n_NextDir_Next_Line( Ptr_Int dl )
  {
    final int NUM_LINES = m_fb.NumLines();

    // Search forward for next line
    boolean found = false;

    if( 1 < NUM_LINES )
    {
      dl.val = ( NUM_LINES-1 <= dl.val ) ? 0 : dl.val+1;

      found = true;
    }
    return found;
  }
  boolean Do_n_NextDir_Search_for_Dir( Ptr_Int dl )
  {
    boolean found_dir = false;

    final int NUM_LINES = m_fb.NumLines();
    final int dl_st = dl.val;

    // Search forward from dl_st:
    while( !found_dir && dl.val<NUM_LINES )
    {
      if( Do_n_NextDir_cursor_on_dir( dl.val ) )
      {
        found_dir = true;
      }
      else dl.val++;
    }
    if( !found_dir )
    {
      // Wrap around back to top and search down to dl_st:
      dl.val = 0;
      while( !found_dir && dl.val<dl_st )
      {
        if( Do_n_NextDir_cursor_on_dir( dl.val ) )
        {
          found_dir = true;
        }
        else dl.val++;
      }
    }
    return found_dir;
  }
  // Go to previous pattern
  void Do_N()
  {
    if( 0 < m_vis.get_regex().length() )
    {
      Do_N_Pattern();
    }
    else if( m_vis.Is_BE_FILE( m_fb ) )
    {
      Do_N_PrevDir();
    }
  }
  void Do_N_Pattern()
  {
    if( 0 < m_fb.NumLines() )
    {
      Set_Cmd_Line_Msg( '/' + m_vis.get_regex() );

      // Next cursor position
      CrsPos ncp = Do_N_FindPrevPattern();

      if( null != ncp )
      {
        GoToCrsPos_Write( ncp.crsLine, ncp.crsChar );
      }
      else {
        // Pattern not found, so put cursor back in view:
        PrintCursor();
      }
    }
  }
  CrsPos Do_N_FindPrevPattern()
  {
    CrsPos ncp = new CrsPos( 0, 0 );

    MoveInBounds_Line();

    final int NUM_LINES = m_fb.NumLines();

    final int OCL = CrsLine();
    final int OCC = CrsChar();

    m_fb.Check_4_New_Regex();

    boolean found_prev_star = false;

    // Search for first star position before current position
    for( int l=OCL; !found_prev_star && 0<=l; l-- )
    {
      m_fb.Find_Regexs_4_Line( l );

      final int LL = m_fb.LineLen( l );

      int p=LL-1;
      if( OCL==l ) p = 0<OCC ? OCC-1 : 0;

      for( ; 0<p && !found_prev_star; p-- )
      {
        for( ; 0<=p && InStar(l,p); p-- )
        {
          found_prev_star = true;
          ncp.crsLine = l;
          ncp.crsChar = p;
        }
      }
    }
    // Near beginning of file and did not find any patterns, so go to last pattern in file
    if( !found_prev_star )
    {
      for( int l=NUM_LINES-1; !found_prev_star && OCL<l; l-- )
      {
        m_fb.Find_Regexs_4_Line( l );

        final int LL = m_fb.LineLen( l );

        int p=LL-1;
        if( OCL==l ) p = 0<OCC ? OCC-1 : 0;

        for( ; 0<p && !found_prev_star; p-- )
        {
          for( ; 0<=p && InStar(l,p); p-- )
          {
            found_prev_star = true;
            ncp.crsLine = l;
            ncp.crsChar = p;
          }
        }
      }
    }
    return found_prev_star ? ncp : null;
  }
  void Do_N_PrevDir()
  {
    if( 1 < m_fb.NumLines() )
    {
      Set_Cmd_Line_Msg("Searching up for dir");
      Ptr_Int dl = new Ptr_Int( CrsLine() ); // Dir line, changed by search methods below

      boolean found_line = true;

      if( Do_n_NextDir_cursor_on_dir( CrsLine() ) )
      {
        // If currently on a dir, go to prev line before searching for dir
        found_line = Do_n_PrevDir_Prev_Line( dl );
      }
      if( found_line )
      {
        boolean found_dir = Do_n_PrevDir_Search_for_Dir( dl );

        if( found_dir )
        {
          final int NCL = dl.val;
          final int NCP = Utils.LLM1( m_fb.LineLen( NCL ) );

          GoToCrsPos_Write( NCL, NCP );
        }
      }
    }
  }
  boolean Do_n_PrevDir_Prev_Line( Ptr_Int dl )
  {
    final int NUM_LINES = m_fb.NumLines();

    // Search forward for next line
    boolean found = false;

    if( 1 < NUM_LINES )
    {
      dl.val = ( dl.val <= 0 ) ? NUM_LINES-1 : dl.val-1;

      found = true;
    }
    return found;
  }
  boolean Do_n_PrevDir_Search_for_Dir( Ptr_Int dl )
  {
    boolean found_dir = false;

    final int NUM_LINES = m_fb.NumLines();
    final int dl_st = dl.val;

    // Search backward from dl_st:
    while( !found_dir && 0<=dl.val )
    {
      if( Do_n_NextDir_cursor_on_dir( dl.val ) )
      {
        found_dir = true;
      }
      else dl.val--;
    }
    if( !found_dir )
    {
      // Wrap around back to bottom and search up to dl_st:
      dl.val = NUM_LINES-1;
      while( !found_dir && dl_st<dl.val )
      {
        if( Do_n_NextDir_cursor_on_dir( dl.val ) )
        {
          found_dir = true;
        }
        else dl.val--;
      }
    }
    return found_dir;
  }
  void Do_v()
  {
    m_inVisualBlock = false;
    m_copy_vis_buf_2_dot_buf = false;

    m_vis.get_states().addFirst( m_run_v_end );
    m_vis.get_states().addFirst( m_run_v_mid );
    m_vis.get_states().addFirst( m_run_v_beg );
  }
  void Do_V()
  {
    m_inVisualBlock = true;
    m_copy_vis_buf_2_dot_buf = false;

    m_vis.get_states().addFirst( m_run_v_end );
    m_vis.get_states().addFirst( m_run_v_mid );
    m_vis.get_states().addFirst( m_run_v_beg );
  }
  void run_v_beg()
  {
    MoveInBounds_Line();
    m_inVisualMode = true;
    m_undo_v       = true;
    DisplayBanner();

    v_st_line = CrsLine();  v_fn_line = v_st_line;
    v_st_char = CrsChar();  v_fn_char = v_st_char;

    m_vis.get_states().removeFirst();
  }
  void run_v_mid()
  {
    // m_console.KeysIn() needs to be first because it Sleep()'s
    if( 0<m_console.KeysIn() )
    {
      final char C = m_console.GetKey();

      if     ( C == 'l' ) GoRight(1);
      else if( C == 'h' ) GoLeft(1);
      else if( C == 'j' ) GoDown(1);
      else if( C == 'k' ) GoUp(1);
      else if( C == 'H' ) GoToTopLineInView();
      else if( C == 'L' ) GoToBotLineInView();
      else if( C == 'M' ) GoToMidLineInView();
      else if( C == 'n' ) Do_n();
      else if( C == 'N' ) Do_N();
      else if( C == '0' ) GoToBegOfLine();
      else if( C == '$' ) GoToEndOfLine();
      else if( C == 'g' ) Do_v_Handle_g();
      else if( C == 'G' ) GoToEndOfFile();
      else if( C == 'F' ) PageDown_v();
      else if( C == 'B' ) PageUp_v();
      else if( C == 'b' ) GoToPrevWord();
      else if( C == 'w' ) GoToNextWord();
      else if( C == 'e' ) GoToEndOfWord();
      else if( C == '%' ) GoToOppositeBracket();
      else if( C == 'z' ) Do_z();
      else if( C == 'f' ) Do_f();
      else if( C == ';' ) m_vis.Handle_SemiColon();
      else if( C == 'y' ) { Do_y_v(); }
      else if( C == 'Y' ) { Do_Y_v(); }
      else if( C == 'x'
            || C == 'd' ) { Do_x_v();     m_copy_vis_buf_2_dot_buf = true; }
      else if( C == 'D' ) { Do_D_v();     m_copy_vis_buf_2_dot_buf = true; }
      else if( C == 's' ) { Do_s_v();     m_copy_vis_buf_2_dot_buf = true; }
      else if( C == '~' ) { Do_Tilda_v(); m_copy_vis_buf_2_dot_buf = true; }
      else if( C == ESC ) { m_inVisualMode = false; }
    }
    if( !m_inVisualMode ) m_vis.get_states().removeFirst();
  }
  void run_v_end()
  {
    Remove_Banner();
    Undo_v();

    m_vis.get_states().removeFirst();

    if( !m_console.get_from_dot_buf() )
    {
      m_console.set_save_2_vis_buf( false );

      if( m_copy_vis_buf_2_dot_buf )
      {
        // setLength( 0 ) followed by append() accomplishes copy:
        m_console.copy_vis_buf_2_dot_buf();
      }
    }
  }
  void Undo_v()
  {
    if( m_undo_v )
    {
      m_fb.Update();
    }
  }

  void Do_v_Handle_g()
  {
    m_vis.get_states().addFirst( m_run_g_v );
  }
  void run_g_v()
  {
    if( 0<m_console.KeysIn() )
    {
      final char c2 = m_console.GetKey();

      if     ( c2 == 'g' ) GoToTopOfFile();
      else if( c2 == '0' ) GoToStartOfRow();
      else if( c2 == '$' ) GoToEndOfRow();
      else if( c2 == 'f' ) Do_v_Handle_gf();
      else if( c2 == 'p' ) Do_v_Handle_gp();

      m_vis.get_states().removeFirst();
    }
  }
  void Do_v_Handle_gf()
  {
    if( v_st_line == v_fn_line )
    {
      final int m_v_st_char = v_st_char < v_fn_char ? v_st_char : v_fn_char;
      final int m_v_fn_char = v_st_char < v_fn_char ? v_fn_char : v_st_char;

      m_sb.setLength( 0 );

      for( int P = m_v_st_char; P<=m_v_fn_char; P++ )
      {
        m_sb.append( m_fb.Get( v_st_line, P ) );
      }
      boolean went_to_file = m_vis.GoToBuffer_Fname( m_sb.toString() );

      if( went_to_file )
      {
        // If we made it to buffer indicated by fname, no need to Undo_v() or
        // Remove_Banner() because the whole view pane will be redrawn
        m_inVisualMode = false;
        m_undo_v       = false;
      }
    }
  }
  void Do_v_Handle_gp()
  {
    if( v_st_line == v_fn_line )
    {
      final int m_v_st_char = v_st_char < v_fn_char ? v_st_char : v_fn_char;
      final int m_v_fn_char = v_st_char < v_fn_char ? v_fn_char : v_st_char;

      StringBuilder pattern = new StringBuilder();

      for( int P = m_v_st_char; P<=m_v_fn_char; P++ )
      {
        pattern.append( m_fb.Get( v_st_line, P  ) );
      }
      m_vis.Handle_Slash_GotPattern( pattern.toString(), false );

      m_inVisualMode = false;
    }
  }

  // This one works better when IN visual mode:
  void PageDown_v()
  {
    final int NUM_LINES = m_fb.NumLines();

    if( 0<NUM_LINES )
    {
      final int OCL = CrsLine(); // Old cursor line

      int NCL = OCL + WorkingRows() - 1; // New cursor line

      // Dont let cursor go past the end of the file:
      if( NUM_LINES-1 < NCL ) NCL = NUM_LINES-1;

      GoToCrsPos_Write( NCL, 0 );
    }
  }
  // This one works better when IN visual mode:
  void PageUp_v()
  {
    final int NUM_LINES = m_fb.NumLines();

    if( 0<NUM_LINES )
    {
      final int OCL = CrsLine(); // Old cursor line

      int NCL = OCL - WorkingRows() + 1; // New cursor line

      // Check for underflow:
      if( NCL < 0 ) NCL = 0;

      GoToCrsPos_Write( NCL, 0 );
    }
  }
  void Do_y_v()
  {
    m_vis.get_reg().clear();

    if( m_inVisualBlock ) Do_y_v_block();
    else                  Do_y_v_st_fn();

    m_inVisualMode = false;
  }
  void Do_y_v_block()
  {
    final int old_v_st_line = v_st_line;
    final int old_v_st_char = v_st_char;

    Swap_Visual_Block_If_Needed();

    for( int L=v_st_line; L<=v_fn_line; L++ )
    {
      Line nlr = new Line();

      final int LL = m_fb.LineLen( L );

      for( int P = v_st_char; P<LL && P <= v_fn_char; P++ )
      {
        nlr.append_c( m_fb.Get( L, P ) );
      }
      m_vis.get_reg().add( nlr );
    }
    m_vis.set_paste_mode( Paste_Mode.BLOCK );

    // Try to put cursor at (old_v_st_line, old_v_st_char), but
    // make sure the cursor is in bounds after the deletion:
    final int NUM_LINES = m_fb.NumLines();
    int ncl = old_v_st_line;
    if( NUM_LINES <= ncl ) ncl = NUM_LINES-1;
    final int NLL = m_fb.LineLen( ncl );
    int ncc = 0;
    if( 0<NLL ) ncc = NLL <= old_v_st_char ? NLL-1 : old_v_st_char;

    GoToCrsPos_NoWrite( ncl, ncc );
  }
  void Do_Y_v()
  {
    m_vis.get_reg().clear();

    if( m_inVisualBlock ) Do_y_v_block();
    else                  Do_Y_v_st_fn();

    m_inVisualMode = false;
  }
  void Do_x_v()
  {
    if( m_inVisualBlock )
    {
      Do_x_range_block( v_st_line, v_st_char, v_fn_line, v_fn_char );
    }
    else {
      Do_x_range( v_st_line, v_st_char, v_fn_line, v_fn_char );
    }
    m_inVisualMode = false;

    Remove_Banner();
  }
  void Do_D_v()
  {
    if( m_inVisualBlock )
    {
      Do_x_range_block( v_st_line, v_st_char, v_fn_line, v_fn_char );
      Remove_Banner();
    }
    else {
      Do_D_v_line();
    }
    m_inVisualMode = false;
  }
//void Do_s_v()
//{
//  final int LL = m_fb.LineLen( CrsLine() );
//  final boolean
//  CURSOR_AT_END_OF_LINE = 0<v_st_char
//                       && 0<LL ? LL-1 <= CrsChar() : false;
//  Do_x_v();
//
//  if( m_inVisualBlock )
//  {
//    if( CURSOR_AT_END_OF_LINE ) Do_a_vb();
//    else                        Do_i_vb();
//  }
//  else {
//    if( CURSOR_AT_END_OF_LINE ) Do_a();
//    else                        Do_i();
//  }
//  m_inVisualMode = false;
//}
  void Do_s_v()
  {
    final int LL = m_fb.LineLen( CrsLine() );
    final boolean
    CURSOR_AT_END_OF_LINE = 0<v_st_char
                         && 0<v_fn_char
                         && 0<LL ? (LL-1 <= v_st_char
                                 || LL-1 <= v_fn_char)
                                 : false;
    Do_x_v();

    if( m_inVisualBlock )
    {
      if( CURSOR_AT_END_OF_LINE ) Do_a_vb();
      else                        Do_i_vb();
    }
    else {
      if( CURSOR_AT_END_OF_LINE ) Do_a();
      else                        Do_i();
    }
    m_inVisualMode = false;
  }
  void Do_y_v_st_fn()
  {
    Swap_Visual_St_Fn_If_Needed();

    for( int L=v_st_line; L<=v_fn_line; L++ )
    {
      Line nlr = new Line();

      final int LL = m_fb.LineLen( L );
      if( 0<LL ) {
        final int P_st = (L==v_st_line) ? v_st_char : 0;
        final int P_fn = (L==v_fn_line) ? Math.min(LL-1,v_fn_char) : LL-1;

        for( int P = P_st; P <= P_fn; P++ )
        {
          nlr.append_c( m_fb.Get( L, P ) );
        }
      }
      m_vis.get_reg().add( nlr );
    }
    m_vis.set_paste_mode( Paste_Mode.ST_FN );
  }
  void Do_Y_v_st_fn()
  {
    m_vis.get_reg().clear();

    if( v_fn_line < v_st_line )
    {
      // Visual mode went backwards over multiple lines
      int T = v_st_line; v_st_line = v_fn_line; v_fn_line = v_st_line;
    }
    for( int L=v_st_line; L<=v_fn_line; L++ )
    {
      Line nlr = new Line();

      final int LL = m_fb.LineLen(L);

      if( 0<LL )
      {
        for( int P = 0; P <= LL-1; P++ )
        {
          nlr.append_c( m_fb.Get( L, P ) );
        }
      }
      m_vis.get_reg().add( nlr );
    }
    m_vis.set_paste_mode( Paste_Mode.LINE );
  }
  void Do_x_range_block( int st_line, int st_char
                       , int fn_line, int fn_char )
  {
    Ptr_Int p_st_line = new Ptr_Int( st_line );
    Ptr_Int p_st_char = new Ptr_Int( st_char );
    Ptr_Int p_fn_line = new Ptr_Int( fn_line );
    Ptr_Int p_fn_char = new Ptr_Int( fn_char );

    Do_x_range_pre( p_st_line, p_st_char, p_fn_line, p_fn_char );

    for( int L = p_st_line.val; L<=p_fn_line.val; L++ )
    {
      Line nlr = new Line();

      final int LL = m_fb.LineLen( L );

      for( int P = p_st_char.val; P<LL && P <= p_fn_char.val; P++ )
      {
        nlr.append_c( m_fb.RemoveChar( L, p_st_char.val ) );
      }
      m_vis.get_reg().add( nlr );
    }
    Do_x_range_post( p_st_line.val, p_st_char.val );
  }
  void Do_D_v_line()
  {
    if( v_fn_line < v_st_line )
    {
      int T = v_st_line; v_st_line = v_fn_line; v_fn_line = T;
          T = v_st_char; v_st_char = v_fn_char; v_fn_char = T;
    }
    m_vis.get_reg().clear();

    boolean removed_line = false;
    // 1. If v_st_line==0, fn_line will go negative in the loop below,
    //    so use int's instead of unsigned's
    // 2. Dont remove all lines in file to avoid crashing
    int fn_line = v_fn_line;
    for( int L = v_st_line; 1 < m_fb.NumLines() && L<=fn_line; fn_line-- )
    {
      Line lr = m_fb.RemoveLine( L );
      m_vis.get_reg().add( lr );

      removed_line = true;
    }
    m_vis.set_paste_mode( Paste_Mode.LINE );

    m_inVisualMode = false;
  //Remove_Banner();
    // D'ed lines will be removed, so no need to Undo_v()

    if( removed_line )
    {
      // Figure out and move to new cursor position:
      final int NUM_LINES = m_fb.NumLines();
      final int OCL       = CrsLine(); // Old cursor line

      int ncl = v_st_line;
      if( NUM_LINES-1 < ncl ) ncl = 0<v_st_line ? v_st_line-1 : 0;

      final int NCLL = m_fb.LineLen( ncl );
      int ncc = 0;
      if( 0<NCLL ) ncc = v_st_char < NCLL ? v_st_char : NCLL-1;

      GoToCrsPos_NoWrite( ncl, ncc );

      m_fb.Update();
    }
  }
  void Do_a_vb()
  {
    final int CL = CrsLine();
    final int LL = m_fb.LineLen( CL );
    if( 0==LL ) { Do_i_vb(); return; }

    final boolean CURSOR_AT_EOL = ( CrsChar() == LL-1 );
    if( CURSOR_AT_EOL )
    {
      GoToCrsPos_NoWrite( CL, LL );
    }
    final boolean CURSOR_AT_RIGHT_COL = ( m_crsCol == WorkingCols()-1 );

    if( CURSOR_AT_RIGHT_COL )
    {
      // Only need to scroll window right, and then enter insert i:
      m_leftChar++; //< This increments CrsChar()
    }
    else if( !CURSOR_AT_EOL ) // If cursor was at EOL, already moved cursor forward
    {
      // Only need to move cursor right, and then enter insert i:
      m_crsCol += 1; //< This increments CrsChar()
    }
    m_fb.Update();

    Do_i_vb();
  }
  void Do_i_vb()
  {
    m_vis.get_states().addFirst( m_run_i_vb_end );
    m_vis.get_states().addFirst( m_run_i_vb_mid );
    m_vis.get_states().addFirst( m_run_i_vb_beg );
  }
  void run_i_vb_beg()
  {
    m_inInsertMode = true;
    DisplayBanner();

    m_i_count = 0;

    m_vis.get_states().removeFirst();
  }
  void run_i_vb_mid()
  {
    if( 0<m_console.KeysIn() )
    {
      final char C = m_console.GetKey();

      if( C == ESC )
      {
        m_vis.get_states().removeFirst(); // Done
      }
      else if( Utils.IsEndOfLineDelim( C ) )
      {
        ; // Ignore end of line delimiters
      }
      else if( BS  == C || DEL == C )
      {
        if( 0<m_i_count )
        {
          InsertBackspace_vb();
          m_i_count--;
          m_fb.Update();
        }
      }
      else {
        InsertAddChar_vb( C );
        m_i_count++;
        m_fb.Update();
      }
    }
  }
  void run_i_vb_end()
  {
    Remove_Banner();
    m_inInsertMode = false;

    m_vis.get_states().removeFirst();
  }

  void Do_Tilda_v()
  {
    if( v_fn_line < v_st_line
     || (v_fn_line == v_st_line && v_fn_char < v_st_char) )
    {
      int T = v_st_line; v_st_line = v_fn_line; v_fn_line = T;
          T = v_st_char; v_st_char = v_fn_char; v_fn_char = T;
    }
    if( m_inVisualBlock ) Do_Tilda_v_block();
    else                  Do_Tilda_v_st_fn();

    m_inVisualMode = false;
  }
  void Do_Tilda_v_st_fn()
  {
    for( int L = v_st_line; L<=v_fn_line; L++ )
    {
      final int LL = m_fb.LineLen( L );
      final int P_st = (L==v_st_line) ? v_st_char : 0;
      final int P_fn = (L==v_fn_line) ? v_fn_char : LL-1;

      for( int P = P_st; P <= P_fn; P++ )
      {
        char C = m_fb.Get( L, P );
        boolean changed = false;
        if     ( Utils.IsUpper( C ) ) { C = Utils.ToLower( C ); changed = true; }
        else if( Utils.IsLower( C ) ) { C = Utils.ToUpper( C ); changed = true; }
        if( changed ) m_fb.Set( L, P, C, true );
      }
    }
  }
  void Do_Tilda_v_block()
  {
    for( int L = v_st_line; L<=v_fn_line; L++ )
    {
      final int LL = m_fb.LineLen( L );

      for( int P = v_st_char; P<LL && P <= v_fn_char; P++ )
      {
        char C = m_fb.Get( L, P );
        boolean changed = false;
        if     ( Utils.IsUpper( C ) ) { C = Utils.ToLower( C ); changed = true; }
        else if( Utils.IsLower( C ) ) { C = Utils.ToUpper( C ); changed = true; }
        if( changed ) m_fb.Set( L, P, C, true );
      }
    }
  }

  void InsertBackspace_vb()
  {
    final int OCL = CrsLine();  // Old cursor line
    final int OCP = CrsChar();  // Old cursor position

    if( 0<OCP )
    {
      final int N_REG_LINES = m_vis.get_reg().size();

      for( int k=0; k<N_REG_LINES; k++ )
      {
        m_fb.RemoveChar( OCL+k, OCP-1 );
      }
      GoToCrsPos_NoWrite( OCL, OCP-1 );
    }
  }
  void InsertAddChar_vb( final char C )
  {
    final int OCL = CrsLine();  // Old cursor line
    final int OCP = CrsChar();  // Old cursor position

    final int N_REG_LINES = m_vis.get_reg().size();

    for( int k=0; k<N_REG_LINES; k++ )
    {
      final int LL = m_fb.LineLen( OCL+k );

      if( LL < OCP )
      {
        // Fill in line with white space up to OCP:
        for( int i=0; i<(OCP-LL); i++ )
        {
          // Insert at end of line so undo will be atomic:
          final int NLL = m_fb.LineLen( OCL+k ); // New line length
          m_fb.InsertChar( OCL+k, NLL, ' ' );
        }
      }
      m_fb.InsertChar( OCL+k, OCP, C );
    }
    GoToCrsPos_NoWrite( OCL, OCP+1 );
  }
  void Do_z()
  {
    m_vis.get_states().addFirst( m_run_z );
  }
  void run_z()
  {
    if( 0<m_console.KeysIn() )
    {
      final char c2 = m_console.GetKey();

      if( c2 == 't' || Utils.IsEndOfLineDelim( c2 ) )
      {
        MoveCurrLineToTop();
      }
      else if( c2 == 'z' )
      {
        MoveCurrLineCenter();
      }
      else if( c2 == 'b' )
      {
        MoveCurrLineToBottom();
      }
      m_vis.get_states().removeFirst();
    }
  }
  void Do_f()
  {
    m_vis.get_states().addFirst( m_run_f );
  }
  void run_f()
  {
    if( 0<m_console.KeysIn() )
    {
      m_vis.set_fast_char( m_console.GetKey() );

      Do_semicolon( m_vis.get_fast_char() );

      m_vis.get_states().removeFirst();
    }
  }
  void Do_semicolon( final char FAST_CHAR )
  {
    final int NUM_LINES = m_fb.NumLines();

    if( 0<NUM_LINES )
    {
      final int OCL = CrsLine();           // Old cursor line
      final int LL  = m_fb.LineLen( OCL ); // Line length
      final int OCP = CrsChar();           // Old cursor position

      if( OCP < Utils.LLM1(LL) )
      {
        int NCP = 0;
        boolean found_char = false;
        for( int p=OCP+1; !found_char && p<LL; p++ )
        {
          final char C = m_fb.Get( OCL, p );

          if( C == FAST_CHAR )
          {
            NCP = p;
            found_char = true;
          }
        }
        if( found_char )
        {
          GoToCrsPos_Write( OCL, NCP );
        }
      }
    }
  }

  boolean Has_Context()
  {
    return 0 != m_topLine
        || 0 != m_leftChar
        || 0 != m_crsRow
        || 0 != m_crsCol ;
  }
  void Set_Context( View vr )
  {
    m_topLine  = vr.m_topLine ;
    m_leftChar = vr.m_leftChar;
    m_crsRow   = vr.m_crsRow  ;
    m_crsCol   = vr.m_crsCol  ;
  }
  void Set_Context( final int topLine
                  , final int leftChar
                  , final int crsRow
                  , final int crsCol )
  {
    m_topLine  = topLine ;
    m_leftChar = leftChar;
    m_crsRow   = crsRow  ;
    m_crsCol   = crsCol  ;
  }
  void Clear_Context()
  {
    m_topLine  = 0;
    m_leftChar = 0;
    m_crsRow   = 0;
    m_crsCol   = 0;
  }
  void Check_Context()
  {
    final int NUM_LINES = m_fb.NumLines();

    if( 0 == NUM_LINES )
    {
      Clear_Context();
    }
    else {
      boolean changed = false;
      int CL = CrsLine();

      if( NUM_LINES <= CrsLine() )
      {
        CL = NUM_LINES-1;
        changed = true;
      }
      final int LL = m_fb.LineLen( CL );
      int CP = CrsChar();
      if( LL <= CP )
      {
        CP = Utils.LLM1(LL);
        changed = true;
      }
      if( changed )
      {
        GoToCrsPos_NoWrite( CL, CP );
      }
    }
  }

  void InsertedLine_Adjust_TopLine( final int l_num )
  {
    if( l_num < m_topLine ) m_topLine++;
  }
  void RemovedLine_Adjust_TopLine( final int l_num )
  {
    if( l_num < m_topLine ) m_topLine--;

    if( m_fb.NumLines() <= CrsLine() )
    {
      // Only one line is removed at a time, so just decrementing should work:
      if     ( 0<m_crsRow  ) m_crsRow--;
      else if( 0<m_topLine ) m_topLine--;
    }
  }
  void Swap_Visual_St_Fn_If_Needed()
  {
    if( v_fn_line < v_st_line
     || (v_fn_line == v_st_line && v_fn_char < v_st_char) )
    {
      // Visual mode went backwards over multiple lines, or
      // Visual mode went backwards over one line
      int T = v_st_line; v_st_line = v_fn_line; v_fn_line = T;
          T = v_st_char; v_st_char = v_fn_char; v_fn_char = T;
    }
  }
  void Swap_Visual_Block_If_Needed()
  {
    if( v_fn_line < v_st_line )
    {
      int T = v_st_line; v_st_line = v_fn_line; v_fn_line = T;
    }
    if( v_fn_char < v_st_char )
    {
      int T = v_st_char; v_st_char = v_fn_char; v_fn_char = T;
    }
  }
  void Set_Cmd_Line_Msg( String msg )
  {
    m_cmd_line_sb.setLength( 0 );
    m_cmd_line_sb.append( msg );
  }

  static final char BS  =   8; // Backspace
  static final char ESC =  27; // Escape
  static final char DEL = 127; // Delete

  VisIF     m_vis;
  FileBuf   m_fb;
  ConsoleIF m_console;
  private int m_x;        // Top left x-position of buffer view in parent window
  private int m_y;        // Top left y-position of buffer view in parent window
  private int m_topLine;  // top  of buffer view line number.
  private int m_leftChar; // left of buffer view character number.
  private int m_crsRow;// cursor row    in buffer view. 0 <= m_crsRow < WorkingRows().
  private int m_crsCol;// cursor column in buffer view. 0 <= m_crsCol < WorkingCols().
  StringBuilder m_sb = new StringBuilder();
  StringBuilder m_cmd_line_sb = new StringBuilder();
  boolean  m_inInsertMode; // true if in insert  mode, else false
  boolean  m_inReplaceMode;
  private
  boolean  m_inVisualMode ;
  boolean  m_inVisualBlock;
  boolean  m_copy_vis_buf_2_dot_buf;
  boolean  m_unsaved_changes;
  boolean  m_changed_externally;
  boolean  m_undo_v;
  boolean  m_in_diff;  // True if this view is being diffed
  int      v_st_line;  // Visual start line number
  int      v_st_char;  // Visual start char number on line
  int      v_fn_line;  // Visual ending line number
  int      v_fn_char;  // Visual ending char number on line
  Tile_Pos m_tile_pos = Tile_Pos.FULL;
  private int m_num_cols; // number of rows in buffer view
  private int m_num_rows; // number of columns in buffer view
  private int m_i_count;

  Thread m_run_i_beg    = new Thread() { public void run() { run_i_beg   (); m_vis.Give(); } };
  Thread m_run_i_mid    = new Thread() { public void run() { run_i_mid   (); m_vis.Give(); } };
  Thread m_run_i_end    = new Thread() { public void run() { run_i_end   (); m_vis.Give(); } };
  Thread m_run_R_beg    = new Thread() { public void run() { run_R_beg   (); m_vis.Give(); } };
  Thread m_run_R_mid    = new Thread() { public void run() { run_R_mid   (); m_vis.Give(); } };
  Thread m_run_R_end    = new Thread() { public void run() { run_R_end   (); m_vis.Give(); } };
  Thread m_run_v_beg    = new Thread() { public void run() { run_v_beg   (); m_vis.Give(); } };
  Thread m_run_v_mid    = new Thread() { public void run() { run_v_mid   (); m_vis.Give(); } };
  Thread m_run_v_end    = new Thread() { public void run() { run_v_end   (); m_vis.Give(); } };
  Thread m_run_i_vb_beg = new Thread() { public void run() { run_i_vb_beg(); m_vis.Give(); } };
  Thread m_run_i_vb_mid = new Thread() { public void run() { run_i_vb_mid(); m_vis.Give(); } };
  Thread m_run_i_vb_end = new Thread() { public void run() { run_i_vb_end(); m_vis.Give(); } };
  Thread m_run_g_v      = new Thread() { public void run() { run_g_v     (); m_vis.Give(); } };
  Thread m_run_z        = new Thread() { public void run() { run_z       (); m_vis.Give(); } };
  Thread m_run_f        = new Thread() { public void run() { run_f       (); m_vis.Give(); } };
}

// Run threads using lambdas.
// The syntax is more concise, but according to my research,
// a new is done every time the lambda is called because the lambda
// captures a method outside the lambda, so dont use for now.
//Thread   m_run_i_beg    = new Thread( ()->run_i_beg   () );
//Thread   m_run_i_mid    = new Thread( ()->run_i_mid   () );
//Thread   m_run_i_end    = new Thread( ()->run_i_end   () );
//Thread   m_run_R_beg    = new Thread( ()->run_R_beg   () );
//Thread   m_run_R_mid    = new Thread( ()->run_R_mid   () );
//Thread   m_run_R_end    = new Thread( ()->run_R_end   () );
//Thread   m_run_v_beg    = new Thread( ()->run_v_beg   () );
//Thread   m_run_v_mid    = new Thread( ()->run_v_mid   () );
//Thread   m_run_v_end    = new Thread( ()->run_v_end   () );
//Thread   m_run_i_vb_beg = new Thread( ()->run_i_vb_beg() );
//Thread   m_run_i_vb_mid = new Thread( ()->run_i_vb_mid() );
//Thread   m_run_i_vb_end = new Thread( ()->run_i_vb_end() );
//Thread   m_run_g_v      = new Thread( ()->run_g_v     () );
//Thread   m_run_z        = new Thread( ()->run_z       () );
//Thread   m_run_f        = new Thread( ()->run_f       () );

