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

class Highlight_CMAKE extends Highlight_Code
{
  Highlight_CMAKE( FileBuf fb )
  {
    super( fb );
  }

  void Find_Styles_Keys()
  {
    Hi_FindKey_CI( m_HiPairs );
  }
  void Find_Styles_Keys_In_Range( final CrsPos st
                                , final int    fn )
  {
    Hi_FindKey_In_Range_CI( m_HiPairs, st, fn );
  }
  HiKeyVal[] m_HiPairs =
  {
    // Flow of control:
    new HiKeyVal( "if"           , Highlight_Type.CONTROL ),
    new HiKeyVal( "else"         , Highlight_Type.CONTROL ),
    new HiKeyVal( "elseif"       , Highlight_Type.CONTROL ),
    new HiKeyVal( "foreach"      , Highlight_Type.CONTROL ),
    new HiKeyVal( "project"      , Highlight_Type.CONTROL ),
    new HiKeyVal( "include"      , Highlight_Type.CONTROL ),
    new HiKeyVal( "list"         , Highlight_Type.CONTROL ),
    new HiKeyVal( "set"          , Highlight_Type.CONTROL ),
    new HiKeyVal( "string"       , Highlight_Type.CONTROL ),
  };
}

