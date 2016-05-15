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

class SameArea
{
  void Clear()
  {
    m_ln_s   = 0;
    m_ln_l   = 0;
    m_nlines = 0;
    m_nbytes = 0;
  }

  void Init( int ln_s, int ln_l, int nbytes )
  {
    m_ln_s   = ln_s;
    m_ln_l   = ln_l;
    m_nlines = 1;
    m_nbytes = nbytes;
  }

  void Inc( int nbytes )
  {
    m_nlines += 1;
    m_nbytes += nbytes;
  }

  void Set( final SameArea a )
  {
    m_ln_s   = a.m_ln_s;
    m_ln_l   = a.m_ln_l;
    m_nlines = a.m_nlines;
    m_nbytes = a.m_nbytes;
  }
  void Print()
  {
    Utils.Log( toString() );
  }
  public String toString()
  {
    return "SameArea:"
         + " lines_s=("+(m_ln_s+1)+","+(m_ln_s+m_nlines)+")"
         + " lines_l=("+(m_ln_l+1)+","+(m_ln_l+m_nlines)+")"
         + " nlines="+m_nlines+" nbytes="+m_nbytes;
  }

  int m_ln_s;   // Beginning line number in short file
  int m_ln_l;   // Beginning line number in long  file
  int m_nlines; // Number of consecutive lines the same
  int m_nbytes; // Number of bytes in consecutive lines the same
}

