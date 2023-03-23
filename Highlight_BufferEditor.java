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

class Highlight_BufferEditor extends Highlight_Base
{
  enum Hi_State
  {
    In_None,
    Done
  }

  Highlight_BufferEditor( FileBuf fb )
  {
    super( fb );
  }

//void Run()
//{
//  m_state = Hi_State.In_None;
//  m_l = 0;
//  m_p = 0;
//
//  while( Hi_State.Done != m_state ) Run_State();
//}

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
    case In_None  : Hi_In_None  (); break;
    default:
      m_state = Hi_State.In_None;
    }
  }

  void Hi_In_None()
  {
    for( ; m_l<m_fb.NumLines(); m_l++ )
    {
            Line lp = m_fb.GetLine( m_l );
      final int  LL = lp.length();
 
      if( 0<LL )
      {
        final char c_end = m_fb.Get( m_l, LL-1 );
        String ls = lp.toString();

        if( ls.equals( m_fb.m_vis.EDIT_BUF_NAME )
         || ls.equals( m_fb.m_vis.HELP_BUF_NAME )
         || ls.equals( m_fb.m_vis.MSG__BUF_NAME )
         || ls.equals( m_fb.m_vis.SHELL_BUF_NAME )
         || ls.equals( m_fb.m_vis.COLON_BUF_NAME )
         || ls.equals( m_fb.m_vis.SLASH_BUF_NAME ) )
        {
          for( int k=0; k<LL; k++ )
          {
            m_fb.SetSyntaxStyle( m_l, k, Highlight_Type.DEFINE.val );
          }
        }
        else if( c_end == Utils.DIR_DELIM )
        {
          for( int k=0; k<LL; k++ )
          {
            final char C = m_fb.Get( m_l, k );
            if( C == Utils.DIR_DELIM )
              m_fb.SetSyntaxStyle( m_l, k, Highlight_Type.CONST.val );
            else
              m_fb.SetSyntaxStyle( m_l, k, Highlight_Type.CONTROL.val );
          }
        }
        else {
          for( int k=0; k<LL; k++ )
          {
            final char C = m_fb.Get( m_l, k );
            if( C == Utils.DIR_DELIM )
              m_fb.SetSyntaxStyle( m_l, k, Highlight_Type.CONST.val );
          }
        }
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
  }

  Hi_State m_state = Hi_State.In_None;
  int      m_l = 0; // Line
  int      m_p = 0; // Position on line
}

