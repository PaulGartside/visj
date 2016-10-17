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

class Highlight_STL extends Highlight_Code
{
  Highlight_STL( FileBuf fb )
  {
    super( fb );
  }

//void Find_Styles_Keys()
//{
//  Hi_FindKey( m_HiPairs );
//}
  void Find_Styles_Keys_In_Range( final CrsPos st
                                , final int    fn )
  {
    Hi_FindKey_In_Range( m_HiPairs, st, fn );
  }
  HiKeyVal[] m_HiPairs =
  {
    // Flow of control:
    new HiKeyVal( "if"           , Highlight_Type.CONTROL ),
    new HiKeyVal( "else"         , Highlight_Type.CONTROL ),
    new HiKeyVal( "for"          , Highlight_Type.CONTROL ),
    new HiKeyVal( "foreach"      , Highlight_Type.CONTROL ),
    new HiKeyVal( "while"        , Highlight_Type.CONTROL ),
    new HiKeyVal( "return"       , Highlight_Type.CONTROL ),
    new HiKeyVal( "sizeof"       , Highlight_Type.CONTROL ),
    new HiKeyVal( "break"        , Highlight_Type.CONTROL ),
    new HiKeyVal( "continue"     , Highlight_Type.CONTROL ),
    new HiKeyVal( "die"          , Highlight_Type.CONTROL ),
    new HiKeyVal( "exit"         , Highlight_Type.CONTROL ),

    // General pupose/OS:
    new HiKeyVal( "print"        , Highlight_Type.CONTROL ),
    new HiKeyVal( "sleep"        , Highlight_Type.CONTROL ),
    new HiKeyVal( "system"       , Highlight_Type.CONTROL ),
    new HiKeyVal( "getTimeOfDay" , Highlight_Type.CONTROL ),
    new HiKeyVal( "rename"       , Highlight_Type.CONTROL ),

    // Math functions:
    new HiKeyVal( "abs"        , Highlight_Type.CONTROL ),
    new HiKeyVal( "acos"       , Highlight_Type.CONTROL ),
    new HiKeyVal( "asin"       , Highlight_Type.CONTROL ),
    new HiKeyVal( "atan"       , Highlight_Type.CONTROL ),
    new HiKeyVal( "atan2"      , Highlight_Type.CONTROL ),
    new HiKeyVal( "cos"        , Highlight_Type.CONTROL ),
    new HiKeyVal( "cosh"       , Highlight_Type.CONTROL ),
    new HiKeyVal( "exp"        , Highlight_Type.CONTROL ),
    new HiKeyVal( "log"        , Highlight_Type.CONTROL ),
    new HiKeyVal( "log10"      , Highlight_Type.CONTROL ),
    new HiKeyVal( "pow"        , Highlight_Type.CONTROL ),
    new HiKeyVal( "sin"        , Highlight_Type.CONTROL ),
    new HiKeyVal( "sinh"       , Highlight_Type.CONTROL ),
    new HiKeyVal( "sqrt"       , Highlight_Type.CONTROL ),
    new HiKeyVal( "tan"        , Highlight_Type.CONTROL ),
    new HiKeyVal( "tanh"       , Highlight_Type.CONTROL ),
    new HiKeyVal( "drand48"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "srand48"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "normal_rand", Highlight_Type.CONTROL ),
    new HiKeyVal( "hton"       , Highlight_Type.CONTROL ),
    new HiKeyVal( "ntoh"       , Highlight_Type.CONTROL ),
    new HiKeyVal( "DFT"        , Highlight_Type.CONTROL ),
    new HiKeyVal( "FFT"        , Highlight_Type.CONTROL ),

    // Variables:
    new HiKeyVal( "uchar"        , Highlight_Type.VARTYPE ),
    new HiKeyVal( "ushort"       , Highlight_Type.VARTYPE ),
    new HiKeyVal( "uint"         , Highlight_Type.VARTYPE ),
    new HiKeyVal( "int"          , Highlight_Type.VARTYPE ),
    new HiKeyVal( "void"         , Highlight_Type.VARTYPE ),
    new HiKeyVal( "bool"         , Highlight_Type.VARTYPE ),
    new HiKeyVal( "const"        , Highlight_Type.VARTYPE ),
    new HiKeyVal( "float"        , Highlight_Type.VARTYPE ),
    new HiKeyVal( "double"       , Highlight_Type.VARTYPE ),
    new HiKeyVal( "string"       , Highlight_Type.VARTYPE ),
    new HiKeyVal( "List"         , Highlight_Type.VARTYPE ),
    new HiKeyVal( "FILE"         , Highlight_Type.VARTYPE ),
    new HiKeyVal( "DIR"          , Highlight_Type.VARTYPE ),
    new HiKeyVal( "class"        , Highlight_Type.VARTYPE ),
    new HiKeyVal( "sub"          , Highlight_Type.VARTYPE ),
    new HiKeyVal( "inline"       , Highlight_Type.VARTYPE ),

  //new HiKeyVal( "true"         , Highlight_Type.CONST   ),
  //new HiKeyVal( "false"        , Highlight_Type.CONST   ),

    new HiKeyVal( "include"      , Highlight_Type.DEFINE  ),
    new HiKeyVal( "__FUNCTION__" , Highlight_Type.DEFINE  ),
    new HiKeyVal( "__FILE__"     , Highlight_Type.DEFINE  ),
    new HiKeyVal( "__LINE__"     , Highlight_Type.DEFINE  ),
  };
}

