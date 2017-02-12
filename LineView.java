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
import java.nio.file.Paths;
import java.util.ArrayList;

class LineView
{
  LineView( VisIF vis, FileBuf fb, ConsoleIF console, final char banner_delim )
  {
    m_vis          = vis;
    m_fb           = fb;
    m_console      = console;
    m_num_rows     = m_console.Num_Rows();
    m_num_cols     = m_console.Num_Cols();
    m_banner_delim = banner_delim;
  }
  int WorkingCols() { return m_num_cols-2-m_prefix_len; }
  int CrsLine()     { return m_topLine; }
  int CrsChar()     { return m_leftChar + m_crsCol; }
  int RightChar()   { return m_leftChar + WorkingCols()-1; }
  int BotLine()     { return m_topLine; }
  int TopLine()     { return m_topLine; }
  int LeftChar()    { return m_leftChar; }
  int CrsCol()      { return m_crsCol; }

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
    return m_x + 1 + m_prefix_len + win_col;
  }

  // Translates zero based file line char position to zero based global column
  int Char_2_GL( final int line_char )
  {
    return m_x + 1 + m_prefix_len - m_leftChar + line_char;
  }

  void SetContext( final int num_cols
                 , final int x
                 , final int y )
  {
    m_num_cols = num_cols;
    m_x        = x;
    m_y        = y;
    m_leftChar = 0;
  }

  void Set_crsCol( final int col )
  {
    Clear_Console_CrsCell();

    m_crsCol = col;

    Set_Console_CrsCell();
  }

  void Clear_Console_CrsCell()
  {
    // Set console current cursor cell to non-cursor hightlighted value:
    final int CL = CrsLine();
    final int CC = CrsChar();
    final int LL = m_fb.LineLen( CL );

    // For readability, display carriage return at end of line as a space
    final char  C = m_fb.Get( CL, CC );
    final Style S = Get_Style( CL, CC );

    m_console.Set( m_y, Col_Win_2_GL( m_crsCol ), C, S );
  }
  void Set_Console_CrsCell()
  {
    // Set console current cursor cell to non-cursor hightlighted value:
    final int CL = CrsLine();
    final int CC = CrsChar();

    final char C = m_fb.Get( CL, CC );

    m_console.Set( m_y, Col_Win_2_GL( m_crsCol ), C, Style.CURSOR );
  }

  void ClearLine()
  {
    for( int col=0; col<m_num_cols-2; col++ )
    {
      m_console.Set( m_y, m_x + 1 + col, ' ', Style.NORMAL );
    }
  }
//void ClearLine()
//{
//  final int LL  = m_fb.LineLen( m_topLine );
//  final int LEN = Math.min( m_prefix_len+LL, m_num_cols-2 );
//
//  for( int col=0; col<LEN; col++ )
//  {
//    m_console.Set( m_y, m_x + 1 + col, ' ', Style.NORMAL );
//  }
//}
  void Update()
  {
    m_fb.Find_Styles( m_topLine + WORKING_ROWS );
    m_fb.Find_Regexs( m_topLine, WORKING_ROWS );

    RepositionView();
    DisplayBanner();
    PrintWorkingView();

    PrintCursor(); // Does m_console.Update()
  }

  void RepositionView()
  {
    // If a window re-size has taken place, and the window has gotten
    // smaller, change top line and left char if needed, so that the
    // cursor is in the window boundaries when it is re-drawn,
    // and cursor position is preserved:
    final int x_adjust = m_crsCol - (WorkingCols()-1);

    if( 0 < x_adjust )
    {
      m_leftChar += x_adjust;
      m_crsCol   -= x_adjust;
    }
  }

  void PrintWorkingView()
  {
    final int WC    = WorkingCols();

    // Dont allow line wrap:
    final int k     = m_topLine;
    final int LL    = m_fb.LineLen( k );
    final int G_ROW = m_y;

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

  // Moves cursor into position on screen:
  //
  void PrintCursor()
  {
    Set_Console_CrsCell();

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
    if( 0<m_fb.NumLines() )
    {
      final int OCL = CrsLine(); // Old cursor line

      GoToCrsPos_Write( OCL, 0 );
    }
  }

  void GoToEndOfLine()
  {
    if( 0<m_fb.NumLines() )
    {
      final int LL  = m_fb.LineLen( CrsLine() );
      final int OCL = CrsLine(); // Old cursor line

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

    if( m_fb.NumLines() <= NCL )
    {
      // Cant move to NCL so just put cursor back where is was
      PrintCursor(); // Does m_console.Update()
    }
    else {
      GoToCrsPos_Write( NCL, 0 );
    }
  }

  void GoToTopOfFile()
  {
    GoToCrsPos_Write( 0, 0 );
  }

  void GoToEndOfFile()
  {
    final int NUM_LINES = m_fb.NumLines();

    if( 0<NUM_LINES )
    {
      GoToCrsPos_Write( NUM_LINES-1, 0 );
    }
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

  void DisplayBanner()
  {
    if( m_console.get_from_dot_buf() ) return;

    final int G_COL = m_x + 1;

    if( m_inInsertMode )
    {
      m_console.Set( m_y, G_COL, 'I', Style.BANNER );
    }
    else if( m_inReplaceMode )
    {
      m_console.Set( m_y, G_COL, 'R', Style.CONST );
    }
    else if( m_inVisualMode )
    {
      m_console.Set( m_y, G_COL, 'V', Style.DEFINE );
    }
    else {
      m_console.Set( m_y, G_COL, 'E', Style.CONTROL );
    }
    m_console.Set( m_y, G_COL+1, m_banner_delim, Style.CONTROL );
    m_console.Update();
  }

  void DisplayBanner_PrintCursor()
  {
    DisplayBanner();

    PrintCursor();
  }

  void GoToCrsPos_Write( int ncp_crsLine
                       , int ncp_crsChar )
  {
    // Limit range of ncp_crsLine to [ 0 to m_fb.NumLines()-1 ]
    ncp_crsLine = Math.max( ncp_crsLine, 0 );
    ncp_crsLine = Math.min( ncp_crsLine, m_fb.NumLines()-1 );

    final int OCP = CrsChar();
    final int NCL = ncp_crsLine;
    final int NCP = ncp_crsChar;

    if( m_topLine == NCL && OCP == NCP )
    {
      // Not moving to new cursor line so just put cursor back where is was
      PrintCursor(); // Does m_console.Update()
    }
    else {
      if( m_inVisualMode )
      {
        v_fn_line = NCL;
        v_fn_char = NCP;
      }
      // These moves refer to View of buffer:
      final boolean MOVE_UP_DN = NCL != m_topLine;
      final boolean MOVE_RIGHT = RightChar() < NCP;
      final boolean MOVE_LEFT  = NCP < m_leftChar;

      final boolean redraw = MOVE_UP_DN || MOVE_RIGHT || MOVE_LEFT;

      if( redraw )
      {
        m_topLine = NCL;

        if     ( MOVE_RIGHT ) m_leftChar = NCP - WorkingCols() + 1;
        else if( MOVE_LEFT  ) m_leftChar = NCP;

        Set_crsCol( NCP - m_leftChar );

        Update();
      }
      else {
        if( m_inVisualMode )
        {
          GoToCrsPos_Write_Visual( OCP, NCP );
        }
        else {
          // m_crsRow and m_crsCol must be set to new values before calling PrintCursor
          Set_crsCol( NCP - m_leftChar );
        }
        PrintCursor();  // Does m_console.Update();
      }
    }
  }

  void GoToCrsPos_NoWrite( final int ncp_crsLine
                         , final int ncp_crsChar )
  {
    m_topLine = ncp_crsLine;

    // These moves refer to View of buffer:
    final boolean MOVE_RIGHT = RightChar() < ncp_crsChar;
    final boolean MOVE_LEFT  = ncp_crsChar < m_leftChar;

    if     ( MOVE_RIGHT ) m_leftChar = ncp_crsChar - WorkingCols() + 1;
    else if( MOVE_LEFT  ) m_leftChar = ncp_crsChar;

    m_crsCol = ncp_crsChar - m_leftChar;
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

  void GoToCrsPos_Write_Visual( final int OCP
                              , final int NCP )
  {
    // (old cursor pos) < (new cursor pos)
    final boolean OCP_LT_NCP = OCP < NCP;

    if( OCP_LT_NCP ) // Cursor moved forward
    {
      GoToCrsPos_WV_Forward( OCP, NCP );
    }
    else // NCP_LT_OCP // Cursor moved backward
    {
      GoToCrsPos_WV_Backward( OCP, NCP );
    }
    Set_crsCol( NCP - m_leftChar );

    PrintCursor(); // Does m_console.Update()
  }

  // Cursor is moving forward
  // Write out from (OCL,OCP) up to but not including (NCL,NCP)
  void GoToCrsPos_WV_Forward( final int OCP
                            , final int NCP )
  {
    final int CL = CrsLine();

    for( int k=OCP; k<NCP; k++ )
    {
      final char C = m_fb.Get( CL, k );

      m_console.Set( m_y, Char_2_GL( k ), C, Get_Style(CL,k) );
    }
  }

  // Cursor is moving backwards
  // Write out from (OCL,OCP) back to but not including (NCL,NCP)
  void GoToCrsPos_WV_Backward( final int OCP
                             , final int NCP )
  {
    final int CL = CrsLine();
    final int LL = m_fb.LineLen( CL ); // Line length

    if( 0<LL )
    {
      final int START = Math.min( OCP, LL-1 );

      for( int k=START; NCP<k; k-- )
      {
        final char C = m_fb.Get( CL, k );

        m_console.Set( m_y, Char_2_GL( k ), C, Get_Style(CL,k) );
      }
    }
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
      return InVisualStFn ( line, pos );
    }
    return false;
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

  // If past end of line, move back to end of line.
  // Returns true if moved, false otherwise.
  //
  boolean MoveInBounds()
  {
    final int CL  = CrsLine();
    final int LL  = m_fb.LineLen( CL );
    final int EOL = 0<LL ? LL-1 : 0;

    if( EOL < CrsChar() ) // Since cursor is now allowed past EOL,
    {                      // it may need to be moved back:
      GoToCrsPos_NoWrite( CL, EOL );
      return true;
    }
    return false;
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
        m_fb.UpdateCmd();
      }
      else if( CC < LL )
      {
        GoToCrsPos_NoWrite( CL, CC+1 );
        m_fb.UpdateCmd();
      }
    }
    Do_i();
  }

  void Do_i()
  {
    m_vis.get_states().addFirst( m_run_i_end );
    m_vis.get_states().addFirst( m_run_i_beg );
  }
  void run_i_beg()
  {
    if( 0 == m_fb.NumLines() ) m_fb.PushLine();

    m_inInsertMode = true;
    Update(); //< Clear any possible message left on command line

    final int LL = m_fb.LineLen( CrsLine() );  // Line length

    // For user friendlyness, move cursor to new position immediately:
    // Since cursor is now allowed past EOL, it may need to be moved back:
    GoToCrsPos_Write( CrsLine(), LL < CrsChar() ? LL : CrsChar() );

    m_i_count     = 0;
    m_i_EOL_delim = false;

    m_vis.get_states().removeFirst();

    final boolean CURSOR_AT_EOL = CrsChar() == LL;

    if( CURSOR_AT_EOL )
    {
      m_vis.get_states().addFirst( m_run_i_tabs );

      Reset_File_Name_Completion_Variables();
    }
    else m_vis.get_states().addFirst( m_run_i_normal );
  }

  void run_i_normal()
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
        m_i_EOL_delim = true;
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
      else {
        InsertAddChar( C );
        m_i_count++;
      }
    }
  }

  void run_i_tabs()
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
        m_i_EOL_delim = true;
        m_vis.get_states().removeFirst(); // Done
      }
      else if( '\t' == C && 0<m_i_count )
      {
        Do_i_tabs_HandleTab();
      }
      else {
        Reset_File_Name_Completion_Variables();

        if( BS == C || DEL == C )
        {
          if( 0<m_i_count )
          {
            InsertBackspace();
            m_i_count--;
          }
        }
        else {
          InsertAddChar( C );
          m_i_count++;
        }
      }
    }
  }
  void run_i_end()
  {
    if( m_i_EOL_delim )
    {
      HandleReturn();
    }
    else {
      // Move cursor back one space:
      if( 0 < m_crsCol )
      {
        Clear_Console_CrsCell();
        m_crsCol--;
      }
      m_inInsertMode = false;
      DisplayBanner_PrintCursor();
    }
    m_vis.get_states().removeFirst();
  }

  void Reset_File_Name_Completion_Variables()
  {
    m_dir_fb      = null;
    m_file_index  = 0;
    m_colon_op    = ColonOp.unknown;
    m_partial_path = "";
    m_search__head = "";
  }

  void Do_i_tabs_HandleTab()
  {
    boolean found_tab_fname = false;

    if( null == m_dir_fb )
    {
      // First consecutive tab pressed:
      found_tab_fname = Find_File_Name_Completion_Variables();
    }
    else {
      // Subsequent consecutive tab pressed:
      found_tab_fname = Have_File_Name_Completion_Variables();
    }
    if( found_tab_fname )
    {
      m_fb.UpdateCmd();

      m_i_count = m_fb.LineLen( CrsLine() );
    }
    else {
      // If we fall through, just treat tab like a space:
      InsertAddChar(' ');
    }
  }
  // Returns true if found tab filename, else false
  boolean Find_File_Name_Completion_Variables()
  {
    boolean found_tab_fname = false;

    m_sb.setLength(0);
    m_sb.append( m_fb.GetLine( CrsLine() ) );

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
        for( int k=0; !found_tab_fname && k<m_dir_fb.NumLines(); k++ )
        {
          Line fname = m_dir_fb.GetLine( k );
      
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
      if( found_tab_fname )
      {
        // Removed "e" above, so add it back here:
        if( ColonOp.e == m_colon_op ) m_sb.insert( 0, "e ");
        else                          m_sb.insert( 0, "w ");

        // Remove line, clear line, copy m_sb into line, put line back:
        Line lp = m_fb.RemoveLine( CrsLine() );
        lp.setLength(0);
        lp.append_s( m_sb.toString() );
        m_fb.InsertLine( CrsLine(), lp );

        GoToCrsPos_NoWrite( CrsLine(), lp.length() );
      }
    }
    return found_tab_fname;
  }
  // Returns true if found tab filename, else false
  boolean Have_File_Name_Completion_Variables()
  {
    boolean found_tab_fname = false;

    // Already have a FileBuf, just search for next matching filename:
    for( int k=m_file_index+1
       ; !found_tab_fname && k<m_file_index+m_dir_fb.NumLines(); k++ )
    {
      Line fname = m_dir_fb.GetLine( k % m_dir_fb.NumLines() );

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

        if( ColonOp.e == m_colon_op ) m_sb.insert( 0, "e ");
        else                          m_sb.insert( 0, "w ");

        Line lp = m_fb.RemoveLine( CrsLine() );
        lp.setLength(0);
        lp.append_s( m_sb.toString() );
        m_fb.InsertLine( CrsLine(), lp );

        GoToCrsPos_NoWrite( CrsLine(), lp.length() );
      }
    }
    return found_tab_fname;
  }
  // m_sb goes in as               some/path/partial_file_name
  //   and if successful:
  //   m_partial_path comes out as some/path the relative path to the files
  //   m_search__head comes out as partial_file_name
  //   m_dir_fb       comes out as full path to m_partial_path
  // Returns true if a m_dir_fb is filled in, else false
  boolean FindFileBuf()
  {
    Ptr_StringBuilder path_tail = new Ptr_StringBuilder();
    Ptr_StringBuilder path_head = new Ptr_StringBuilder();

    Path_2_TailHead( m_sb, path_tail, path_head );

    String f_full_path = ".";
    if( 0 < path_tail.val.length() ) f_full_path = path_tail.val.toString();

    final int FILE_NUM = m_vis.Curr_FileNum();

    if( m_vis.USER_FILE <= FILE_NUM )
    {
      // Get full file name relative to path of current file:
      f_full_path = m_vis.CV().m_fb.Relative_2_FullFname( f_full_path );
    }
    else {
      // Get full file name relative to CWD:
      f_full_path = Utils.FindFullFileName_Path( m_vis.get_cwd(), f_full_path );
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
        m_dir_fb = m_vis.get_FileBuf( file_index.val );
        return true;
      }
      else {
        m_dir_fb = new FileBuf( m_vis, f_full_path, true );
        boolean ok = m_dir_fb.ReadFile();
        if( ok ) {
          m_vis.Add_FileBuf_2_Lists_Create_Views( m_dir_fb, f_full_path );
          return true;
        }
      }
    }
    return false;
  }
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
      for( int i = last_slash_idx + 1; i<in_fname_len  ; i++ )
      {
        path_head.val.append( in_fname.charAt( i ) );
      }
    }
    else if( 0 < last_slash_idx )
    {
      for( int i = 0; i<last_slash_idx; i++ )
      {
        path_tail.val.append( in_fname.charAt( i ) );
      }
      for( int i = last_slash_idx + 1; i<in_fname_len  ; i++ )
      {
        path_head.val.append( in_fname.charAt( i ) );
      }
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

  void Do_R()
  {
    m_vis.get_states().addFirst( m_run_R_end );
    m_vis.get_states().addFirst( m_run_R_mid );
    m_vis.get_states().addFirst( m_run_R_beg );
  }
  void run_R_beg()
  {
    if( 0 == m_fb.NumLines() ) m_fb.PushLine();

    m_inReplaceMode = true;
    m_i_EOL_delim = false;

    DisplayBanner_PrintCursor();

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
      else if( Utils.IsEndOfLineDelim( C ) )
      {
        m_vis.get_states().removeFirst();
        HandleReturn();
        m_i_EOL_delim = true;
      }
    //else if( BS == C || DEL == C )
    //{
    //  m_fb.Undo( this );
    //}
      else {
        ReplaceAddChars( C );
      }
    }
  }
  void run_R_end()
  {
    // Move cursor back one space:
    if( 0<m_crsCol )
    {
      Set_crsCol( m_crsCol-1 );
    }
    m_inReplaceMode = false;
    DisplayBanner_PrintCursor();

    m_vis.get_states().removeFirst();
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
    m_fb.UpdateCmd();
  }

  void InsertBackspace()
  {
    // If no lines in buffer, no backspacing to be done
    if( 0<m_fb.NumLines() )
    {
      final int CL = CrsLine();  // Cursor line
      final int CP = CrsChar();  // Cursor position

      if( 0<CP )
      {
        m_fb.RemoveChar( CL, CP-1 );

        if( 0 < m_crsCol ) m_crsCol -= 1;
        else               m_leftChar -= 1;

        m_fb.UpdateCmd();
      }
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
    m_fb.UpdateCmd();
  }

  // 1. Remove current colon command line and copy it into m_vis.m_sb:
  // 2. If last line is blank, remove it:
  // 3. Remove any other lines in colon file that match current colon command:
  // 4. Add current colon command to end of colon file:
  void HandleReturn()
  {
    m_inInsertMode = false;

    final int CL = m_topLine;
    final int LL = m_fb.LineLen( CL ); // Current line length

    // 1. Remove current colon command line and copy it into m_vis.m_sb:
    final Line lp = m_fb.RemoveLine( CL );

    m_vis.set_cmd( lp.toString() );

    // 2. If last line is blank, remove it:
    int NL = m_fb.NumLines(); // Number of colon file lines
    if( 0<NL && 0 == m_fb.LineLen( NL-1 ) )
    {
      m_fb.RemoveLine( NL-1 ); NL--;
    }

    // 3. Remove any other lines in colon file that match current colon command:
    for( int ln=0; ln<NL; ln++ )
    {
      final Line t_lp = m_fb.GetLine( ln );
      // Lines are not NULL terminated, so use strncmp with line length:
      if( t_lp.equals( lp ) )
      {
        m_fb.RemoveLine( ln ); NL--; ln--;
      }
    }

    // 4. Add current colon command to end of colon file:
    m_fb.PushLine( lp ); NL++;

    if( 0 < LL )
    {
      m_fb.PushLine(); NL++;
    }
    GoToCrsPos_NoWrite( NL-1, 0 );

    m_fb.UpdateCmd();
    ClearLine();
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
        m_fb.UpdateCmd();
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

    m_fb.UpdateCmd();

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
      Do_dd_Normal( ONL );
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

    m_fb.UpdateCmd();
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
    if( p_fn_line.val < p_st_line.val
     || (p_fn_line.val == p_st_line.val && p_fn_char.val < p_st_char.val) )
    {
      Utils.Swap( p_st_line, p_fn_line );
      Utils.Swap( p_st_char, p_fn_char );
    }
    m_vis.get_reg().clear();
  }

  void Do_x_range_post( final int st_line, final int st_char )
  {
    m_vis.set_paste_mode( Paste_Mode.ST_FN );

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

    m_fb.UpdateCmd(); //<- No need to Undo_v() or Remove_Banner() because of this
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

      m_fb.UpdateCmd();
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
    m_fb.UpdateCmd();
  }

  void Do_p()
  {
    final Paste_Mode PM = m_vis.get_paste_mode();

    if     ( Paste_Mode.ST_FN == PM ) Do_p_or_P_st_fn( Paste_Pos.After );
    else /*( Paste_Mode.LINE  == PM*/ Do_p_line();
  }

  void Do_P()
  {
    final Paste_Mode PM = m_vis.get_paste_mode();

    if     ( Paste_Mode.ST_FN == PM ) Do_p_or_P_st_fn( Paste_Pos.Before );
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
        MoveInBounds();
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
    m_fb.UpdateCmd();
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
    m_fb.UpdateCmd();
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
    m_fb.UpdateCmd();
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

//void Do_u()
//{
//  m_fb.Undo( this );
//}
//void Do_U()
//{
//  m_fb.UndoAll( this );
//}

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

  void Do_Tilda()
  {
    if( 0==m_fb.NumLines() ) return;

    final int CL = CrsLine(); // Old cursor line
    final int CP = CrsChar(); // Old cursor position
    final int LL = m_fb.LineLen( CL );

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
        else Clear_Console_CrsCell();

        // Need to move cursor right:
        m_crsCol++;
      }
      else if( RightChar() < LL-1 )
      {
        // Need to scroll window right:
        if( changed ) m_fb.Set( CL, CP, C, CONT_LAST_UPDATE );
        else Clear_Console_CrsCell();

        m_leftChar++;
      }
      else // RightChar() == LL-1
      {
        // At end of line so cant move or scroll right:
        if( changed ) m_fb.Set( CL, CP, C, CONT_LAST_UPDATE );
      }
      m_fb.UpdateCmd();
    }
  }

  void GoToOppositeBracket()
  {
    MoveInBounds();
    final int NUM_LINES = m_fb.NumLines();
    final int CL = CrsLine();
    final int CC = CrsChar();
    final int LL = m_fb.LineLen( CL );

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

  // Go to next pattern
  void Do_n()
  {
    if( 0<m_fb.NumLines() )
    {
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

    // Move past current star on current line:
    final int LL = m_fb.LineLen( OCL );

    for( ; st_c<LL && InStar(OCL,st_c); st_c++ ) ;

    // If at end of current line, go down to next line:
    if( LL <= st_c ) { st_c=0; st_l++; }

    // Search for first star position past current position
    for( int l=st_l; !found_next_star && l<NUM_LINES; l++ )
    {
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

  // Go to previous pattern
  void Do_N()
  {
    if( 0 < m_fb.NumLines() )
    {
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

    MoveInBounds();

    final int NUM_LINES = m_fb.NumLines();
  
    final int OCL = CrsLine();
    final int OCC = CrsChar();
  
    boolean found_prev_star = false;
  
    // Search for first star position before current position
    for( int l=OCL; !found_prev_star && 0<=l; l-- )
    {
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

  void Do_v()
  {
    m_vis.get_states().addFirst( m_run_v_end );
    m_vis.get_states().addFirst( m_run_v_mid );
    m_vis.get_states().addFirst( m_run_v_beg );
  }
  void run_v_beg()
  {
    MoveInBounds();
    m_inVisualMode = true;
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
      else if( C == '0' ) GoToBegOfLine();
      else if( C == '$' ) GoToEndOfLine();
      else if( C == 'g' ) Do_v_Handle_g();
      else if( C == 'b' ) GoToPrevWord();
      else if( C == 'w' ) GoToNextWord();
      else if( C == 'e' ) GoToEndOfWord();
      else if( C == 'f' ) Do_f();
      else if( C == ';' ) m_vis.Handle_SemiColon();
      else if( C == 'y' ) Do_y_v();
      else if( C == 'Y' ) Do_Y_v();
      else if( C == 'x'
            || C == 'd' ) Do_x_v();
      else if( C == 'D' ) Do_D_v();
      else if( C == 's' ) Do_s_v();
      else if( C == '~' ) Do_Tilda_v();
      else if( C == ESC ) m_inVisualMode = false;
    }
    if( !m_inVisualMode ) m_vis.get_states().removeFirst();
  }
  void run_v_end()
  {
    DisplayBanner();
    Undo_v();

    m_vis.get_states().removeFirst();
  }
  void Undo_v()
  {
    m_fb.UpdateCmd();
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
      final int m_v_st_char = v_st_char < v_fn_char
                            ? v_st_char : v_fn_char;
      final int m_v_fn_char = v_st_char < v_fn_char
                            ? v_fn_char : v_st_char;

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
      }
    }
  }
  void Do_v_Handle_gp()
  {
    if( v_st_line == v_fn_line )
    {
      Swap_Visual_St_Fn_If_Needed();

      StringBuilder pattern = new StringBuilder();

      for( int P = v_st_char; P<=v_fn_char; P++ )
      {
        pattern.append( m_fb.Get( v_st_line, P  ) );
      }
      m_vis.Handle_Slash_GotPattern( pattern.toString(), false );

      m_inVisualMode = false;
      Undo_v();
      DisplayBanner_PrintCursor();
    }
  }

  void Do_y_v()
  {
    m_vis.get_reg().clear();

    Do_y_v_st_fn();

    m_inVisualMode = false;
  }

  void Do_Y_v()
  {
    m_vis.get_reg().clear();

    Do_Y_v_st_fn();

    m_inVisualMode = false;
  }

  void Do_x_v()
  {
    Do_x_range( v_st_line, v_st_char, v_fn_line, v_fn_char );

    m_inVisualMode = false;

    DisplayBanner_PrintCursor();
  }

  void Do_D_v()
  {
    Do_D_v_line();

    m_inVisualMode = false;
  }

  boolean Do_s_v_cursor_at_end_of_line()
  {
    final int LL = m_fb.LineLen( CrsLine() );

    return 0<LL ? CrsChar() == LL-1 : false;
  }

  void Do_s_v()
  {
    // Need to know if cursor is at end of line before Do_x_v() is called:
    final boolean CURSOR_AT_END_OF_LINE = Do_s_v_cursor_at_end_of_line();

    Do_x_v();

    if( CURSOR_AT_END_OF_LINE ) Do_a();
    else                        Do_i();

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

  void Do_D_v_line()
  {
    Swap_Visual_St_Fn_If_Needed();

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
    DisplayBanner();
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

      m_fb.UpdateCmd();
    }
  }

  void Do_Tilda_v()
  {
    Swap_Visual_St_Fn_If_Needed();

    Do_Tilda_v_st_fn();

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

  void CoverKey()
  {
    m_sb.setLength( 0 );

    if( m_vis.get_diff_mode() ) m_vis.get_diff().Clear_Console_CrsCell();
    else                              m_vis.CV().Clear_Console_CrsCell();

    final int G_COL = m_x + 1;
    m_console.SetS( m_y, G_COL, m_cover_msg, Style.NORMAL );
    m_console.Set( m_y, G_COL + m_cover_msg_len, ' ', Style.CURSOR );
    m_console.Update();

    m_vis.get_states().addFirst( m_run_cover_key );
  }
  // Get cover key:
  void run_cover_key()
  {
    final int WC = m_num_cols - 2;

    if( 0<m_console.KeysIn() )
    {
      final char C = m_console.GetKey();

      if( Utils.IsEndOfLineDelim( C ) )
      {
        m_cover_key = m_sb.toString();

        ClearLine();
        m_vis.CV().PrintCursor();

        m_vis.get_states().removeFirst(); //< Drop out of m_run_cover_key
      }
      else {
        if( ConsoleIF.BS != C && ConsoleIF.DEL != C )
        {
          // Normal
          m_sb.append( C );

          final int local_COL = Math.min( m_cover_msg_len+m_sb.length(), WC-1 );

          // Output cd and move cursor forward:
          m_console.Set( m_y, m_x + local_COL  , '*', Style.NORMAL );
          m_console.Set( m_y, m_x + local_COL+1, ' ', Style.CURSOR );
        }
        else {
          if( 0 < m_sb.length() )
          {
            final int local_COL = Math.min( m_cover_msg_len+m_sb.length(), WC-1 );

            m_console.Set( m_y, m_x + local_COL+1, ' ', Style.NORMAL );
            m_console.Set( m_y, m_x + local_COL  , ' ', Style.CURSOR );
            m_console.Update();

            m_sb.deleteCharAt( m_sb.length()-1 );
          }
        }
      }
      m_console.Update();
    }
  }
  void Do_Cover()
  {
    View cv = m_vis.CV();
    FileBuf fb = cv.m_fb;

    if( fb.m_isDir )
    {
      cv.PrintCursor();
    }
    else {
      final int seed = fb.GetSize() % 256;

      Cover.Cover_Array( fb, m_cover_buf, seed, m_cover_key );

      // Fill in m_cover_buf from old file data:
      // Clear old file:
      fb.ClearLines();

      // Read in covered file:
      fb.ReadArray( m_cover_buf );

      // Make sure all windows have proper change status in borders
      m_vis.Update_Change_Statuses();

      // Reset view position:
      cv.Clear_Context();

      cv.Update();
    }
  }

  static final char BS  =   8; // Backspace
  static final char ESC =  27; // Escape
  static final char DEL = 127; // Delete

  static final int WORKING_ROWS = 1;

  VisIF     m_vis;
  FileBuf   m_fb;
  ConsoleIF m_console;
  int       m_x; // Top left x-position of buffer view in parent window
  int       m_y; // Top left y-position of buffer view in parent window
  final
  int      m_prefix_len = 2;
  private int m_topLine;  // top  of buffer view line number.
  private int m_leftChar; // left of buffer view character number.
  private int m_crsCol;// cursor column in buffer view. 0 <= m_crsCol < WorkingCols().
  StringBuilder m_sb = new StringBuilder();
  boolean  m_inInsertMode; // true if in insert  mode, else false
  boolean  m_inReplaceMode;
  private
  boolean  m_inVisualMode ;
  int      v_st_line;  // Visual start line number
  int      v_st_char;  // Visual start char number on line
  int      v_fn_line;  // Visual ending line number
  int      v_fn_char;  // Visual ending char number on line
  int      m_num_cols; // number of rows in buffer view
  int      m_num_rows; // number of columns in buffer view
  int      m_i_count;
  boolean  m_i_EOL_delim;
  final
  char     m_banner_delim;

  // Tab file name completion variables:
  FileBuf m_dir_fb;
  int     m_file_index;
  ColonOp m_colon_op;
  String  m_partial_path;
  String  m_search__head;

  // Cover variables:
        String          m_cover_key     = "";
        ArrayList<Byte> m_cover_buf     = new ArrayList<>();
  final String          m_cover_msg     = "Enter cover key:";
  final int             m_cover_msg_len = m_cover_msg.length();

  Thread m_run_i_beg    = new Thread() { public void run() { run_i_beg    (); m_vis.Give(); } };
  Thread m_run_i_normal = new Thread() { public void run() { run_i_normal (); m_vis.Give(); } };
  Thread m_run_i_tabs   = new Thread() { public void run() { run_i_tabs   (); m_vis.Give(); } };
  Thread m_run_i_end    = new Thread() { public void run() { run_i_end    (); m_vis.Give(); } };
  Thread m_run_R_beg    = new Thread() { public void run() { run_R_beg    (); m_vis.Give(); } };
  Thread m_run_R_mid    = new Thread() { public void run() { run_R_mid    (); m_vis.Give(); } };
  Thread m_run_R_end    = new Thread() { public void run() { run_R_end    (); m_vis.Give(); } };
  Thread m_run_v_beg    = new Thread() { public void run() { run_v_beg    (); m_vis.Give(); } };
  Thread m_run_v_mid    = new Thread() { public void run() { run_v_mid    (); m_vis.Give(); } };
  Thread m_run_v_end    = new Thread() { public void run() { run_v_end    (); m_vis.Give(); } };
  Thread m_run_g_v      = new Thread() { public void run() { run_g_v      (); m_vis.Give(); } };
  Thread m_run_f        = new Thread() { public void run() { run_f        (); m_vis.Give(); } };
  Thread m_run_cover_key= new Thread() { public void run() { run_cover_key(); m_vis.Give(); } };
}

enum ColonOp
{ 
  unknown,
  e,
  w
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

