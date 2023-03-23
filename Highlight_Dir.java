////////////////////////////////////////////////////////////////////////////////
// VI-Simplified (vis) Java Implementation                                    //
// Copyright (c) 19 May 2016 Paul J. Gartside                                 //
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

class Highlight_Dir extends Highlight_Base
{
  enum Hi_State
  {
    In_None,
    Done
  }

  Highlight_Dir( FileBuf fb )
  {
    super( fb );
  }

  // Find styles starting on st up to but not including fn line
  void Run_Range( final CrsPos st
                , final int    fn )
  {
    m_state = Hi_State.In_None;

    m_l = st.crsLine;
    m_p = st.crsChar;

    while( Hi_State.Done != m_state
        && m_l<fn )
    {
      Run_State();
    }
  }

  void Run_State()
  {
    switch( m_state )
    {
    case In_None: Hi_In_None(); break;
    default:
      m_state = Hi_State.In_None;
    }
  }

  void Hi_In_None()
  {
    for( ; m_l<m_fb.NumLines(); m_l++ )
    {
      final Line lp = m_fb.GetLine( m_l );
      final int  LL = lp.length();
 
      if( 0<LL )
      {
        final char c_end = m_fb.Get( m_l, LL-1 );

        final char C0 =        m_fb.Get( m_l, 0 );
        final char C1 = 1<LL ? m_fb.Get( m_l, 1 ) : 0;

        if( 2==LL && C0=='.' && C1=='.' )
        {
          m_fb.SetSyntaxStyle( m_l, 0, Highlight_Type.DEFINE.val );
          m_fb.SetSyntaxStyle( m_l, 1, Highlight_Type.DEFINE.val );
        }
        else if( c_end == Utils.DIR_DELIM )
        {
          Hi_In_None_Dir( m_l, LL );
        }
        else  {
          Hi_In_None_File( m_l, LL );
        }
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
  }

  void Hi_In_None_Dir( final int l, final int LL )
  {
    boolean found_sym_link = false;

    for( int k=0; k<LL; k++ )
    {
      final char C0 = 0<k ? m_fb.Get( l, k-1 ) : 0;
      final char C1 =       m_fb.Get( l, k );

      if( C1 == '.' )
      {
        m_fb.SetSyntaxStyle( l, k, Highlight_Type.VARTYPE.val );
      }
      else if( C0 == '-' && C1 == '>' )
      {
        found_sym_link = true;
        // -> means symbolic link
        m_fb.SetSyntaxStyle( l, k-1, Highlight_Type.DEFINE.val );
        m_fb.SetSyntaxStyle( l, k  , Highlight_Type.DEFINE.val );
      }
      else if( found_sym_link && C1 == Utils.DIR_DELIM )
      {
        m_fb.SetSyntaxStyle( l, k, Highlight_Type.CONST.val );
      }
      else {
        m_fb.SetSyntaxStyle( l, k, Highlight_Type.CONTROL.val );
      }
    }
    m_fb.SetSyntaxStyle( l, LL-1, Highlight_Type.CONST.val );
  }

  void Hi_In_None_File( final int l, final int LL )
  {
    boolean found_sym_link = false;

    for( int k=0; k<LL; k++ )
    {
      final char C0 = 0<k ? m_fb.Get( l, k-1 ) : 0;
      final char C1 =       m_fb.Get( l, k );

      if( C1 == '.' )
      {
        m_fb.SetSyntaxStyle( l, k, Highlight_Type.VARTYPE.val );
      }
      else if( C0 == '-' && C1 == '>' )
      {
        found_sym_link = true;
        // -> means symbolic link
        m_fb.SetSyntaxStyle( l, k-1, Highlight_Type.DEFINE.val );
        m_fb.SetSyntaxStyle( l, k  , Highlight_Type.DEFINE.val );
      }
      else if( found_sym_link && C1 == Utils.DIR_DELIM )
      {
        m_fb.SetSyntaxStyle( l, k, Highlight_Type.CONST.val );
      }
    }
  }

  Hi_State m_state = Hi_State.In_None;
  int      m_l = 0; // Line
  int      m_p = 0; // Position on line
}

