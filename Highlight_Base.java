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

abstract class Highlight_Base
{
  Highlight_Base( FileBuf fb )
  {
    m_fb = fb;
  }

  // Find styles starting on st up to but not including fn line
  abstract void Run_Range( final CrsPos st
                         , final int    fn );

  // Find keyes starting on st up to but not including fn line
  //
  void Hi_FindKey_In_Range( HiKeyVal[] HiPairs
                          , final CrsPos st
                          , final int    fn )
  {
    final int NUM_LINES = m_fb.NumLines();
 
    for( int l=st.crsLine; l<fn && l<NUM_LINES; l++ )
    {
      final Line lr = m_fb.GetLine( l );
      final Line sr = m_fb.GetStyle( l );
      final int LL = lr.length();
 
      final int st_pos = st.crsLine==l ? st.crsChar : 0;
      final int fn_pos = 0<LL ? LL-1 : 0;
 
      for( int p=st_pos; p<=fn_pos && p<LL; p++ )
      {
        boolean key_st = 0==sr.charAt(p)
                      && Utils.line_start_or_prev_C_non_ident( lr, p );
 
        for( int h=0; key_st && h<HiPairs.length; h++ )
        {
          boolean matches = true;
          final String         key     = HiPairs[h].m_key;
          final Highlight_Type HI_TYPE = HiPairs[h].m_val;
          final int KEY_LEN = key.length();
 
          for( int k=0; matches && (p+k)<LL && k<KEY_LEN; k++ )
          {
            if( 0!=sr.charAt(p+k) || key.charAt(k) != lr.charAt(p+k) ) matches = false;
            else {
              if( k+1 == KEY_LEN ) // Found pattern
              {
                matches = Utils.line_end_or_non_ident( lr, LL, p+k );
                if( matches ) {
                  for( int m=p; m<p+KEY_LEN; m++ ) m_fb.SetSyntaxStyle( l, m, HI_TYPE.val );
                  // Increment p one less than KEY_LEN, because p
                  // will be incremented again by the for loop
                  p += KEY_LEN-1;
                  // Set key_st to false here to break out of h for loop
                  key_st = false;
                }
              }
            }
          }
        }
      }
    }
  }

  // Case Insensitive version of Hi_FindKey.
  // Find keyes starting on st up to but not including fn line
  //
  void Hi_FindKey_In_Range_CI( HiKeyVal[] HiPairs
                             , final CrsPos st
                             , final int    fn )
  {
    final int NUM_LINES = m_fb.NumLines();
 
    for( int l=st.crsLine; l<fn && l<NUM_LINES; l++ )
    {
      final Line lr = m_fb.GetLine( l );
      final Line sr = m_fb.GetStyle( l );
      final int LL = lr.length();
 
      final int st_pos = st.crsLine==l ? st.crsChar : 0;
      final int fn_pos = 0<LL ? LL-1 : 0;
 
      for( int p=st_pos; p<=fn_pos && p<LL; p++ )
      {
        boolean key_st = 0==sr.charAt(p)
                      && Utils.line_start_or_prev_C_non_ident( lr, p );
 
        for( int h=0; key_st && h<HiPairs.length; h++ )
        {
          boolean matches = true;
          final String         key     = HiPairs[h].m_key;
          final Highlight_Type HI_TYPE = HiPairs[h].m_val;
          final int KEY_LEN = key.length();
 
          for( int k=0; matches && (p+k)<LL && k<KEY_LEN; k++ )
          {
            final char  key_char = Character.toLowerCase( key.charAt(k) );
            final char line_char = Character.toLowerCase(  lr.charAt(p+k) );

            if( 0!=sr.charAt(p+k) || key_char != line_char ) matches = false;
            else {
              if( k+1 == KEY_LEN ) // Found pattern
              {
                matches = Utils.line_end_or_non_ident( lr, LL, p+k );
                if( matches ) {
                  for( int m=p; m<p+KEY_LEN; m++ ) m_fb.SetSyntaxStyle( l, m, HI_TYPE.val );
                  // Increment p one less than KEY_LEN, because p
                  // will be incremented again by the for loop
                  p += KEY_LEN-1;
                  // Set key_st to false here to break out of h for loop
                  key_st = false;
                }
              }
            }
          }
        }
      }
    }
  }
  FileBuf m_fb;
}

