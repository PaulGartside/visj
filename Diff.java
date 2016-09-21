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

class Diff
{
  Diff( Vis vis, Console console )
  {
    m_vis     = vis;
    m_console = console;
  }
  // Returns true if diff took place, else false
  //
  boolean Run( View v0, View v1 )
  {
    // Each buffer must be displaying a different file to do diff:
    if( v0.m_fb != v1.m_fb )
    {
      final Tile_Pos tp0 = v0.m_tile_pos;
      final Tile_Pos tp1 = v1.m_tile_pos;
 
      // Buffers must be vertically split to do diff:
      if( (Tile_Pos.LEFT_HALF == tp0 && Tile_Pos.RITE_HALF == tp1)
       || (Tile_Pos.LEFT_HALF == tp1 && Tile_Pos.RITE_HALF == tp0) )
      {
        if( DiffSameAsPrev( v0, v1 ) )
        {
          // Dont need to re-run the diff, just display the results:
          Update();
        }
        else {
          CleanDiff(); //< Start over with clean slate
 
          final int nLines_0 = v0.m_fb.NumLines();
          final int nLines_1 = v1.m_fb.NumLines();
 
          m_vS = nLines_0 < nLines_1 ? v0 : v1; // Short view
          m_vL = nLines_0 < nLines_1 ? v1 : v0; // Long  view
          m_fS = m_vS.m_fb;
          m_fL = m_vL.m_fb;
          m_mod_time_s = m_fS.m_mod_time;
          m_mod_time_l = m_fL.m_mod_time;
 
          // All lines in both files:
          DiffArea CA = new DiffArea( 0, m_fS.NumLines(), 0, m_fL.NumLines() );
 
          RunDiff( CA );
        }
        return true;
      }
    }
    return false;
  }

  void RunDiff( final DiffArea CA )
  {
    final long t1 = System.currentTimeMillis();

    Popu_SameList( CA );
    Sort_SameList();
  //PrintSameList();
    Popu_DiffList( CA );
  //PrintDiffList();
    Popu_DI_List( CA );
  //PrintDI_List( CA );

    final long t2 = System.currentTimeMillis();
  //m_diff_secs = ( t1 - t2 )/1000;
    m_diff_ms = t2 - t1;
    m_printed_diff_ms = false;
  }

  void CleanDiff()
  {
    m_sameList.clear();
    m_diffList.clear();

    m_DI_List_S.clear();
    m_DI_List_L.clear();

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

  void Update()
  {
    if( m_vis.m_console.m_get_from_dot_buf ) return;

    m_vis.Update_Change_Statuses();

    // Update long view:
    m_fL.Find_Styles( ViewLine( m_vL, m_topLine ) + WorkingRows( m_vL ) );
    m_fL.ClearStars();
    m_fL.Find_Stars();
 
    m_vL.RepositionView();
    m_vL.Print_Borders();
    PrintWorkingView( m_vL );
    PrintStsLine( m_vL );
    m_vL.PrintFileLine();
    PrintCmdLine( m_vL );
 
    // Update short view:
    m_fS.Find_Styles( ViewLine( m_vS, m_topLine ) + WorkingRows( m_vS ) );
    m_fS.ClearStars();
    m_fS.Find_Stars();
 
    m_vS.RepositionView();
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
    m_console.Update();
  }

  int WorkingRows( View pV ) { return pV.m_num_rows - 5; }
  int WorkingCols( View pV ) { return pV.m_num_cols - 2; }
  int CrsLine  () { return m_topLine  + m_crsRow; }
  int CrsChar  () { return m_leftChar + m_crsCol; }
  int BotLine  ( View pV ) { return m_topLine  + WorkingRows( pV )-1; }
  int RightChar( View pV ) { return m_leftChar + WorkingCols( pV )-1; }

  int Row_Win_2_GL( View pV, final int win_row )
  {
    return pV.m_y + 1 + win_row;
  }
  int Col_Win_2_GL( View pV, final int win_col )
  {
    return pV.m_x + 1 + win_col;
  }
  int Line_2_GL( View pV, final int file_line )
  {
    return pV.m_y + 1 + file_line - m_topLine;
  }
  int Char_2_GL( View pV, final int line_char )
  {
    return pV.m_x + 1 + line_char - m_leftChar;
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
    if( Diff_Type.UNKN0WN == rDI.diff_type
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

  int DiffLine_S( final int view_line )
  {
    final int LEN = m_DI_List_S.size();

    // Diff line is greater or equal to view line,
    // so start at view line number and search forward
    boolean ok = true;
    for( int k=view_line; k<LEN && ok; k++ )
    {
      Diff_Info di = m_DI_List_S.get( k );
  
      if( Diff_Type.SAME     == di.diff_type
       || Diff_Type.CHANGED  == di.diff_type
       || Diff_Type.INSERTED == di.diff_type )
      {
        if( view_line == di.line_num ) return k;
      }
    }
    Utils.Assert( false, "view_line : "+ view_line +" : not found");
    return 0;
  }

  int DiffLine_L( final int view_line )
  {
    final int LEN = m_DI_List_L.size();

    // Diff line is greater or equal to view line,
    // so start at view line number and search forward
    boolean ok = true;
    for( int k=view_line; k<LEN && ok; k++ )
    {
      Diff_Info di = m_DI_List_L.get( k );
  
      if( Diff_Type.SAME     == di.diff_type
       || Diff_Type.CHANGED  == di.diff_type
       || Diff_Type.INSERTED == di.diff_type )
      {
        if( view_line == di.line_num ) return k;
      }
    }
    Utils.Assert( false, "view_line : "+ view_line +" : not found");
    return 0;
  }

  void PrintCursor()
  {
    Set_Console_CrsCell();
    m_console.Update();
  }
  void PrintWorkingView( View pV )
  {
    final int NUM_LINES = NumLines();
    final int WR        = WorkingRows( pV );
    final int WC        = WorkingCols( pV );

    int row = 0; // (dl=diff line)
    for( int dl=m_topLine; dl<NUM_LINES && row<WR; dl++, row++ )
    {
      int col=0;
      final int G_ROW = Row_Win_2_GL( pV, row );
      final Diff_Type DT = DiffType( pV, dl );
      if( DT == Diff_Type.UNKN0WN )
      {
        for( ; col<WC; col++ )
        {
          m_console.Set( G_ROW, Col_Win_2_GL( pV, col ), '~', Style.DIFF_DEL );
        }
      }
      else if( DT == Diff_Type.DELETED )
      {
        for( ; col<WC; col++ )
        {
          m_console.Set( G_ROW, Col_Win_2_GL( pV, col ), '-', Style.DIFF_DEL );
        }
      }
      else if( DT == Diff_Type.CHANGED )
      {
        PrintWorkingView_DT_CHANGED( pV, WC, G_ROW, dl, col );
      }
      else // DT == Diff_Type.INSERTED || DT == Diff_Type.SAME
      {
        final int vl = ViewLine( pV, dl ); //(vl=view line)
        final int LL = pV.m_fb.LineLen( vl );
        for( int i=m_leftChar; i<LL && col<WC; i++, col++ )
        {
          char  c = pV.m_fb.Get( vl, i );
          Style s = Get_Style( pV, dl, vl, i );

          if( DT == Diff_Type.INSERTED ) s = DiffStyle( s );
          pV.PrintWorkingView_Set( LL, G_ROW, col, i, c, s );
        }
        for( ; col<WC; col++ )
        {
          m_console.Set( G_ROW, Col_Win_2_GL( pV, col ), ' '
                       , DT==Diff_Type.SAME ? Style.NORMAL : Style.DIFF_NORMAL );
        }
      }
    }
    // Not enough lines to display, fill in with ~
    for( ; row < WR; row++ )
    {
      final int G_ROW = Row_Win_2_GL( pV, row );

      m_console.Set( G_ROW, Col_Win_2_GL( pV, 0 ), '~', Style.EMPTY );

      for( int col=1; col<WC; col++ )
      {
        m_console.Set( G_ROW, Col_Win_2_GL( pV, col ), ' ', Style.EMPTY );
      }
    }
  }

  void PrintWorkingView_DT_CHANGED( View  pV
                                  , final int WC
                                  , final int G_ROW
                                  , final int dl
                                  ,       int col )
  {
    final int vl = ViewLine( pV, dl ); //(vl=view line)
    final int LL = pV.m_fb.LineLen( vl );
    Diff_Info di = (pV == m_vS) ? m_DI_List_S.get( dl ) : m_DI_List_L.get( dl );

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
        else //( Diff_Type.UNKN0WN  == dt )
        {
          m_console.Set( G_ROW, Col_Win_2_GL( pV, col ), '~', Style.DIFF_DEL );
        }
      }
      // Past end of line:
      for( ; col<WC; col++ )
      {
        m_console.Set( G_ROW, Col_Win_2_GL( pV, col ), ' ', Style.NORMAL );
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

  void PrintStsLine( View pV )
  {
    ArrayList<Diff_Info> DI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L;
    FileBuf fb = pV.m_fb;
    final int CLd = CrsLine();                   // Line position diff
    final int CLv = DI_List.get( CLd ).line_num; // Line position view
    final int CC = CrsChar();                    // Char position
    final int LL = 0<NumLines() ? fb.LineLen( CLv )  : 0;
    final int WC = WorkingCols( pV );

    String str = "";
    // When inserting text at the end of a line, CrsChar() == LL
    if( 0 < LL && CC < LL ) // Print current char info:
    {
      final char c = fb.Get( CLv, CC );

      if     (  9 == c ) str = String.valueOf( c ) + "\\t";
      else if( 13 == c ) str = String.valueOf( c ) + "\\r";
      else               str = String.valueOf( c ) +","+ (char)c;
    }
    final int fileSize = fb.GetSize();
    final int  crsByte = fb.GetCursorByte( CLv, CC );
    int percent = (char)(100*(double)crsByte/(double)fileSize + 0.5);
    // Screen width so far
    m_sb.setLength( 0 );
    m_sb.append( "Pos=("+(CLv+1)+","+(CC+1)+")"
               + "  ("+percent+"%, "+crsByte
               + Utils.DIR_DELIM_STR + fb.GetSize()+")"
               + "  Char=("+str+")  ");

    final int SW = m_sb.length(); // Screen width so far

    if     ( SW < WC ) { for( int k=SW; k<WC; k++ ) m_sb.append(' '); }
    else if( WC < SW ) { m_sb.setLength( WC ); } //< Truncate extra part

    m_console.SetS( pV.Sts__Line_Row()
                  , pV.Col_Win_2_GL( 0 )
                  , m_sb.toString()
                  , Style.STATUS );
  }
  void PrintCmdLine( View pV )
  {
    // Prints "--INSERT--" banner, and/or clears command line
    int i=0;
    // Draw insert banner if needed
    if( pV.m_inInsertMode )
    {
      i=10; // Strlen of "--INSERT--"
      m_console.SetS( pV.Cmd__Line_Row(), pV.Col_Win_2_GL( 0 ), "--INSERT--", Style.BANNER );
    }
    final int WC = WorkingCols( pV );

    for( ; i<WC-7; i++ )
    {
      m_console.Set( pV.Cmd__Line_Row(), pV.Col_Win_2_GL( i ), ' ', Style.NORMAL );
    }
    m_console.SetS( pV.Cmd__Line_Row(), pV.Col_Win_2_GL( WC-8 ), "--DIFF--", Style.BANNER );
  }

  Style Get_Style( View  pV
                 , final int DL // Diff line
                 , final int VL // View line
                 , final int pos )
  {
    Style S = Style.EMPTY;

    if( pos < pV.m_fb.LineLen( VL ) )
    {
      S = Style.NORMAL;

      if    ( InVisualArea( pV, DL, pos ) ) S = Style.RV_VISUAL;
      else if( pV.InStar      ( VL, pos ) ) S = Style.STAR;
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

    if     ( L_DT == Diff_Type.UNKN0WN ) return Style.DIFF_DEL;
    else if( L_DT == Diff_Type.DELETED ) return Style.DIFF_DEL;
    else if( L_DT == Diff_Type.SAME
          || L_DT == Diff_Type.INSERTED )
    {
      Style S = Get_Style( pV, DL, VL, pos );
      if( L_DT == Diff_Type.INSERTED ) S = DiffStyle( S );
      return S;
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
        if( di.pLineInfo.size() <= pos ) return Style.NORMAL;
        Diff_Type c_dt = di.pLineInfo.get( pos ); // Char Diff_Type

        if     ( Diff_Type.SAME == c_dt ) return Get_Style( pV, DL, VL, pos );
        else if( Diff_Type.CHANGED  == c_dt
              || Diff_Type.INSERTED == c_dt )
        {
          return DiffStyle( Get_Style( pV, DL, VL, pos ) );
        }
        else if( Diff_Type.DELETED == c_dt ) return Style.DIFF_DEL;
        else /*( Diff_Type.UNKN0WN == c_dt*/ return Style.DIFF_DEL;
      }
    }
    // Fall through.  Should only get here if
    // L_DT == Diff_Type.CHANGED && di.pLineInfo.size() <= pos
    return Style.NORMAL;
  }
  char Get_Char_2( View  pV
                 , final int DL // Diff line
                 , final int VL // View line
                 , final int pos )
  {
    final Diff_Type L_DT = DiffType( pV, DL ); // Line Diff_Type

    if( L_DT == Diff_Type.UNKN0WN
     || L_DT == Diff_Type.DELETED )
    {
      return '-';
    }
    return pV.m_fb.Get( VL, pos );
  }

  // Translation of non-diff styles to diff styles for diff areas
  Style DiffStyle( final Style s )
  {
    // If s is already a DIFF style, just return it
    Style diff_s = s;

    if     ( s == Style.NORMAL
          || s == Style.EMPTY  ) diff_s = Style.DIFF_NORMAL ;
    else if( s == Style.STAR   ) diff_s = Style.DIFF_STAR   ;
    else if( s == Style.COMMENT) diff_s = Style.DIFF_COMMENT;
    else if( s == Style.DEFINE ) diff_s = Style.DIFF_DEFINE ;
    else if( s == Style.CONST  ) diff_s = Style.DIFF_CONST  ;
    else if( s == Style.CONTROL) diff_s = Style.DIFF_CONTROL;
    else if( s == Style.VARTYPE) diff_s = Style.DIFF_VARTYPE;
    else if( s == Style.VISUAL ) diff_s = Style.DIFF_VISUAL ;

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
    if( m_console.m_get_from_dot_buf ) return;

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

        if( ls.chksum() != ll.chksum() ) { cur_same.Clear(); ln_s = _ln_s; }
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
        , m_fS.m_fname, same.m_ln_s+1, same.m_ln_s+same.m_nlines
        , m_fL.m_fname, same.m_ln_l+1, same.m_ln_l+same.m_nlines
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
             , m_fS.m_fname, da.ln_s+1, da.ln_s+da.nlines_s
             , m_fL.m_fname, da.ln_l+1, da.ln_l+da.nlines_l );
    }
  }

  void Popu_DI_List( final DiffArea CA )
  {
    Clear_DI_List_CA( CA.ln_s, CA.fnl_s(), m_DI_List_S );
    Clear_DI_List_CA( CA.ln_l, CA.fnl_l(), m_DI_List_L );

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

  void Clear_DI_List_CA( final int st_line
                       , final int fn_line
                       , ArrayList<Diff_Info> DI_List )
  {
    // Since, Clear_DI_List_CA will only be call when DI_List is
    // fully populated, the Diff_Info.line_num's will be at indexes
    // greater than or equal to st_line
    for( int k=st_line; k<DI_List.size(); k++ )
    {
      Diff_Info di = DI_List.get( k );

      if( st_line <= di.line_num && di.line_num < fn_line )
      {
        DI_List.remove( k );
      }
      else if( fn_line <= di.line_num )
      {
        // Past the range of line_num's we want to remove
        break;
      }
    }
  }

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

    DiffArea da = m_diffList.get( 0 );

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
      Diff_Info dis = new Diff_Info( Diff_Type.SAME, sa.m_ln_s+k, new LineInfo() );
      Diff_Info dil = new Diff_Info( Diff_Type.SAME, sa.m_ln_l+k, new LineInfo() );

      m_DI_List_S.add( dis );
      m_DI_List_L.add( dil );
    }
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
        m_DI_List_S.add( dis );
        m_DI_List_L.add( dil );
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
      DI_List_s.add( dis );
      DI_List_l.add( dil );
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

  int Compare_Lines( Line ls, LineInfo li_s
                   , Line ll, LineInfo li_l )
  {
    if( 0==ls.length() && 0==ll.length() ) { return 1; }
    li_s.clear(); li_l.clear();
    SameLineSec max_same = new SameLineSec();
    Line pls = ls; LineInfo pli_s = li_s;
    Line pll = ll; LineInfo pli_l = li_l;
    if( ll.length() < ls.length() ) { pls = ll; pli_s = li_l;
                                      pll = ls; pli_l = li_s; }
    final int SLL = pls.length();
    final int LLL = pll.length();

    for( int _ch_s = 0; _ch_s<SLL; _ch_s++ )
    {
      int ch_s = _ch_s;
      SameLineSec cur_same = new SameLineSec();

      for( int ch_l = 0; ch_s<SLL && ch_l<LLL; ch_l++ )
      {
        final char cs = pls.charAt( ch_s );
        final char cl = pll.charAt( ch_l );

        if( cs != cl ) { cur_same.m_nbytes = 0; ch_s = _ch_s; }
        else {
          if( 0 == max_same.m_nbytes ) // First char match
          {
            max_same.Init( ch_s, ch_l );
            cur_same.Init( ch_s, ch_l );
          }
          else if( 0 == cur_same.m_nbytes ) // First char match this outer loop
          {
            cur_same.Init( ch_s, ch_l );
          }
          else { // Continuation of cur_same
            cur_same.m_nbytes++;
            if( max_same.m_nbytes < cur_same.m_nbytes ) max_same.Set( cur_same );
          }
          ch_s++;
        }
      }
    }
    Fill_In_LineInfo( SLL, LLL, pli_s, pli_l, max_same, pls, pll );

    return max_same.m_nbytes;
  }

  void Fill_In_LineInfo( final int SLL
                       , final int LLL
                       , final LineInfo pli_s
                       , final LineInfo pli_l
                       , SameLineSec max_same
                       , Line pls
                       , Line pll )
  {
    pli_l.ensureCapacity( LLL );
    pli_s.ensureCapacity( LLL );

    for( int k=0; k<SLL; k++ )
    {
      pli_s.add( Diff_Type.CHANGED );
      pli_l.add( Diff_Type.CHANGED );
    }
    for( int k=SLL; k<LLL; k++ )
    {
      pli_s.add( Diff_Type.DELETED  );
      pli_l.add( Diff_Type.INSERTED );
    }
    for( int k=0; k<max_same.m_nbytes; k++ )
    {
      pli_s.set( k+max_same.m_ch_s, Diff_Type.SAME );
      pli_l.set( k+max_same.m_ch_l, Diff_Type.SAME );
    }
    final int SAME_ST = Math.min( max_same.m_ch_s, max_same.m_ch_l );
    final int SAME_FN = Math.max( max_same.m_ch_s+max_same.m_nbytes
                                , max_same.m_ch_l+max_same.m_nbytes );

    for( int k=0; k<SAME_ST; k++ )
    {
      if( pls.charAt( k ) == pll.charAt( k ) )
      {
        pli_s.set( k, Diff_Type.SAME );
        pli_l.set( k, Diff_Type.SAME );
      }
    }
    for( int k=SAME_FN; k<SLL; k++ )
    {
      if( pls.charAt( k ) == pll.charAt( k ) )
      {
        pli_s.set( k, Diff_Type.SAME );
        pli_l.set( k, Diff_Type.SAME );
      }
    }
  }

  void Set_crsRow( final int row )
  {
    Clear_Console_CrsCell();

    m_crsRow = row;

    Set_Console_CrsCell();
  }
  void Set_crsCol( final int col )
  {
    Clear_Console_CrsCell();

    m_crsCol = col;

    Set_Console_CrsCell();
  }
  void Set_crsRowCol( final int row, final int col )
  {
    Clear_Console_CrsCell();

    m_crsRow = row;
    m_crsCol = col;

    Set_Console_CrsCell();
  }
  void Clear_Console_CrsCell()
  {
    // Set console current cursor cell to non-cursor hightlighted value:
    View pV = m_vis.CV();

    Clear_Console_CrsCell( pV );
  }
  void Clear_Console_CrsCell( View V )
  {
    // Set console current cursor cell to non-cursor hightlighted value:
    final int CL = CrsLine(); // Diff line number

    // It is possible that the last diff line was just deleted,
    // in which case we will crash if we try to move off of it
    if( CL < NumLines( V ) )
    {
      final int VL = ViewLine( V, CL ); // View line number
      final int CC = CrsChar();

      final char  C = Get_Char_2 ( V, CL, VL, CC );
      final Style S = Get_Style_2( V, CL, VL, CC );

      m_console.Set( Row_Win_2_GL( V, m_crsRow )
                   , Col_Win_2_GL( V, m_crsCol )
                   , C, S );
    }
  }
  void Set_Console_CrsCell()
  {
    // Set console current cursor cell to cursor hightlighted value:
    View pV = m_vis.CV();

    Set_Console_CrsCell( pV );
  }
  void Set_Console_CrsCell( View V )
  {
    // Set console current cursor cell to cursor hightlighted value:
    final int CL = CrsLine();
    final int CC = CrsChar();

    final int VL = ViewLine( V, CL ); //(VL=view line)

    final char  C = Get_Char_2 ( V, CL, VL, CC );

    m_console.Set( Row_Win_2_GL( V, m_crsRow )
                 , Col_Win_2_GL( V, m_crsCol )
                 , C, Style.CURSOR );
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

        // m_crsRow and m_crsCol must be set to new values before calling CalcNewCrsByte
        m_crsRow = NCL - m_topLine;
        m_crsCol = NCP - m_leftChar;

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
        PrintCursor();  // Put cursor into position.
      }
    }
  }

//void GoToCrsPos_Write_Visual( final int OCL, final int OCP
//                            , final int NCL, final int NCP )
//{
//  View pV = m_vis.CV();
//  // Visual (visual start) < (new cursor pos)
//  //      (old cursor pos) < (new cursor pos)
//  final boolean VST_LT_NCP = v_st_line < NCL || (v_st_line == NCL && v_st_char < NCP);
//  final boolean NCP_LT_VST = NCL < v_st_line || (v_st_line == NCL && NCP < v_st_char);
//
//  final boolean OCP_LT_NCP = OCL < NCL || (OCL == NCL && OCP < NCP);
//  final boolean NCP_LT_OCP = NCL < OCL || (OCL == NCL && NCP < OCP);
//  final boolean OCP_LT_VST = OCL < v_st_line || (OCL == v_st_line && OCP < v_st_char);
//  final boolean VST_LT_OCP = v_st_line < OCL || (OCL == v_st_line && v_st_char < OCP);
//
//  if( OCP_LT_NCP ) // Cursor moved forward
//  {
//    if( NCP_LT_VST )
//    {
//      GoToCrsPos_WV_Forward( OCL, OCP, NCL, NCP );
//    }
//    else // VST <= NCP
//    {
//      if( OCP_LT_VST )
//      {
//        GoToCrsPos_WV_Forward( OCL, OCP, v_st_line, v_st_char );
//        GoToCrsPos_WV_Forward( v_st_line, v_st_char, NCL, NCP );
//      }
//      else {
//        GoToCrsPos_WV_Forward( OCL, OCP, NCL, NCP );
//      }
//    }
//  }
//  else // NCP_LT_OCP // Cursor moved backward
//  {
//    if( VST_LT_NCP )
//    {
//      GoToCrsPos_WV_Backward( OCL, OCP, NCL, NCP );
//    }
//    else // NCP <= VST
//    {
//      if( VST_LT_OCP )
//      {
//        GoToCrsPos_WV_Backward( OCL, OCP, v_st_line, v_st_char );
//        GoToCrsPos_WV_Backward( v_st_line, v_st_char, NCL, NCP );
//      }
//      else {
//        GoToCrsPos_WV_Backward( OCL, OCP, NCL, NCP );
//      }
//    }
//  }
//  Set_crsRowCol( NCL - m_topLine, NCP - m_leftChar );
//  PrintCursor(); // Does m_console.Update()
//}
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
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;
    // Convert OCL and NCL, which are diff lines, to view lines:
    final int OCLv = ViewLine( pV, OCL );
    final int NCLv = ViewLine( pV, NCL );

    if( OCL == NCL ) // Only one line:
    {
      for( int k=OCP; k<NCP; k++ )
      {
        char C = pfb.Get( OCLv, k );
        m_console.Set( Line_2_GL( pV, OCL ), Char_2_GL( pV, k ), C
                     , Get_Style(pV,OCL,OCLv,k) );
      }
    }
    else { // Multiple lines
      // Write out first line:
      final Diff_Type FIRST_LINE_DIFF_TYPE = DiffType( pV, OCL );
      if( FIRST_LINE_DIFF_TYPE != Diff_Type.DELETED )
      {
        final int OCLL = pfb.LineLen( OCLv ); // Old cursor line length
        final int END_FIRST_LINE = Math.min( RightChar( pV )+1, OCLL );
        for( int k=OCP; k<END_FIRST_LINE; k++ )
        {
          char C = pfb.Get( OCLv, k );
          m_console.Set( Line_2_GL( pV, OCL ), Char_2_GL( pV, k ), C
                       , Get_Style(pV,OCL,OCLv,k) );
        }
      }
      // Write out intermediate lines:
      for( int l=OCL+1; l<NCL; l++ )
      {
        final Diff_Type LINE_DIFF_TYPE = DiffType( pV, l );
        if( LINE_DIFF_TYPE != Diff_Type.DELETED )
        {
          // Convert OCL, which is diff line, to view line
          final int Vl = ViewLine( pV, l );
          final int LL = pfb.LineLen( Vl ); // Line length
          final int END_OF_LINE = Math.min( RightChar( pV )+1, LL );
          for( int k=m_leftChar; k<END_OF_LINE; k++ )
          {
            char C = pfb.Get( Vl, k );
            m_console.Set( Line_2_GL( pV, l ), Char_2_GL( pV, k ), C
                         , Get_Style(pV,l,Vl,k) );
          }
        }
      }
      // Write out last line:
      final Diff_Type LAST_LINE_DIFF_TYPE = DiffType( pV, NCL );
      if( LAST_LINE_DIFF_TYPE != Diff_Type.DELETED )
      {
        // Print from beginning of next line to new cursor position:
        final int NCLL = pfb.LineLen( NCLv ); // Line length
        final int END_LAST_LINE = Math.min( NCLL, NCP );
        for( int k=m_leftChar; k<END_LAST_LINE; k++ )
        {
          char C = pfb.Get( NCLv, k );
          m_console.Set( Line_2_GL( pV, NCL ), Char_2_GL( pV, k ), C
                       , Get_Style(pV,NCL,NCLv,k)  );
        }
      }
    }
  }

  // Cursor is moving backwards
  // Write out from (OCL,OCP) back to but not including (NCL,NCP)
  void GoToCrsPos_WV_Backward( final int OCL, final int OCP
                             , final int NCL, final int NCP )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;
    // Convert OCL and NCL, which are diff lines, to view lines:
    final int OCLv = ViewLine( pV, OCL );
    final int NCLv = ViewLine( pV, NCL );
  
    if( OCL == NCL ) // Only one line:
    {
      for( int k=OCP; NCP<k; k-- )
      {
        char C = pfb.Get( OCLv, k );
        m_console.Set( Line_2_GL( pV, OCL ), Char_2_GL( pV, k ), C
                     , Get_Style(pV,OCL,OCLv,k) );
      }
    }
    else { // Multiple lines
      // Write out first line:
      final Diff_Type FIRST_LINE_DIFF_TYPE = DiffType( pV, OCL );
      if( FIRST_LINE_DIFF_TYPE != Diff_Type.DELETED )
      {
        final int OCLL = pfb.LineLen( OCLv ); // Old cursor line length
        final int RIGHT_MOST_POS = Math.min( OCP, 0<OCLL ? OCLL-1 : 0 );
        for( int k=RIGHT_MOST_POS; m_leftChar<k; k-- )
        {
          char C = pfb.Get( OCLv, k );
          m_console.Set( Line_2_GL( pV, OCL ), Char_2_GL( pV, k ), C
                       , Get_Style(pV,OCL,OCLv,k) );
        }
        if( m_leftChar < OCLL ) {
          char C = pfb.Get( OCLv, m_leftChar );
          m_console.Set( Line_2_GL( pV, OCL ), Char_2_GL( pV, m_leftChar ), C
                       , Get_Style(pV,OCL,OCLv,m_leftChar) );
        }
      }
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
            char C = pfb.Get( Vl, k );
            m_console.Set( Line_2_GL( pV, l ), Char_2_GL( pV, k ), C
                         , Get_Style(pV,l,Vl,k) );
          }
        }
      }
      // Write out last line:
      final Diff_Type LAST_LINE_DIFF_TYPE = DiffType( pV, NCL );
      if( LAST_LINE_DIFF_TYPE != Diff_Type.DELETED )
      {
        // Print from end of last line to new cursor position:
        final int NCLL = pfb.LineLen( NCLv ); // New cursor line length
        final int END_LAST_LINE = Math.min( RightChar( pV ), 0<NCLL ? NCLL-1 : 0 );
        for( int k=END_LAST_LINE; NCP<k; k-- )
        {
          char C = pfb.Get( NCLv, k );
          m_console.Set( Line_2_GL( pV, NCL ), Char_2_GL( pV, k ), C
                       , Get_Style(pV,NCL,NCLv,k) );
        }
      //if( NCP < NCLL ) {
      //  char C = pfb.Get( NCLv, NCP );
      //  m_console.Set( Line_2_GL( pV, NCL ), Char_2_GL( pV, NCP ), C
      //               , Get_Style(pV,NCL,NCLv,NCP) );
      //}
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
  
    Clear_Console_CrsCell();

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
    final int NCL       = OCL+num;   // New cursor line

    if( 0<NUM_LINES && NCL < NUM_LINES )
    {
      final int OCP = CrsChar(); // Old cursor position
            int NCP = OCP;

      GoToCrsPos_Write( NCL, NCP );
    }
  }

  void GoUp( final int num )
  {
    final int NUM_LINES = NumLines();
    final int OCL       = CrsLine(); // Old cursor line
    final int NCL       = OCL-num;   // New cursor line

    if( 0<NUM_LINES && 0<=NCL )
    {
      final int OCP = CrsChar(); // Old cursor position
            int NCP = OCP;

      GoToCrsPos_Write( NCL, NCP );
    }
  }

  void GoLeft()
  {
    final int CP = CrsChar(); // Cursor position

    if( 0<NumLines() && 0<CP )
    {
      final int CL = CrsLine(); // Cursor line

      GoToCrsPos_Write( CL, CP-1 );
    }
  }
  void GoRight()
  {
    final int LL = LineLen();
    final int CP = CrsChar(); // Cursor position

    if( 0<NumLines() && 0<LL && CP<LL-1 )
    {
      final int CL = CrsLine(); // Cursor line

      GoToCrsPos_Write( CL, CP+1 );
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
    GoToCrsPos_Write( m_topLine, 0 );
  }

  void GoToBotLineInView()
  {
    View pV = m_vis.CV();

    final int NUM_LINES = NumLines();

    int bottom_line_in_view = m_topLine + WorkingRows( pV )-1;

    bottom_line_in_view = Math.min( NUM_LINES-1, bottom_line_in_view );

    GoToCrsPos_Write( bottom_line_in_view, 0 );
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
  
    pV.MoveInBounds();
  
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
 
    pV.MoveInBounds();
 
    final char  start_char = '}';
    final char finish_char = '{';
    GoToOppositeBracket_Backward( start_char, finish_char );
  }
  void GoToRightSquigglyBracket()
  {
    View pV = m_vis.CV();
 
    pV.MoveInBounds();
 
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
    final int OCP = CrsChar();                 // Old cursor position

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

  void Do_n()
  {
    if( 0 < m_vis.m_star.length() ) Do_n_Pattern();
    else                            Do_n_Diff();
  }

  void Do_n_Pattern()
  {
    View pV = m_vis.CV();

    final int NUM_LINES = pV.m_fb.NumLines();

    if( NUM_LINES <= 0 ) return;

    CrsPos ncp = new CrsPos( 0, 0 ); // Next cursor position

    if( Do_n_FindNextPattern( ncp ) )
    {
      GoToCrsPos_Write( ncp.crsLine, ncp.crsChar );
    }
  }

  boolean Do_n_FindNextPattern( CrsPos ncp )
  {
    View pV = m_vis.CV();
    FileBuf pfb = pV.m_fb;
  
    final int NUM_LINES = pfb.NumLines();
    final int STAR_LEN  = m_vis.m_star.length();
  
    final int OCL = CrsLine(); // Diff line
    final int OCC = CrsChar();

    final int OCLv = ViewLine( pV, OCL ); // View line

    int st_l = OCLv;
    int st_c = OCC;

    boolean found_next_star = false;

    // Move past current star:
    final int LL = pfb.LineLen( OCLv );
  
    for( ; st_c<LL && pV.InStar(OCLv,st_c); st_c++ ) ;
  
    // Go down to next line
    if( LL <= st_c ) { st_c=0; st_l++; }
  
    // Search for first star position past current position
    for( int l=st_l; !found_next_star && l<NUM_LINES; l++ )
    {
      final int LL2 = pfb.LineLen( l );
  
      for( int p=st_c
         ; !found_next_star && p<LL2
         ; p++ )
      {
        if( pV.InStar(l,p) )
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
        final int LL3 = pfb.LineLen( l );
        final int END_C = (OCLv==l) ? Math.min( OCC, LL3 ) : LL3;
  
        for( int p=0; !found_next_star && p<END_C; p++ )
        {
          if( pV.InStar(l,p) )
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

  void Do_n_Diff()
  {
    final int NUM_LINES = NumLines();
    if( 0==NUM_LINES ) return;

    Ptr_Int dl = new Ptr_Int( CrsLine() );

    View pV = m_vis.CV();

    ArrayList<Diff_Info> DI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L;

    final Diff_Type DT = DI_List.get( dl.val ).diff_type; // Current diff type

    boolean found = true;

    if( DT == Diff_Type.CHANGED
     || DT == Diff_Type.INSERTED
     || DT == Diff_Type.DELETED )
    {
      found = Do_n_Search_for_Same( dl, DI_List );
    }
    if( found )
    {
      found = Do_n_Search_for_Diff( dl, DI_List );

      if( found )
      {
        final int NCL = dl.val;
        final int NCP = CrsChar();

        GoToCrsPos_Write( NCL, NCP );
      }
    }
  }

  boolean Do_n_Search_for_Same( Ptr_Int dl
                              , final ArrayList<Diff_Info> DI_List )
  {
    final int NUM_LINES = NumLines();

    // Search forward for Diff_Type.SAME
    boolean found = false;

    while( !found && dl.val<NUM_LINES )
    {
      final Diff_Type DT = DI_List.get( dl.val ).diff_type;

      if( DT == Diff_Type.SAME )
      {
        found = true;
      }
      else dl.val++;
    }
    return found;
  }

  boolean Do_n_Search_for_Diff( Ptr_Int dl
                              , final ArrayList<Diff_Info> DI_List )
  {
    final int NUM_LINES = NumLines();
  
    // Search forward for non-Diff_Type.SAME
    boolean found = false;
  
    while( !found && dl.val<NUM_LINES )
    {
      final Diff_Type DT = DI_List.get( dl.val ).diff_type;
  
      if( DT == Diff_Type.CHANGED
       || DT == Diff_Type.INSERTED
       || DT == Diff_Type.DELETED )
      {
        found = true;
      }
      else dl.val++;
    }
    return found;
  }

  void Do_N()
  {
    if( 0 < m_vis.m_star.length() ) Do_N_Pattern();
    else                            Do_N_Diff();
  }

  void Do_N_Pattern()
  {
    View pV = m_vis.CV();

    final int NUM_LINES = pV.m_fb.NumLines();

    if( NUM_LINES == 0 ) return;

    CrsPos ncp = new CrsPos( 0, 0 ); // Next cursor position

    if( Do_N_FindPrevPattern( ncp ) )
    {
      GoToCrsPos_Write( ncp.crsLine, ncp.crsChar );
    }
  }

  boolean Do_N_FindPrevPattern( CrsPos ncp )
  {
    MoveInBounds();

    View pV = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int NUM_LINES = pfb.NumLines();
    final int STAR_LEN  = m_vis.m_star.length();

    final int OCL = CrsLine();
    final int OCC = CrsChar();
  
    final int OCLv = ViewLine( pV, OCL ); // View line
  
    boolean found_prev_star = false;
  
    // Search for first star position before current position
    for( int l=OCLv; !found_prev_star && 0<=l; l-- )
    {
      final int LL = pfb.LineLen( l );
  
      int p=LL-1;
      if( OCLv==l ) p = 0<OCC ? OCC-1 : 0;
  
      for( ; 0<p && !found_prev_star; p-- )
      {
        for( ; 0<=p && pV.InStar(l,p); p-- )
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
        final int LL = pfb.LineLen( l );
  
        int p=LL-1;
        if( OCLv==l ) p = 0<OCC ? OCC-1 : 0;
  
        for( ; 0<p && !found_prev_star; p-- )
        {
          for( ; 0<=p && pV.InStar(l,p); p-- )
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
    final int NUM_LINES = NumLines();
    if( 0==NUM_LINES
     || CrsLine() <= 0 ) return;

    Ptr_Int dl = new Ptr_Int( CrsLine() );

    View pV = m_vis.CV();

    ArrayList<Diff_Info> DI_List = (pV == m_vS) ? m_DI_List_S : m_DI_List_L;

    final Diff_Type DT = DI_List.get( dl.val ).diff_type; // Current diff type

    boolean found = true;
    if( DT == Diff_Type.CHANGED
     || DT == Diff_Type.INSERTED
     || DT == Diff_Type.DELETED )
    {
      found = Do_N_Search_for_Same( dl, DI_List );
    }
    if( found )
    {
      found = Do_N_Search_for_Diff( dl, DI_List );
  
      if( found )
      {
        final int NCL = dl.val;
        final int NCP = CrsChar();
  
        GoToCrsPos_Write( NCL, NCP );
      }
    }
  }

  boolean Do_N_Search_for_Same( Ptr_Int dl
                              , final ArrayList<Diff_Info> DI_List )
  {
    // Search backwards for Diff_Type.SAME
    boolean found = false;
  
    while( !found && 0<=dl.val )
    {
      if( Diff_Type.SAME == DI_List.get( dl.val ).diff_type )
      {
        found = true;
      }
      else dl.val--;
    }
    return found;
  }

  boolean Do_N_Search_for_Diff( Ptr_Int dl
                              , final ArrayList<Diff_Info> DI_List )
  {
    // Search backwards for non-Diff_Type.SAME
    boolean found = false;

    while( !found && 0<=dl.val )
    {
      final Diff_Type DT = DI_List.get( dl.val ).diff_type;

      if( DT == Diff_Type.CHANGED
       || DT == Diff_Type.INSERTED
       || DT == Diff_Type.DELETED )
      {
        found = true;
      }
      else dl.val--;
    }
    return found;
  }

  void Do_f()
  {
    m_vis.m_states.addFirst( m_run_f );
  }
  void run_f()
  {
    if( 0<m_console.KeysIn() )
    {
      m_vis.m_fast_char = m_console.GetKey();

      Do_semicolon( m_vis.m_fast_char );

      m_vis.m_states.removeFirst();
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
  boolean MoveInBounds()
  {
    View pV  = m_vis.CV();

    final int DL  = CrsLine();  // Diff line
    final int VL  = ViewLine( pV, DL );      // View line
    final int LL  = pV.m_fb.LineLen( VL );
    final int EOL = 0<LL ? LL-1 : 0;

    if( EOL < CrsChar() ) // Since cursor is now allowed past EOL,
    {                      // it may need to be moved back:
      GoToCrsPos_NoWrite( DL, EOL );
      return true;
    }
    return false;
  }

  void Do_z()
  {
    m_vis.m_states.addFirst( m_run_z );
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
      m_vis.m_states.removeFirst();
    }
  }
  void MoveCurrLineToTop()
  {
    if( 0<m_crsRow )
    {
      // Make changes manually:
      Clear_Console_CrsCell();
      m_topLine += m_crsRow;
      m_crsRow = 0;
      Set_Console_CrsCell();

      Update();
    }
  }

  void MoveCurrLineCenter()
  {
    View pV = m_vis.CV();

    final int center = (int)( 0.5*WorkingRows(pV) + 0.5 );

    final int OCL = CrsLine(); // Old cursor line

    if( 0 < OCL && OCL < center && 0 < m_topLine )
    {
      // Cursor line cannot be moved to center, but can be moved closer to center
      // CrsLine() does not change:
      Clear_Console_CrsCell();
      m_crsRow += m_topLine;
      m_topLine = 0;
      Set_Console_CrsCell();

      Update();
    }
    else if( center < OCL
          && center != m_crsRow )
    {
      Clear_Console_CrsCell();
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
      View pV = m_vis.CV();
  
      final int WR  = WorkingRows( pV );
      final int OCL = CrsLine(); // Old cursor line

      if( WR-1 <= OCL )
      {
        Clear_Console_CrsCell();
        m_topLine -= WR - m_crsRow - 1;
        m_crsRow = WR-1;
        Set_Console_CrsCell();

        Update();
      }
      else {
        // Cursor line cannot be moved to bottom, but can be moved closer to bottom
        // CrsLine() does not change:
        Clear_Console_CrsCell();
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

    final int CL = CrsLine();
    // Convert CL, which is diff line, to view line:
    final int CLv = ViewLine( pV, CrsLine() );
    final int LL  = pfb.LineLen( CLv );

    StringBuilder new_star = null;

    if( 0<LL )
    {
      new_star = new StringBuilder();

      MoveInBounds();
      final int  CC = CrsChar();
      final char c  = pfb.Get( CLv,  CC );

      if( Utils.IsIdent( c ) )
      {
        new_star.append( c );

        // Search forward:
        for( int k=CC+1; k<LL; k++ )
        {
          final char c1 = pfb.Get( CLv, k );
          if( Utils.IsIdent( c1 ) ) new_star.append( c1 );
          else                      break;
        }
        // Search backward:
        for( int k=CC-1; 0<=k; k-- )
        {
          final char c2 = pfb.Get( CLv, k );
          if( Utils.IsIdent( c2 ) ) new_star.insert( 0, c2 );
          else                      break;
        }
      }
      else {
        if( !Utils.IsSpace( c ) ) new_star.append( c );
      }
    }
    return null != new_star ? new_star.toString() : "";
  }

  void Do_i()
  {
    m_vis.m_states.addFirst( m_run_i_end );
    m_vis.m_states.addFirst( m_run_i_mid );
    m_vis.m_states.addFirst( m_run_i_beg );
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

    m_vis.m_states.removeFirst();
  }
  void run_i_mid()
  {
    if( 0<m_console.KeysIn() )
    {
      final char c = m_console.GetKey();

      if( c == ESC )
      {
        m_vis.m_states.removeFirst(); // Done
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
    m_vis.m_states.removeFirst();
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

    m_crsCol -= 1;

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
        m_vis.m_reg.clear();
        m_vis.m_reg.add( nlr );
        m_vis.m_paste_mode = Paste_Mode.ST_FN;

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
    else if( EOL < CP )
    {
      // Extend line out to where cursor is:
      for( int k=LL; k<CP; k++ )
      {
        pfb.PushChar( VL, ' ' );
      }
      Patch_Diff_Info_Changed( pV, DL );
      Do_a();
    }
    else // CP == EOL
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
      m_vis.m_reg.clear();
      m_vis.m_reg.add( lrd );
      m_vis.m_paste_mode = Paste_Mode.ST_FN;

      // If cursor is not at beginning of line, move it back one space.
      if( 0<m_crsCol ) m_crsCol--;

      Patch_Diff_Info_Changed( pV, DL );
      Update();
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
      if( DT != Diff_Type.UNKN0WN && DT != Diff_Type.DELETED )
      {
        final int VL = ViewLine( pV, DL ); // View line

        // Remove line from FileBuf and save in paste register:
        Line lp = pV.m_fb.RemoveLine( VL );

        // m_vis.m_reg will own lp
        m_vis.m_reg.clear();
        m_vis.m_reg.add( lp );
        m_vis.m_paste_mode = Paste_Mode.LINE;

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

        Update();
      }
    }
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

        Update();
      }
    }
  }
  void Do_dw()
  {
  }
  void Do_cw()
  {
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
      if( DT != Diff_Type.UNKN0WN && DT != Diff_Type.DELETED )
      {
        final int VL = ViewLine( pV, DL ); // View Cursor line

        // Get a copy of CrsLine() line:
        Line l = new Line( pV.m_fb.GetLine( VL ) );

        m_vis.m_reg.clear();
        m_vis.m_reg.add( l );

        m_vis.m_paste_mode = Paste_Mode.LINE;
      }
    }
  }
  void Do_y_v()
  {
    m_vis.m_reg.clear();

    if( m_inVisualBlock ) Do_y_v_block();
    else                  Do_y_v_st_fn();

    m_inVisualMode = false;
  }
  void Do_y_v_block()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    Swap_Visual_Block_If_Needed();

    for( int DL=v_st_line; DL<=v_fn_line; DL++ )
    {
      Line nlr = new Line();

      final int VL = ViewLine( pV, DL );
      final int LL = pfb.LineLen( VL );

      for( int P = v_st_char; P<LL && P <= v_fn_char; P++ )
      {
        nlr.append_c( pfb.Get( VL, P ) );
      }
      m_vis.m_reg.add( nlr );
    }
    m_vis.m_paste_mode = Paste_Mode.BLOCK;

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
  void Do_y_v_st_fn()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    Swap_Visual_Block_If_Needed();

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
        m_vis.m_reg.add( nlp );
      }
    }
    m_vis.m_paste_mode = Paste_Mode.ST_FN;
  }
  void Do_Y_v()
  {
    m_vis.m_reg.clear();

    if( m_inVisualBlock ) Do_y_v_block();
    else                  Do_Y_v_st_fn();

    m_inVisualMode = false;
  }
  void Do_Y_v_st_fn()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    Swap_Visual_Block_If_Needed();

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
        m_vis.m_reg.add( nlp );
      }
    }
    m_vis.m_paste_mode = Paste_Mode.LINE;
  }
  void Do_D_v()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    m_vis.m_reg.clear();
    Swap_Visual_Block_If_Needed();

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
        m_vis.m_reg.add( lp ); // m_vis.m_reg will delete lp

        Patch_Diff_Info_Deleted( pV, DL );

        removed_line = true;
        // If line on other side is Diff_Type.DELETED, a diff line will be removed
        // from both sides, so decrement DL to stay on same DL, decrement
        // v_fn_line because it just moved up a line
        if( oDT == Diff_Type.DELETED ) { DL--; v_fn_line--; }
      }
    }
    m_vis.m_paste_mode = Paste_Mode.LINE;

    // Deleted lines will be removed, so no need to Undo_v()
    m_inVisualMode = false;

    if( removed_line )
    {
      Do_D_v_find_new_crs_pos();
      Update();
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

  void Do_s_v()
  {
    // Need to know if cursor is at end of line before Do_x_v() is called:
    final int LL = LineLen();
    final boolean CURSOR_AT_END_OF_LINE = 0<LL ? CrsChar() == LL-1 : false;

    Do_x_v();

    if( m_inVisualBlock )
    {
    //if( CURSOR_AT_END_OF_LINE ) Do_a_vb();
    //else                        Do_i_vb(); 
    }
    else {
      if( CURSOR_AT_END_OF_LINE ) Do_a();
      else                        Do_i();
    }
    m_inVisualMode = false;
  }

  void Do_Tilda_v()
  {
    Swap_Visual_Block_If_Needed();

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
      Do_x_range();
    }
    m_inVisualMode = false;

    Update(); //<- No need to Undo_v() or Remove_Banner() because of this
  }
  void Do_x_range()
  {
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
    Swap_Visual_Block_If_Needed();

    m_vis.m_reg.clear();
  }
  void Do_x_range_post( final int st_line, final int st_char )
  {
    if( m_inVisualBlock ) m_vis.m_paste_mode = Paste_Mode.BLOCK;
    else                  m_vis.m_paste_mode = Paste_Mode.ST_FN;
 
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
  }
  void Do_x_range_single( final int L
                        , final int st_char
                        , final int fn_char )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int DL = L;
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

    m_vis.m_reg.add( nlp );
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

      if( cDT != Diff_Type.SAME       // If cDT is UNKN0WN or DELETED,
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
      m_vis.m_reg.add( nlp );
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
      m_vis.m_reg.add( nlr );
    }
    Do_x_range_post( v_st_line, v_st_char );
  }

  void Do_p()
  {
    if     ( Paste_Mode.ST_FN == m_vis.m_paste_mode ) Do_p_or_P_st_fn( Paste_Pos.After );
    else if( Paste_Mode.BLOCK == m_vis.m_paste_mode ) Do_p_block();
    else /*( Paste_Mode.LINE  == m_vis.m_paste_mode*/ Do_p_line();
  }
  // New:
  void Do_p_line()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int DL = CrsLine();          // Diff line
    final int VL = ViewLine( pV, DL ); // View line

    final int NUM_LINES_TO_INSERT = m_vis.m_reg.size();

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
      Line nl = new Line( m_vis.m_reg.get(k) );
      pfb.InsertLine( VL_START+k, nl );

      Patch_Diff_Info_Inserted( pV, DL_START+k, ODVL0 );
      ODVL0 = false;
    }
    Update();
  }
  void Do_p_or_P_st_fn( Paste_Pos paste_pos )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int NUM_LINES = m_vis.m_reg.size();
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
    Update();
  }
  void Do_p_or_P_st_fn_FirstLine( Paste_Pos paste_pos
                                , final int k
                                , final int ODL
                                , final int OVL
                                , final boolean ON_DELETED )
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    final int NUM_LINES = m_vis.m_reg.size();

    final int NLL = m_vis.m_reg.get( k ).length();  // New line length
    final int VL  = ViewLine( pV, ODL+k ); // View line

    if( ON_DELETED )
    {
      final boolean ODVL0 = On_Deleted_View_Line_Zero( ODL );

      // In FileBuf: Put reg on line below:
      pfb.InsertLine( ODVL0 ? VL : VL+1, new Line( m_vis.m_reg.get(0) ) );

      Patch_Diff_Info_Inserted( pV, ODL+k, ODVL0 );
    }
    else {
      MoveInBounds();
      final int LL = pfb.LineLen( VL );
      final int CP = CrsChar();         // Cursor position

      // If line we are pasting to is zero length, dont paste a space forward
      final int forward = 0<LL ? ( paste_pos==Paste_Pos.After ? 1 : 0 ) : 0;

      for( int i=0; i<NLL; i++ )
      {
        char C = m_vis.m_reg.get(k).charAt(i);

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
    final int NLL = m_vis.m_reg.get( k ).length();  // New line length

    if( ON_DELETED )
    {
      pfb.InsertLine( VL+1, new Line( m_vis.m_reg.get(k) ) );
      Patch_Diff_Info_Inserted( pV, ODL+k, false );
    }
    else {
      for( int i=0; i<NLL; i++ )
      {
        char C = m_vis.m_reg.get(k).charAt(i);
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

    final int NUM_LINES = m_vis.m_reg.size();

    final int NLL = m_vis.m_reg.get( k ).length();  // New line length
    final int VL  = ViewLine( pV, ODL+k ); // View line

    if( ON_DELETED )
    {
      // In FileBuf: Put reg on line below:
      pfb.InsertLine( VL+1, new Line( m_vis.m_reg.get(k) ) );

      Patch_Diff_Info_Inserted( pV, ODL+k, false );
    }
    else {
      MoveInBounds();
      final int LL = pfb.LineLen( VL );

      for( int i=0; i<NLL; i++ )
      {
        char C = m_vis.m_reg.get(k).charAt(i);

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
    final int N_REG_LINES = m_vis.m_reg.size();

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
    Update();
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
    Line reg_line = m_vis.m_reg.get(k);
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
    Line reg_line = m_vis.m_reg.get(k);
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
    if     ( Paste_Mode.ST_FN == m_vis.m_paste_mode ) Do_p_or_P_st_fn( Paste_Pos.After );
    else if( Paste_Mode.BLOCK == m_vis.m_paste_mode ) Do_P_block();
    else /*( Paste_Mode.LINE  == m_vis.m_paste_mode*/ Do_P_line();
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

    final int N_REG_LINES = m_vis.m_reg.size();

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
    Update();
  }

  void Do_v()
  {
    m_inVisualBlock = false;
    m_copy_vis_buf_2_dot_buf = false;

    m_vis.m_states.addFirst( m_run_v_end );
    m_vis.m_states.addFirst( m_run_v_mid );
    m_vis.m_states.addFirst( m_run_v_beg );
  }
  void Do_V()
  {
    m_inVisualBlock = true;
    m_copy_vis_buf_2_dot_buf = false;

    m_vis.m_states.addFirst( m_run_v_end );
    m_vis.m_states.addFirst( m_run_v_mid );
    m_vis.m_states.addFirst( m_run_v_beg );
  }
  void run_v_beg()
  {
    MoveInBounds();
    m_inVisualMode = true;
    m_undo_v       = true;
    DisplayBanner();

    v_st_line = CrsLine();  v_fn_line = v_st_line;
    v_st_char = CrsChar();  v_fn_char = v_st_char;

    // Write current byte in visual:
    Replace_Crs_Char( Style.VISUAL );

    m_vis.m_states.removeFirst();
  }
  void run_v_mid()
  {
    // m_console.KeysIn() needs to be first because it Sleep()'s
    if( 0<m_console.KeysIn() )
    {
      final char C = m_console.GetKey();

      if     ( C == 'l' ) GoRight();
      else if( C == 'h' ) GoLeft();
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
    if( !m_inVisualMode ) m_vis.m_states.removeFirst();
  }
  void run_v_end()
  {
    Undo_v();
    Remove_Banner();

    m_vis.m_states.removeFirst();

    if( !m_console.m_get_from_dot_buf )
    {
      m_console.m_save_2_vis_buf = false;

      if( m_copy_vis_buf_2_dot_buf )
      {
        // setLength( 0 ) followed by append() accomplishes copy:
        m_console.m_dot_buf.setLength( 0 );
        m_console.m_dot_buf.append( m_console.m_vis_buf );
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
    m_vis.m_states.addFirst( m_run_g_v );
  }
  void run_g_v()
  {
    if( 0<m_console.KeysIn() )
    {
      final char c2 = m_console.GetKey();

      if     ( c2 == 'g' ) GoToTopOfFile();
      else if( c2 == '0' ) GoToStartOfRow();
      else if( c2 == '$' ) GoToEndOfRow();
    //else if( c2 == 'f' ) Do_v_Handle_gf();
      else if( c2 == 'p' ) Do_v_Handle_gp();

      m_vis.m_states.removeFirst();
    }
  }
//void Do_v_Handle_gf()
//{
//  if( v_st_line == v_fn_line )
//  {
//    View    pV  = m_vis.CV();
//    FileBuf pfb = pV.m_fb;
//
//    final int m_v_st_char = v_st_char < v_fn_char ? v_st_char : v_fn_char;
//    final int m_v_fn_char = v_st_char < v_fn_char ? v_fn_char : v_st_char;
//
//    m_sb.setLength( 0 );
//
//    for( int P = m_v_st_char; P<=m_v_fn_char; P++ )
//    {
//      m_sb.append( pfb.Get( v_st_line, P ) );
//    }
//    boolean went_to_file = m_vis.GoToBuffer_Fname( m_sb.toString() );
//
//    if( went_to_file )
//    {
//      // If we made it to buffer indicated by fname, no need to Undo_v() or
//      // Remove_Banner() because the whole view pane will be redrawn
//      m_inVisualMode = false;
//      m_undo_v       = false;
//    }
//  }
//}
  void Do_v_Handle_gp()
  {
    if( v_st_line == v_fn_line )
    {
      View    pV  = m_vis.CV();
      FileBuf pfb = pV.m_fb;

      Swap_Visual_Block_If_Needed();

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
    m_vis.m_states.addFirst( m_run_R_end );
    m_vis.m_states.addFirst( m_run_R_mid );
    m_vis.m_states.addFirst( m_run_R_beg );
  }
  void run_R_beg()
  {
    View    pV  = m_vis.CV();
    FileBuf pfb = pV.m_fb;

    pV.m_inReplaceMode = true;

    DisplayBanner();

    if( 0 == pfb.NumLines() ) pfb.PushLine();

    m_i_count = 0; // Re-use m_i_count form m_R_count

    m_vis.m_states.removeFirst();
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
        m_vis.m_states.removeFirst(); // Done
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

    m_vis.m_states.removeFirst();
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
      if( ls.chksum() == ll.chksum() ) // Lines are now equal
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

        if( ls.chksum() == ll.chksum() ) // Lines are equal
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
  void Patch_Diff_Info_Inserted_Inc( final int DPL
                                   , final boolean ON_DELETED_VIEW_LINE_ZERO
                                   , ArrayList<Diff_Info> cDI_List )
  {
    // If started inserting into empty first line in file, dont increment
    // Diff_Info line_num, because DELETED first line starts at zero:
    int inc_st = DPL;
    if( ON_DELETED_VIEW_LINE_ZERO ) {
      // If there are Diff_Type.DELETED lines directly below where
      // we inserted a line, decrement their Diff_Info.line_num's
      // because they were incremented in Patch_Diff_Info_Inserted()
      // and they should not be incremented here:
      for( int k=DPL+1; k<cDI_List.size(); k++ )
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
//void Swap_Visual_St_Fn_If_Needed()
//{
//  if( v_fn_line < v_st_line
//   || (v_fn_line == v_st_line && v_fn_char < v_st_char) )
//  {
//    // Visual mode went backwards over multiple lines, or
//    // Visual mode went backwards over one line
//    int T = v_st_line; v_st_line = v_fn_line; v_fn_line = T;
//        T = v_st_char; v_st_char = v_fn_char; v_fn_char = T;
//  }
//}
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

  static final char BS  =   8; // Backspace
  static final char ESC =  27; // Escape
  static final char DEL = 127; // Delete

  Vis     m_vis;
  Console m_console;
  StringBuilder m_sb = new StringBuilder();

  int m_topLine;   // top  of buffer view line number.
  int m_leftChar;  // left of buffer view character number.
  int m_crsRow;    // cursor row    in buffer view. 0 <= m_crsRow < WorkingRows().
  int m_crsCol;    // cursor column in buffer view. 0 <= m_crsCol < WorkingCols().

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

  ArrayList<SimLines> m_simiList        = new ArrayList<>();
  ArrayList<LineInfo> m_line_info_cache = new ArrayList<>();

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

  Thread m_run_i_beg = new Thread() { public void run() { run_i_beg(); } };
  Thread m_run_i_mid = new Thread() { public void run() { run_i_mid(); } };
  Thread m_run_i_end = new Thread() { public void run() { run_i_end(); } };
  Thread m_run_R_beg = new Thread() { public void run() { run_R_beg(); } };
  Thread m_run_R_mid = new Thread() { public void run() { run_R_mid(); } };
  Thread m_run_R_end = new Thread() { public void run() { run_R_end(); } };
  Thread m_run_v_beg = new Thread() { public void run() { run_v_beg(); } };
  Thread m_run_v_mid = new Thread() { public void run() { run_v_mid(); } };
  Thread m_run_v_end = new Thread() { public void run() { run_v_end(); } };
  Thread m_run_g_v   = new Thread() { public void run() { run_g_v  (); } };
  Thread m_run_z     = new Thread() { public void run() { run_z    (); } };
  Thread m_run_f     = new Thread() { public void run() { run_f    (); } };
}

// Run threads using lambdas.
// The syntax is more concise, but according to my research,
// a new is done every time the lambda is called because the lambda
// captures a method outside the lambda, so dont use for now.
//Thread m_run_i_beg = new Thread( ()->run_i_beg() );
//Thread m_run_i_mid = new Thread( ()->run_i_mid() );
//Thread m_run_i_end = new Thread( ()->run_i_end() );
//Thread m_run_z     = new Thread( ()->run_z    () );
//Thread m_run_f     = new Thread( ()->run_f    () );

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
  Diff_Type diff_type;
  int       line_num;  // Line number in file to which this Diff_Info applies (view line)
  LineInfo  pLineInfo;
}

