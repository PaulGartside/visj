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
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.nio.file.Files;
import java.nio.file.Paths;

class Diff
{
  Diff( VisIF vis, ConsoleFx console )
  {
    m_vis     = vis;
    m_console = console;
  }
  // Returns true if diff took place, else false
  //
  boolean Run( View v0, View v1 )
  {
    boolean ran_diff = false;
    // Each buffer must be displaying a different file to do diff:
    if( v0.m_fb != v1.m_fb )
    {
      if( !DiffSameAsPrev( v0, v1 ) )
      {
        ClearDiff(); //< Start over with clean slate

        Set_ShortLong_ViewfileMod_Vars( v0, v1 );

        // All lines in both files:
        DiffArea CA = new DiffArea( 0, m_fS.NumLines(), 0, m_fL.NumLines() );
        RunDiff( CA );

      //m_console.Clear_Crs_Cell(); //< Not sure if this is needed
        Find_Context();
      }
      ran_diff = true;
    }
    return ran_diff;
  }

  void RunDiff( final DiffArea CA )
  {
    final long t1 = System.currentTimeMillis();

    Popu_SameList( CA ); // clears and uses CA to fill in m_sameList
    Sort_SameList();
  //PrintSameList();
    Popu_DiffList( CA ); // clears and uses CA,m_samelist to fill in m_diffList
  //PrintDiffList();
    Popu_DI_List( CA ); // uses CA,m_diffList to fill in m_DI_List_S,L
  //PrintDI_List( CA );

    final long t2 = System.currentTimeMillis();
    m_diff_ms = t2 - t1;
    m_printed_diff_ms = false;
  }

  boolean Has_Context()
  {
    return 0 != m_topLine
        || 0 != m_leftChar
        || 0 != m_crsRow
        || 0 != m_crsCol ;
  }
  void Find_Context()
  {
    if( !Has_Context() )
    {
      View pV = m_vis.CV();

      if( pV.Has_Context() )
      {
        Copy_ViewContext_2_DiffContext();
      }
      else {
        Do_n_Diff( false );
        MoveCurrLineCenter( false );
      }
    }
  }
  void Copy_ViewContext_2_DiffContext()
  {
    View pV = m_vis.CV();

    // View context -> diff context
    final int diff_topLine = DiffLine( pV, pV.TopLine() );
    final int diff_crsLine = DiffLine( pV, pV.CrsLine() );
    final int diff_crsRow  = diff_crsLine - diff_topLine;

    m_topLine  = diff_topLine;
    m_leftChar = pV.LeftChar();
    m_crsRow   = diff_crsRow;
    m_crsCol   = pV.CrsCol();
  }
  void Copy_DiffContext_2_Remaining_ViewContext()
  {
    View cV = m_vis.CV();
    View remaining_view = cV == m_vL ? m_vS : m_vL;

    remaining_view.Set_Context( GetTopLine( remaining_view )
                              , GetLeftChar()
                              , GetCrsRow()
                              , GetCrsCol() );
  }

  void ClearDiff()
  {
    m_sameList.clear();
    m_diffList.clear();

    m_DI_List_S.clear();
    m_DI_List_L.clear();
    m_DI_L_ins_idx = 0;

    m_simiList.clear();

    // Reset some other variables:
    m_topLine  = 0;
    m_leftChar = 0;
    m_crsRow   = 0;
    m_crsCol   = 0;
    m_inVisualMode = false;
    v_st_line  = 0;
    v_st_char  = 0;
    v_fn_line  = 0;
    v_fn_char  = 0;
    m_vS = null;
    m_vL = null;
    m_fS = null;
    m_fL = null;
  }

  boolean DiffSameAsPrev( final View v0, final View v1 )
  {
          boolean DATES_SAME_AS_BEFORE = false;
    final boolean FILES_SAME_AS_BEFORE =
                  null != m_fS
               && null != m_fL
               && (
                    ( v0.m_fb == m_fS && v1.m_fb == m_fL )
                 || ( v0.m_fb == m_fL && v1.m_fb == m_fS )
                  );
    if( FILES_SAME_AS_BEFORE )
    {
      DATES_SAME_AS_BEFORE =
      (
        ( m_mod_time_s == m_fS.m_mod_time && m_mod_time_l == m_fL.m_mod_time )
     || ( m_mod_time_l == m_fS.m_mod_time && m_mod_time_s == m_fL.m_mod_time )
      );
    }
    return FILES_SAME_AS_BEFORE
        && DATES_SAME_AS_BEFORE;
  }

  void Set_ShortLong_ViewfileMod_Vars( View v0, View v1 )
  {
    final int nLines_0 = v0.m_fb.NumLines();
    final int nLines_1 = v1.m_fb.NumLines();

    m_vS = nLines_0 < nLines_1 ? v0 : v1; // Short view
    m_vL = nLines_0 < nLines_1 ? v1 : v0; // Long  view
    m_fS = m_vS.m_fb;
    m_fL = m_vL.m_fb;
    m_mod_time_s = m_fS.m_mod_time;
    m_mod_time_l = m_fL.m_mod_time;
  }

  void Update()
  {
    if( m_vis.get_Console().get_from_dot_buf() ) return;

    m_vis.Update_Change_Statuses();

    // Update long view:
    m_fL.Find_Styles( ViewLine( m_vL, m_topLine ) + WorkingRows( m_vL ) );
    m_fL.Find_Regexs( ViewLine( m_vL, m_topLine ), WorkingRows( m_vL ) );

    RepositionViews();

  //m_vL.RepositionView();
    m_vL.Print_Borders();
    PrintWorkingView( m_vL );
    PrintStsLine( m_vL );
    m_vL.PrintFileLine();
    PrintCmdLine( m_vL );

    // Update short view:
    m_fS.Find_Styles( ViewLine( m_vS, m_topLine ) + WorkingRows( m_vS ) );
    m_fS.Find_Regexs( ViewLine( m_vS, m_topLine ), WorkingRows( m_vS ) );

  //m_vS.RepositionView();
    m_vS.Print_Borders();
    PrintWorkingView( m_vS );
    PrintStsLine( m_vS );
    m_vS.PrintFileLine();
    PrintCmdLine( m_vS );

    Set_Console_CrsCell();

    if( ! m_printed_diff_ms )
    {
      m_vis.CmdLineMessage( "Diff took: "+ m_diff_ms +" ms" );
      m_printed_diff_ms = true;
    }
  //m_console.Print_Cursor();
  }

  void RepositionViews()
  {
    // If a window re-size has taken place, and the window has gotten
    // smaller, change top line and left char if needed, so that the
    // cursor is in the buffer when it is re-drawn
    View pV = m_vis.CV();

    if( WorkingRows( pV ) <= m_crsRow )
    {
      m_topLine += ( m_crsRow - WorkingRows( pV ) + 1 );
      m_crsRow  -= ( m_crsRow - WorkingRows( pV ) + 1 );
    }
    if( WorkingCols( pV ) <= m_crsCol )
    {
      m_leftChar += ( m_crsCol - WorkingCols( pV ) + 1 );
      m_crsCol   -= ( m_crsCol - WorkingCols( pV ) + 1 );
    }
  }

  int GetTopLine( View pV )
  {
    if( pV == m_vS || pV == m_vL )
    {
      return ViewLine( pV, m_topLine );
    }
    return 0;
  }
  int GetTopLine () { return m_topLine;  }
  int GetLeftChar() { return m_leftChar; }
  int GetCrsRow  () { return m_crsRow;  }
  int GetCrsCol  () { return m_crsCol;  }

  int WorkingRows( View pV ) { return pV.WorkingRows(); }
  int WorkingCols( View pV ) { return pV.WorkingCols(); }
  int CrsLine  () { return m_topLine  + m_crsRow; }
  int CrsChar  () { return m_leftChar + m_crsCol; }
  int BotLine  ( View pV ) { return m_topLine  + WorkingRows( pV )-1; }
  int RightChar( View pV ) { return m_leftChar + WorkingCols( pV )-1; }

  int Row_Win_2_GL( View pV, final int win_row )
  {
    return pV.Y() + 1 + win_row;
  }
  int Col_Win_2_GL( View pV, final int win_col )
  {
    return pV.X() + 1 + win_col;
  }
  int Line_2_GL( View pV, final int file_line )
  {
    return pV.Y() + 1 + file_line - m_topLine;
  }
  int Char_2_GL( View pV, final int line_char )
  {
    return pV.X() + 1 + line_char - m_leftChar;
  }

  int NumLines()
  {
    // m_DI_List_S and m_DI_List_L should be the same length
    return m_DI_List_L.size();
  }
  int NumLines( final View pV )
  {
    return ( pV == m_vS ) ? m_DI_List_S.size()
                          : m_DI_List_L.size();
  }
  int LineLen()
  {
    View pV = m_vis.CV();

    final int diff_line = CrsLine();

    Diff_Info rDI = ( pV == m_vS ) ? m_DI_List_S.get( diff_line )
                                   : m_DI_List_L.get( diff_line );
    if( Diff_Type.UNKNOWN == rDI.diff_type
     || Diff_Type.DELETED == rDI.diff_type )
    {
      return 0;
    }
    final int view_line = rDI.line_num;

    return pV.m_fb.LineLen( view_line );
  }

  int DiffLine( final View pV, final int view_line )
  {
    return ( pV == m_vS ) ? DiffLine_S( view_line )
                          : DiffLine_L( view_line );
  }

  int ViewLine( final View pV, final int diff_line )
  {
    return ( pV == m_vS ) ? m_DI_List_S.get( diff_line ).line_num
                          : m_DI_List_L.get( diff_line ).line_num;
  }

  Diff_Type DiffType( final View pV, final int diff_line )
  {
    return ( pV == m_vS ) ? m_DI_List_S.get( diff_line ).diff_type
                          : m_DI_List_L.get( diff_line ).diff_type;
  }

//// Return the diff line of the view line on the short side
//int DiffLine_S( final int view_line )
//{
//  int diff_line = 0;
//  final int NUM_LINES_VS = m_vS.m_fb.NumLines();
//
//  if( 0 < NUM_LINES_VS )
//  {
//    final int DI_LEN = m_DI_List_S.size();
//
//    if( NUM_LINES_VS <= view_line ) diff_line = DI_LEN-1;
//    else {
//      // Diff line is greater than or equal to view line,
//      // so start at view line number and search forward
//      int k = view_line;
//      Diff_Info di = m_DI_List_S.get( view_line );
//      k += view_line - di.line_num;
//      boolean found = false;
//      for( ; !found && k<DI_LEN; k += view_line - di.line_num )
//      {
//        di = m_DI_List_S.get( k );
//
//        if( Diff_Type.SAME       == di.diff_type
//         || Diff_Type.CHANGED    == di.diff_type
//         || Diff_Type.INSERTED   == di.diff_type
//         || Diff_Type.DIFF_FILES == di.diff_type )
//        {
//          if( view_line == di.line_num )
//          {
//            found = true;
//            diff_line = k;
//          }
//        }
//      }
//      if( !found ) {
//        Utils.Assert( false, "view_line : "+ view_line +" : not found");
//      }
//    }
//  }
//  return diff_line;
//}
  // Return the diff line of the view line on the short side
  int DiffLine_S( final int view_line )
  {
    int diff_line = 0;
    final int NUM_LINES_VS = m_vS.m_fb.NumLines();

    if( 0 < NUM_LINES_VS )
    {
      final int DI_LEN = m_DI_List_S.size();

      if( NUM_LINES_VS <= view_line ) diff_line = DI_LEN-1;
      else if( 0 < view_line )
      { // Diff line is greater than or equal to view line,
        // so start at view line number and search forward
        int k = view_line;
        Diff_Info di = m_DI_List_S.get( view_line );
        k += view_line - di.line_num;
        boolean found = false;

        for( ; !found && k<DI_LEN; k += view_line - di.line_num )
        {
          di = m_DI_List_S.get( k );

          if( view_line == di.line_num )
          {
            found = true;
            diff_line = k;
          }
        }
        if( !found ) {
          Utils.Assert( false, "view_line : "+ view_line +" : not found");
        }
      }
    }
    return diff_line;
  }

//// Return the diff line of the view line on the long side
//int DiffLine_L( final int view_line )
//{
//  int diff_line = 0;
//  final int NUM_LINES_VL = m_vL.m_fb.NumLines();
//
//  if( 0 < NUM_LINES_VL )
//  {
//    final int DI_LEN = m_DI_List_L.size();
//
//    if( NUM_LINES_VL <= view_line ) diff_line = DI_LEN-1;
//    else {
//      // Diff line is greater than or equal to view line,
//      // so start at view line number and search forward
//      int k = view_line;
//      Diff_Info di = m_DI_List_L.get( view_line );
//      k += view_line - di.line_num;
//      boolean found = false;
//      for( ; !found && k<DI_LEN; k += view_line - di.line_num )
//      {
//        di = m_DI_List_L.get( k );
//
//        if( Diff_Type.SAME       == di.diff_type
//         || Diff_Type.CHANGED    == di.diff_type
//         || Diff_Type.INSERTED   == di.diff_type
//         || Diff_Type.DIFF_FILES == di.diff_type )
//        {
//          if( view_line == di.line_num )
//          {
//            found = true;
//            diff_line = k;
//          }
//        }
//      }
//      if( !found ) {
//        Utils.Assert( false, "view_line : "+ view_line +" : not found");
//      }
//    }
//  }
//  return diff_line;
//}
  // Return the diff line of the view line on the long side
  int DiffLine_L( final int view_line )
  {
    int diff_line = 0;
    final int NUM_LINES_VL = m_vL.m_fb.NumLines();

    if( 0 < NUM_LINES_VL )
    {
      final int DI_LEN = m_DI_List_L.size();

      if( NUM_LINES_VL <= view_line ) diff_line = DI_LEN-1;
      else if( 0 < view_line )
      { // Diff line is greater than or equal to view line,
        // so start at view line number and search forward
        int k = view_line;
        Diff_Info di = m_DI_List_L.get( view_line );
        k += view_line - di.line_num;
        boolean found = false;
        for( ; !found && k<DI_LEN; k += view_line - di.line_num )
        {
          di = m_DI_List_L.get( k );

          if( view_line == di.line_num )
          {
            found = true;
            diff_line = k;
          }
        }
        if( !found ) {
          Utils.Assert( false, "view_line : "+ view_line +" : not found");
        }
      }
    }
    return diff_line;
  }

  void PrintCursor()
  {
    Set_Console_CrsCell();
  //m_console.Print_Cursor();
  }
  void PrintWorkingView( View pV )
  {
    final int NUM_LINES = NumLines();
    final int WR        = WorkingRows( pV );
    final int WC        = WorkingCols( pV );

    int row = 0; // (dl=diff line)
    for( int dl=m_topLine; dl<NUM_LINES && row<WR; dl++, row++ )
    {
      final int G_ROW = Row_Win_2_GL( pV, row );
      final Diff_Type DT = DiffType( pV, dl );
      if( DT == Diff_Type.UNKNOWN )
      {
        for( int col=0; col<WC; col++ )
        {
          m_console.Set( G_ROW, Col_Win_2_GL( pV, col ), '~', Style.DIFF_DEL );
        }
      }
      else if( DT == Diff_Type.DELETED )
      {
        for( int col=0; col<WC; col++ )
        {
          m_console.Set( G_ROW, Col_Win_2_GL( pV, col ), '-', Style.DIFF_DEL );
        }
      }
      else if( DT == Diff_Type.CHANGED )
      {
        PrintWorkingView_DT_CHANGED( pV, WC, G_ROW, dl );
      }
      else if( DT == Diff_Type.DIFF_FILES )
      {
        PrintWorkingView_DT_DIFF_FILES( pV, WC, G_ROW, dl );
      }
      else // DT == Diff_Type.INSERTED || DT == Diff_Type.SAME
      {
        PrintWorkingView_DT_INSERTED_SAME( pV, WC, G_ROW, dl, DT );
      }
    }
    // Not enough lines to display, fill in with ~
    for( ; row < WR; row++ )
    {
      final int G_ROW = Row_Win_2_GL( pV, row );

      m_console.Set( G_ROW, Col_Win_2_GL( pV, 0 ), '~', Style.EOF );

      for( int col=1; col<WC; col++ )
      {
        m_console.Set( G_ROW, Col_Win_2_GL( pV, col ), ' ', Style.EOF );
      }
    }
  }

  void PrintWorkingView_DT_CHANGED( View  pV
                                  , final int WC
                                  , final int G_ROW
                                  , final int dl )
  {
    final int vl = ViewLine( pV, dl ); //(vl=view line)
    final int LL = pV.m_fb.LineLen( vl );
    Diff_Info di = (pV == m_vS) ? m_DI_List_S.get( dl ) : m_DI_List_L.get( dl );
    int col = 0;

    if( null != di.pLineInfo )
    {
      final int LIL = di.pLineInfo.size();
      int cp = m_leftChar; // char position
      for( int i=m_leftChar; cp<LL && i<LIL && col<WC; i++, col++ )
      {
        Diff_Type dt = di.pLineInfo.get(i);

        if( Diff_Type.SAME == dt )
        {
          Style s = Get_Style( pV, dl, vl, cp );
          char  c = pV.m_fb.Get( vl, cp );
          pV.PrintWorkingView_Set( LL, G_ROW, col, cp, c, s );
          cp++;
        }
        else if( Diff_Type.CHANGED == dt || Diff_Type.INSERTED == dt )
        {
          Style s = Get_Style( pV, dl, vl, cp ); s = DiffStyle( s );
          char  c = pV.m_fb.Get( vl, cp );
          pV.PrintWorkingView_Set( LL, G_ROW, col, cp, c, s );
          cp++;
        }
        else if( Diff_Type.DELETED == dt )
        {
          m_console.Set( G_ROW, Col_Win_2_GL( pV, col ), '-', Style.DIFF_DEL );
        }
        else //( Diff_Type.UNKNOWN  == dt )
        {
          m_console.Set( G_ROW, Col_Win_2_GL( pV, col ), '~', Style.DIFF_DEL );
        }
      }
      // Past end of line:
      for( ; col<WC; col++ )
      {
        m_console.Set( G_ROW, Col_Win_2_GL( pV, col ), ' ', Style.EMPTY );
      }
    }
    else {
      for( int i=m_leftChar; i<LL && col<WC; i++, col++ )
      {
        Style s = Get_Style( pV, dl, vl, i );
              s = DiffStyle( s );
        char  c = pV.m_fb.Get( vl, i );
        pV.PrintWorkingView_Set( LL, G_ROW, col, i, c, s );
      }
      // Past end of line:
      for( ; col<WC; col++ )
      {
        m_console.Set( G_ROW, Col_Win_2_GL( pV, col ), ' ', Style.DIFF_NORMAL );
      }
    }
  }
  void PrintWorkingView_DT_DIFF_FILES( View  pV
                                     , final int WC
                                     , final int G_ROW
                                     , final int dl )
  {
    final int vl = ViewLine( pV, dl ); //(vl=view line)
    final int LL = pV.m_fb.LineLen( vl );
    int col = 0;
    for( int i=m_leftChar; i<LL && col<WC; i++, col++ )
    {
      char  c = pV.m_fb.Get( vl, i );
      Style s = Get_Style( pV, dl, vl, i );

      pV.PrintWorkingView_Set( LL, G_ROW, col, i, c, s );
    }
    for( ; col<WC; col++ )
    {
      m_console.Set( G_ROW, Col_Win_2_GL( pV, col ), ' '
                   , col%2==0 ? Style.NORMAL : Style.DIFF_NORMAL );
    }
  }
  void PrintWorkingView_DT_INSERTED_SAME( View  pV
                                        , final int WC
                                        , final int G_ROW
                                        , final int dl
                                        , final Diff_Type DT )
  {
    final int vl = ViewLine( pV, dl ); //(vl=view line)
    final int LL = pV.m_fb.LineLen( vl );

    int col = 0;
    // In visual mode, empty line gets one highlighted space at beginning:
    if( 0 == LL && 0 == m_leftChar && InVisualArea( pV, dl, 0 ) )
    {
      m_console.Set( G_ROW, Char_2_GL( pV, 0 ), ' ', Style.RV_NORMAL );
      col++;
    }
    for( int i=m_leftChar; i<LL && col<WC; i++, col++ )
    {
      final char  C = pV.m_fb.Get( vl, i );
            Style S = Get_Style( pV, dl, vl, i );

      if( DT == Diff_Type.INSERTED ) S = DiffStyle( S );

      pV.PrintWorkingView_Set( LL, G_ROW, col, i, C, S );
    }
    for( ; col<WC; col++ )
    {
      final Style S = DT==Diff_Type.SAME ? Style.EMPTY : Style.DIFF_NORMAL;
      m_console.Set( G_ROW, Col_Win_2_GL( pV, col ), ' ', S );
    }
  }

  void PrintStsLine( View pV )
  {
    ArrayList<Diff_Info> DI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L;
    FileBuf fb = pV.m_fb;
    final int CLd = CrsLine();                   // Line position diff
    final int CLv = DI_List.get( CLd ).line_num; // Line position view
    final int CC = CrsChar();                    // Char position
    final int LL = 0<NumLines()
                 ? ( 0 < fb.NumLines() ? fb.LineLen( CLv ) : 0 )
                 : 0;
    final int WC = WorkingCols( pV );
    if( 0 < WC )
    {
      String str = "";
      // When inserting text at the end of a line, CrsChar() == LL
      if( 0 < LL && CC < LL ) // Print current char info:
      {
        final int C = fb.Get( CLv, CC );

        if     (  9 == C ) str = String.valueOf( C ) + ",\\t";
        else if( 13 == C ) str = String.valueOf( C ) + ",\\r";
        else               str = String.valueOf( C ) +","+ (char)C;
      }
      final int fileSize = fb.GetSize();
      final int  crsByte = fb.GetCursorByte( CLv, CC );
      int percent = (char)(100*(double)crsByte/(double)fileSize + 0.5);
      // Screen width so far
      m_sb.setLength( 0 );
      m_sb.append( "Pos=("+(CLv+1)+","+(CC+1)+")"
                 + "  ("+percent+"%, "+crsByte+"/"+fileSize+")"
                 + "  Char=("+str+")  ");

      final int SW = m_sb.length(); // Screen width so far

      if     ( SW < WC ) { for( int k=SW; k<WC; k++ ) m_sb.append(' '); }
      else if( WC < SW ) { m_sb.setLength( WC ); } //< Truncate extra part

      m_console.SetS( pV.Sts__Line_Row()
                    , pV.Col_Win_2_GL( 0 )
                    , m_sb.toString()
                    , Style.STATUS );
    }
  }
  void PrintCmdLine( View pV )
  {
    // Prints "--INSERT--" banner, and/or clears command line
    final int CMD_LINE_ROW = pV.Cmd__Line_Row();
    int col=0;
    // Draw insert banner if needed
    if( pV.m_inInsertMode )
    {
      col=10; // Strlen of "--INSERT--"
      m_console.SetS( CMD_LINE_ROW, pV.Col_Win_2_GL( 0 ), "--INSERT--", Style.BANNER );
    }
    else if( 0 < m_cmd_line_sb.length() )
    {
      col = m_cmd_line_sb.length();
      for( int k=0; k<col; k++ )
      {
        final char C =  m_cmd_line_sb.charAt(k);
        m_console.Set( CMD_LINE_ROW, pV.Col_Win_2_GL( k ), C, Style.NORMAL );
      }
    }
    final int WC = WorkingCols( pV );

    for( ; col<WC-7; col++ )
    {
      m_console.Set( CMD_LINE_ROW, pV.Col_Win_2_GL( col ), ' ', Style.NORMAL );
    }
    m_console.SetS( CMD_LINE_ROW, pV.Col_Win_2_GL( WC-8 ), "--DIFF--", Style.BANNER );
  }

  Style Get_Style( View  pV
                 , final int DL // Diff line
                 , final int VL // View line
                 , final int pos )
  {
    Style S = Style.EMPTY;

    FileBuf fb = pV.m_fb;

    // VL and pos are checked here:
    if( VL < fb.NumLines() && pos < fb.LineLen( VL ) )
    {
      S = Style.NORMAL;

      if    ( InVisualArea( pV, DL, pos ) ) S = Style.RV_VISUAL;
      else if( pV.InStar      ( VL, pos ) ) S = Style.STAR;
      else if( pV.InStarInF   ( VL, pos ) ) S = Style.STAR_IN_F;
      else if( pV.InDefine    ( VL, pos ) ) S = Style.DEFINE;
      else if( pV.InComment   ( VL, pos ) ) S = Style.COMMENT;
      else if( pV.InConst     ( VL, pos ) ) S = Style.CONST;
      else if( pV.InControl   ( VL, pos ) ) S = Style.CONTROL;
      else if( pV.InVarType   ( VL, pos ) ) S = Style.VARTYPE;
    }
    return S;
  }
  Style Get_Style_2( View  pV
                   , final int DL // Diff line
                   , final int VL // View line
                   , final int pos )
  {
    final Diff_Type L_DT = DiffType( pV, DL ); // Line Diff_Type

    if     ( L_DT == Diff_Type.UNKNOWN ) return Style.DIFF_DEL;
    else if( L_DT == Diff_Type.DELETED ) return Style.DIFF_DEL;
    else if( L_DT == Diff_Type.SAME    ) return Get_Style( pV, DL, VL, pos );
    else if( L_DT == Diff_Type.INSERTED )
    {
      Style S = Get_Style( pV, DL, VL, pos );
      return DiffStyle( S );
    }
    else if( L_DT == Diff_Type.DIFF_FILES )
    {
      Style S = Get_Style( pV, DL, VL, pos );
      Style DS = S;
      if     ( S == Style.STAR     ) DS = Style.DIFF_STAR;
      else if( S == Style.STAR_IN_F) DS = Style.DIFF_STAR_IN_F;
      return DS;
    }
    else if( L_DT == Diff_Type.CHANGED )
    {
      Diff_Info di = (pV == m_vS) ? m_DI_List_S.get( DL )
                                  : m_DI_List_L.get( DL );
      if( null == di.pLineInfo )
      {
        return DiffStyle( Get_Style( pV, DL, VL, pos ) );
      }
      else if( pos < di.pLineInfo.size() )
      {
        Diff_Type c_dt = di.pLineInfo.get( pos ); // Char Diff_Type

        if     ( Diff_Type.SAME == c_dt ) return Get_Style( pV, DL, VL, pos );
        else if( Diff_Type.CHANGED  == c_dt
              || Diff_Type.INSERTED == c_dt )
        {
          return DiffStyle( Get_Style( pV, DL, VL, pos ) );
        }
        else if( Diff_Type.DELETED == c_dt ) return Style.DIFF_DEL;
        else /*( Diff_Type.UNKNOWN == c_dt*/ return Style.DIFF_DEL;
      }
    }
    // Fall through.  Should only get here if
    // L_DT == Diff_Type.CHANGED && di.pLineInfo.size() <= pos
    return Style.NORMAL;
  }
//char Get_Char_2( View  pV
//               , final int DL // Diff line
//               , final int VL // View line
//               , final int pos )
//{
//  final Diff_Type L_DT = DiffType( pV, DL ); // Line Diff_Type
//
//  if( L_DT == Diff_Type.UNKNOWN
//   || L_DT == Diff_Type.DELETED )
//  {
//    return '-';
//  }
//  return pV.m_fb.Get( VL, pos );
//}

  // Translation of non-diff styles to diff styles for diff areas
  Style DiffStyle( final Style s )
  {
    // If s is already a DIFF style, just return it
    Style diff_s = s;

    if     ( s == Style.NORMAL
          || s == Style.EMPTY    ) diff_s = Style.DIFF_NORMAL   ;
    else if( s == Style.STAR     ) diff_s = Style.DIFF_STAR     ;
    else if( s == Style.STAR_IN_F) diff_s = Style.DIFF_STAR_IN_F;
    else if( s == Style.COMMENT  ) diff_s = Style.DIFF_COMMENT  ;
    else if( s == Style.DEFINE   ) diff_s = Style.DIFF_DEFINE   ;
    else if( s == Style.CONST    ) diff_s = Style.DIFF_CONST    ;
    else if( s == Style.CONTROL  ) diff_s = Style.DIFF_CONTROL  ;
    else if( s == Style.VARTYPE  ) diff_s = Style.DIFF_VARTYPE  ;
    else if( s == Style.VISUAL   ) diff_s = Style.DIFF_VISUAL   ;

    return diff_s;
  }

  boolean InVisualArea( View pV, final int DL, final int pos )
  {
    // Only one diff view, current view, can be in visual mode.
    if( m_vis.CV() == pV && m_inVisualMode )
    {
      if( m_inVisualBlock ) return InVisualBlock( DL, pos );
      else                  return InVisualStFn ( DL, pos );
    }
    return false;
  }
  boolean InVisualBlock( final int DL, final int pos )
  {
    return ( v_st_line <= DL && DL <= v_fn_line && v_st_char <= pos  && pos  <= v_fn_char ) // bot rite
        || ( v_st_line <= DL && DL <= v_fn_line && v_fn_char <= pos  && pos  <= v_st_char ) // bot left
        || ( v_fn_line <= DL && DL <= v_st_line && v_st_char <= pos  && pos  <= v_fn_char ) // top rite
        || ( v_fn_line <= DL && DL <= v_st_line && v_fn_char <= pos  && pos  <= v_st_char );// top left
  }
  boolean InVisualStFn( final int DL, final int pos )
  {
    if( v_st_line == DL && DL == v_fn_line )
    {
      return (v_st_char <= pos && pos <= v_fn_char)
          || (v_fn_char <= pos && pos <= v_st_char);
    }
    else if( (v_st_line < DL && DL < v_fn_line)
          || (v_fn_line < DL && DL < v_st_line) )
    {
      return true;
    }
    else if( v_st_line == DL && DL < v_fn_line )
    {
      return v_st_char <= pos;
    }
    else if( v_fn_line == DL && DL < v_st_line )
    {
      return v_fn_char <= pos;
    }
    else if( v_st_line < DL && DL == v_fn_line )
    {
      return pos <= v_fn_char;
    }
    else if( v_fn_line < DL && DL == v_st_line )
    {
      return pos <= v_st_char;
    }
    return false;
  }

  void DisplayBanner()
  {
    View pV = m_vis.CV();

    // Command line row in window:
    final int WIN_ROW = WorkingRows( pV ) + 2;
    final int WIN_COL = 0;

    final int G_ROW = Row_Win_2_GL( pV, WIN_ROW );
    final int G_COL = Col_Win_2_GL( pV, WIN_COL );

    if( pV.m_inInsertMode )
    {
      m_console.SetS( G_ROW, G_COL, "--INSERT --", Style.BANNER );
    }
    else if( pV.m_inReplaceMode )
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
    View pV = m_vis.CV();

    final int WC = WorkingCols( pV );
    final int N  = Math.min( WC, 11 );

    // Command line row in window:
    final int WIN_ROW = WorkingRows( pV ) + 2;

    // Clear command line:
    for( int k=0; k<N; k++ )
    {
      m_console.Set( Row_Win_2_GL( pV, WIN_ROW )
                   , Col_Win_2_GL( pV, k )
                   , ' '
                   , Style.NORMAL );
    }
    PrintCursor(); // Does m_console.Update()
  }

  void DisplayMapping()
  {
    if( m_console.get_from_dot_buf() ) return;

    View pV = m_vis.CV();

    final String mapping = "--MAPPING--";
    final int    mapping_len = mapping.length();

    // Command line row in window:
    final int WIN_ROW = WorkingRows( pV ) + 2;

    final int G_ROW = Row_Win_2_GL( pV, WIN_ROW );
    final int G_COL = Col_Win_2_GL( pV, WorkingCols( pV ) - mapping_len );

    m_console.SetS( G_ROW, G_COL, mapping, Style.BANNER );

    PrintCursor(); // Does m_console.Update()
  }

  void Popu_SameList( final DiffArea CA )
  {
    m_sameList.clear();
    ArrayDeque<DiffArea> compList = new ArrayDeque<>();
    compList.push( CA );
    int count = 0;

    while( 0<compList.size() )
    {
      DiffArea ca = compList.pop();
      SameArea same = Find_Max_Same( ca, count++ );

      if( 0<same.m_nlines && 0<same.m_nbytes ) //< Dont count a single empty line as a same area
      {
        m_sameList.add( same );

        final int SAME_FNL_S = same.m_ln_s+same.m_nlines; // Same finish line short
        final int SAME_FNL_L = same.m_ln_l+same.m_nlines; // Same finish line long

        if( ( same.m_ln_s == ca.ln_s || same.m_ln_l == ca.ln_l )
         && SAME_FNL_S < ca.fnl_s()
         && SAME_FNL_L < ca.fnl_l() )
        {
          // Only one new DiffArea after same:
          DiffArea ca1 = new DiffArea( SAME_FNL_S, ca.fnl_s()-SAME_FNL_S
                                     , SAME_FNL_L, ca.fnl_l()-SAME_FNL_L );
          compList.push( ca1 );
        }
        else if( ( SAME_FNL_S == ca.fnl_s() || SAME_FNL_L == ca.fnl_l() )
              && ca.ln_s < same.m_ln_s
              && ca.ln_l < same.m_ln_l )
        {
          // Only one new DiffArea before same:
          DiffArea ca1 = new DiffArea( ca.ln_s, same.m_ln_s-ca.ln_s
                                     , ca.ln_l, same.m_ln_l-ca.ln_l );
          compList.push( ca1 );
        }
        else if( ca.ln_s < same.m_ln_s && SAME_FNL_S < ca.fnl_s()
              && ca.ln_l < same.m_ln_l && SAME_FNL_L < ca.fnl_l() )
        {
          // Two new DiffArea's, one before same, and one after same:
          DiffArea ca1 = new DiffArea( ca.ln_s, same.m_ln_s-ca.ln_s
                                     , ca.ln_l, same.m_ln_l-ca.ln_l );
          DiffArea ca2 = new DiffArea( SAME_FNL_S, ca.fnl_s()-SAME_FNL_S
                                     , SAME_FNL_L, ca.fnl_l()-SAME_FNL_L );
          compList.push( ca1 );
          compList.push( ca2 );
        }
      }
    }
  }

  // New, optimized:
  SameArea Find_Max_Same( final DiffArea ca, final int count )
  {
    SameArea max_same = new SameArea();
    SameArea cur_same = new SameArea();

    for( int _ln_s = ca.ln_s; _ln_s<ca.fnl_s()-max_same.m_nlines; _ln_s++ )
    {
      int ln_s = _ln_s;
      cur_same.Clear();
      for( int ln_l = ca.ln_l; ln_s<ca.fnl_s() && ln_l<ca.fnl_l(); ln_l++ )
      {
        Line ls = m_fS.GetLine( ln_s ); // Line from short view
        Line ll = m_fL.GetLine( ln_l ); // Line from long  view

        if( ls.chksum_diff() != ll.chksum_diff() ) { cur_same.Clear(); ln_s = _ln_s; }
        else {
          if( 0 == max_same.m_nlines   // First line match this outer loop
           || 0 == cur_same.m_nlines ) // First line match since cur_same.Clear()
          {
            cur_same.Init( ln_s, ln_l, ls.length()+1 ); // Add one to account for line delimiter
          }
          else { // Continuation of cur_same
            cur_same.Inc( Math.min( ls.length()+1, ll.length()+1 ) ); // Add one to account for line delimiter
          }
          if( max_same.m_nbytes < cur_same.m_nbytes )
          {
            max_same.Set( cur_same );
          }
          ln_s++;
        }
      }
      // This line makes the diff run faster:
      if( 0<max_same.m_nlines ) _ln_s = Math.max( _ln_s, max_same.m_ln_s+max_same.m_nlines-1 );
    }
    return max_same;
  }

  void Sort_SameList()
  {
    // Sorts m_sameList from least to greatest in terms of SameArea.m_ln_l
    final int SLL = m_sameList.size();

    for( int k=0; k<SLL; k++ )
    {
      for( int j=SLL-1; k<j; j-- )
      {
        SameArea sa0 = m_sameList.get( j-1 );
        SameArea sa1 = m_sameList.get( j   );

        if( sa1.m_ln_l < sa0.m_ln_l )
        {
          m_sameList.set( j-1, sa1 );
          m_sameList.set( j  , sa0 );
        }
      }
    }
  }

  void PrintSameList()
  {
    for( int k=0; k<m_sameList.size(); k++ )
    {
      SameArea same = m_sameList.get( k );

      System.out.printf(
          "Same: (%s):(%d-%d), (%s):(%d-%d), nlines=%d, nbytes=%d\n"
        , m_fS.m_pname, same.m_ln_s+1, same.m_ln_s+same.m_nlines
        , m_fL.m_pname, same.m_ln_l+1, same.m_ln_l+same.m_nlines
        , same.m_nlines
        , same.m_nbytes );
    }
  }

  void Popu_DiffList( final DiffArea CA )
  {
    m_diffList.clear();

    Popu_DiffList_Begin( CA );

    final int SLL = m_sameList.size();

    for( int k=1; k<SLL; k++ )
    {
      SameArea sa0 = m_sameList.get( k-1 );
      SameArea sa1 = m_sameList.get( k   );

      int da_ln_s = sa0.m_ln_s+sa0.m_nlines;
      int da_ln_l = sa0.m_ln_l+sa0.m_nlines;

      DiffArea da = new DiffArea( da_ln_s                 // da.ln_s
                                , sa1.m_ln_s - da_ln_s    // da.nline_s
                                , da_ln_l                 // da.ln_l
                                , sa1.m_ln_l - da_ln_l ); // da.nline_l
      m_diffList.add( da );
    }
    Popu_DiffList_End( CA );
  }

  void Popu_DiffList_Begin( final DiffArea CA )
  {
    if( 0<m_sameList.size() ) // Add DiffArea before first SameArea if needed:
    {
      SameArea sa = m_sameList.get( 0 );

      final int nlines_s_da = sa.m_ln_s - CA.ln_s; // Num lines in short diff area
      final int nlines_l_da = sa.m_ln_l - CA.ln_l; // Num lines in long  diff area

      if( 0<nlines_s_da || 0<nlines_l_da )
      {
        // DiffArea at beginning of DiffArea:
        DiffArea da = new DiffArea( CA.ln_s, nlines_s_da, CA.ln_l, nlines_l_da );
        m_diffList.add( da );
      }
    }
  }

  void Popu_DiffList_End( final DiffArea CA )
  {
    final int SLL = m_sameList.size();
    final int nLines_S_CA = CA.nlines_s;
    final int nLines_L_CA = CA.nlines_l;

    if( 0<SLL ) // Add DiffArea after last SameArea if needed:
    {
      SameArea sa = m_sameList.get( SLL-1 );
      final int sa_s_end = sa.m_ln_s + sa.m_nlines;
      final int sa_l_end = sa.m_ln_l + sa.m_nlines;

      if( sa_s_end < CA.fnl_s()
       || sa_l_end < CA.fnl_l() ) // DiffArea at end of file:
      {
        // Number of lines of short and long equal to
        // start of SameArea short and long
        DiffArea da = new DiffArea( sa_s_end
                                  , CA.fnl_s() - sa_s_end
                                  , sa_l_end
                                  , CA.fnl_l() - sa_l_end );
        m_diffList.add( da );
      }
    }
    else // No SameArea, so whole DiffArea is a DiffArea:
    {
      DiffArea da = new DiffArea( CA.ln_s, nLines_S_CA, CA.ln_l, nLines_L_CA );
      m_diffList.add(da );
    }
  }

  void PrintDiffList()
  {
    for( int k=0; k<m_diffList.size(); k++ )
    {
      DiffArea da = m_diffList.get( k );
    //System.out.printf( "Diff: (%s):(%d-%d), (%s):(%d-%d)\n"
      System.out.printf( "Diff: (%s):(%d-%d)\n      (%s):(%d-%d)\n"
             , m_fS.m_pname, da.ln_s+1, da.ln_s+da.nlines_s
             , m_fL.m_pname, da.ln_l+1, da.ln_l+da.nlines_l );
    }
  }

  void Popu_DI_List( final DiffArea CA )
  {
  //Clear_DI_List_CA( CA.ln_s, CA.fnl_s(), m_DI_List_S );
  //Clear_DI_List_CA( CA.ln_l, CA.fnl_l(), m_DI_List_L );

    final int SLL = m_sameList.size();
    final int DLL = m_diffList.size();

    if     ( SLL <= 0 ) Popu_DI_List_NoSameArea();
    else if( DLL <= 0 ) Popu_DI_List_NoDiffArea();
    else                Popu_DI_List_DiffAndSame( CA );
  }
  void PrintDI_List( final DiffArea CA )
  {
    final int DILL = m_DI_List_S.size();

    for( int k=CA.ln_s; k<DILL; k++ )
    {
      Diff_Info dis = m_DI_List_S.get(k);
      Diff_Info dil = m_DI_List_L.get(k);

      Utils.Log("DIS ("+(dis.line_num+1)+":"+dis.diff_type+"),"
              +" DIL ("+(dil.line_num+1)+","+dil.diff_type+")");

      if( CA.fnl_s() <= dis.line_num ) break;
    }
  }

//void Clear_DI_List_CA( final int st_line
//                     , final int fn_line
//                     , ArrayList<Diff_Info> DI_List )
//{
//  // Since, Clear_DI_List_CA will only be call when DI_List is
//  // fully populated, the Diff_Info.line_num's will be at indexes
//  // greater than or equal to st_line
//  for( int k=st_line; k<DI_List.size(); k++ )
//  {
//    Diff_Info di = DI_List.get( k );
//
//    if( st_line <= di.line_num && di.line_num < fn_line )
//    {
//      DI_List.remove( k );
//    }
//    else if( fn_line <= di.line_num )
//    {
//      // Past the range of line_num's we want to remove
//      break;
//    }
//  }
//}

  void Popu_DI_List_NoSameArea()
  {
    // Should only be one DiffArea, which is the whole DiffArea:
    final int DLL = m_diffList.size();
    Utils.Assert( DLL==1, "DLL = "+ DLL +", Expected 1");

    Popu_DI_List_AddDiff( m_diffList.get( 0 ) );
  }

  void Popu_DI_List_NoDiffArea()
  {
    // Should only be one SameArea, which is the whole DiffArea:
    final int SLL = m_sameList.size();
    Utils.Assert( SLL==1, "SLL = "+ SLL +", Expected 1");

    Popu_DI_List_AddSame( m_sameList.get( 0 ) );
  }

  void Popu_DI_List_DiffAndSame( final DiffArea CA )
  {
    final int SLL = m_sameList.size();
    final int DLL = m_diffList.size();
    DiffArea  da  = m_diffList.get( 0 );

    if( CA.ln_s==da.ln_s && CA.ln_l==da.ln_l )
    {
      // Start with DiffArea, and then alternate between SameArea and DiffArea.
      // There should be at least as many DiffArea's as SameArea's.
      Utils.Assert( SLL<=DLL, "SLL<=DLL" );

      for( int k=0; k<SLL; k++ )
      {
        DiffArea da1 = m_diffList.get( k ); Popu_DI_List_AddDiff( da1 );
        SameArea sa1 = m_sameList.get( k ); Popu_DI_List_AddSame( sa1 );
      }
      if( SLL < DLL )
      {
        Utils.Assert( SLL+1==DLL, "SLL+1==DLL" );
        DiffArea da1 = m_diffList.get( DLL-1 ); Popu_DI_List_AddDiff( da1 );
      }
    }
    else {
      // Start with SameArea, and then alternate between DiffArea and SameArea.
      // There should be at least as many SameArea's as DiffArea's.
      Utils.Assert( DLL<=SLL, "DLL<=SLL" );

      for( int k=0; k<DLL; k++ )
      {
        SameArea sa1 = m_sameList.get( k ); Popu_DI_List_AddSame( sa1 );
        DiffArea da1 = m_diffList.get( k ); Popu_DI_List_AddDiff( da1 );
      }
      if( DLL < SLL )
      {
        Utils.Assert( DLL+1==SLL, "DLL+1==SLL" );
        SameArea sa = m_sameList.get( SLL-1 ); Popu_DI_List_AddSame( sa );
      }
    }
  }

  void Popu_DI_List_AddSame( final SameArea sa )
  {
    for( int k=0; k<sa.m_nlines; k++ )
    {
      final Diff_Type DT = Popu_DI_List_Have_Diff_Files( sa.m_ln_s+k, sa.m_ln_l+k )
                         ? Diff_Type.DIFF_FILES
                         : Diff_Type.SAME;

      Diff_Info dis = new Diff_Info( DT, sa.m_ln_s+k, null );
      Diff_Info dil = new Diff_Info( DT, sa.m_ln_l+k, null );

      m_DI_List_S.add( m_DI_L_ins_idx, dis );
      m_DI_List_L.add( m_DI_L_ins_idx, dil ); m_DI_L_ins_idx++;
    }
  }

  // Returns true if the two lines, line_s and line_l, in the two files
  // being compared, are the names of files that differ
  boolean Popu_DI_List_Have_Diff_Files( final int line_s, final int line_l )
  {
    boolean files_differ = false;

    if( m_fS.m_isDir && m_fL.m_isDir )
    {
      String fname_s = m_fS.GetLine( line_s ).toString();
      String fname_l = m_fL.GetLine( line_l ).toString();

      if( !fname_s.equals("..") && !fname_s.endsWith( Utils.DIR_DELIM_STR )
       && !fname_l.equals("..") && !fname_l.endsWith( Utils.DIR_DELIM_STR ) )
      {
        // pname_s and pname_l should both be full paths of regular files
        String pname_s = m_fS.m_dname + fname_s;
        String pname_l = m_fL.m_dname + fname_l;

        // Add files if they have not been added:
        m_vis.NotHaveFileAddFile( pname_s );
        m_vis.NotHaveFileAddFile( pname_l );

        FileBuf fb_s = m_vis.get_FileBuf( pname_s );
        FileBuf fb_l = m_vis.get_FileBuf( pname_l );

        if( fb_s != null && fb_l != null )
        {
          // Fast: Compare files already cached in memory:
          files_differ = !Utils.Files_Are_Same( fb_s, fb_l );
        }
        else { // Should never get here:
          // Slow: Compare files by opening and reading from the file system:
          files_differ = !Utils.Files_Are_Same( pname_s, pname_l );
        }
      }
    }
    return files_differ;
  }

  void Popu_DI_List_AddDiff( final DiffArea da )
  {
    if( da.nlines_s < da.nlines_l )
    {
      Popu_DI_List_AddDiff_Common( da.ln_s
                                 , da.ln_l
                                 , da.nlines_s
                                 , da.nlines_l
                                 , m_DI_List_S
                                 , m_DI_List_L
                                 , m_fS, m_fL );
    }
    else if( da.nlines_l < da.nlines_s )
    {
      Popu_DI_List_AddDiff_Common( da.ln_l
                                 , da.ln_s
                                 , da.nlines_l
                                 , da.nlines_s
                                 , m_DI_List_L
                                 , m_DI_List_S
                                 , m_fL, m_fS );
    }
    else // da.nlines_s == da.nlines_l
    {
      for( int k=0; k<da.nlines_l; k++ )
      {
        Line ls = m_fS.GetLine( da.ln_s+k ); // Line from short area
        Line ll = m_fL.GetLine( da.ln_l+k ); // Line from long  area

        LineInfo li_s = new LineInfo();
        LineInfo li_l = new LineInfo();

        int bytes_same = Compare_Lines( ls, li_s, ll, li_l );

        Diff_Info dis = new Diff_Info( Diff_Type.CHANGED, da.ln_s+k, li_s );
        Diff_Info dil = new Diff_Info( Diff_Type.CHANGED, da.ln_l+k, li_l );
        m_DI_List_S.add( m_DI_L_ins_idx, dis );
        m_DI_List_L.add( m_DI_L_ins_idx, dil ); m_DI_L_ins_idx++;
      }
    }
  }

  void Popu_DI_List_AddDiff_Common( final int da_ln_s
                                  , final int da_ln_l
                                  , final int da_nlines_s
                                  , final int da_nlines_l
                                  , ArrayList<Diff_Info> DI_List_s
                                  , ArrayList<Diff_Info> DI_List_l
                                  , FileBuf   pfs
                                  , FileBuf   pfl )
  {
    Popu_SimiList( da_ln_s
                 , da_ln_l
                 , da_nlines_s
                 , da_nlines_l
                 , pfs
                 , pfl );
    Sort_SimiList();
  //PrintSimiList();

    SimiList_2_DI_Lists( da_ln_s
                       , da_ln_l
                       , da_nlines_s
                       , da_nlines_l
                       , DI_List_s
                       , DI_List_l );
  }

  void Popu_SimiList( final int da_ln_s
                    , final int da_ln_l
                    , final int da_nlines_s
                    , final int da_nlines_l
                    , FileBuf   pfs
                    , FileBuf   pfl )
  {
    m_simiList.clear();

    if( 0<da_nlines_s && 0<da_nlines_l )
    {
      DiffArea ca = new DiffArea( da_ln_s, da_nlines_s
                                , da_ln_l, da_nlines_l );

      ArrayDeque<DiffArea> compList = new ArrayDeque<>();
                           compList.add( ca );

      while( 0 < compList.size() )
      {
        ca = compList.pop();
        SimLines siml = Find_Lines_Most_Same( ca, pfs, pfl );

        if( m_simiList.size() == da_nlines_s )
        {
          // Not putting siml into m_simiList, so delete any new'ed memory:
          siml.li_s = null;
          siml.li_l = null;
          return;
        }
        m_simiList.add( siml );
        if( ( siml.ln_s == ca.ln_s || siml.ln_l == ca.ln_l )
         && siml.ln_s+1 < ca.fnl_s()
         && siml.ln_l+1 < ca.fnl_l() )
        {
          // Only one new DiffArea after siml:
          DiffArea ca1 = new DiffArea( siml.ln_s+1, ca.fnl_s()-siml.ln_s-1
                                     , siml.ln_l+1, ca.fnl_l()-siml.ln_l-1 );
          compList.push( ca1 );
        }
        else if( ( siml.ln_s+1 == ca.fnl_s() || siml.ln_l+1 == ca.fnl_l() )
              && ca.ln_s < siml.ln_s
              && ca.ln_l < siml.ln_l )
        {
          // Only one new DiffArea before siml:
          DiffArea ca1 = new DiffArea( ca.ln_s, siml.ln_s-ca.ln_s
                                     , ca.ln_l, siml.ln_l-ca.ln_l );
          compList.push( ca1 );
        }
        else if( ca.ln_s < siml.ln_s && siml.ln_s+1 < ca.fnl_s()
              && ca.ln_l < siml.ln_l && siml.ln_l+1 < ca.fnl_l() )
        {
          // Two new DiffArea's, one before siml, and one after siml:
          DiffArea ca1 = new DiffArea( ca.ln_s, siml.ln_s-ca.ln_s
                                     , ca.ln_l, siml.ln_l-ca.ln_l );
          DiffArea ca2 = new DiffArea( siml.ln_s+1, ca.fnl_s()-siml.ln_s-1
                                     , siml.ln_l+1, ca.fnl_l()-siml.ln_l-1 );
          compList.push( ca1 );
          compList.push( ca2 );
        }
      }
    }
  }

  void Sort_SimiList()
  {
    // Sorts m_simiList from least to greatest in terms of SameLines.ln_l
    final int SLL = m_simiList.size();

    for( int k=0; k<SLL; k++ )
    {
      for( int j=SLL-1; k<j; j-- )
      {
        SimLines sl0 = m_simiList.get( j-1 );
        SimLines sl1 = m_simiList.get( j   );

        if( sl1.ln_l < sl0.ln_l )
        {
          m_simiList.set( j-1, sl1 );
          m_simiList.set( j  , sl0 );
        }
      }
    }
  }

  void PrintSimiList()
  {
    final int SLL = m_simiList.size();

    for( int k=0; k<SLL; k++ )
    {
      SimLines sl = m_simiList.get( k );

      System.out.printf("SimLines: ln_s=%u, ln_l=%u, nbytes=%u\n"
                       , sl.ln_s+1, sl.ln_l+1, sl.nbytes );
    }
  }

  void SimiList_2_DI_Lists( final int da_ln_s
                          , final int da_ln_l
                          , final int da_nlines_s
                          , final int da_nlines_l
                          , ArrayList<Diff_Info> DI_List_s
                          , ArrayList<Diff_Info> DI_List_l )
  {
    // Since diff info short has by default a Diff_Type of deleted,
    // it gets the line number of the previous non-deleted line.
    // Diff info short line number:
    int dis_ln = 0<da_ln_s ? da_ln_s-1 : 0;

    for( int k=0; k<da_nlines_l; k++ )
    {
      Diff_Info dis = new Diff_Info( Diff_Type.DELETED , dis_ln   , null );
      Diff_Info dil = new Diff_Info( Diff_Type.INSERTED, da_ln_l+k, null );

      for( int j=0; j<m_simiList.size(); j++ )
      {
        SimLines siml = m_simiList.get( j );

        if( siml.ln_l == da_ln_l+k )
        {
          dis.diff_type = Diff_Type.CHANGED;
          dis.line_num  = siml.ln_s;
          dis.pLineInfo = siml.li_s;

          dil.diff_type = Diff_Type.CHANGED;
          dil.pLineInfo = siml.li_l;

          dis_ln = dis.line_num;
          break;
        }
      }
      // DI_List_s and DI_List_l now own LineInfo objects:
      DI_List_s.add( m_DI_L_ins_idx, dis );
      DI_List_l.add( m_DI_L_ins_idx, dil ); m_DI_L_ins_idx++;
    }
  }

  SimLines Find_Lines_Most_Same( DiffArea ca, FileBuf pfs, FileBuf pfl )
  {
    // LD = Length Difference between long area and short area
    final int LD = ca.nlines_l - ca.nlines_s;

    SimLines most_same = new SimLines();
    for( int ln_s = ca.ln_s; ln_s<ca.fnl_s(); ln_s++ )
    {
      final int ST_L = ca.ln_l+(ln_s-ca.ln_s);

      for( int ln_l = ST_L; ln_l<ca.fnl_l() && ln_l<ST_L+LD+1; ln_l++ )
      {
        Line ls = pfs.GetLine( ln_s ); // Line from short area
        Line ll = pfl.GetLine( ln_l ); // Line from long  area

        LineInfo li_s = new LineInfo();
        LineInfo li_l = new LineInfo();
        int bytes_same = Compare_Lines( ls, li_s, ll, li_l );

        if( most_same.nbytes < bytes_same )
        {
          most_same.ln_s   = ln_s;
          most_same.ln_l   = ln_l;
          most_same.nbytes = bytes_same;
          most_same.li_s   = li_s; // Hand off li_s
          most_same.li_l   = li_l; // and      li_l
        }
      }
    }
    if( 0==most_same.nbytes )
    {
      // This if() block ensures that each line in the short DiffArea is
      // matched to a line in the long DiffArea.  Each line in the short
      // DiffArea must be matched to a line in the long DiffArea or else
      // SimiList_2_DI_Lists wont work right.
      most_same.ln_s   = ca.ln_s;
      most_same.ln_l   = ca.ln_l;
      most_same.nbytes = 1;
    }
    return most_same;
  }

  // Returns number of bytes that are the same between the two lines
  // and fills in li_s and li_l
  int Compare_Lines( Line ls, LineInfo li_s
                   , Line ll, LineInfo li_l )
  {
    if( 0==ls.length() && 0==ll.length() ) { return 1; }
    li_s.clear(); li_l.clear();
    Line pls = ls; LineInfo pli_s = li_s;
    Line pll = ll; LineInfo pli_l = li_l;
    if( ll.length() < ls.length() ) { pls = ll; pli_s = li_l;
                                      pll = ls; pli_l = li_s; }
    final int SLL = pls.length();
    final int LLL = pll.length();

    pli_l.clear(); pli_l.ensureCapacity( LLL );
    pli_s.clear(); pli_s.ensureCapacity( LLL );

    int num_same = 0;
    int i_s = 0;
    int i_l = 0;

    while( i_s < SLL && i_l < LLL )
    {
      final char cs = pls.charAt( i_s );
      final char cl = pll.charAt( i_l );

      if( cs == cl )
      {
        num_same++;
        pli_s.add( Diff_Type.SAME ); i_s++;
        pli_l.add( Diff_Type.SAME ); i_l++;
      }
      else {
        final int remaining_s = SLL - i_s;
        final int remaining_l = LLL - i_l;

        if( 0<remaining_s
         && 0<remaining_l && remaining_s == remaining_l )
        {
          pli_s.add( Diff_Type.CHANGED ); i_s++;
          pli_l.add( Diff_Type.CHANGED ); i_l++;
        }
        else if( remaining_s < remaining_l )
        {
          pli_l.add( Diff_Type.INSERTED ); i_l++;
        }
        else if( remaining_l < remaining_s )
        {
          pli_s.add( Diff_Type.INSERTED ); i_s++;
        }
      }
    }
    for( int k=SLL; k<LLL; k++ ) pli_s.add( Diff_Type.DELETED );
    for( int k=i_l; k<LLL; k++ ) pli_l.add( Diff_Type.INSERTED );

    return num_same;
  }

//void Set_crsRow( final int row )
//{
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
    View pV = m_vis.CV();

  //m_console.Set_Crs_Cell( this, pV, pV, m_crsRow, m_crsCol );
    m_console.Set_Crs_Cell( this, pV, m_crsRow, m_crsCol );
  }
  void Set_Console_CrsCell( View pV_old, View pV_new )
  {
  //m_console.Set_Crs_Cell( this, pV_old, pV_new, m_crsRow, m_crsCol );
    m_console.Set_Crs_Cell( this, pV_new, m_crsRow, m_crsCol );
  }

  void GoToCrsPos_Write( final int ncp_crsLine
                       , final int ncp_crsChar )
  {
    final int OCL = CrsLine();
    final int OCP = CrsChar();
    final int NCL = ncp_crsLine;
    final int NCP = ncp_crsChar;

    if( OCL == NCL && OCP == NCP )
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
      View pV = m_vis.CV();

      // These moves refer to View of buffer:
      final boolean MOVE_DOWN  = BotLine( pV )   < NCL;
      final boolean MOVE_RIGHT = RightChar( pV ) < NCP;
      final boolean MOVE_UP    = NCL < m_topLine;
      final boolean MOVE_LEFT  = NCP < m_leftChar;

      boolean redraw = MOVE_DOWN || MOVE_RIGHT || MOVE_UP || MOVE_LEFT;

      if( redraw )
      {
        if     ( MOVE_DOWN ) m_topLine = NCL - WorkingRows( pV ) + 1;
        else if( MOVE_UP   ) m_topLine = NCL;

        if     ( MOVE_RIGHT ) m_leftChar = NCP - WorkingCols( pV ) + 1;
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
        PrintStsLine( pV );
        PrintCmdLine( pV );
        PrintCursor();  // Put cursor into position.
      }
    }
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
      View    pV  = m_vis.CV();
      FileBuf pfb = pV.m_fb;
      // Convert OCL and NCL, which are diff lines, to view lines:
      final int OCLv = ViewLine( pV, OCL );

      for( int k=OCP; k<NCP; k++ )
      {
        final char C = pfb.Get( OCLv, k );
        m_console.Set( Line_2_GL( pV, OCL ), Char_2_GL( pV, k )
                     , C, Get_Style(pV,OCL,OCLv,k) );
      }
    }
    else { // Multiple lines
      GoToCrsPos_WV_Forward_FirstLine( OCL, OCP );
      GoToCrsPos_WV_Forward_MiddleLines( OCL, NCL );
      GoToCrsPos_WV_Forward_LastLine( NCL, NCP );
    }
  }
  void GoToCrsPos_WV_Forward_FirstLine( final int OCL, final int OCP )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;
    // Convert OCL, which is a diff line, to view line:
    final int OCLv = ViewLine( pV, OCL );

    final Diff_Type FIRST_LINE_DIFF_TYPE = DiffType( pV, OCL );

    if( FIRST_LINE_DIFF_TYPE != Diff_Type.DELETED )
    {
      final int OCLL = pfb.LineLen( OCLv ); // Old cursor line length
      final int END_FIRST_LINE = Math.min( RightChar( pV )+1, OCLL );

      // Empty line gets one highlighted space at beginning:
      if( OCLL == 0 && m_leftChar == 0 )
      {
        final Style S = InVisualArea( pV, OCL, 0 ) ? Style.RV_NORMAL : Style.EMPTY;
        m_console.Set( Line_2_GL( pV, OCL ), Char_2_GL( pV, 0 ), ' ', S );
      }
      for( int k=OCP; k<END_FIRST_LINE; k++ )
      {
        final char C = pfb.Get( OCLv, k );
        m_console.Set( Line_2_GL( pV, OCL ), Char_2_GL( pV, k )
                     , C, Get_Style(pV,OCL,OCLv,k) );
      }
    }
  }
  void GoToCrsPos_WV_Forward_MiddleLines( final int OCL, final int NCL )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    for( int l=OCL+1; l<NCL; l++ )
    {
      final Diff_Type LINE_DIFF_TYPE = DiffType( pV, l );
      if( LINE_DIFF_TYPE != Diff_Type.DELETED )
      {
        final int Vl = ViewLine( pV, l );
        final int LL = pfb.LineLen( Vl ); // Line length
        final int END_OF_LINE = Math.min( RightChar( pV )+1, LL );

        // Empty line gets one highlighted space at beginning:
        if( LL == 0 && m_leftChar == 0 )
        {
          final Style S = InVisualArea( pV, l, 0 ) ? Style.RV_NORMAL : Style.EMPTY;
          m_console.Set( Line_2_GL( pV, l ), Char_2_GL( pV, 0 ), ' ', S );
        }
        for( int k=m_leftChar; k<END_OF_LINE; k++ )
        {
          final char C = pfb.Get( Vl, k );
          m_console.Set( Line_2_GL( pV, l ), Char_2_GL( pV, k )
                       , C, Get_Style(pV,l,Vl,k) );
        }
      }
    }
  }
  void GoToCrsPos_WV_Forward_LastLine( final int NCL, final int NCP )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;
    // Convert NCL, which is a diff line, to view line:
    final int NCLv = ViewLine( pV, NCL );

    final Diff_Type LAST_LINE_DIFF_TYPE = DiffType( pV, NCL );

    if( LAST_LINE_DIFF_TYPE != Diff_Type.DELETED )
    {
      // Print from beginning of next line to new cursor position:
      final int NCLL = pfb.LineLen( NCLv ); // Line length
      final int END_LAST_LINE = Math.min( NCLL, NCP );

      for( int k=m_leftChar; k<END_LAST_LINE; k++ )
      {
        final char C = pfb.Get( NCLv, k );
        m_console.Set( Line_2_GL( pV, NCL ), Char_2_GL( pV, k )
                     , C, Get_Style(pV,NCL,NCLv,k)  );
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
      View    pV  = m_vis.CV();
      FileBuf pfb = pV.m_fb;
      // Convert OCL and NCL, which are diff lines, to view lines:
      final int OCLv = ViewLine( pV, OCL );

      for( int k=OCP; NCP<k; k-- )
      {
        final char C = pfb.Get( OCLv, k );
        m_console.Set( Line_2_GL( pV, OCL ), Char_2_GL( pV, k )
                     , C, Get_Style(pV,OCL,OCLv,k) );
      }
    }
    else { // Multiple lines
      GoToCrsPos_WV_Backward_FirstLine( OCL, OCP );
      GoToCrsPos_WV_Backward_MiddleLines( OCL, NCL );
      GoToCrsPos_WV_Backward_LastLine( NCL, NCP );
    }
  }
  void GoToCrsPos_WV_Backward_FirstLine( final int OCL, final int OCP )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;
    // Convert OCL, which is a diff line, to view line:
    final int OCLv = ViewLine( pV, OCL );

    final Diff_Type FIRST_LINE_DIFF_TYPE = DiffType( pV, OCL );

    if( FIRST_LINE_DIFF_TYPE != Diff_Type.DELETED )
    {
      final int OCLL = pfb.LineLen( OCLv ); // Old cursor line length
      final int RIGHT_MOST_POS = Math.min( OCP, 0<OCLL ? OCLL-1 : 0 );

      for( int k=RIGHT_MOST_POS; m_leftChar<k; k-- )
      {
        final char C = pfb.Get( OCLv, k );
        m_console.Set( Line_2_GL( pV, OCL ), Char_2_GL( pV, k )
                     , C, Get_Style(pV,OCL,OCLv,k) );
      }
      if( m_leftChar < OCLL ) {
        final char C = pfb.Get( OCLv, m_leftChar );
        m_console.Set( Line_2_GL( pV, OCL ), Char_2_GL( pV, m_leftChar )
                     , C, Get_Style(pV,OCL,OCLv,m_leftChar) );
      }
      // Empty line gets one highlighted space at beginning:
      if( OCLL == 0 && m_leftChar == 0 )
      {
        final Style S = InVisualArea( pV, OCL, 0 ) ? Style.RV_NORMAL : Style.EMPTY;
        m_console.Set( Line_2_GL( pV, OCL ), Char_2_GL( pV, 0 ), ' ', S );
      }
    }
  }
  void GoToCrsPos_WV_Backward_MiddleLines( final int OCL, final int NCL )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    // Write out intermediate lines:
    for( int l=OCL-1; NCL<l; l-- )
    {
      final Diff_Type LINE_DIFF_TYPE = DiffType( pV, l );
      if( LINE_DIFF_TYPE != Diff_Type.DELETED )
      {
        // Convert l, which is diff line, to view line:
        final int Vl = ViewLine( pV, l );
        final int LL = pfb.LineLen( Vl ); // Line length
        final int END_OF_LINE = Math.min( RightChar( pV ), 0<LL ? LL-1 : 0 );

        for( int k=END_OF_LINE; m_leftChar<k; k-- )
        {
          final char C = pfb.Get( Vl, k );
          m_console.Set( Line_2_GL( pV, l ), Char_2_GL( pV, k )
                       , C, Get_Style(pV,l,Vl,k) );
        }
        if( m_leftChar < LL ) {
          final char C = pfb.Get( Vl, m_leftChar );
          m_console.Set( Line_2_GL( pV, l ), Char_2_GL( pV, m_leftChar )
                       , C, Get_Style(pV,l,Vl,m_leftChar) );
        }
        // Empty line gets one highlighted space at beginning:
        if( LL == 0 && m_leftChar == 0 )
        {
          final Style S = InVisualArea( pV, l, 0 ) ? Style.RV_NORMAL : Style.EMPTY;
          m_console.Set( Line_2_GL( pV, l ), Char_2_GL( pV, 0 ), ' ', S );
        }
      }
    }
  }
  void GoToCrsPos_WV_Backward_LastLine( final int NCL, final int NCP )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;
    // Convert NCL, which is a diff line, to view line:
    final int NCLv = ViewLine( pV, NCL );

    final Diff_Type LAST_LINE_DIFF_TYPE = DiffType( pV, NCL );

    if( LAST_LINE_DIFF_TYPE != Diff_Type.DELETED )
    {
      // Print from end of last line to new cursor position:
      final int NCLL = pfb.LineLen( NCLv ); // New cursor line length
      final int END_LAST_LINE = Math.min( RightChar( pV ), 0<NCLL ? NCLL-1 : 0 );

      for( int k=END_LAST_LINE; NCP<k; k-- )
      {
        final char C = pfb.Get( NCLv, k );
        m_console.Set( Line_2_GL( pV, NCL ), Char_2_GL( pV, k ), C
                     , Get_Style(pV,NCL,NCLv,k) );
      }
    }
  }

  void GoToCrsPos_Write_VisualBlock( final int OCL
                                   , final int OCP
                                   , final int NCL
                                   , final int NCP )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;
    // v_fn_line == NCL && v_fn_char == NCP, so dont need to include
    // v_fn_line       and v_fn_char in Min and Max calls below:
    final int vis_box_left = Math.min( v_st_char, Math.min( OCP, NCP ) );
    final int vis_box_rite = Math.max( v_st_char, Math.max( OCP, NCP ) );
    final int vis_box_top  = Math.min( v_st_line, Math.min( OCL, NCL ) );
    final int vis_box_bot  = Math.max( v_st_line, Math.max( OCL, NCL ) );

    final int draw_box_left = Math.max( m_leftChar   , vis_box_left );
    final int draw_box_rite = Math.min( RightChar(pV), vis_box_rite );
    final int draw_box_top  = Math.max( m_topLine    , vis_box_top  );
    final int draw_box_bot  = Math.min( BotLine(pV)  , vis_box_bot  );

    for( int DL=draw_box_top; DL<=draw_box_bot; DL++ )
    {
      final int VL = ViewLine( pV, DL ); // View line number

      final int LL = pfb.LineLen( VL );

      for( int k=draw_box_left; k<LL && k<=draw_box_rite; k++ )
      {
        final char  C = pfb.Get( VL, k );
        final Style S = Get_Style( pV, DL, VL, k );

        m_console.Set( Line_2_GL( pV, DL ), Char_2_GL( pV, k ), C, S );
      }
    }
    Set_crsRowCol( NCL - m_topLine, NCP - m_leftChar );

    PrintCursor(); // Does m_console.Update()
  }

  void GoToCrsPos_NoWrite( final int ncp_crsLine
                         , final int ncp_crsChar )
  {
    View pV = m_vis.CV();

    // These moves refer to View of buffer:
    final boolean MOVE_DOWN  = BotLine( pV )   < ncp_crsLine;
    final boolean MOVE_RIGHT = RightChar( pV ) < ncp_crsChar;
    final boolean MOVE_UP    = ncp_crsLine     < m_topLine;
    final boolean MOVE_LEFT  = ncp_crsChar     < m_leftChar;

    if     ( MOVE_DOWN ) m_topLine = ncp_crsLine - WorkingRows( pV ) + 1;
    else if( MOVE_UP   ) m_topLine = ncp_crsLine;
    m_crsRow  = ncp_crsLine - m_topLine;

    if     ( MOVE_RIGHT ) m_leftChar = ncp_crsChar - WorkingCols( pV ) + 1;
    else if( MOVE_LEFT  ) m_leftChar = ncp_crsChar;
    m_crsCol   = ncp_crsChar - m_leftChar;

    Set_Console_CrsCell();
  }

  void PageDown()
  {
    final int NUM_LINES = NumLines();

    if( 0<NUM_LINES )
    {
      View pV = m_vis.CV();

      // new diff top line:
      final int newTopLine = m_topLine + WorkingRows( pV ) - 1;
      // Subtracting 1 above leaves one line in common between the 2 pages.

      if( newTopLine < NUM_LINES )
      {
        m_crsCol = 0;
        m_topLine = newTopLine;

        // Dont let cursor go past the end of the file:
        if( NUM_LINES <= CrsLine() )
        {
          // This line places the cursor at the top of the screen, which I prefer:
          m_crsRow = 0;
        }
        Update();
      }
    }
  }

  void PageUp()
  {
    // Dont scroll if we are at the top of the file:
    if( 0<m_topLine )
    {
      //Leave m_crsRow unchanged.
      m_crsCol = 0;

      View pV = m_vis.CV();

      // Dont scroll past the top of the file:
      if( m_topLine < WorkingRows( pV ) - 1 )
      {
        m_topLine = 0;
      }
      else {
        m_topLine -= WorkingRows( pV ) - 1;
      }
      Update();
    }
  }

  void GoDown( final int num )
  {
    final int NUM_LINES = NumLines();
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
    final int NUM_LINES = NumLines();
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

    if( 0<NumLines() && 0<OCP )
    {
      final int NCP = Math.max( 0, OCP-num ); // New cursor position
      final int CL  = CrsLine(); // Cursor line

      GoToCrsPos_Write( CL, NCP );
    }
  }
  void GoRight( final int num )
  {
    if( 0<NumLines() )
    {
      final int CL  = CrsLine(); // Cursor line
      final int LL  = LineLen();
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
    if( 0<NumLines() )
    {
      final int CL = CrsLine(); // Cursor line

      GoToCrsPos_Write( CL, 0 );
    }
  }
  void GoToEndOfLine()
  {
    if( 0<NumLines() )
    {
      final int LL = LineLen();

      final int OCL = CrsLine(); // Old cursor line

      GoToCrsPos_Write( OCL, 0<LL ? LL-1 : 0 );
    }
  }

  void GoToBegOfNextLine()
  {
    final int NUM_LINES = NumLines();

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

  void GoToEndOfNextLine()
  {
    final int NUM_LINES = NumLines(); // Diff

    if( 0<NUM_LINES )
    {
      final int OCL = CrsLine(); // Old cursor diff line

      if( OCL < (NUM_LINES-1) )
      {
        // Before last line, so can go down
        View    pV  = m_vis.CV();
        FileBuf pfb = pV.m_fb;
        final int VL = ViewLine( pV, OCL+1 ); // View line
        final int LL = pfb.LineLen( VL );

        GoToCrsPos_Write( OCL+1, Utils.LLM1( LL ) );
      }
    }
  }

  void GoToLine( final int user_line_num )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    // Internal line number is 1 less than user line number:
    final int NCLv = user_line_num - 1; // New cursor view line number

    if( pfb.NumLines() <= NCLv )
    {
      // Cant move to NCLv so just put cursor back where is was
      PrintCursor(); // Does m_console.Update()
    }
    else {
      final int NCLd = DiffLine( pV, NCLv );

      GoToCrsPos_Write( NCLd, 0 );
    }
  }
  void GoToTopLineInView()
  {
    GoToCrsPos_Write( m_topLine, CrsChar() );
  }

  void GoToBotLineInView()
  {
    View pV = m_vis.CV();

    final int NUM_LINES = NumLines();

    int bottom_line_in_view = m_topLine + WorkingRows( pV )-1;

    bottom_line_in_view = Math.min( NUM_LINES-1, bottom_line_in_view );

    GoToCrsPos_Write( bottom_line_in_view, CrsChar() );
  }

  void GoToMidLineInView()
  {
    View pV = m_vis.CV();

    final int NUM_LINES = NumLines();

    // Default: Last line in file is not in view
    int crsLine = m_topLine + WorkingRows( pV )/2;

    if( NUM_LINES-1 < BotLine( pV ) )
    {
      // Last line in file above bottom of view
      crsLine = m_topLine + (NUM_LINES-1 - m_topLine)/2;
    }
    GoToCrsPos_Write( crsLine, 0 );
  }

  void GoToTopOfFile()
  {
    GoToCrsPos_Write( 0, 0 );
  }

  void GoToEndOfFile()
  {
    final int NUM_LINES = NumLines();

    if( 0<NUM_LINES )
    {
      GoToCrsPos_Write( NUM_LINES-1, 0 );
    }
  }

  void GoToStartOfRow()
  {
    if( 0<NumLines() )
    {
      final int OCL = CrsLine(); // Old cursor line

      GoToCrsPos_Write( OCL, m_leftChar );
    }
  }
  void GoToEndOfRow()
  {
    if( 0<NumLines() )
    {
      View    pV  = m_vis.CV();
      FileBuf pfb = pV.m_fb;

      final int DL = CrsLine();          // Diff line
      final int VL = ViewLine( pV, DL ); // View line

      final int LL = pfb.LineLen( VL );
      if( 0 < LL )
      {
        final int NCP = Math.min( LL-1, m_leftChar + WorkingCols( pV ) - 1 );

        GoToCrsPos_Write( DL, NCP );
      }
    }
  }

  void GoToOppositeBracket()
  {
    View pV = m_vis.CV();

    MoveInBounds_Line();

    final int NUM_LINES = pV.m_fb.NumLines();
    final int CL        = ViewLine( pV, CrsLine() ); //< View line
    final int CC        = CrsChar();
    final int LL        = LineLen();

    if( 0==NUM_LINES || 0==LL ) return;

    final char C = pV.m_fb.Get( CL, CC );

    if( C=='{' || C=='[' || C=='(' )
    {
      char finish_char = 0;
      if     ( C=='{' ) finish_char = '}';
      else if( C=='[' ) finish_char = ']';
      else if( C=='(' ) finish_char = ')';
      else              Utils.Assert( false, "Un-handled case" );

      GoToOppositeBracket_Forward( C, finish_char );
    }
    else if( C=='}' || C==']' || C==')' )
    {
      char finish_char = 0;
      if     ( C=='}' ) finish_char = '{';
      else if( C==']' ) finish_char = '[';
      else if( C==')' ) finish_char = '(';
      else              Utils.Assert( false, "Un-handled case" );

      GoToOppositeBracket_Backward( C, finish_char );
    }
  }

  void GoToLeftSquigglyBracket()
  {
    View pV = m_vis.CV();

    MoveInBounds_Line();

    final char  start_char = '}';
    final char finish_char = '{';
    GoToOppositeBracket_Backward( start_char, finish_char );
  }
  void GoToRightSquigglyBracket()
  {
    View pV = m_vis.CV();

    MoveInBounds_Line();

    final char  start_char = '{';
    final char finish_char = '}';
    GoToOppositeBracket_Forward( start_char, finish_char );
  }

  void GoToOppositeBracket_Forward( final char ST_C, final char FN_C )
  {
    View pV = m_vis.CV();

    final int NUM_LINES = pV.m_fb.NumLines();

    // Convert from diff line (CrsLine()), to view line:
    final int CL = ViewLine( pV, CrsLine() );
    final int CC = CrsChar();

    // Search forward
    int level = 0;
    boolean     found = false;

    for( int vl=CL; !found && vl<NUM_LINES; vl++ )
    {
      final int LL = pV.m_fb.LineLen( vl );

      for( int p=(CL==vl)?(CC+1):0; !found && p<LL; p++ )
      {
        final char C = pV.m_fb.Get( vl, p );

        if     ( C==ST_C ) level++;
        else if( C==FN_C )
        {
          if( 0 < level ) level--;
          else {
            found = true;

            // Convert from view line back to diff line:
            final int dl = DiffLine(pV, vl);

            GoToCrsPos_Write( dl, p );
          }
        }
      }
    }
  }

  void GoToOppositeBracket_Backward( final char ST_C, final char FN_C )
  {
    View pV = m_vis.CV();

    // Convert from diff line (CrsLine()), to view line:
    final int CL = ViewLine( pV, CrsLine() );
    final int CC = CrsChar();

    // Search forward
    int level = 0;
    boolean     found = false;

    for( int vl=CL; !found && 0<=vl; vl-- )
    {
      final int LL = pV.m_fb.LineLen( vl );

      for( int p=(CL==vl)?(CC-1):(LL-1); !found && 0<=p; p-- )
      {
        final char C = pV.m_fb.Get( vl, p );

        if     ( C==ST_C ) level++;
        else if( C==FN_C )
        {
          if( 0 < level ) level--;
          else {
            found = true;

            // Convert from view line back to dif line:
            final int dl = DiffLine( pV, vl );

            GoToCrsPos_Write( dl, p );
          }
        }
      }
    }
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
    View pV = m_vis.CV();

    final int NUM_LINES = pV.m_fb.NumLines();
    if( NUM_LINES<=0 ) return null;

    int ncp_crsLine = 0;
    int ncp_crsChar = 0;

    boolean found_space = false;
    boolean found_word  = false;

    // Convert from diff line (CrsLine()), to view line:
    final int OCL = ViewLine( pV, CrsLine() ); //< Old cursor view line
    final int OCP = CrsChar();                 //< Old cursor position

    boolean ident = true; // Looking for identifier

    // Find white space, and then find non-white space
    for( int vl=OCL; (!found_space || !found_word) && vl<NUM_LINES; vl++ )
    {
      final int LL = pV.m_fb.LineLen( vl );
      if( LL==0 || OCL<vl )
      {
        found_space = true;
        // Once we have encountered a space, word is anything non-space.
        // An empty line is considered to be a space.
        ident = false;
      }
      final int START_C = OCL==vl ? OCP : 0;

      for( int p=START_C; (!found_space || !found_word) && p<LL; p++ )
      {
        final char C = pV.m_fb.Get( vl, p );

        if( found_space  )
        {
          if( IsWord( C, ident ) ) found_word = true;
        }
        else {
          if( !IsWord( C, ident ) ) found_space = true;
        }
        // Once we have encountered a space, word is anything non-space
        if( Utils.IsSpace( C ) ) ident = false;

        if( found_space && found_word )
        {
          // Convert from view line back to diff line:
          final int dl = DiffLine( pV, vl );

          ncp_crsLine = dl;
          ncp_crsChar = p;
        }
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
    View pV = m_vis.CV();

    final int NUM_LINES = pV.m_fb.NumLines();
    if( NUM_LINES<=0 ) return null;

    // Convert from diff line (CrsLine()), to view line:
    final int OCL = ViewLine( pV, CrsLine() );
    final int LL  = pV.m_fb.LineLen( OCL );

    if( LL < CrsChar() ) // Since cursor is now allowed past EOL,
    {                    // it may need to be moved back:
      if( 0<LL && !Utils.IsSpace( pV.m_fb.Get( OCL, LL-1 ) ) )
      {
        // Backed up to non-white space, which is previous word, so return true
        // Convert from view line back to diff line:
        return new CrsPos( CrsLine(), LL-1 ); //< diff line
      }
      else {
        GoToCrsPos_NoWrite( CrsLine(), 0<LL ? LL-1 : 0 );
      }
    }
    int ncp_crsLine = 0;
    int ncp_crsChar = 0;

    boolean found_space = false;
    boolean found_word  = false;
    final int OCP = CrsChar(); // Old cursor position

    boolean ident = false; // Looking for identifier

    // Find word to non-word transition
    for( int vl=OCL; (!found_space || !found_word) && -1<vl; vl-- )
    {
      final int LL2 = pV.m_fb.LineLen( vl );
      if( LL2==0 || vl<OCL )
      {
        // Once we have encountered a space, word is anything non-space.
        // An empty line is considered to be a space.
        ident = false;
      }
      final int START_C = OCL==vl ? OCP-1 : LL2-1;

      for( int p=START_C; (!found_space || !found_word) && -1<p; p-- )
      {
        final char C = pV.m_fb.Get( vl, p);

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

        if( found_space && found_word )
        {
          // Convert from view line back to diff line:
          final int dl = DiffLine( pV, vl );

          ncp_crsLine = dl;
          ncp_crsChar = p;
        }
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
  boolean IsWord( final char c, final boolean ident )
  {
    if( ident ) return Utils.IsWord_Ident( c );
                return Utils.NotSpace( c );
  }

  void GoToEndOfWord()
  {
    CrsPos ncp = GoToEndOfWord_GetPosition();

    if( null != ncp )
    {
      GoToCrsPos_Write( ncp.crsLine, ncp.crsChar );
    }
  }

  // Returns true if found end of word, else false
  // 1. If at end of word, or end of non-word, move to next char
  // 2. If on white space, skip past white space
  // 3. If on word, go to end of word
  // 4. If on non-white-non-word, go to end of non-white-non-word
  CrsPos GoToEndOfWord_GetPosition()
  {
    View pV = m_vis.CV();

    final int NUM_LINES = pV.m_fb.NumLines();
    if( 0==NUM_LINES ) return null;

    // Convert from diff line (CrsLine()), to view line:
    final int CL = ViewLine( pV, CrsLine() );
    final int LL = pV.m_fb.LineLen( CL );
          int CP = CrsChar(); // Cursor position

    // At end of line, or line too short:
    if( (LL-1) <= CP || LL < 2 ) return null;

    char CC = pV.m_fb.Get( CL, CP );   // Current char
    char NC = pV.m_fb.Get( CL, CP+1 ); // Next char

    // 1. If at end of word, or end of non-word, move to next char
    if( (Utils.IsWord_Ident   ( CC ) && !Utils.IsWord_Ident   ( NC ))
     || (Utils.IsWord_NonIdent( CC ) && !Utils.IsWord_NonIdent( NC )) ) CP++;

    // 2. If on white space, skip past white space
    if( Utils.IsSpace( pV.m_fb.Get(CL, CP) ) )
    {
      for( ; CP<LL && Utils.IsSpace( pV.m_fb.Get(CL, CP) ); CP++ ) ;
      if( LL <= CP ) return null; // Did not find non-white space
    }
    // At this point (CL,CP) should be non-white space
    CC = pV.m_fb.Get( CL, CP );  // Current char

    int ncp_crsLine = CrsLine(); // Diff line
    int ncp_crsChar = 0;

    if( Utils.IsWord_Ident( CC ) ) // On identity
    {
      // 3. If on word space, go to end of word space
      for( ; CP<LL && Utils.IsWord_Ident( pV.m_fb.Get(CL, CP) ); CP++ )
      {
        ncp_crsChar = CP;
      }
    }
    else if( Utils.IsWord_NonIdent( CC ) )// On Non-identity, non-white space
    {
      // 4. If on non-white-non-word, go to end of non-white-non-word
      for( ; CP<LL && Utils.IsWord_NonIdent( pV.m_fb.Get(CL, CP) ); CP++ )
      {
        ncp_crsChar = CP;
      }
    }
    else { // Should never get here:
      return null;
    }
    return new CrsPos( ncp_crsLine, ncp_crsChar );
  }

  void GoToFile()
  {
    View pV = m_vis.CV();

    ArrayList<Diff_Info> cDI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L;
    ArrayList<Diff_Info> oDI_List = (pV == m_vS) ? m_DI_List_L : m_DI_List_S;

    final Diff_Type cDT = cDI_List.get( CrsLine() ).diff_type; // Current diff type
    final Diff_Type oDT = oDI_List.get( CrsLine() ).diff_type; // Other   diff type

    String fname = GetFileName_UnderCursor();

    if( null != fname )
    {
      boolean did_diff = false;
      // Special case, look at two files in diff mode:
      View cV = (pV == m_vS) ? m_vS : m_vL; // Current view
      View oV = (pV == m_vS) ? m_vL : m_vS; // Other   view

      String cPath = cV.m_fb.Relative_2_FullFname( fname );
      String oPath = oV.m_fb.Relative_2_FullFname( fname );

      Ptr_Int c_file_idx = new Ptr_Int( 0 );
      Ptr_Int o_file_idx = new Ptr_Int( 0 );

      if( GetBufferIndex( cPath, c_file_idx )
       && GetBufferIndex( oPath, o_file_idx ) )
      {
        FileBuf c_file_buf = m_vis.get_FileBuf( c_file_idx.val );
        FileBuf o_file_buf = m_vis.get_FileBuf( o_file_idx.val );
        // Files with same name and different contents
        // or directories with same name but different paths
        if( (cDT == Diff_Type.DIFF_FILES && oDT == Diff_Type.DIFF_FILES)
         || (cV.m_fb.m_isDir && oV.m_fb.m_isDir
          && c_file_buf.m_fname.equals( o_file_buf.m_fname )
          &&!c_file_buf.m_dname.equals( o_file_buf.m_dname ) ) )
        {
          final int cV_vl_cl = ViewLine( cV, CrsLine() );
          final int cV_vl_tl = ViewLine( cV, m_topLine );
          cV.SetTopLine( cV_vl_tl );
          cV.SetCrsRow( cV_vl_cl - cV_vl_tl );
          cV.SetLeftChar( m_leftChar );
          cV.SetCrsCol  ( m_crsCol );

          final int oV_vl_cl = ViewLine( oV, CrsLine() );
          final int oV_vl_tl = ViewLine( oV, m_topLine );
          oV.SetTopLine( oV_vl_tl );
          oV.SetCrsRow( oV_vl_cl - oV_vl_tl );
          oV.SetLeftChar( m_leftChar );
          oV.SetCrsCol  ( m_crsCol );

          did_diff = m_vis.Diff_By_File_Indexes( cV, c_file_idx.val, oV, o_file_idx.val );
        }
      }
      if( !did_diff ) {
        // Normal case, dropping out of diff mode to look at file:
        m_vis.GoToBuffer_Fname( fname );
      }
    }
  }

  String GetFileName_UnderCursor()
  {
    StringBuilder fname = null;

    View pV = m_vis.CV();

    final int DL = CrsLine();
    final int VL = ViewLine( pV, DL ); // View line number
    final int LL = pV.m_fb.LineLen( VL );

    if( 0<LL ) {
      MoveInBounds_Line();
      final int CP = CrsChar();
      char C = pV.m_fb.Get( VL, CP );

      if( Utils.IsFileNameChar( C ) )
      {
        // Get the file name:
        fname = new StringBuilder();
        fname.append( C );

        // Search backwards, until white space is found:
        for( int k=CP-1; -1<k; k-- )
        {
          C = pV.m_fb.Get( VL, k );

          if( !Utils.IsFileNameChar( C ) ) break;
          else fname.insert( 0, C );
        }
        // Search forwards, until white space is found:
        for( int k=CP+1; k<LL; k++ )
        {
          C = pV.m_fb.Get( VL, k );

          if( !Utils.IsFileNameChar( C ) ) break;
          else fname.append( C );
        }
        fname = Utils.EnvKeys2Vals( fname );
      }
    }
    return null != fname ? fname.toString() : null;
  }

  boolean GetBufferIndex( String file_path, Ptr_Int file_index )
  {
    // 1. Search for file_path in buffer list
    if( m_vis.HaveFile( file_path, file_index ) )
    {
      return true;
    }
    // 2. See if file exists, and if so, add a file buffer
    if( Files.exists( Paths.get( file_path ) ) )
    {
      FileBuf fb = new FileBuf( m_vis, file_path, true );
      boolean ok = fb.ReadFile();
      if( ok ) {
        m_vis.Add_FileBuf_2_Lists_Create_Views( fb, file_path );

        if( m_vis.HaveFile( file_path, file_index ) )
        {
          return true;
        }
      }
    }
    return false;
  }

  void Do_n()
  {
    if( 0 < m_vis.get_regex().length() ) Do_n_Pattern();
    else                                 Do_n_Diff( true );
  }

  void Do_n_Pattern()
  {
    View pV = m_vis.CV();

    if( 0 < pV.m_fb.NumLines() )
    {
      Set_Cmd_Line_Msg( '/' + m_vis.get_regex() );

      CrsPos ncp = new CrsPos( 0, 0 ); // Next cursor position

      if( Do_n_FindNextPattern( ncp ) )
      {
        GoToCrsPos_Write( ncp.crsLine, ncp.crsChar );
      }
    }
  }

  boolean Do_n_FindNextPattern( CrsPos ncp )
  {
    View pV = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int NUM_LINES = pfb.NumLines();

    final int OCL = CrsLine(); // Diff line
    final int OCC = CrsChar();

    final int OCLv = ViewLine( pV, OCL ); // View line

    int st_l = OCLv;
    int st_c = OCC;

    boolean found_next_star = false;

    // Move past current star:
    final int LL = pfb.LineLen( OCLv );

    pfb.Check_4_New_Regex();
    pfb.Find_Regexs_4_Line( OCL );

    // Move past current pattern:
    for( ; st_c<LL && pV.InStarOrStarInF(OCLv,st_c); st_c++ ) ;

    // If at end of current line, go down to next line:
    if( LL <= st_c ) { st_c=0; st_l++; }

    // Search for first pattern position past current position
    for( int l=st_l; !found_next_star && l<NUM_LINES; l++ )
    {
      pfb.Find_Regexs_4_Line( l );

      final int LL2 = pfb.LineLen( l );

      for( int p=st_c ; !found_next_star && p<LL2 ; p++ )
      {
        if( pV.InStarOrStarInF(l,p) )
        {
          found_next_star = true;
          // Convert from view line back to diff line:
          final int dl = DiffLine( pV, l );
          ncp.crsLine = dl;
          ncp.crsChar = p;
        }
      }
      // After first line, always start at beginning of line
      st_c = 0;
    }
    // Near end of file and did not find any patterns, so go to first pattern in file
    if( !found_next_star )
    {
      for( int l=0; !found_next_star && l<=OCLv; l++ )
      {
        pfb.Find_Regexs_4_Line( l );

        final int LL3 = pfb.LineLen( l );
        final int END_C = (OCLv==l) ? Math.min( OCC, LL3 ) : LL3;

        for( int p=0; !found_next_star && p<END_C; p++ )
        {
          if( pV.InStarOrStarInF(l,p) )
          {
            found_next_star = true;
            // Convert from view line back to diff line:
            final int dl = DiffLine( pV, l );
            ncp.crsLine = dl;
            ncp.crsChar = p;
          }
        }
      }
    }
    return found_next_star;
  }

  void Do_n_Diff( final boolean write )
  {
    if( 0 < NumLines() )
    {
      Set_Cmd_Line_Msg("Searching down for diff");

      Ptr_Int dl = new Ptr_Int( CrsLine() ); // Diff line, changed by search methods below

      View pV = m_vis.CV();

      ArrayList<Diff_Info> DI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L;

      final Diff_Type DT = DI_List.get( dl.val ).diff_type; // Current diff type

      boolean found_same = true;

      if( DT == Diff_Type.CHANGED
       || DT == Diff_Type.INSERTED
       || DT == Diff_Type.DELETED
       || DT == Diff_Type.DIFF_FILES )
      {
        // If currently on a diff, search for same before searching for diff
        found_same = Do_n_Search_for_Same( dl, DI_List );
      }
      if( found_same )
      {
        boolean found_diff = Do_n_Search_for_Diff( dl, DI_List );

        int NCL = 0, NCP = 0; //< Init to zero to turn off compiler warning
        if( found_diff )
        {
          NCL = dl.val;
          NCP = Do_n_Find_Crs_Pos( NCL, DI_List );
        }
        else // Could not find a difference.
        {    // Check if one file ends in LF and the other does not:
          if( m_fS.m_LF_at_EOF != m_fL.m_LF_at_EOF )
          {
            found_diff = true;
            NCL = DI_List.size() - 1;
            NCP = pV.m_fb.LineLen( DI_List.get( NCL ).line_num );
          }
        }
        if( found_diff )
        {
          if( write ) GoToCrsPos_Write( NCL, NCP );
          else        GoToCrsPos_NoWrite( NCL, NCP );
        }
      }
    }
  }

  boolean Do_n_Search_for_Same( Ptr_Int dl
                              , final ArrayList<Diff_Info> DI_List )
  {
    final int NUM_LINES = NumLines();
    final int dl_st = dl.val;

    // Search forward for Diff_Type.SAME
    boolean found = false;

    if( 1 < NUM_LINES )
    {
      while( !found && dl.val<NUM_LINES )
      {
        final Diff_Type DT = DI_List.get( dl.val ).diff_type;

        if( DT == Diff_Type.SAME )
        {
          found = true;
        }
        else dl.val++;
      }
      if( !found )
      {
        // Wrap around back to top and search again:
        dl.val = 0;
        while( !found && dl.val<dl_st )
        {
          final Diff_Type DT = DI_List.get( dl.val ).diff_type;

          if( DT == Diff_Type.SAME )
          {
            found = true;
          }
          else dl.val++;
        }
      }
    }
    return found;
  }

  boolean Do_n_Search_for_Diff( Ptr_Int dl
                              , final ArrayList<Diff_Info> DI_List )
  {
    final int dl_st = dl.val;

    // Search forward for non-Diff_Type.SAME
    boolean found_diff = false;

    if( 1 < NumLines() )
    {
      found_diff = Do_n_Search_for_Diff_DT( dl, DI_List );

      if( !found_diff )
      {
        dl.val = dl_st;
        found_diff = Do_n_Search_for_Diff_WhiteSpace( dl, DI_List );
      }
    }
    return found_diff;
  }

  // Look for difference based on Diff_Info:
  boolean Do_n_Search_for_Diff_DT( Ptr_Int dl
                                 , final ArrayList<Diff_Info> DI_List )
  {
    boolean found_diff = false;

    final int NUM_LINES = NumLines();
    final int dl_st = dl.val;

    // Search forward from dl_st:
    while( !found_diff && dl.val<NUM_LINES )
    {
      final Diff_Type DT = DI_List.get( dl.val ).diff_type;

      if( DT == Diff_Type.CHANGED
       || DT == Diff_Type.INSERTED
       || DT == Diff_Type.DELETED
       || DT == Diff_Type.DIFF_FILES )
      {
        found_diff = true;
      }
      else dl.val++;
    }
    if( !found_diff )
    {
      // Wrap around back to top and search down to dl_st:
      dl.val = 0;
      while( !found_diff && dl.val<dl_st )
      {
        final Diff_Type DT = DI_List.get( dl.val ).diff_type;

        if( DT == Diff_Type.CHANGED
         || DT == Diff_Type.INSERTED
         || DT == Diff_Type.DELETED
         || DT == Diff_Type.DIFF_FILES )
        {
          found_diff = true;
        }
        else dl.val++;
      }
    }
    return found_diff;
  }

  // Look for difference in white space at beginning or ending of lines:
  boolean Do_n_Search_for_Diff_WhiteSpace( Ptr_Int dl
                                         , final ArrayList<Diff_Info> DI_List )
  {
    boolean found_diff = false;

    final int NUM_LINES = NumLines();

    ArrayList<Diff_Info> DI_List_o = (DI_List == m_DI_List_S) ? m_DI_List_L : m_DI_List_S;
    FileBuf pF_m = (DI_List == m_DI_List_S) ? m_fS : m_fL;
    FileBuf pF_o = (DI_List == m_DI_List_S) ? m_fL : m_fS;

    // If the current line has a difference in white space at beginning or end, start
    // searching on next line so the current line number is not automatically returned.
    boolean curr_line_has_LT_WS_diff
      = Line_Has_Leading_or_Trailing_WS_Diff( dl, dl.val
                                            , DI_List, DI_List_o
                                            , pF_m, pF_o );
    final int dl_st = curr_line_has_LT_WS_diff
                    ? (dl.val + 1) % NUM_LINES
                    : dl.val;

    // Search from dl_st to end for lines of different length:
    for( int k=dl_st; !found_diff && k<NUM_LINES; k++ )
    {
      found_diff = Line_Has_Leading_or_Trailing_WS_Diff( dl, k
                                                       , DI_List, DI_List_o
                                                       , pF_m, pF_o );
    }
    if( !found_diff )
    {
      // Search from top to dl_st for lines of different length:
      for( int k=0; !found_diff && k<dl_st; k++ )
      {
        found_diff = Line_Has_Leading_or_Trailing_WS_Diff( dl, k
                                                         , DI_List, DI_List_o
                                                         , pF_m, pF_o );
      }
    }
    return found_diff;
  }

  boolean
  Line_Has_Leading_or_Trailing_WS_Diff( Ptr_Int dl
                                      , final int k
                                      , final ArrayList<Diff_Info> DI_List
                                      , final ArrayList<Diff_Info> DI_List_o
                                      , final FileBuf pF_m
                                      , final FileBuf pF_o )
  {
    boolean L_T_WS_diff = false;

    final Diff_Info Di_m = DI_List.get( k );
    final Diff_Info Di_o = DI_List_o.get( k );

    if( Di_m.diff_type == Diff_Type.SAME
     && Di_o.diff_type == Diff_Type.SAME )
    {
      Line lm = pF_m.GetLine( Di_m.line_num ); // Line from my    view
      Line lo = pF_o.GetLine( Di_o.line_num ); // Line from other view

      if( lm.length() != lo.length() )
      {
        L_T_WS_diff = true;
        dl.val = k;
      }
    }
    return L_T_WS_diff;
  }

  int Do_n_Find_Crs_Pos( final int NCL, ArrayList<Diff_Info> DI_List )
  {
    int NCP = 0;

    final Diff_Type DT_new = DI_List.get( NCL ).diff_type;

    if( DT_new == Diff_Type.CHANGED )
    {
      LineInfo rLI_s = m_DI_List_S.get( NCL ).pLineInfo;
      LineInfo rLI_l = m_DI_List_L.get( NCL ).pLineInfo;

      for( int k=0; null != rLI_s && k<rLI_s.size()
                 && null != rLI_l && k<rLI_l.size(); k++ )
      {
        Diff_Type dt_s = rLI_s.get( k );
        Diff_Type dt_l = rLI_l.get( k );

        if( dt_s != Diff_Type.SAME
         || dt_l != Diff_Type.SAME )
        {
          NCP = k;
          break;
        }
      }
    }
    return NCP;
  }

  void Do_N()
  {
    if( 0 < m_vis.get_regex().length() ) Do_N_Pattern();
    else                                 Do_N_Diff();
  }

  void Do_N_Pattern()
  {
    View pV = m_vis.CV();

    if( 0 < pV.m_fb.NumLines() )
    {
      Set_Cmd_Line_Msg( '/' + m_vis.get_regex() );

      CrsPos ncp = new CrsPos( 0, 0 ); // Next cursor position

      if( Do_N_FindPrevPattern( ncp ) )
      {
        GoToCrsPos_Write( ncp.crsLine, ncp.crsChar );
      }
    }
  }

  boolean Do_N_FindPrevPattern( CrsPos ncp )
  {
    MoveInBounds_Line();

    View pV = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int NUM_LINES = pfb.NumLines();

    final int OCL = CrsLine();
    final int OCC = CrsChar();

    final int OCLv = ViewLine( pV, OCL ); // View line

    pfb.Check_4_New_Regex();

    boolean found_prev_star = false;

    // Search for first star position before current position
    for( int l=OCLv; !found_prev_star && 0<=l; l-- )
    {
      pfb.Find_Regexs_4_Line( l );

      final int LL = pfb.LineLen( l );

      int p=LL-1;
      if( OCLv==l ) p = 0<OCC ? OCC-1 : 0;

      for( ; 0<p && !found_prev_star; p-- )
      {
        for( ; 0<=p && pV.InStarOrStarInF(l,p); p-- )
        {
          found_prev_star = true;
          // Convert from view line back to diff line:
          final int dl = DiffLine( pV, l );
          ncp.crsLine = dl;
          ncp.crsChar = p;
        }
      }
    }
    // Near beginning of file and did not find any patterns, so go to last pattern in file
    if( !found_prev_star )
    {
      for( int l=NUM_LINES-1; !found_prev_star && OCLv<l; l-- )
      {
        pfb.Find_Regexs_4_Line( l );

        final int LL = pfb.LineLen( l );

        int p=LL-1;
        if( OCLv==l ) p = 0<OCC ? OCC-1 : 0;

        for( ; 0<p && !found_prev_star; p-- )
        {
          for( ; 0<=p && pV.InStarOrStarInF(l,p); p-- )
          {
            found_prev_star = true;
            // Convert from view line back to diff line:
            final int dl = DiffLine( pV, l );
            ncp.crsLine = dl;
            ncp.crsChar = p;
          }
        }
      }
    }
    return found_prev_star;
  }

  void Do_N_Diff()
  {
    if( 0 < NumLines() )
    {
      Set_Cmd_Line_Msg("Searching up for diff");

      Ptr_Int dl = new Ptr_Int( CrsLine() );

      View pV = m_vis.CV();

      ArrayList<Diff_Info> DI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L;

      final Diff_Type DT = DI_List.get( dl.val ).diff_type; // Current diff type

      boolean found_same = true;

      if( DT == Diff_Type.CHANGED
       || DT == Diff_Type.INSERTED
       || DT == Diff_Type.DELETED
       || DT == Diff_Type.DIFF_FILES )
      {
        // If currently on a diff, search for same before searching for diff
        found_same = Do_N_Search_for_Same( dl, DI_List );
      }
      if( found_same )
      {
        boolean found_diff = Do_N_Search_for_Diff( dl, DI_List );

        if( found_diff )
        {
          final int NCL = dl.val;
          final int NCP = Do_n_Find_Crs_Pos( NCL, DI_List );

          GoToCrsPos_Write( NCL, NCP );
        }
      }
    }
  }

  boolean Do_N_Search_for_Same( Ptr_Int dl
                              , final ArrayList<Diff_Info> DI_List )
  {
    final int NUM_LINES = NumLines();
    final int dl_st = dl.val;

    // Search backwards for Diff_Type.SAME
    boolean found = false;

    if( 1 < NUM_LINES )
    {
      while( !found && 0<=dl.val )
      {
        final Diff_Type DT = DI_List.get( dl.val ).diff_type;

        if( DT == Diff_Type.SAME )
        {
          found = true;
        }
        else dl.val--;
      }
      if( !found )
      {
        // Wrap around back to bottom and search again:
        dl.val = NUM_LINES-1;
        while( !found && dl_st<dl.val )
        {
          final Diff_Type DT = DI_List.get( dl.val ).diff_type;

          if( DT == Diff_Type.SAME )
          {
            found = true;
          }
          else dl.val--;
        }
      }
    }
    return found;
  }

  boolean Do_N_Search_for_Diff( Ptr_Int dl
                              , final ArrayList<Diff_Info> DI_List )
  {
    final int dl_st = dl.val;

    // Search backwards for non-Diff_Type.SAME
    boolean found_diff = false;

    if( 1 < NumLines() )
    {
      found_diff = Do_N_Search_for_Diff_DT( dl, DI_List );

      if( !found_diff )
      {
        dl.val = dl_st;
        found_diff = Do_N_Search_for_Diff_WhiteSpace( dl, DI_List );
      }
    }
    return found_diff;
  }

  // Look for difference based on Diff_Info:
  boolean Do_N_Search_for_Diff_DT( Ptr_Int dl
                                 , final ArrayList<Diff_Info> DI_List )
  {
    boolean found_diff = false;

    final int dl_st = dl.val;

    // Search backward from dl_st:
    while( !found_diff && 0<=dl.val )
    {
      final Diff_Type DT = DI_List.get( dl.val ).diff_type;

      if( DT == Diff_Type.CHANGED
       || DT == Diff_Type.INSERTED
       || DT == Diff_Type.DELETED
       || DT == Diff_Type.DIFF_FILES )
      {
        found_diff = true;
      }
      else dl.val--;
    }
    if( !found_diff )
    {
      // Wrap around back to bottom up to dl_st:
      dl.val = NumLines()-1;
      while( !found_diff && dl_st<dl.val )
      {
        final Diff_Type DT = DI_List.get( dl.val ).diff_type;

        if( DT == Diff_Type.CHANGED
         || DT == Diff_Type.INSERTED
         || DT == Diff_Type.DELETED
         || DT == Diff_Type.DIFF_FILES )
        {
          found_diff = true;
        }
        else dl.val--;
      }
    }
    return found_diff;
  }

  // Look for difference in white space at beginning or ending of lines:
  boolean Do_N_Search_for_Diff_WhiteSpace( Ptr_Int dl
                                         , final ArrayList<Diff_Info> DI_List )
  {
    boolean found_diff = false;

    final int NUM_LINES = NumLines();

    ArrayList<Diff_Info> DI_List_o = (DI_List == m_DI_List_S) ? m_DI_List_L : m_DI_List_S;
    FileBuf pF_m = (DI_List == m_DI_List_S) ? m_fS : m_fL;
    FileBuf pF_o = (DI_List == m_DI_List_S) ? m_fL : m_fS;

    // If the current line has a difference in white space at beginning or end, start
    // searching on previous line so the current line number is not automatically returned.
    boolean curr_line_has_LT_WS_diff
      = Line_Has_Leading_or_Trailing_WS_Diff( dl, dl.val
                                            , DI_List, DI_List_o
                                            , pF_m, pF_o );
    final int dl_st = curr_line_has_LT_WS_diff
                    ? ( 0 < dl.val ? (dl.val - 1) % NUM_LINES
                                   : NUM_LINES-1 )
                    : dl.val;

    // Search from dl_st to top for lines of different length:
    for( int k=dl_st; !found_diff && 0<=k; k-- )
    {
      found_diff = Line_Has_Leading_or_Trailing_WS_Diff( dl, k
                                                       , DI_List, DI_List_o
                                                       , pF_m, pF_o );
    }
    if( !found_diff )
    {
      // Search from bottom to dl_st for lines of different length:
      for( int k=NUM_LINES-1; !found_diff && dl_st<k; k-- )
      {
        found_diff = Line_Has_Leading_or_Trailing_WS_Diff( dl, k
                                                         , DI_List, DI_List_o
                                                         , pF_m, pF_o );
      }
    }
    return found_diff;
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
    if( 0<NumLines() )
    {
      final int LL = LineLen(); // Line length
      final int CP = CrsChar(); // Cursor position

      if( CP < LL-1 )
      {
        View pV  = m_vis.CV();

        final int DL = CrsLine();          // Diff line
        final int VL = ViewLine( pV, DL ); // View line

        int NCP = 0;
        boolean found_char = false;
        for( int p=CP+1; !found_char && p<LL; p++ )
        {
          final char C = pV.m_fb.Get( VL, p );

          if( C == FAST_CHAR )
          {
            NCP = p;
            found_char = true;
          }
        }
        if( found_char )
        {
          GoToCrsPos_Write( DL, NCP );
        }
      }
    }
  }

  // If past end of line, move back to end of line.
  // Returns true if moved, false otherwise.
  //
  void MoveInBounds_Line()
  {
    View pV = m_vis.CV();

    final int DL  = CrsLine();  // Diff line
    final int VL  = ViewLine( pV, DL );      // View line
    final int LL  = pV.m_fb.LineLen( VL );
    final int EOL = 0<LL ? LL-1 : 0;

    // Since cursor is now allowed past EOL, it may need to be moved back:
    if( EOL < CrsChar() ) GoToCrsPos_NoWrite( DL, EOL );
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
        MoveCurrLineCenter( true );
      }
      else if( c2 == 'b' )
      {
        MoveCurrLineToBottom();
      }
      m_vis.get_states().removeFirst();
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

  void MoveCurrLineCenter( final boolean write )
  {
    View pV = m_vis.CV();

    final int center = (int)( 0.5*WorkingRows(pV) + 0.5 );

    final int OCL = CrsLine(); // Old cursor line

    if( 0 < OCL && OCL < center && 0 < m_topLine )
    {
      // Cursor line cannot be moved to center, but can be moved closer to center
      // CrsLine() does not change:
      m_crsRow += m_topLine;
      m_topLine = 0;
      Set_Console_CrsCell();

      if( write ) Update();
    }
    else if( center < OCL
          && center != m_crsRow )
    {
      m_topLine += m_crsRow - center;
      m_crsRow = center;
      Set_Console_CrsCell();

      if( write ) Update();
    }
  }

  void MoveCurrLineToBottom()
  {
    if( 0 < m_topLine )
    {
      View pV = m_vis.CV();

      final int WR  = WorkingRows( pV );
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

  String Do_Star_GetNewPattern()
  {
    View pV = m_vis.CV();
    FileBuf pfb = pV.m_fb;
    if( pfb.NumLines() == 0 ) return "";

    // Convert CL, which is diff line, to view line:
    final int CLv = ViewLine( pV, CrsLine() );
    final int LL  = pfb.LineLen( CLv );

    StringBuilder pattern = null;

    if( 0<LL )
    {
      pattern = new StringBuilder();

      MoveInBounds_Line();
      final int  CC = CrsChar();
      final char C  = pfb.Get( CLv,  CC );

      if( Utils.IsIdent( C ) )
      {
        pattern.append( C );

        // Search forward:
        for( int k=CC+1; k<LL; k++ )
        {
          final char c1 = pfb.Get( CLv, k );
          if( Utils.IsIdent( c1 ) ) pattern.append( c1 );
          else                      break;
        }
        // Search backward:
        for( int k=CC-1; 0<=k; k-- )
        {
          final char c2 = pfb.Get( CLv, k );
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

  void Do_i()
  {
    m_vis.get_states().addFirst( m_run_i_end );
    m_vis.get_states().addFirst( m_run_i_mid );
    m_vis.get_states().addFirst( m_run_i_beg );
  }
  void run_i_beg()
  {
    View pV = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    pV.m_inInsertMode = true;

    DisplayBanner();

    if( 0 == pfb.NumLines() ) pfb.PushLine();

    final int DL = CrsLine(); // Diff line number
    final int VL = ViewLine( pV, DL ); // View line number
    final int LL = pfb.LineLen( VL );  // Line length

    if( LL < CrsChar() ) // Since cursor is now allowed past EOL,
    {                    // it may need to be moved back:
      // For user friendlyness, move cursor to new position immediately:
      GoToCrsPos_Write( DL, LL );
    }
    m_i_count = 0;

    m_vis.get_states().removeFirst();
  }
  void run_i_mid()
  {
    if( 0<m_console.KeysIn() )
    {
      final char c = m_console.GetKey();

      if( c == ESC )
      {
        m_vis.get_states().removeFirst(); // Done
      }
      else if( BS == c || DEL == c )
      {
        if( 0<m_i_count )
        {
          InsertBackspace();
          m_i_count--;
        }
      }
      else if( Utils.IsEndOfLineDelim( c ) )
      {
        InsertAddReturn();
        m_i_count++;
      }
      else {
        InsertAddChar( c );
        m_i_count++;
      }
    }
  }
  void run_i_end()
  {
    Remove_Banner();

    View pV = m_vis.CV();
    FileBuf pfb = pV.m_fb;
    pV.m_inInsertMode = false;

    // Move cursor back one space:
    if( 0<m_crsCol )
    {
      Set_crsCol( m_crsCol-1 );
      Update();
    }
    m_vis.get_states().removeFirst();
  }

  void InsertAddChar( final char c )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    if( pfb.NumLines()<=0 )
    {
      pfb.PushLine();
      Patch_Diff_Info_Inserted( pV, 0, false );
    }
    final int DL = CrsLine(); // Diff line number
    final int VL = ViewLine( pV, DL ); // View line number

    ArrayList<Diff_Info> cDI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L; // Current
    Diff_Info cDI = cDI_List.get( DL );

    if( Diff_Type.DELETED == cDI.diff_type )
    {
      final boolean ODVL0 = On_Deleted_View_Line_Zero( DL );

      m_crsCol = 0;
      pfb.InsertLine( ODVL0 ? VL : VL+1 );
      pfb.InsertChar( ODVL0 ? VL : VL+1, 0, c );
      Patch_Diff_Info_Inserted( pV, DL, ODVL0 );
    }
    else {
      pfb.InsertChar( VL, CrsChar(), c );
      Patch_Diff_Info_Changed( pV, DL );
    }
    if( WorkingCols( pV ) <= m_crsCol+1 )
    {
      // On last working column, need to scroll right:
      m_leftChar++;
    }
    else {
      m_crsCol += 1;
    }
    Update();
  }

  void InsertAddReturn()
  {
    View pV = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    // The lines in fb do not end with '\n's.
    // When the file is written, '\n's are added to the ends of the lines.
    Line new_line = new Line();
    final int DL = CrsLine();          // Diff line number
    final int VL = ViewLine( pV, DL ); // View line number
    final int OLL = pfb.LineLen( VL ); // Old line length
    final int OCP = CrsChar();          // Old cursor position

    for( int k=OCP; k<OLL; k++ )
    {
      final char C = pfb.RemoveChar( VL, OCP );
      new_line.append_c( C );
    }
    // Truncate the rest of the old line:
    // Add the new line:
    final int new_line_num = VL+1;
    pfb.InsertLine( new_line_num, new_line );
    m_crsCol = 0;
    m_leftChar = 0;
    if( DL < BotLine( pV ) ) m_crsRow++;
    else {
      // If we were on the bottom working line, scroll screen down
      // one line so that the cursor line is not below the screen.
      m_topLine++;
    }
    Patch_Diff_Info_Changed ( pV, DL );
    Patch_Diff_Info_Inserted( pV, DL+1, false );
    Update();
  }

  void InsertBackspace()
  {
    View pV = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    // If no lines in buffer, no backspacing to be done
    if( 0<pfb.NumLines() )
    {
      final int DL = CrsLine();  // Diff line

      final int OCP = CrsChar(); // Old cursor position

      if( 0<OCP ) InsertBackspace_RmC ( DL, OCP );
      else        InsertBackspace_RmNL( DL );
    }
  }

  void InsertBackspace_RmC( final int DL
                          , final int OCP )
  {
    View pV = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int VL = ViewLine( pV, DL ); // View line number

    pfb.RemoveChar( VL, OCP-1 );

    if( 0 < m_crsCol ) m_crsCol -= 1;
    else               m_leftChar -= 1;

    Patch_Diff_Info_Changed( pV, DL );
    Update();
  }

  void InsertBackspace_RmNL( final int DL )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int VL = ViewLine( pV, DL ); // View line number

    // Cursor Line Position is zero, so:
    // 1. Save previous line, end of line + 1 position
    int ncp_crsLine = DL-1;
    int ncp_crsChar = pfb.LineLen( VL-1 );

    // 2. Remove the line
    Line lp = pfb.RemoveLine( VL );

    // 3. Append rest of line to previous line
    pfb.AppendLineToLine( VL-1, lp );

    // 4. Put cursor at the old previous line end of line + 1 position
    final boolean MOVE_UP    = ncp_crsLine < m_topLine;
    final boolean MOVE_RIGHT = RightChar( pV ) < ncp_crsChar;

    if( MOVE_UP ) m_topLine = ncp_crsLine;
                  m_crsRow  = ncp_crsLine - m_topLine;

    if( MOVE_RIGHT ) m_leftChar = ncp_crsChar - WorkingCols( pV ) + 1;
                     m_crsCol   = ncp_crsChar - m_leftChar;

    // 5. Removed a line, so update to re-draw window view
    Patch_Diff_Info_Deleted( pV, DL   );
    Patch_Diff_Info_Changed( pV, DL-1 );
    Update();
  }

  void Do_a()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    if( 0<pfb.NumLines() )
    {
      final int DL = CrsLine();
      final int VL = ViewLine( pV, DL ); // View line number
      final int LL = pfb.LineLen( VL );

      if( 0<LL ) {
        final boolean CURSOR_AT_EOL = ( CrsChar() == LL-1 );
        if( CURSOR_AT_EOL )
        {
          Set_crsCol( LL );
        }
        final boolean CURSOR_AT_RIGHT_COL = ( m_crsCol == WorkingCols( pV )-1 );

        if( CURSOR_AT_RIGHT_COL )
        {
          // Only need to scroll window right, and then enter insert i:
          m_leftChar++; //< This increments CrsChar()
        }
        else if( !CURSOR_AT_EOL ) // If cursor was at EOL, already moved cursor forward
        {
          // Only need to move cursor right, and then enter insert i:
          Set_crsCol( m_crsCol + 1 );
        }
        Update();
      }
    }
    Do_i();
  }
  void Do_A()
  {
    GoToEndOfLine();
    Do_a();
  }
  void Do_o()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int NL = pfb.NumLines(); // Number of lines
    final int DL = CrsLine();      // Diff line

    ArrayList<Diff_Info> cDI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L; // Current

    final boolean ON_DELETED = Diff_Type.DELETED == cDI_List.get( DL ).diff_type;

    // If no lines or on a deleted line, just Do_i()
    if( 0<NL && !ON_DELETED )
    {
      final int VL = ViewLine( pV, DL ); // View line

      pfb.InsertLine( VL+1 );
      Set_crsCol( 0 );
      m_leftChar = 0;
      if( DL < BotLine( pV ) ) m_crsRow++;
      else {
        // If we were on the bottom working line, scroll screen down
        // one line so that the cursor line is not below the screen.
        m_topLine++;
      }
      Patch_Diff_Info_Inserted( pV, DL+1, false );

      Update();
    }
    Do_i();
  }
  // Wrapper around Do_o approach:
  void Do_O()
  {
    final int DL = CrsLine();          // Diff line

    if( 0<DL )
    {
      // Not on top line, so just back up and then Do_o:
      GoToCrsPos_NoWrite( DL-1, CrsChar() );
      Do_o();
    }
    else {
      // On top line, so cannot move up a line and then Do_o,
      // so use some custom code:
      View    pV  = m_vis.CV();
      FileBuf pfb = pV.m_fb;

      pfb.InsertLine( 0 );
      Patch_Diff_Info_Inserted( pV, 0, true );

      Update();
      Do_i();
    }
  }
  void Do_x()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    // If there is nothing to 'x', just return:
    if( 0<pfb.NumLines() )
    {
      final int DL = CrsLine(); // Diff line number
      final int VL = ViewLine( pV, DL ); // View line number
      final int LL = pfb.LineLen( VL );

      // If nothing on line, just return:
      if( 0 < LL )
      {
        // If past end of line, move to end of line:
        if( LL-1 < CrsChar() )
        {
          GoToCrsPos_Write( DL, LL-1 );
        }
        final char C = pfb.RemoveChar( VL, CrsChar() );

        // Put char x'ed into register:
        Line nlr = new Line();
        nlr.append_c( C );
        m_vis.get_reg().clear();
        m_vis.get_reg().add( nlr );
        m_vis.set_paste_mode( Paste_Mode.ST_FN );

        final int NLL = pfb.LineLen( VL ); // New line length

        // Reposition the cursor:
        if( NLL <= m_leftChar+m_crsCol )
        {
          // The char x'ed is the last char on the line, so move the cursor
          //   back one space.  Above, a char was removed from the line,
          //   but m_crsCol has not changed, so the last char is now NLL.
          // If cursor is not at beginning of line, move it back one more space.
          if( 0 < m_crsCol ) m_crsCol--;
        }
        Patch_Diff_Info_Changed( pV, DL );
        Update();
      }
    }
  }
  void Do_s()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int DL  = CrsLine();          // Diff line
    final int VL  = ViewLine( pV, DL ); // View line
    final int LL  = pfb.LineLen( VL );
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
  void Do_D()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int NUM_LINES = pfb.NumLines();
    final int DL = CrsLine();  // Old cursor line
    final int VL = ViewLine( pV, DL ); // View line
    final int CP = CrsChar();  // Old cursor position
    final int LL = pfb.LineLen( VL );  // Old line length

    // If there is nothing to 'D', just return:
    if( 0<NUM_LINES && 0<LL && CP<LL )
    {
      Line lrd = new Line();

      for( int k=CP; k<LL; k++ )
      {
        char c = pfb.RemoveChar( VL, CP );
        lrd.append_c( c );
      }
      m_vis.get_reg().clear();
      m_vis.get_reg().add( lrd );
      m_vis.set_paste_mode( Paste_Mode.ST_FN );

      // If cursor is not at beginning of line, move it back one space.
      if( 0<m_crsCol ) m_crsCol--;

      Patch_Diff_Info_Changed( pV, DL );
      if( !ReDiff() ) Update();
    }
  }
  void Do_dd()
  {
    View pV = m_vis.CV();

    final int NVL = pV.m_fb.NumLines();   // Number of view lines

    if( 1 < NVL ) // Dont delete a single solitary line
    {
      final int DL = CrsLine(); // Diff line

      // Cant delete a deleted or unknown line
      final Diff_Type DT = DiffType( pV, DL );
      if( DT != Diff_Type.UNKNOWN && DT != Diff_Type.DELETED )
      {
        final int VL = ViewLine( pV, DL ); // View line

        // Remove line from FileBuf and save in paste register:
        Line lp = pV.m_fb.RemoveLine( VL );

        // m_vis.m_reg will own lp
        m_vis.get_reg().clear();
        m_vis.get_reg().add( lp );
        m_vis.set_paste_mode( Paste_Mode.LINE );

        Patch_Diff_Info_Deleted( pV, DL );

        // Figure out where to put cursor after deletion:
        final boolean DELETED_LAST_LINE = VL == NVL-1;

        int ncld = DL;
        // Deleting last line of file, so move to line above:
        if( DELETED_LAST_LINE ) ncld--;
        else {
          // If cursor is now sitting on a deleted line, move to line below:
          final Diff_Type DTN = DiffType( pV, DL );
          if( DTN == Diff_Type.DELETED ) ncld++;
        }
        GoToCrsPos_NoWrite( ncld, CrsChar() );

        if( !ReDiff() ) Update();
      }
    }
  }

  // If nothing was deleted, return 0.
  // If last char on line was deleted, return 2,
  // Else return 1.
  int Do_dw()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    // If there is nothing to 'yw', just return:
    if( 0 < pfb.NumLines() )
    {
      final int  DL = CrsLine(); // Diff line
      final Diff_Type DT = DiffType( pV, DL );

      if( DT == Diff_Type.SAME
       || DT == Diff_Type.CHANGED
       || DT == Diff_Type.INSERTED )
      {
        final int st_line_v = ViewLine( pV, DL ); // View line
        final int st_char   = CrsChar();

        final int LL = pfb.LineLen( st_line_v );

        // If past end of line, nothing to do
        if( st_char < LL )
        {
          // Determine m_fn_line_d, m_fn_char:
          CrsPos ncp = Do_dw_get_fn( DL, st_char );

          if( null != ncp )
          {
            Do_x_range( DL, st_char, ncp.crsLine, ncp.crsChar );

            boolean deleted_last_char = ncp.crsChar == LL-1;

            return deleted_last_char ? 2 : 1;
          }
        }
      }
    }
    return 0;
  }
  void Do_cw()
  {
    final int result = Do_dw();

    if     ( result==1 ) Do_i();
    else if( result==2 ) Do_a();
  }
  // CrsPos and st_line_d are in terms of diff line
  CrsPos Do_dw_get_fn( final int st_line_d, final int st_char )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int  st_line_v = ViewLine( pV, st_line_d );
    final int  LL        = pfb.LineLen( st_line_v );
    final char C         = pfb.Get( st_line_v, st_char );

    if( Utils.IsSpace( C )          // On white space
      || ( st_char < Utils.LLM1(LL) // On non-white space before white space
        && Utils.IsSpace( pfb.Get( st_line_v, st_char+1 ) ) ) )
    {
      // w:
      CrsPos ncp_w = GoToNextWord_GetPosition();

      if( null != ncp_w && 0 < ncp_w.crsChar ) ncp_w.crsChar--;
      if( null != ncp_w && st_line_d == ncp_w.crsLine
                        && st_char   <= ncp_w.crsChar )
      {
        return ncp_w;
      }
    }
    // if not on white space, and
    // not on non-white space before white space,
    // or fell through, try e:
    CrsPos ncp_e = GoToEndOfWord_GetPosition();

    if( null != ncp_e && st_line_d == ncp_e.crsLine
                      && st_char   <= ncp_e.crsChar )
    {
      return ncp_e;
    }
    return null;
  }

  void Do_J()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int DL  = CrsLine(); // Diff line
    final int VL  = ViewLine( pV, DL ); // View line

    if( VL < pfb.NumLines()-1 )
    {
      ArrayList<Diff_Info> cDI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L; // Current diff info list
      final Diff_Type cDT = cDI_List.get(DL).diff_type; // Current diff type

      if( 0 < VL
       && ( cDT == Diff_Type.SAME
         || cDT == Diff_Type.CHANGED
         || cDT == Diff_Type.INSERTED ) )
      {
        final int DLp = DiffLine( pV, VL+1 ); // Diff line for VL+1

        Line lp = pfb.RemoveLine( VL+1 );
        Patch_Diff_Info_Deleted( pV, DLp );

        pfb.AppendLineToLine( VL, lp );
        Patch_Diff_Info_Changed( pV, DL );

        if( !ReDiff() ) Update();
      }
    }
  }
  void Do_yy()
  {
    View pV = m_vis.CV();

    // If there is nothing to 'yy', just return:
    if( 0 < pV.m_fb.NumLines() )
    {
      final int DL = CrsLine();  // Diff line

      // Cant yank a deleted or unknown line
      final Diff_Type DT = DiffType( pV, DL );
      if( DT != Diff_Type.UNKNOWN && DT != Diff_Type.DELETED )
      {
        final int VL = ViewLine( pV, DL ); // View Cursor line

        // Get a copy of CrsLine() line:
        Line l = new Line( pV.m_fb.GetLine( VL ) );

        m_vis.get_reg().clear();
        m_vis.get_reg().add( l );

        m_vis.set_paste_mode( Paste_Mode.LINE );
      }
    }
  }
  void Do_yw()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    // If there is nothing to 'yw', just return:
    if( 0<pfb.NumLines() )
    {
      final int       DL = CrsLine(); // Diff line
      final Diff_Type DT = DiffType( pV, DL );

      if( DT == Diff_Type.SAME
       || DT == Diff_Type.CHANGED
       || DT == Diff_Type.INSERTED )
      {
        final int st_line_v = ViewLine( pV, DL ); // View line
        final int st_char   = CrsChar();

        // Determine fn_line_d, fn_char:
        CrsPos ncp = Do_dw_get_fn( DL, st_char );

        if( null != ncp )
        {
          final int fn_line_d = ncp.crsLine; // Finish line diff
          final int fn_char   = ncp.crsChar;

          Line nlr = new Line();
          m_vis.get_reg().clear();
          m_vis.get_reg().add( nlr );

          // DL and fn_line_d should be the same
          for( int k=st_char; k<=fn_char; k++ )
          {
          //m_vis.get_reg().get(0).append_c( pfb.Get( st_line_v, k ) );
            nlr.append_c( pfb.Get( st_line_v, k ) );
          }
          m_vis.set_paste_mode( Paste_Mode.ST_FN );
        }
      }
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
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    Swap_Visual_St_Fn_If_Needed();

    for( int DL=v_st_line; DL<=v_fn_line; DL++ )
    {
      Line nlr = new Line();

      final int VL = ViewLine( pV, DL );
      final int LL = pfb.LineLen( VL );

      for( int P = v_st_char; P<LL && P <= v_fn_char; P++ )
      {
        nlr.append_c( pfb.Get( VL, P ) );
      }
      m_vis.get_reg().add( nlr );
    }
    m_vis.set_paste_mode( Paste_Mode.BLOCK );

    // Try to put cursor at (v_st_line, v_st_char), but
    // make sure the cursor is in bounds after the deletion:
    final int NUM_LINES = NumLines( pV );
    int ncl = v_st_line;
    if( NUM_LINES <= ncl ) ncl = NUM_LINES-1;
    final int NLL = pfb.LineLen( ViewLine( pV, ncl ) );
    int ncc = 0;
    if( 0<NLL ) ncc = NLL <= v_st_char ? NLL-1 : v_st_char;

    GoToCrsPos_NoWrite( ncl, ncc );
  }
  void Do_r_v_block()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    Swap_Visual_St_Fn_If_Needed();

    final int old_v_st_line = v_st_line;
    final int old_v_st_char = v_st_char;

    // Add visual block area to m_vis.m_reg:
    for( int L=v_st_line; L<=v_fn_line; L++ )
    {
      Line nlr = new Line();

      final int LL = pfb.LineLen( L );

      boolean continue_last_update = false;

      for( int P = v_st_char; P<LL && P <= v_fn_char; P++ )
      {
        nlr.append_c( pfb.Get( L, P ) );
                      pfb.Set( L, P, ' ', continue_last_update );

        continue_last_update = true;
      }
      m_vis.get_reg().add( nlr );
    }
    m_vis.set_paste_mode( Paste_Mode.BLOCK );

    // Try to put cursor at (old_v_st_line, old_v_st_char), but
    // make sure the cursor is in bounds after the deletion:
    final int NUM_LINES = pfb.NumLines();
    final int ncl = NUM_LINES <= old_v_st_line ? NUM_LINES-1
                                               : old_v_st_line;
    final int NLL = pfb.LineLen( ncl );
    final int ncc = NLL <= 0               ? 0
                  : old_v_st_char <= 0     ? 0
                  : NLL <= old_v_st_char-1 ? NLL-1
                                           : old_v_st_char-1;
    GoToCrsPos_NoWrite( ncl, ncc );
  }
  void Do_y_v_st_fn()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    Swap_Visual_St_Fn_If_Needed();

    for( int L=v_st_line; L<=v_fn_line; L++ )
    {
      final Diff_Type LINE_DIFF_TYPE = DiffType( pV, L );
      if( LINE_DIFF_TYPE != Diff_Type.DELETED )
      {
        Line nlp = new Line();

        // Convert L, which is diff line, to view line
        final int VL = ViewLine( pV, L );
        final int LL = pfb.LineLen( VL );

        if( 0<LL )
        {
          final int P_st = (L==v_st_line) ? v_st_char : 0;
          final int P_fn = (L==v_fn_line) ? Math.min(LL-1,v_fn_char) : LL-1;

          for( int P = P_st; P <= P_fn; P++ )
          {
            nlp.append_c( pfb.Get( VL, P ) );
          }
        }
        // m_vis.reg will delete nlp
        m_vis.get_reg().add( nlp );
      }
    }
    m_vis.set_paste_mode( Paste_Mode.ST_FN );
  }
  void Do_r_v_st_fn()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    Swap_Visual_St_Fn_If_Needed();

    final int old_v_st_line = v_st_line;
    final int old_v_st_char = v_st_char;

    // Add visual start-finish area to m_vis.m_reg:
    for( int L=v_st_line; L<=v_fn_line; L++ )
    {
      Line nlr = new Line();

      boolean continue_last_update = false;

      final int LL = pfb.LineLen( L );
      if( 0<LL ) {
        final int P_st = (L==v_st_line) ? v_st_char : 0;
        final int P_fn = (L==v_fn_line) ? Math.min(LL-1,v_fn_char) : LL-1;

        for( int P = P_st; P <= P_fn; P++ )
        {
          nlr.append_c( pfb.Get( L, P ) );
                        pfb.Set( L, P, ' ', continue_last_update );

          continue_last_update = true;
        }
      }
      m_vis.get_reg().add( nlr );
    }
    m_vis.set_paste_mode( Paste_Mode.ST_FN );

    // Try to put cursor at (old_v_st_line, old_v_st_char-1), but
    // make sure the cursor is in bounds after the deletion:
    final int NUM_LINES = pfb.NumLines();
    final int ncl = NUM_LINES <= old_v_st_line ? NUM_LINES-1
                                               : old_v_st_line;
    final int NLL = pfb.LineLen( ncl );
    final int ncc = NLL <= 0               ? 0
                  : old_v_st_char <= 0     ? 0
                  : NLL <= old_v_st_char-1 ? NLL-1
                                           : old_v_st_char-1;
    GoToCrsPos_NoWrite( ncl, ncc );
  }
  void Do_Y_v()
  {
    m_vis.get_reg().clear();

    if( m_inVisualBlock ) Do_y_v_block();
    else                  Do_Y_v_st_fn();

    m_inVisualMode = false;
  }
  void Do_r_v()
  {
    m_vis.get_reg().clear();

    if( m_inVisualBlock ) Do_r_v_block();
    else                  Do_r_v_st_fn();

    m_inVisualMode = false;
  }
  void Do_Y_v_st_fn()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    Swap_Visual_St_Fn_If_Needed();

    for( int L=v_st_line; L<=v_fn_line; L++ )
    {
      final Diff_Type LINE_DIFF_TYPE = DiffType( pV, L );
      if( LINE_DIFF_TYPE != Diff_Type.DELETED )
      {
        Line nlp = new Line();

        // Convert L, which is diff line, to view line
        final int VL = ViewLine( pV, L );
        final int LL = pfb.LineLen( VL );

        if( 0<LL )
        {
          for( int P = 0; P <= LL-1; P++ )
          {
            nlp.append_c( pfb.Get( VL, P ) );
          }
        }
        // m_vis.m_reg will delete nlp
        m_vis.get_reg().add( nlp );
      }
    }
    m_vis.set_paste_mode( Paste_Mode.LINE );
  }
  void Do_D_v()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    m_vis.get_reg().clear();
    Swap_Visual_St_Fn_If_Needed();

    boolean removed_line = false;
    ArrayList<Diff_Info> cDI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L; // Current diff info list
    ArrayList<Diff_Info> oDI_List = (pV == m_vS) ? m_DI_List_L : m_DI_List_S; // Other   diff info list
    final int VL = ViewLine( pV, v_st_line ); // View line

    // To avoid crashing, dont remove all lines in file
    for( int DL = v_st_line; 1 < pfb.NumLines() && DL<=v_fn_line; DL++ )
    {
      final Diff_Type cDT = cDI_List.get(DL).diff_type; // Current diff type
      final Diff_Type oDT = oDI_List.get(DL).diff_type; // Other   diff type

      if( cDT == Diff_Type.SAME
       || cDT == Diff_Type.CHANGED
       || cDT == Diff_Type.INSERTED )
      {
        Line lp = pfb.RemoveLine( VL );
        m_vis.get_reg().add( lp ); // m_vis.m_reg will delete lp

        Patch_Diff_Info_Deleted( pV, DL );

        removed_line = true;
        // If line on other side is Diff_Type.DELETED, a diff line will be removed
        // from both sides, so decrement DL to stay on same DL, decrement
        // v_fn_line because it just moved up a line
        if( oDT == Diff_Type.DELETED ) { DL--; v_fn_line--; }
      }
    }
    m_vis.set_paste_mode( Paste_Mode.LINE );

    // Deleted lines will be removed, so no need to Undo_v()
    m_inVisualMode = false;

    if( removed_line )
    {
      Do_D_v_find_new_crs_pos();
      if( !ReDiff() ) Update();
    }
  }
  void Do_D_v_find_new_crs_pos()
  {
    View pV = m_vis.CV();

    ArrayList<Diff_Info> cDI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L; // Current diff info list

    // Figure out new cursor position:
    int ncld = v_fn_line+1; // New cursor line diff
    if( cDI_List.size()-1 < ncld ) ncld = cDI_List.size()-1;

    final int nclv = ViewLine( pV, ncld ); // New cursor line view
    final int NCLL = pV.m_fb.LineLen( nclv );

    int ncc = 0;
    if( 0<NCLL ) ncc = v_st_char < NCLL ? v_st_char : NCLL-1;

    GoToCrsPos_NoWrite( ncld, ncc );
  }

  void InsertBackspace_vb()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int DL = CrsLine();          // Diff line number
    final int VL = ViewLine( pV, DL ); // View line number
    final int CP = CrsChar();          // Cursor position

    if( 0<CP )
    {
      final int N_REG_LINES = m_vis.get_reg().size();

      for( int k=0; k<N_REG_LINES; k++ )
      {
        pfb.RemoveChar( VL+k, CP-1 );

        Patch_Diff_Info_Changed( pV, DL+k );
      }
      GoToCrsPos_NoWrite( DL, CP-1 );
    }
  }

  void InsertAddChar_vb( final char c )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int DL = CrsLine();          // Diff line number
    final int VL = ViewLine( pV, DL ); // View line number
    final int CP = CrsChar();          // Cursor position

    final int N_REG_LINES = m_vis.get_reg().size();

    for( int k=0; k<N_REG_LINES; k++ )
    {
      final int LL = pfb.LineLen( VL+k );

      if( LL < CP )
      {
        // Fill in line with white space up to CP:
        for( int i=0; i<(CP-LL); i++ )
        {
          // Insert at end of line so undo will be atomic:
          final int NLL = pfb.LineLen( VL+k ); // New line length
          pfb.InsertChar( VL+k, NLL, ' ' );
        }
      }
      pfb.InsertChar( VL+k, CP, c );

      Patch_Diff_Info_Changed( pV, DL+k );
    }
    GoToCrsPos_NoWrite( DL, CP+1 );
  }

  void Do_i_vb()
  {
    m_vis.get_states().addFirst( m_run_i_vb_end );
    m_vis.get_states().addFirst( m_run_i_vb_mid );
    m_vis.get_states().addFirst( m_run_i_vb_beg );
  }
  void run_i_vb_end()
  {
    View pV = m_vis.CV();

    pV.m_inInsertMode = true;
    DisplayBanner();

    m_i_count = 0;

    m_vis.get_states().removeFirst();
  }
  void run_i_vb_mid()
  {
    if( 0<m_console.KeysIn() )
    {
      final char c = m_console.GetKey();

      if( c == ESC )
      {
        m_vis.get_states().removeFirst(); // Done
      }
      else if( BS == c || DEL == c )
      {
        if( 0<m_i_count )
        {
          InsertBackspace_vb();
          m_i_count--;
          Update();
        }
      }
      else if( Utils.IsEndOfLineDelim( c ) )
      {
        ; // Ignore end of line delimiters
      }
      else {
        InsertAddChar_vb( c );
        m_i_count++;
        Update();
      }
    }
  }
  void run_i_vb_beg()
  {
    View pV = m_vis.CV();

    Remove_Banner();
    pV.m_inInsertMode = false;

    m_vis.get_states().removeFirst();
  }

  void Do_a_vb()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int DL = CrsLine();          // Diff line number
    final int VL = ViewLine( pV, DL ); // View line number
    final int LL = pfb.LineLen( VL );

    if( 0==LL ) { Do_i_vb(); return; }

    final boolean CURSOR_AT_EOL = ( CrsChar() == LL-1 );
    if( CURSOR_AT_EOL )
    {
      GoToCrsPos_NoWrite( DL, LL );
    }
    final boolean CURSOR_AT_RIGHT_COL = ( m_crsCol == WorkingCols( pV )-1 );

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
    Update();

    Do_i_vb();
  }

  boolean Do_s_v_cursor_at_end_of_line()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int DL = CrsLine();  // Diff line
    final int VL = ViewLine( pV, DL );
    final int LL = pfb.LineLen( VL );

    if( m_inVisualBlock )
    {
      return 0<LL ? LL-1 <= CrsChar()
                  : 0    <  CrsChar();
    }
    return 0<LL ? CrsChar() == LL-1 : false;
  }

  void Do_s_v()
  {
    // Need to know if cursor is at end of line before Do_x_v() is called:
    final int LL = LineLen();
    final boolean CURSOR_AT_END_OF_LINE = Do_s_v_cursor_at_end_of_line();

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

  void Do_Tilda_v()
  {
    Swap_Visual_St_Fn_If_Needed();

    if( m_inVisualBlock ) Do_Tilda_v_block();
    else                  Do_Tilda_v_st_fn();

    m_inVisualMode = false;
  }
  void Do_Tilda_v_st_fn()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    for( int DL = v_st_line; DL<=v_fn_line; DL++ )
    {
      final int VL = ViewLine( pV, DL );
      final int LL = pfb.LineLen( VL );
      final int P_st = (DL==v_st_line) ? v_st_char : 0;
      final int P_fn = (DL==v_fn_line) ? v_fn_char : LL-1;

      boolean changed_line = false;

      for( int P = P_st; P <= P_fn; P++ )
      {
        char C = pfb.Get( VL, P );
        boolean changed = false;
        if     ( Utils.IsUpper( C ) ) { C = Utils.ToLower( C ); changed = true; }
        else if( Utils.IsLower( C ) ) { C = Utils.ToUpper( C ); changed = true; }

        if( changed )
        {
          pfb.Set( VL, P, C, true );
          changed_line = true;
        }
      }
      if( changed_line )
      {
        Patch_Diff_Info_Changed( pV, DL );
      }
    }
  }
  void Do_Tilda_v_block()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    for( int L = v_st_line; L<=v_fn_line; L++ )
    {
      final int LL = pfb.LineLen( L );

      for( int P = v_st_char; P<LL && P <= v_fn_char; P++ )
      {
        char C = pfb.Get( L, P );
        boolean changed = false;
        if     ( Utils.IsUpper( C ) ) { C = Utils.ToLower( C ); changed = true; }
        else if( Utils.IsLower( C ) ) { C = Utils.ToUpper( C ); changed = true; }
        if( changed ) pfb.Set( L, P, C, true );
      }
    }
  }

  void Do_x_v()
  {
    if( m_inVisualBlock )
    {
      Do_x_range_block();
    }
    else {
      // Do_x_range is used by Do_dw, so it needs to accept parameters
      Do_x_range( v_st_line, v_st_char, v_fn_line, v_fn_char );
    }
    m_inVisualMode = false;

    Update(); //<- No need to Undo_v() or Remove_Banner() because of this
  }
  void Do_x_range( int st_line, int st_char
                 , int fn_line, int fn_char )
  {
    v_st_line = st_line;
    v_st_char = st_char;
    v_fn_line = fn_line;
    v_fn_char = fn_char;

    Do_x_range_pre();

    if( v_st_line == v_fn_line )
    {
      Do_x_range_single( v_st_line, v_st_char, v_fn_char );
    }
    else {
      Do_x_range_multiple( v_st_line, v_st_char, v_fn_line, v_fn_char );
    }
    Do_x_range_post( v_st_line, v_st_char );
  }
  void Do_x_range_pre()
  {
    Swap_Visual_St_Fn_If_Needed();

    m_vis.get_reg().clear();
  }
  void Do_x_range_post( final int st_line, final int st_char )
  {
    if( m_inVisualBlock ) m_vis.set_paste_mode( Paste_Mode.BLOCK );
    else                  m_vis.set_paste_mode( Paste_Mode.ST_FN );

    View pV = m_vis.CV();

    ArrayList<Diff_Info> cDI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L; // Current diff info list

    // Figure out new cursor position:
    int ncld = st_line; // New cursor line diff // New cursor line diff
    if( cDI_List.size()-1 < ncld ) ncld = cDI_List.size()-1;

    final int nclv = ViewLine( pV, ncld ); // New cursor line view
    final int NCLL = pV.m_fb.LineLen( nclv );

    int ncc = 0;
    if( 0<NCLL ) ncc = st_char < NCLL ? st_char : NCLL-1;

    GoToCrsPos_NoWrite( ncld, ncc );

    m_inVisualMode = false;

    if( !ReDiff() ) Update(); //<- No need to Undo_v() or Remove_Banner() because of this
  }
  void Do_x_range_single( final int DL
                        , final int st_char
                        , final int fn_char )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int VL = ViewLine( pV, DL ); // View line

    Line nlp = new Line();

    int LL = pfb.LineLen( VL );

    // Dont remove a single line, or else Q wont work right
    boolean removed_char = false;

    for( int P = st_char; st_char < LL && P <= fn_char; P++ )
    {
      nlp.append_c( pfb.RemoveChar( VL, st_char ) );
      LL = pfb.LineLen( VL ); // Removed a char, so re-set LL
      removed_char = true;
    }
    if( removed_char ) Patch_Diff_Info_Changed( pV, DL );

    m_vis.get_reg().add( nlp );
  }
  void Do_x_range_multiple( final int st_line
                          , final int st_char
                          , final int fn_line
                          , final int fn_char )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    ArrayList<Diff_Info> cDI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L; // Current diff info list
    ArrayList<Diff_Info> oDI_List = (pV == m_vS) ? m_DI_List_L : m_DI_List_S; // Other   diff info list

    boolean started_in_middle = false;
    boolean ended___in_middle = false;

    int n_fn_line = fn_line; // New finish line

    for( int DL = st_line; DL<=n_fn_line; DL++ )
    {
      final Diff_Type cDT = cDI_List.get(DL).diff_type; // Current diff type
      final Diff_Type oDT = oDI_List.get(DL).diff_type; // Other diff type

      if( cDT != Diff_Type.SAME       // If cDT is UNKNOWN or DELETED,
       && cDT != Diff_Type.CHANGED    // nothing to do so continue
       && cDT != Diff_Type.INSERTED ) continue;

      final int VL  = ViewLine( pV, DL ); // View line
      final int OLL = pfb.LineLen( VL ); // Original line length

      Line nlp = new Line();

      final int P_st = (DL==  st_line) ? Math.min(st_char,OLL-1) : 0;
      final int P_fn = (DL==n_fn_line) ? Math.min(fn_char,OLL-1) : OLL-1;

      if(   st_line == DL && 0    < P_st  ) started_in_middle = true;
      if( n_fn_line == DL && P_fn < OLL-1 ) ended___in_middle = true;

      boolean removed_char = false;
      int LL = OLL;
      for( int P = P_st; P_st < LL && P <= P_fn; P++ )
      {
        nlp.append_c( pfb.RemoveChar( VL, P_st ) );
        LL = pfb.LineLen( VL ); // Removed a char, so re-calculate LL
        removed_char = true;
      }
      if( 0 == P_st && OLL-1 == P_fn )
      {
        pfb.RemoveLine( VL );
        Patch_Diff_Info_Deleted( pV, DL );
        // If line on other side is Diff_Type.DELETED, a diff line will be removed
        // from both sides, so decrement DL to stay on same DL, decrement
        // n_fn_line because it just moved up a line
        if( oDT == Diff_Type.DELETED ) { DL--; n_fn_line--; }
      }
      else {
        if( removed_char ) Patch_Diff_Info_Changed( pV, DL );
      }
      m_vis.get_reg().add( nlp );
    }
    if( started_in_middle && ended___in_middle )
    {
      final int v_st_line  = ViewLine( pV, st_line ); // View line start
      final int v_fn_line  = ViewLine( pV, fn_line ); // View line finish

      Line lr = pfb.RemoveLine( v_fn_line );
      pfb.AppendLineToLine( v_st_line, lr );

      Patch_Diff_Info_Deleted( pV, fn_line );
      Patch_Diff_Info_Changed( pV, st_line );
    }
  }
  void Do_x_range_block()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    Do_x_range_pre();

    for( int DL = v_st_line; DL<=v_fn_line; DL++ )
    {
      final int VL = ViewLine( pV, DL ); // View line

      Line nlr = new Line();

      final int LL = pfb.LineLen( VL );

      for( int P = v_st_char; P<LL && P <= v_fn_char; P++ )
      {
        nlr.append_c( pfb.RemoveChar( VL, v_st_char ) );
      }
      m_vis.get_reg().add( nlr );
    }
    Do_x_range_post( v_st_line, v_st_char );
  }

  void Do_p()
  {
    final Paste_Mode PM = m_vis.get_paste_mode();

    if     ( Paste_Mode.ST_FN == PM ) Do_p_or_P_st_fn( Paste_Pos.After );
    else if( Paste_Mode.BLOCK == PM ) Do_p_block();
    else /*( Paste_Mode.LINE  == PM*/ Do_p_line();
  }
  void Do_p_line()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int DL = CrsLine();          // Diff line
    final int VL = ViewLine( pV, DL ); // View line

    final int NUM_LINES_TO_INSERT = m_vis.get_reg().size();

    ArrayList<Diff_Info> cDI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L; // Current
    Diff_Info cDI = cDI_List.get( DL );

    // If cursor is on a deleted diff line, start inserting lines into that deleted diff line
    // If cursor is NOT on a deleted diff line, start inserting lines below diff cursor line
    final boolean ON_DELETED = Diff_Type.DELETED == cDI.diff_type;
          boolean ODVL0 = On_Deleted_View_Line_Zero( DL );
    final int DL_START = ON_DELETED ? DL : DL+1;
    final int VL_START = ODVL0      ? VL : VL+1;

    for( int k=0; k<NUM_LINES_TO_INSERT; k++ )
    {
      // In FileBuf: Put reg on line below:
      Line nl = new Line( m_vis.get_reg().get(k) );
      pfb.InsertLine( VL_START+k, nl );

      Patch_Diff_Info_Inserted( pV, DL_START+k, ODVL0 );
      ODVL0 = false;
    }
    if( !ReDiff() ) Update();
  }
  void Do_p_or_P_st_fn( Paste_Pos paste_pos )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int NUM_LINES = m_vis.get_reg().size();
    final int ODL       = CrsLine();           // Original Diff line
    final int OVL       = ViewLine( pV, ODL ); // Original View line

    ArrayList<Diff_Info> cDI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L; // Current

    for( int k=0; k<NUM_LINES; k++ )
    {
      Diff_Info cDI = cDI_List.get( ODL+k );

      final boolean ON_DELETED = Diff_Type.DELETED == cDI.diff_type;

      if( 0 == k ) // Add to current line
      {
        Do_p_or_P_st_fn_FirstLine( paste_pos, k, ODL, OVL, ON_DELETED );
      }
      else if( NUM_LINES-1 == k ) // Last line
      {
        Do_p_or_P_st_fn_LastLine( k, ODL, OVL, ON_DELETED );
      }
      else { // Intermediate line
        Do_p_or_P_st_fn_IntermediatLine( k, ODL, OVL, ON_DELETED );
      }
    }
    if( !ReDiff() ) Update();
  }
  void Do_p_or_P_st_fn_FirstLine( Paste_Pos paste_pos
                                , final int k
                                , final int ODL
                                , final int OVL
                                , final boolean ON_DELETED )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int NUM_LINES = m_vis.get_reg().size();

    final int NLL = m_vis.get_reg().get( k ).length();  // New line length
    final int VL  = ViewLine( pV, ODL+k ); // View line

    if( ON_DELETED )
    {
      final boolean ODVL0 = On_Deleted_View_Line_Zero( ODL );

      // In FileBuf: Put reg on line below:
      pfb.InsertLine( ODVL0 ? VL : VL+1, new Line( m_vis.get_reg().get(0) ) );

      Patch_Diff_Info_Inserted( pV, ODL+k, ODVL0 );
    }
    else {
      MoveInBounds_Line();
      final int LL = pfb.LineLen( VL );
      final int CP = CrsChar();         // Cursor position

      // If line we are pasting to is zero length, dont paste a space forward
      final int forward = 0<LL ? ( paste_pos==Paste_Pos.After ? 1 : 0 ) : 0;

      for( int i=0; i<NLL; i++ )
      {
        char C = m_vis.get_reg().get(k).charAt(i);

        pfb.InsertChar( VL, CP+i+forward, C );
      }
      Patch_Diff_Info_Changed( pV, ODL+k );

      // Move rest of first line onto new line below
      if( 1 < NUM_LINES && CP+forward < LL )
      {
        pfb.InsertLine( VL+1 );
        for( int i=0; i<(LL-CP-forward); i++ )
        {
          char C = pfb.RemoveChar( VL, CP + NLL+forward );
          pfb.PushChar( VL+1, C );
        }
        Patch_Diff_Info_Inserted( pV, ODL+k+1, false ); //< Always false since we are starting on line below
      }
    }
  }
  void Do_p_or_P_st_fn_LastLine( final int k
                               , final int ODL
                               , final int OVL
                               , final boolean ON_DELETED )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int VL  = ViewLine( pV, ODL+k ); // View line
    final int NLL = m_vis.get_reg().get( k ).length();  // New line length

    if( ON_DELETED )
    {
      pfb.InsertLine( VL+1, new Line( m_vis.get_reg().get(k) ) );
      Patch_Diff_Info_Inserted( pV, ODL+k, false );
    }
    else {
      for( int i=0; i<NLL; i++ )
      {
        char C = m_vis.get_reg().get(k).charAt(i);
        pfb.InsertChar( VL, i, C );
      }
      Patch_Diff_Info_Changed( pV, ODL+k );
    }
  }
  void Do_p_or_P_st_fn_IntermediatLine( final int k
                                      , final int ODL
                                      , final int OVL
                                      , final boolean ON_DELETED )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int NUM_LINES = m_vis.get_reg().size();

    final int NLL = m_vis.get_reg().get( k ).length();  // New line length
    final int VL  = ViewLine( pV, ODL+k ); // View line

    if( ON_DELETED )
    {
      // In FileBuf: Put reg on line below:
      pfb.InsertLine( VL+1, new Line( m_vis.get_reg().get(k) ) );

      Patch_Diff_Info_Inserted( pV, ODL+k, false );
    }
    else {
      MoveInBounds_Line();
      final int LL = pfb.LineLen( VL );

      for( int i=0; i<NLL; i++ )
      {
        char C = m_vis.get_reg().get(k).charAt(i);

        pfb.InsertChar( VL, i, C );
      }
      Patch_Diff_Info_Changed( pV, ODL+k );

      // Move rest of first line onto new line below
      if( 1 < NUM_LINES && 0 < LL )
      {
        pfb.InsertLine( VL+1 );
        for( int i=0; i<LL; i++ )
        {
          char C = pfb.RemoveChar( VL, NLL );
          pfb.PushChar( VL+1, C );
        }
        Patch_Diff_Info_Inserted( pV, ODL+k+1, false ); //< Always false since we are starting on line below
      }
    }
  }
  void Do_p_block()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;
    ArrayList<Diff_Info> cDI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L; // Current

    final int DL = CrsLine();          // Diff line
    final int CP = CrsChar();          // Cursor position
    final int VL = ViewLine( pV, DL ); // View line
    final boolean ON_DELETED = Diff_Type.DELETED == cDI_List.get( DL ).diff_type;
    final int LL = ON_DELETED ? 0 : pfb.LineLen( VL ); // Line length
    final int ISP = 0<CP ? CP+1        // Insert position
                  : ( 0<LL ? 1:0 );    // If at beginning of line,
                                       // and LL is zero insert at 0,
                                       // else insert at 1
    final int N_REG_LINES = m_vis.get_reg().size();

    for( int k=0; k<N_REG_LINES; k++ )
    {
      if( VL+k < pfb.NumLines()
       && Diff_Type.DELETED != cDI_List.get( DL+k ).diff_type )
      {
        Do_p_block_Change_Line( k, DL, VL, ISP );
      }
      else {
        Do_p_block_Insert_Line( k, DL, 0<VL?VL+1:0, ISP );
      }
    }
    if( !ReDiff() ) Update();
  }
  void Do_p_block_Insert_Line( final int k
                             , final int DL
                             , final int VL
                             , final int ISP )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    pfb.InsertLine( VL+k );

    final int LL_k = pfb.LineLen( VL+k );

    if( LL_k < ISP )
    {
      // Fill in line with white space up to ISP:
      for( int i=0; i<(ISP-LL_k); i++ )
      {
        // Insert at end of line so undo will be atomic:
        final int NLL = pfb.LineLen( VL+k ); // New line length
        pfb.InsertChar( VL+k, NLL, ' ' );
      }
    }
    Line reg_line = m_vis.get_reg().get(k);
    final int RLL = reg_line.length();

    for( int i=0; i<RLL; i++ )
    {
      char C = reg_line.charAt(i);

      pfb.InsertChar( VL+k, ISP+i, C );
    }
    final boolean ODVL0 = On_Deleted_View_Line_Zero( DL+k );

    Patch_Diff_Info_Inserted( pV, DL+k, ODVL0 );
  }
  void Do_p_block_Change_Line( final int k
                             , final int DL
                             , final int VL
                             , final int ISP )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int LL_k = pfb.LineLen( VL+k );

    if( LL_k < ISP )
    {
      // Fill in line with white space up to ISP:
      for( int i=0; i<(ISP-LL_k); i++ )
      {
        // Insert at end of line so undo will be atomic:
        final int NLL = pfb.LineLen( VL+k ); // New line length
        pfb.InsertChar( VL+k, NLL, ' ' );
      }
    }
    Line reg_line = m_vis.get_reg().get(k);
    final int RLL = reg_line.length();

    for( int i=0; i<RLL; i++ )
    {
      char C = reg_line.charAt(i);

      pfb.InsertChar( VL+k, ISP+i, C );
    }
    Patch_Diff_Info_Changed( pV, DL+k );
  }

  void Do_P()
  {
    final Paste_Mode PM = m_vis.get_paste_mode();

    if     ( Paste_Mode.ST_FN == PM ) Do_p_or_P_st_fn( Paste_Pos.After );
    else if( Paste_Mode.BLOCK == PM ) Do_P_block();
    else /*( Paste_Mode.LINE  == PM*/ Do_P_line();
  }
  void Do_P_line()
  {
    final int DL = CrsLine(); // Diff line

    // Move to line above, and then do 'p':
    if( 0<DL ) GoToCrsPos_NoWrite( DL-1, CrsChar() );

    Do_p_line();
  }
  void Do_P_block()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;
    ArrayList<Diff_Info> cDI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L; // Current

    final int DL = CrsLine();          // Diff line
    final int CP = CrsChar();          // Cursor position
    final int VL = ViewLine( pV, DL ); // View line
    final boolean ON_DELETED = Diff_Type.DELETED == cDI_List.get( DL ).diff_type;
    final int LL = ON_DELETED ? 0 : pfb.LineLen( VL ); // Line length
    final int ISP = 0<CP ? CP : 0;     // Insert position

    final int N_REG_LINES = m_vis.get_reg().size();

    for( int k=0; k<N_REG_LINES; k++ )
    {
      if( VL+k < pfb.NumLines()
       && Diff_Type.DELETED != cDI_List.get( DL+k ).diff_type )
      {
        Do_p_block_Change_Line( k, DL, VL, ISP );
      }
      else {
        Do_p_block_Insert_Line( k, DL, 0<VL?VL+1:0, ISP );
      }
    }
    if( !ReDiff() ) Update();
  }

  void Do_r()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int ODL = CrsLine();           // Old Diff line
    final int OVL = ViewLine( pV, ODL ); // View line

  //final int OCL = CrsLine();           // Old cursor line
    final int OCP = CrsChar();           // Old cursor position
    final int OLL = pfb.LineLen( OVL );  // Old line length

    // Insert position. If OLL is zero, insert at 0, else insert in from of OCP
    final int ISP = 0<OLL ? OCP+1 : 0;

    final int N_REG_LINES = m_vis.get_reg().size();

    for( int k=0; k<N_REG_LINES; k++ )
    {
      // Make sure file has a line where register line will be inserted:
      if( pfb.NumLines()<=OVL+k ) pfb.InsertLine( OVL+k );

      final int LL = pfb.LineLen( OVL+k );

      // Make sure file line is as long as ISP before inserting register line:
      if( LL < ISP )
      {
        // Fill in line with white space up to ISP:
        for( int i=0; i<(ISP-LL); i++ )
        {
          // Insert at end of line so undo will be atomic:
          final int NLL = pfb.LineLen( OVL+k ); // New line length
          pfb.InsertChar( OVL+k, NLL, ' ' );
        }
      }
      Do_r_replace_white_space_with_register_line( k, OVL, ISP );

      Patch_Diff_Info_Changed( pV, ODL+k );
    }
    Update();
  }
  void Do_r_replace_white_space_with_register_line( final int k
                                                  , final int OCL
                                                  , final int ISP )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    // Replace white space with register line, insert after white space used:
    Line reg_line = m_vis.get_reg().get(k);
    final int RLL = reg_line.length();
    final int OLL = pfb.LineLen( OCL+k );

    boolean continue_last_update = false;

    for( int i=0; i<RLL; i++ )
    {
      char C_new = reg_line.charAt(i);

      boolean replaced_space = false;

      if( ISP+i < OLL )
      {
        char C_old = pfb.Get( OCL+k, ISP+i );

        if( C_old == ' ' )
        {
          // Replace ' ' with C_new:
          pfb.Set( OCL+k, ISP+i, C_new, continue_last_update );
          replaced_space = true;
          continue_last_update = true;
        }
      }
      // No more spaces or end of line, so insert:
      if( !replaced_space ) pfb.InsertChar( OCL+k, ISP+i, C_new );
    }
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

    // Write current byte in visual:
    Replace_Crs_Char( Style.VISUAL );

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
      else if( C == 'r' ) { Do_r_v(); }
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
    Undo_v();
    Remove_Banner();

    m_vis.get_states().removeFirst();

    if( !m_console.get_from_dot_buf() )
    {
      m_console.set_save_2_vis_buf( false );

      if( m_copy_vis_buf_2_dot_buf )
      {
        m_console.copy_vis_buf_2_dot_buf();
      }
    }
  }
  void Undo_v()
  {
    if( m_undo_v )
    {
      Update();
    }
  }

  void Replace_Crs_Char( Style style )
  {
    View pV  = m_vis.CV();

    // Convert CL, which is diff line, to view line:
    final int CLv = ViewLine( pV, CrsLine() );

    final int LL = pV.m_fb.LineLen( CLv ); // Line length
    if( 0<LL )
    {
      char C = pV.m_fb.Get( CLv, CrsChar() );

      m_console.Set( Row_Win_2_GL( pV, CrsLine()-m_topLine )
                   , Col_Win_2_GL( pV, CrsChar()-m_leftChar )
                   , C, style );
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
      View    pV  = m_vis.CV();
      FileBuf pfb = pV.m_fb;

      Swap_Visual_St_Fn_If_Needed();

      final int VL = ViewLine( pV, v_st_line );

      m_sb.setLength( 0 );

      for( int P = v_st_char; P<=v_fn_char; P++ )
      {
        m_sb.append( pfb.Get( VL, P ) );
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
      View    pV  = m_vis.CV();
      FileBuf pfb = pV.m_fb;

      Swap_Visual_St_Fn_If_Needed();

      final int VL = ViewLine( pV, v_st_line );

      StringBuilder pattern = new StringBuilder();

      for( int P = v_st_char; P<=v_fn_char; P++ )
      {
        pattern.append( pfb.Get( VL, P  ) );
      }
      m_vis.Handle_Slash_GotPattern( pattern.toString(), false );

      m_inVisualMode = false;
    }
  }

  // This one works better when IN visual mode:
  void PageDown_v()
  {
    final int NUM_LINES = NumLines();

    if( 0<NUM_LINES )
    {
      final int OCLd = CrsLine(); // Old cursor line diff

      int NCLd = OCLd + WorkingRows( m_vis.CV() ) - 1; // New cursor line diff

      // Dont let cursor go past the end of the file:
      if( NUM_LINES-1 < NCLd ) NCLd = NUM_LINES-1;

      GoToCrsPos_Write( NCLd, 0 );
    }
  }
  // This one works better when IN visual mode:
  void PageUp_v()
  {
    final int NUM_LINES = NumLines();

    if( 0<NUM_LINES )
    {
      final int OCLd = CrsLine(); // Old cursor line diff

      int NCLd = OCLd - WorkingRows( m_vis.CV() ) + 1; // New cursor line diff

      // Check for underflow:
      if( NCLd < 0 ) NCLd = 0;

      GoToCrsPos_Write( NCLd, 0 );
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
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    pV.m_inReplaceMode = true;

    DisplayBanner();

    if( 0 == pfb.NumLines() ) pfb.PushLine();

    m_i_count = 0; // Re-use m_i_count form m_R_count

    m_vis.get_states().removeFirst();
  }
  void run_R_mid()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

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
        ReplaceAddReturn();
        m_i_count++;
      }
      else {
        ReplaceAddChar( C );
        m_i_count++;
      }
    }
  }
  void run_R_end()
  {
    Remove_Banner();
    m_vis.CV().m_inReplaceMode = false;

    // Move cursor back one space:
    if( 0<m_crsCol )
    {
      Set_crsCol( m_crsCol-1 );
    }
    Update();

    m_vis.get_states().removeFirst();
  }
  void ReplaceAddReturn()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;
    // The lines in fb do not end with '\n's.
    // When the file is written, '\n's are added to the ends of the lines.
    Line new_line = new Line();
    final int ODL = CrsLine();
    final int OVL = ViewLine( pV, ODL ); // View line number
    final int OLL = pfb.LineLen( OVL );
    final int OCP = CrsChar();

    for( int k=OCP; k<OLL; k++ )
    {
      final char C = pfb.RemoveChar( OVL, OCP );
      new_line.append_c( C );
    }
    // Truncate the rest of the old line:
    // Add the new line:
    final int new_line_num = OVL+1;
    pfb.InsertLine( new_line_num, new_line );
    GoToCrsPos_NoWrite( ODL+1, 0 );

    Patch_Diff_Info_Changed ( pV, ODL );
    Patch_Diff_Info_Inserted( pV, ODL+1, false );
    Update();
  }
  void ReplaceAddChar( final char C )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    if( pfb.NumLines()==0 ) pfb.PushLine();

    final int DL = CrsLine();
    final int VL = ViewLine( pV, DL ); // View line number

    ArrayList<Diff_Info> cDI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L; // Current
    Diff_Info cDI = cDI_List.get( DL );

    final boolean ON_DELETED = Diff_Type.DELETED == cDI.diff_type;
    if( ON_DELETED )
    {
      ReplaceAddChar_ON_DELETED( C, DL, VL );
    }
    else {
      final int CP = CrsChar();
      final int LL = pfb.LineLen( VL );
      final int EOL = 0<LL ? LL-1 : 0;

      if( EOL < CP )
      {
        // Extend line out to where cursor is:
        for( int k=LL; k<CP; k++ ) pfb.PushChar( VL, ' ' );
      }
      // Put char back in file buffer
      final boolean continue_last_update = false;
      if( CP < LL ) pfb.Set( VL, CP, C, continue_last_update );
      else {
        pfb.PushChar( VL, C );
      }
      Patch_Diff_Info_Changed( pV, DL );
    }
    if( m_crsCol < WorkingCols( pV )-1 )
    {
      m_crsCol++;
    }
    else {
      m_leftChar++;
    }
    Update();
  }
  void ReplaceAddChar_ON_DELETED( final char C
                                , final int DL
                                , final int VL )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    ArrayList<Diff_Info> cDI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L; // Current

    final boolean ODVL0 = On_Deleted_View_Line_Zero( DL );

    Line nl = new Line();
    nl.append_c( C );
    pfb.InsertLine( ODVL0 ? VL : VL+1, nl );
    Patch_Diff_Info_Inserted( pV, DL, ODVL0 );
  }

  void Do_Tilda()
  {
    View pV = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    if( 0==pfb.NumLines() ) return;

    final int DL = CrsLine();          // Diff line
    final int VL = ViewLine( pV, DL ); // View line
    final int CP = CrsChar();          // Cursor position
    final int LL = pfb.LineLen( VL );

    if( 0<LL && CP<LL )
    {
      char C = pfb.Get( VL, CP );
      boolean changed = false;
      if     ( Character.isUpperCase( C ) ) { C = Character.toLowerCase( C ); changed = true; }
      else if( Character.isLowerCase( C ) ) { C = Character.toUpperCase( C ); changed = true; }

      final boolean CONT_LAST_UPDATE = true;
      if( m_crsCol < Math.min( LL-1, WorkingCols( pV )-1 ) )
      {
        if( changed ) pfb.Set( VL, CP, C, CONT_LAST_UPDATE );
        // Need to move cursor right:
        m_crsCol++;
      }
      else if( RightChar( pV ) < LL-1 )
      {
        // Need to scroll window right:
        if( changed ) pfb.Set( VL, CP, C, CONT_LAST_UPDATE );
        m_leftChar++;
      }
      else // RightChar() == LL-1
      {
        // At end of line so cant move or scroll right:
        if( changed ) pfb.Set( VL, CP, C, CONT_LAST_UPDATE );
      }
      if( changed ) Patch_Diff_Info_Changed( pV, DL );
      Update();
    }
  }
  void Do_u()
  {
    View pV = m_vis.CV();

    pV.m_fb.Undo( pV );
  }
  void Do_U()
  {
    View pV = m_vis.CV();

    pV.m_fb.UndoAll( pV );
  }

  boolean On_Deleted_View_Line_Zero( final int DL )
  {
    boolean ODVL0 = false; // On Deleted View Line Zero

    View pV = m_vis.CV();
    ArrayList<Diff_Info> cDI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L; // Current
    Diff_Info cDI = cDI_List.get( DL );

    if( Diff_Type.DELETED == cDI.diff_type )
    {
      ODVL0 = true;

      for( int k=0; ODVL0 && k<DL; k++ )
      {
        if( Diff_Type.DELETED != cDI_List.get(k).diff_type ) ODVL0 = false;
      }
    }
    return ODVL0;
  }
  //| Action | ThisSide | OtherSide | Action
  //--------------------------------------------------------------------------------
  //| Change | SAME     | SAME      | Change this side and other side to CHANGED
  //|        | CHANGED  | CHANGED   | Compare sides, if same change both to SAME, else leave both CHANGED
  //|        | INSERTED | DELETED   | Dont change anything
  void Patch_Diff_Info_Changed( View pV, final int DPL )
  {
    ArrayList<Diff_Info> cDI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L; // Current
    ArrayList<Diff_Info> oDI_List = (pV == m_vS) ? m_DI_List_L : m_DI_List_S; // Other

    Diff_Info cDI = cDI_List.get( DPL ); // Current Diff_Info
    Diff_Info oDI = oDI_List.get( DPL ); // Other   Diff_Info

    Diff_Info sDI = m_DI_List_S.get( DPL ); // Short   Diff_Info
    Diff_Info lDI = m_DI_List_L.get( DPL ); // Long    Diff_Info

    Line ls = m_fS.GetLine( sDI.line_num ); // Line from short view
    Line ll = m_fL.GetLine( lDI.line_num ); // Line from long  view

    if( Diff_Type.SAME == cDI.diff_type )
    {
      if( null == sDI.pLineInfo ) sDI.pLineInfo = new LineInfo();
      if( null == lDI.pLineInfo ) lDI.pLineInfo = new LineInfo();

      Compare_Lines( ls, sDI.pLineInfo, ll, lDI.pLineInfo );

      cDI.diff_type = Diff_Type.CHANGED;
      oDI.diff_type = Diff_Type.CHANGED;
    }
    else if( Diff_Type.CHANGED == cDI.diff_type )
    {
      if( ls.chksum_diff() == ll.chksum_diff() ) // Lines are now equal
      {
        cDI.diff_type = Diff_Type.SAME;
        oDI.diff_type = Diff_Type.SAME;

        cDI.pLineInfo = null;
        oDI.pLineInfo = null;
      }
      else { // Lines are still different
        if( null == sDI.pLineInfo ) sDI.pLineInfo = new LineInfo();
        if( null == lDI.pLineInfo ) lDI.pLineInfo = new LineInfo();

        Compare_Lines( ls, sDI.pLineInfo, ll, lDI.pLineInfo );
      }
    }
  }

  //| Action | ThisSide | OtherSide | Action
  //--------------------------------------------------------------------------------
  //| Insert | DELETED  | INSERTED  | Compare sides, if same set both to SAME, else set both to CHANGED
  //|        | -------- | ANY OTHER | Add line to both sides, and set this side to INSERTED and other side to DELETED
  void Patch_Diff_Info_Inserted( View pV, final int DPL, final boolean ON_DELETED_VIEW_LINE_ZERO )
  {
    ArrayList<Diff_Info> cDI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L; // Current
    ArrayList<Diff_Info> oDI_List = (pV == m_vS) ? m_DI_List_L : m_DI_List_S; // Other

    final int DI_Len = cDI_List.size();

    if( DI_Len <= DPL )
    {
      // Inserting onto end of Diff_Info lists:
      Diff_Info dic = new Diff_Info( Diff_Type.INSERTED, cDI_List.get( DI_Len-1 ).line_num+1, null );
      Diff_Info dio = new Diff_Info( Diff_Type.DELETED , oDI_List.get( DI_Len-1 ).line_num  , null );

      cDI_List.add( dic );
      oDI_List.add( dio );
    }
    else { // Inserting into beginning or middle of Diff_Info lists:
      Diff_Info cDI = cDI_List.get( DPL );
      Diff_Info oDI = oDI_List.get( DPL );

      if( Diff_Type.DELETED == cDI.diff_type )
      {
        Patch_Diff_Info_Inserted_Inc( DPL, ON_DELETED_VIEW_LINE_ZERO, cDI_List );

        Diff_Info sDI = m_DI_List_S.get( DPL ); // Short Diff_Info
        Diff_Info lDI = m_DI_List_L.get( DPL ); // Long  Diff_Info

        Line ls = m_fS.GetLine( sDI.line_num ); // Line from short view
        Line ll = m_fL.GetLine( lDI.line_num ); // Line from long  view

        if( ls.chksum_diff() == ll.chksum_diff() ) // Lines are equal
        {
          cDI.diff_type = Diff_Type.SAME;
          oDI.diff_type = Diff_Type.SAME;
        }
        else { // Lines are different
          if( null == sDI.pLineInfo ) sDI.pLineInfo = new LineInfo();
          if( null == lDI.pLineInfo ) lDI.pLineInfo = new LineInfo();

          Compare_Lines( ls, sDI.pLineInfo, ll, lDI.pLineInfo );

          cDI.diff_type = Diff_Type.CHANGED;
          oDI.diff_type = Diff_Type.CHANGED;
        }
      }
      else { // Diff_Type.DELETED != cDI.diff_type
        int dio_line = Diff_Type.DELETED==oDI.diff_type
                     ? oDI.line_num                            // Use current  line number
                     : (0<oDI.line_num ? oDI.line_num-1 : 0 ); // Use previous line number
        // Current side gets current line number
        Diff_Info dic = new Diff_Info( Diff_Type.INSERTED, cDI.line_num, null );
        Diff_Info dio = new Diff_Info( Diff_Type.DELETED , dio_line    , null );
        cDI_List.add( DPL, dic );
        oDI_List.add( DPL, dio );

        // Added a view line, so increment all following view line numbers:
        for( int k=DPL+1; k<cDI_List.size(); k++ )
        {
          cDI_List.get( k ).line_num++;
        }
      }
    }
  }
  // Since a line was just inserted, increment line numbers of all lines
  // following, and increment line number of inserted line if needed.
  void Patch_Diff_Info_Inserted_Inc( final int DPL
                                   , final boolean ON_DELETED_VIEW_LINE_ZERO
                                   , ArrayList<Diff_Info> cDI_List )
  {
    // If started inserting into empty first line in file, dont increment
    // Diff_Info line_num, because DELETED first line starts at zero:
    int inc_st = DPL;
    if( ON_DELETED_VIEW_LINE_ZERO )
    {
      inc_st = DPL+1;
      // Since we just inserted into DELETED_VIEW_LINE_ZERO,
      // current line is line zero.
      // Move increment start down to first non-DELETED line after current line.
      for( int k=inc_st; k<cDI_List.size(); k++ )
      {
        Diff_Info di = cDI_List.get( k );
        if( Diff_Type.DELETED == di.diff_type )
        {
          inc_st = k+1;
        }
        else break;
      }
    }
    // Added a view line, so increment all following view line numbers:
    for( int k=inc_st; k<cDI_List.size(); k++ )
    {
      cDI_List.get( k ).line_num++;
    }
  }

  //| Action | ThisSide | OtherSide | Action
  //--------------------------------------------------------------------------------
  //| Delete | SAME     | SAME      | Change this side to DELETED and other side to INSERTED
  //|        | CHANGED  | CHANGED   | Change this side to DELETED and other side to INSERTED
  //|        | INSERTED | DELETED   | Remove line on both sides
  //|        | DELETED  | --------- | Do nothing
  void Patch_Diff_Info_Deleted( View pV, final int DPL )
  {
    ArrayList<Diff_Info> cDI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L; // Current
    ArrayList<Diff_Info> oDI_List = (pV == m_vS) ? m_DI_List_L : m_DI_List_S; // Other

    Diff_Info cDI = cDI_List.get( DPL );
    Diff_Info oDI = oDI_List.get( DPL );

    if( Diff_Type.SAME == cDI.diff_type )
    {
      cDI.diff_type = Diff_Type.DELETED;
      oDI.diff_type = Diff_Type.INSERTED;
    }
    else if( Diff_Type.CHANGED == cDI.diff_type )
    {
      cDI.diff_type = Diff_Type.DELETED;
      oDI.diff_type = Diff_Type.INSERTED;

      cDI.pLineInfo = null;
      oDI.pLineInfo = null;
    }
    else if( Diff_Type.INSERTED == cDI.diff_type )
    {
      cDI_List.remove( DPL );
      oDI_List.remove( DPL );
    }
    else {
    }
    // Removed a view line, so decrement current and all following view line numbers:
    for( int k=DPL; k<cDI_List.size(); k++ )
    {
      if( 0<cDI_List.get( k ).line_num )
      {
        cDI_List.get( k ).line_num--;
      }
    }
  }
  void Swap_Visual_St_Fn_If_Needed()
  {
    if( m_inVisualBlock )
    {
      if( v_fn_line < v_st_line ) { int T = v_st_line; v_st_line = v_fn_line; v_fn_line = T; }
      if( v_fn_char < v_st_char ) { int T = v_st_char; v_st_char = v_fn_char; v_fn_char = T; }
    }
    if( v_fn_line < v_st_line
     || (v_fn_line == v_st_line && v_fn_char < v_st_char) )
    {
      // Visual mode went backwards over multiple lines, or
      // Visual mode went backwards over one line
      int T = v_st_line; v_st_line = v_fn_line; v_fn_line = T;
          T = v_st_char; v_st_char = v_fn_char; v_fn_char = T;
    }
  }

  // Returns success or failure
  boolean ReDiff()
  {
    boolean ok = false;
    final int DL = CrsLine(); // Diff line number
    final int NUM_DLs = m_DI_List_L.size();
    final int SIDE_BAND = 50;
    int DL_st = DL < SIDE_BAND ? 0 : DL - SIDE_BAND;
    int DL_fn = NUM_DLs;
    if( SIDE_BAND < NUM_DLs )
    {
      DL_fn = NUM_DLs-SIDE_BAND < DL ? NUM_DLs : DL + SIDE_BAND;
    }
    DiffArea da = ReDiff_GetDiffArea( DL_st, DL_fn );

    if( null == da )
    {
      m_vis.CmdLineMessage("rediff: DiffArea not found");
    }
    else {
    //Utils.Log("ReDiff DiffArea:"); da.Print();
      ok = true;

      m_DI_L_ins_idx = Remove_From_DI_Lists( da );

      RunDiff( da );
      Update();
    }
    return ok;
  }

  DiffArea ReDiff_GetDiffArea( final int DL_st, final int DL_fn )
  {
    DiffArea da = null;

    // Diff area if from l_DL_st up to but not including l_DL_fn
    Ptr_Int l_DL_st = new Ptr_Int( DL_st ); // local diff line start
    Ptr_Int l_DL_fn = new Ptr_Int( DL_fn ); // local diff line finish

    boolean found_st = ReDiff_FindDiffSt( l_DL_st );
    boolean found_fn = false;

    if( found_st )
    {
      found_fn = l_DL_fn.val < m_DI_List_L.size()
               ? ReDiff_FindDiffFn( l_DL_fn )
               : true;
    }
    boolean found_diff_area = found_st && found_fn;

    if( found_diff_area )
    {
      da = DL_st_fn_2_DiffArea( l_DL_st.val, l_DL_fn.val );
    }
    else {
      final int DL = CrsLine(); // Diff line number

      Ptr_Int l_DL_st_2 = new Ptr_Int( DL ); // local diff line start
      Ptr_Int l_DL_fn_2 = new Ptr_Int( DL ); // local diff line finish

      found_diff_area = ReDiff_FindDiffSt( l_DL_st_2 )
                     && ReDiff_FindDiffFn( l_DL_fn_2 );

      if( found_diff_area )
      {
        da = DL_st_fn_2_DiffArea( l_DL_st_2.val, l_DL_fn_2.val );
      }
    }
    return da;
  }

  DiffArea DL_st_fn_2_DiffArea( final int DL_st
                              , final int DL_fn )
  {
    DiffArea da = new DiffArea( 0, 0, 0, 0 );

    da.ln_s = ( Diff_Type.DELETED == m_DI_List_S.get( DL_st ).diff_type )
            ? m_DI_List_S.get(DL_st).line_num+1
            : m_DI_List_S.get(DL_st).line_num;

    da.ln_l = ( Diff_Type.DELETED == m_DI_List_L.get( DL_st ).diff_type )
            ? m_DI_List_L.get(DL_st).line_num+1
            : m_DI_List_L.get(DL_st).line_num;

    if( DL_fn < m_DI_List_L.size() )
    {
      da.nlines_s = m_DI_List_S.get(DL_fn).line_num - da.ln_s;
      da.nlines_l = m_DI_List_L.get(DL_fn).line_num - da.ln_l;
    }
    else {
      // Need the extra -1 here to avoid a crash.
      // Not sure why it is needed.
    //da.nlines_s = m_fS.NumLines() - da.ln_s - 1;
    //da.nlines_l = m_fL.NumLines() - da.ln_l - 1;
      da.nlines_s = m_fS.NumLines() - da.ln_s;
      da.nlines_l = m_fL.NumLines() - da.ln_l;
    }
    return da;
  }

  boolean ReDiff_FindDiffSt( Ptr_Int DL_st )
  {
    boolean found_diff_st = false;

    final boolean in_short = m_vis.CV() == m_vS;
    ArrayList<Diff_Info> cDI_List = in_short ? m_DI_List_S : m_DI_List_L; // Current
    Diff_Info cDI_st = cDI_List.get( DL_st.val );

    if( Diff_Type.SAME == cDI_st.diff_type )
    {
      found_diff_st = ReDiff_GetSt_Search_4_Diff_Then_Same( DL_st );
    }
    else if( Diff_Type.CHANGED  == cDI_st.diff_type
          || Diff_Type.INSERTED == cDI_st.diff_type
          || Diff_Type.DELETED  == cDI_st.diff_type )
    {
      found_diff_st = ReDiff_GetDiffSt_Search_4_Same( DL_st );
    }
    return found_diff_st;
  }

  boolean ReDiff_FindDiffFn( Ptr_Int DL_fn )
  {
    boolean found_diff_fn = false;

    final boolean in_short = m_vis.CV() == m_vS;
    ArrayList<Diff_Info> cDI_List = in_short ? m_DI_List_S : m_DI_List_L; // Current
    Diff_Info cDI_fn = cDI_List.get( DL_fn.val );

    if( Diff_Type.SAME == cDI_fn.diff_type )
    {
      found_diff_fn = ReDiff_GetFn_Search_4_Diff_Then_Same( DL_fn );
    }
    else if( Diff_Type.CHANGED  == cDI_fn.diff_type
          || Diff_Type.INSERTED == cDI_fn.diff_type
          || Diff_Type.DELETED  == cDI_fn.diff_type )
    {
      found_diff_fn = ReDiff_GetDiffFn_Search_4_Same( DL_fn );
    }
    else {
    }
    return found_diff_fn;
  }

  boolean ReDiff_GetDiffSt_Search_4_Same( Ptr_Int DL_st )
  {
    final boolean in_short = m_vis.CV() == m_vS;
    ArrayList<Diff_Info> cDI_List = in_short ? m_DI_List_S : m_DI_List_L; // Current

    // Search up for SAME
    boolean found = false;
    int L = DL_st.val;
    for( ; !found && 0<=L; L-- )
    {
      Diff_Info di = cDI_List.get( L );
      if( Diff_Type.SAME == di.diff_type )
      {
        found = true;
        DL_st.val = L+1; // Diff area starts on first diff after first same
      }
    }
    if( !found && L < 0 )
    {
      found = true;
      DL_st.val = 0; // Diff area starts at beginning of file
    }
    return found;
  }

  boolean ReDiff_GetSt_Search_4_Diff_Then_Same( Ptr_Int DL_st )
  {
    final boolean in_short = m_vis.CV() == m_vS;
    ArrayList<Diff_Info> cDI_List = in_short ? m_DI_List_S : m_DI_List_L; // Current

    // Search up for CHANGED, INSERTED or DELETED and then for SAME
    boolean found = false;
    int L = DL_st.val;
    for( ; !found && 0<=L; L-- )
    {
      Diff_Info di = cDI_List.get( L );
      if( Diff_Type.CHANGED  == di.diff_type
       || Diff_Type.INSERTED == di.diff_type
       || Diff_Type.DELETED  == di.diff_type )
      {
        found = true;
      }
    }
    if( found )
    {
      found = false;
      for( ; !found && 0<=L; L-- )
      {
        Diff_Info di = cDI_List.get( L );
        if( Diff_Type.SAME == di.diff_type )
        {
          found = true;
          DL_st.val = L+1; // Diff area starts on first diff after first same
        }
      }
    }
    if( !found && L < 0 )
    {
      found = true;
      DL_st.val = 0; // Diff area starts at beginning of file
    }
    return found;
  }

  boolean ReDiff_GetDiffFn_Search_4_Same( Ptr_Int DL_fn )
  {
    final boolean in_short = m_vis.CV() == m_vS;
    ArrayList<Diff_Info> cDI_List = in_short ? m_DI_List_S : m_DI_List_L; // Current

    // Search down for SAME
    boolean found = false;
    int L = DL_fn.val;
    for( ; !found && L<cDI_List.size(); L++ )
    {
      Diff_Info di = cDI_List.get( L );
      if( Diff_Type.SAME == di.diff_type )
      {
        found = true;
        DL_fn.val = L;
      }
    }
    if( !found && cDI_List.size() < L )
    {
      found = true;
      DL_fn.val = cDI_List.size(); // Diff area ends at end of file
    }
    return found;
  }

  boolean ReDiff_GetFn_Search_4_Diff_Then_Same( Ptr_Int DL_fn )
  {
    final boolean in_short = m_vis.CV() == m_vS;
    ArrayList<Diff_Info> cDI_List = in_short ? m_DI_List_S : m_DI_List_L; // Current

    // Search down for CHANGED, INSERTED or DELETED and then for SAME
    boolean found = false;
    int L = DL_fn.val;
    for( ; !found && L<cDI_List.size(); L++ )
    {
      Diff_Info di = cDI_List.get( L );
      if( Diff_Type.CHANGED  == di.diff_type
       || Diff_Type.INSERTED == di.diff_type
       || Diff_Type.DELETED  == di.diff_type )
      {
        found = true;
      }
    }
    if( found )
    {
      found = false;
      for( ; !found && L<cDI_List.size(); L++ )
      {
        Diff_Info di = cDI_List.get( L );
        if( Diff_Type.SAME == di.diff_type )
        {
          found = true;
          DL_fn.val = L;
        }
      }
    }
    if( !found && cDI_List.size() < L )
    {
      found = true;
      DL_fn.val = cDI_List.size(); // Diff area ends at end of file
    }
    return found;
  }

//int Remove_From_DI_Lists( DiffArea da )
//{
//  int DI_lists_insert_idx = 0;
//
//  int DI_list_s_remove_st = DiffLine_S( da.ln_s );
//  int DI_list_l_remove_st = DiffLine_L( da.ln_l );
//  int DI_list_remove_st = Math.min( DI_list_s_remove_st
//                                  , DI_list_l_remove_st );
//  DI_lists_insert_idx = DI_list_remove_st;
//
//  int DI_list_s_remove_fn = DiffLine_S( da.fnl_s() );
//  int DI_list_l_remove_fn = DiffLine_L( da.fnl_l() );
//  int DI_list_remove_fn = Math.max( DI_list_s_remove_fn
//                                  , DI_list_l_remove_fn );
////Utils.Log("(DI_list_remove_st,DI_list_remove_fn) = ("
////         + (DI_list_remove_st+1)+","+(DI_list_remove_fn+1) +")");
//
//  for( int k=DI_list_remove_st; k<DI_list_remove_fn; k++ )
//  {
//    m_DI_List_S.remove( DI_lists_insert_idx );
//    m_DI_List_L.remove( DI_lists_insert_idx );
//  }
//  return DI_lists_insert_idx;
//}

  int Remove_From_DI_Lists( DiffArea da )
  {
    int DI_lists_insert_idx = 0;

    int DI_list_s_remove_st = DiffLine_S( da.ln_s );
    int DI_list_l_remove_st = DiffLine_L( da.ln_l );
    int DI_list_remove_st = Math.min( DI_list_s_remove_st
                                    , DI_list_l_remove_st );
    DI_lists_insert_idx = DI_list_remove_st;

    int DI_list_s_remove_fn = m_vS.m_fb.NumLines() <= da.fnl_s()
                            ? m_DI_List_S.size()
                            : DiffLine_S( da.fnl_s() );
    int DI_list_l_remove_fn = m_vL.m_fb.NumLines() <= da.fnl_l()
                            ? m_DI_List_L.size()
                            : DiffLine_L( da.fnl_l() );

    int DI_list_remove_fn = Math.max( DI_list_s_remove_fn
                                    , DI_list_l_remove_fn );
  //Utils.Log("(DI_list_remove_st,DI_list_remove_fn) = ("
  //         + (DI_list_remove_st+1)+","+(DI_list_remove_fn+1) +")");

    for( int k=DI_list_remove_st; k<DI_list_remove_fn; k++ )
    {
      m_DI_List_S.remove( DI_lists_insert_idx );
      m_DI_List_L.remove( DI_lists_insert_idx );
    }
    return DI_lists_insert_idx;
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
  ConsoleFx m_console;
  StringBuilder m_sb = new StringBuilder();
  StringBuilder m_cmd_line_sb = new StringBuilder();

  private int m_topLine;  // top  of buffer diff line number.
  private int m_leftChar; // left of buffer view character number.
  private int m_crsRow;   // cursor row    in buffer view. 0 <= m_crsRow < WorkingRows().
  private int m_crsCol;   // cursor column in buffer view. 0 <= m_crsCol < WorkingCols().

  View    m_vS;
  View    m_vL;
  FileBuf m_fS;
  FileBuf m_fL;
  double  m_mod_time_s;
  double  m_mod_time_l;

  ArrayList<SameArea> m_sameList = new ArrayList<>();
  ArrayList<DiffArea> m_diffList = new ArrayList<>();

  ArrayList<Diff_Info> m_DI_List_S = new ArrayList<>();
  ArrayList<Diff_Info> m_DI_List_L = new ArrayList<>();
  int                  m_DI_L_ins_idx;

  ArrayList<SimLines> m_simiList = new ArrayList<>();

  private
  boolean  m_inVisualMode; // true if in visual  mode, else false
  boolean  m_inVisualBlock;
  boolean  m_copy_vis_buf_2_dot_buf;
  boolean  m_undo_v;

  int      v_st_line; // Visual start line number
  int      v_st_char; // Visual start char number on line
  int      v_fn_line; // Visual ending line number
  int      v_fn_char; // Visual ending char number on line

  long    m_diff_ms;
  boolean m_printed_diff_ms;
  int     m_i_count;

  Thread m_run_i_beg    = new Thread( ()->{ run_i_beg   (); m_vis.Give(); } );
  Thread m_run_i_mid    = new Thread( ()->{ run_i_mid   (); m_vis.Give(); } );
  Thread m_run_i_end    = new Thread( ()->{ run_i_end   (); m_vis.Give(); } );
  Thread m_run_R_beg    = new Thread( ()->{ run_R_beg   (); m_vis.Give(); } );
  Thread m_run_R_mid    = new Thread( ()->{ run_R_mid   (); m_vis.Give(); } );
  Thread m_run_R_end    = new Thread( ()->{ run_R_end   (); m_vis.Give(); } );
  Thread m_run_v_beg    = new Thread( ()->{ run_v_beg   (); m_vis.Give(); } );
  Thread m_run_v_mid    = new Thread( ()->{ run_v_mid   (); m_vis.Give(); } );
  Thread m_run_v_end    = new Thread( ()->{ run_v_end   (); m_vis.Give(); } );
  Thread m_run_g_v      = new Thread( ()->{ run_g_v     (); m_vis.Give(); } );
  Thread m_run_z        = new Thread( ()->{ run_z       (); m_vis.Give(); } );
  Thread m_run_f        = new Thread( ()->{ run_f       (); m_vis.Give(); } );
  Thread m_run_i_vb_beg = new Thread( ()->{ run_i_vb_beg(); m_vis.Give(); } );
  Thread m_run_i_vb_mid = new Thread( ()->{ run_i_vb_mid(); m_vis.Give(); } );
  Thread m_run_i_vb_end = new Thread( ()->{ run_i_vb_end(); m_vis.Give(); } );
}

// Diff or Comparison area
class DiffArea
{
  DiffArea( final int ln_s, final int nlines_s
          , final int ln_l, final int nlines_l )
  {
    this.ln_s     = ln_s    ;
    this.ln_l     = ln_l    ;
    this.nlines_s = nlines_s;
    this.nlines_l = nlines_l;
  }
  void Print()
  {
    Utils.Log( toString() );
  }
  public String toString()
  {
    return "DiffArea:"
         + " lines_s=("+(ln_s+1)+","+(fnl_s())+")"
         + " lines_l=("+(ln_l+1)+","+(fnl_l())+")";
  }
  int fnl_s() { return ln_s + nlines_s; }
  int fnl_l() { return ln_l + nlines_l; }

  int ln_s;     // Beginning line number in short file
  int ln_l;     // Beginning line number in long  file
  int nlines_s; // Number of consecutive lines in short file
  int nlines_l; // Number of consecutive lines in long  file
}

class SimLines // Similar lines
{
  int      ln_s;   // Line number in short comp area
  int      ln_l;   // Line number in long  comp area
  int      nbytes; // Number of bytes in common between lines
  LineInfo li_s;   // Line comparison info in short comp area
  LineInfo li_l;   // Line comparison info in long  comp area
}

class Diff_Info
{
  Diff_Info( Diff_Type diff_type
           , int       line_num
           , LineInfo  pLineInfo )
  {
    this.diff_type = diff_type;
    this.line_num  = line_num ;
    this.pLineInfo = pLineInfo;
  }
  Diff_Type diff_type; // Diff type of line this Diff_Info refers to
  int       line_num;  // Line number in file to which this Diff_Info applies (view line)
  LineInfo  pLineInfo; // Only non-nullptr if diff_type is DT_CHANGED
}

