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

import java.util.ArrayList;

class ChangeHist
{
  ChangeHist( FileBuf fb )
  {
    m_fb = fb;
  }
  void Clear()
  {
    m_changes.clear();
  }

  boolean Has_Changes()
  {
    return 0<m_changes.size();
  }

  void Undo( final View rV )
  {
    if( 0 < m_changes.size() )
    {
      LineChange rlc = m_changes.remove( m_changes.size()-1 );

      final ChangeType ct = rlc.m_type;
      if     ( ct == ChangeType.Insert_Line  ) Undo_InsertLine( rlc, rV );
      else if( ct == ChangeType.Remove_Line  ) Undo_RemoveLine( rlc, rV );
      else if( ct == ChangeType.Insert_Text  ) Undo_InsertChar( rlc, rV );
      else if( ct == ChangeType.Remove_Text  ) Undo_RemoveChar( rlc, rV );
      else if( ct == ChangeType.Replace_Text ) Undo_Set       ( rlc, rV );
    }
  }
  void UndoAll( final View rV )
  {
    while( 0 < m_changes.size() )
    {
      Undo( rV );
    }
  }

  void Save_Set( final int     l_num
               , final int     c_pos
               , final char    old_C
               , final boolean continue_last_update )
  {
    final int NUM_CHANGES = m_changes.size();
  
    if( 0<NUM_CHANGES
     && continue_last_update
     && 0<c_pos
     && ChangeType.Replace_Text == m_changes.get(NUM_CHANGES-1).m_type
     && l_num                   == m_changes.get(NUM_CHANGES-1).m_lnum
     && c_pos                   == ( m_changes.get(NUM_CHANGES-1).m_cpos
                                   + m_changes.get(NUM_CHANGES-1).m_line.length() ) )
    {
      // Continuation of previous replacement:
      m_changes.get(NUM_CHANGES-1).m_line.append_c( old_C );
    }
    else {
      // Start of new replacement:
      LineChange lc = new LineChange( ChangeType.Replace_Text, l_num, c_pos );
      lc.m_line.append_c( old_C );

      m_changes.add( lc );
    }
  }

  void Save_InsertLine( final int l_num )
  {
    LineChange lc = new LineChange( ChangeType.Insert_Line, l_num, 0 );
  
    m_changes.add( lc );
  }

  void Save_InsertChar( final int l_num
                      , final int c_pos )
  {
    final int NUM_CHANGES = m_changes.size();
  
    if( 0<NUM_CHANGES
     && 0<c_pos
     && ChangeType.Insert_Text == m_changes.get(NUM_CHANGES-1).m_type
     && l_num                  == m_changes.get(NUM_CHANGES-1).m_lnum
     && c_pos                  == ( m_changes.get(NUM_CHANGES-1).m_cpos
                                  + m_changes.get(NUM_CHANGES-1).m_line.length() ) )
    {
      // Continuation of previous insertion:
      m_changes.get(NUM_CHANGES-1).m_line.append_c( (char)0 );
    }
    else {
      // Start of new insertion:
      LineChange lc = new LineChange( ChangeType.Insert_Text, l_num, c_pos );
      lc.m_line.append_c( (char)0 );

      m_changes.add( lc );
    }
  }

  void Save_RemoveLine( final int  l_num
                      , final Line line )
  {
    LineChange lc = new LineChange( ChangeType.Remove_Line, l_num, 0, line );

    m_changes.add( lc );
  }

  void Save_RemoveChar( final int  l_num
                      , final int  c_pos
                      , final char old_C )
  {
    final int NUM_CHANGES = m_changes.size();
  
    if( 0<NUM_CHANGES
     && ChangeType.Remove_Text == m_changes.get( NUM_CHANGES-1 ).m_type
     && l_num                  == m_changes.get( NUM_CHANGES-1 ).m_lnum
     && c_pos                  == m_changes.get( NUM_CHANGES-1 ).m_cpos )
    {
      // Continuation of previous removal:
      m_changes.get( NUM_CHANGES-1 ).m_line.append_c( old_C );
    }
    else {
      // Start of new removal:
      LineChange lc = new LineChange( ChangeType.Remove_Text, l_num, c_pos );
      lc.m_line.append_c( old_C );

      m_changes.add( lc );
    }
  }

  void Save_SwapLines( final int l_num_1
                     , final int l_num_2 )
  {
  }

  void Undo_Set( LineChange rlc, final View rV )
  {
    final int LINE_LEN = rlc.m_line.length();

    for( int k=0; k<LINE_LEN; k++ )
    {
      final char C = rlc.m_line.charAt( k );

      m_fb.Set( rlc.m_lnum, rlc.m_cpos+k, C, true );
    }
    rV.GoToCrsPos_Write( rlc.m_lnum, rlc.m_cpos );

    m_fb.Update();
  }

  void Undo_InsertLine( LineChange rlc, final View rV )
  {
    // Undo an inserted line by removing the inserted line
    m_fb.RemoveLine( rlc.m_lnum );
  
  //CrsPos cp = { rlc.m_lnum, rlc.m_cpos };
    // If last line of file was just removed, rlc.m_lnum is out of range,
    // so go to NUM_LINES-1 instead:
    final int NUM_LINES = m_fb.NumLines();
    final int LINE_NUM  = rlc.m_lnum < NUM_LINES ? rlc.m_lnum : NUM_LINES-1;
  
    rV.GoToCrsPos_Write( LINE_NUM, rlc.m_cpos );
  
    m_fb.Update();
  }

  void Undo_RemoveLine( LineChange rlc, final View rV )
  {
    // Undo a removed line by inserting the removed line
    m_fb.InsertLine( rlc.m_lnum, rlc.m_line );

    rV.GoToCrsPos_Write( rlc.m_lnum, rlc.m_cpos );

    m_fb.Update();
  }

  void Undo_InsertChar( LineChange rlc, final View rV )
  {
    final int LINE_LEN = rlc.m_line.length();

    // Undo inserted chars by removing the inserted chars
    for( int k=0; k<LINE_LEN; k++ )
    {
      m_fb.RemoveChar( rlc.m_lnum, rlc.m_cpos );
    }
    rV.GoToCrsPos_Write( rlc.m_lnum, rlc.m_cpos );

    m_fb.Update();
  }

  void Undo_RemoveChar( LineChange rlc, final View rV )
  {
    final int LINE_LEN = rlc.m_line.length();

    // Undo removed chars by inserting the removed chars
    for( int k=0; k<LINE_LEN; k++ )
    {
      final char C = rlc.m_line.charAt( k );

      m_fb.InsertChar( rlc.m_lnum, rlc.m_cpos+k, C );
    }
    rV.GoToCrsPos_Write( rlc.m_lnum, rlc.m_cpos );

    m_fb.Update();
  }

  FileBuf               m_fb;
  ArrayList<LineChange> m_changes = new ArrayList<>();
}

