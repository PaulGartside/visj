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

class Line
{
  Line()
  {
    m_sb = new StringBuilder();
  }
  Line( CharSequence seq )
  {
    m_sb = new StringBuilder( seq );
  }
  Line( int capacity )
  {
    m_sb = new StringBuilder( capacity );
  }
  Line( Line other )
  {
    m_sb = new StringBuilder( other.m_sb );
  }
  int length()
  {
    return m_sb.length();
  }
  void setLength( int newLength )
  {
    m_sb.setLength( newLength );
  }
  char charAt( int index )
  {
    return m_sb.charAt( index );
  }
  void setCharAt( int index, char c )
  {
    m_sb.setCharAt( index, c );

    m_chksum_all_valid = false;
    m_chksum_diff_valid = false;
  }

  Line append_c( char c )
  {
    m_sb.append( c );

    m_chksum_all_valid = false;
    m_chksum_diff_valid = false;

    return this;
  }
  Line append_i( int i )
  {
    m_sb.append( i );

    m_chksum_all_valid = false;
    m_chksum_diff_valid = false;

    return this;
  }
  Line append_s( String str )
  {
    m_sb.append( str );

    m_chksum_all_valid = false;
    m_chksum_diff_valid = false;

    return this;
  }
  Line append_l( Line other )
  {
    m_sb.append( other.m_sb );

    m_chksum_all_valid = false;
    m_chksum_diff_valid = false;

    return this;
  }
  Line deleteCharAt( int index )
  {
    m_sb.deleteCharAt( index );

    m_chksum_all_valid = false;
    m_chksum_diff_valid = false;

    return this;
  }
  Line insert( int offset, char c )
  {
    m_sb.insert( offset, c );

    m_chksum_all_valid = false;
    m_chksum_diff_valid = false;

    return this;
  }
  String substring( int start )
  {
    return m_sb.substring( start );
  }
  int chksum_all()
  {
    if( !m_chksum_all_valid )
    {
      m_chksum_all = calc_chksum_all();

      m_chksum_all_valid = true;
    }
    return m_chksum_all;
  }
  int chksum_diff()
  {
    if( !m_chksum_diff_valid )
    {
      m_chksum_diff = calc_chksum_diff();

      m_chksum_diff_valid = true;
    }
    return m_chksum_diff;
  }
  private int calc_chksum_all()
  {
    int chk_sum = 0;

    final int start  = 0;
    final int finish = m_sb.length();

    for( int i=start; i<finish; i++ )
    {
      chk_sum ^= m_primes[(i-start)%m_num_primes] ^ m_sb.charAt( i );
      chk_sum = ((chk_sum << 13)&0xFFFFE000)
              | ((chk_sum >> 19)&0x00001FFF);
    }
//Utils.Log(chk_sum+":'"+toString()+"'");
    return chk_sum;
  }
  private int calc_chksum_diff()
  {
    int chk_sum = 0;

    int start = 0;
    int finish = 0<m_sb.length() ? m_sb.length()-1 : -1;
 
    start  = skip_white_beg( m_sb, start );
    finish = skip_white_end( m_sb, start, finish );

    for( int i=start; i<=finish; i++ )
    {
      chk_sum ^= m_primes[(i-start)%m_num_primes] ^ m_sb.charAt( i );
      chk_sum = ((chk_sum << 13)&0xFFFFE000)
              | ((chk_sum >> 19)&0x00001FFF);
    }
    return chk_sum;
  }
  int skip_white_beg( StringBuilder sb, int start )
  {
    final int LEN = sb.length();

    if( 0<LEN )
    for( char C = sb.charAt( start )
       ; start<LEN && (' '==C || '\t'==C || '\r'==C); )
    {
      start++;
      if( start<LEN ) C = sb.charAt( start );
    }
    return start;
  }
  int skip_white_end( StringBuilder sb
                    , final int start
                    , int finish )
  {
    if( -1<finish )
    for( char C = sb.charAt( finish )
       ; start<=finish && (' '==C || '\t'==C || '\r'==C); )
    {
      finish--;
      if( start<=finish ) C = sb.charAt( finish );
    }
    return finish;
  }
  public String toString()
  {
    return m_sb.toString();
  }
//boolean equals( Line l )
//{
//  return toString().equals( l.toString() );
//}
  boolean equals( Line l )
  {
    return m_sb.length() == l.m_sb.length()
        &&  chksum_all() == l.chksum_all();
  }
  private StringBuilder m_sb;
  private boolean       m_chksum_all_valid = false;
  private boolean       m_chksum_diff_valid = false;
  private int           m_chksum_all;
  private int           m_chksum_diff;
  private static int[]  m_primes = {  43, 101, 149, 193, 241, 293,
                                     353, 409, 461, 521, 587, 641,
                                     691, 757, 823, 881, 947 };
  private static final int m_num_primes = m_primes.length;
}

