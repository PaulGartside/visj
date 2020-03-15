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

class Cover
{
  static final int m_seq_len = 7;
  static int m_seq_inc[] = {  79, 101, 127, 139, 163, 181, 199 };
  static int m_seq_mod[] = { 131, 151, 173, 191, 211, 229, 251 };
  static int m_seq_val[] = new int[ m_seq_len ];

  static void Cover_Array( FileBuf         in
                         , ArrayList<Byte> out
                         , final int       seed
                         , final String    key )
  {
    Init_seq_val( seed, key );

    out.clear();
    out.ensureCapacity( in.GetSize() );

    final int NUM_LINES = in.NumLines();

    for( int l=0; l<NUM_LINES; l++ )
    {
      final int LL = in.LineLen( l );

      for( int p=0; p<LL; p++ )
      {
        final int B = in.Get( l, p );
        final int C = Cover_Byte() ^ B;
        out.add( (byte)C );
      }
      if( l<NUM_LINES-1 || in.m_LF_at_EOF )
      {
        final int C = Cover_Byte() ^ '\n';
        out.add( (byte)C );
      }
    }
  }

  static void Init_seq_val( final int    seed
                          , final String key )
  {
    // Initialize m_seq_val:
    final int key_len = key.length();

    for( int k=0; k<m_seq_len; k++ )
    {
      m_seq_val[k] = (seed + m_seq_inc[k]) % m_seq_mod[k];
    }
    for( int k=0; k<m_seq_len*key_len; k+=1 )
    {
      final int k_m = k % m_seq_len;

      m_seq_val[ k_m ] ^= key.charAt( k % key_len );
      m_seq_val[ k_m ] %= m_seq_mod[ k_m ];
    }
  }

  static int Cover_Byte()
  {
    int cb = 0xAA;
    for( int k=0; k<m_seq_len; k+=1 )
    {
      m_seq_val[k] = ( m_seq_val[k] + m_seq_inc[k] ) % m_seq_mod[k];

      cb ^= m_seq_val[k];
    }
    return cb;
  }
}

