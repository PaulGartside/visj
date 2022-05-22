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

class Highlight_Go extends Highlight_Code
{
  Highlight_Go( FileBuf fb )
  {
    super( fb );
  }

//void Find_Styles_Keys()
//{
//  Hi_FindKey( m_HiPairs );
//}
  // Find keys starting on st up to but not including fn line
  void Find_Styles_Keys_In_Range( final CrsPos st
                                , final int    fn )
  {
    Hi_FindKey_In_Range( m_HiPairs, st, fn );
  }
  HiKeyVal[] m_HiPairs =
  {
    // Keywords
    new HiKeyVal( "break"              , Highlight_Type.CONTROL ),
    new HiKeyVal( "case"               , Highlight_Type.CONTROL ),
    new HiKeyVal( "chan"               , Highlight_Type.CONTROL ),
    new HiKeyVal( "continue"           , Highlight_Type.CONTROL ),
    new HiKeyVal( "default"            , Highlight_Type.CONTROL ),
    new HiKeyVal( "defer"              , Highlight_Type.CONTROL ),
    new HiKeyVal( "else"               , Highlight_Type.CONTROL ),
    new HiKeyVal( "fallthrough"        , Highlight_Type.CONTROL ),
    new HiKeyVal( "for"                , Highlight_Type.CONTROL ),
    new HiKeyVal( "func"               , Highlight_Type.CONTROL ),
    new HiKeyVal( "go"                 , Highlight_Type.CONTROL ),
    new HiKeyVal( "goto"               , Highlight_Type.CONTROL ),
    new HiKeyVal( "if"                 , Highlight_Type.CONTROL ),
    new HiKeyVal( "range"              , Highlight_Type.CONTROL ),
    new HiKeyVal( "return"             , Highlight_Type.CONTROL ),
    new HiKeyVal( "select"             , Highlight_Type.CONTROL ),
    new HiKeyVal( "switch"             , Highlight_Type.CONTROL ),

    // Built in functions
    new HiKeyVal( "make"               , Highlight_Type.CONTROL ),
    new HiKeyVal( "len"                , Highlight_Type.CONTROL ),
    new HiKeyVal( "cap"                , Highlight_Type.CONTROL ),
    new HiKeyVal( "new"                , Highlight_Type.CONTROL ),
    new HiKeyVal( "append"             , Highlight_Type.CONTROL ),
    new HiKeyVal( "copy"               , Highlight_Type.CONTROL ),
    new HiKeyVal( "close"              , Highlight_Type.CONTROL ),
    new HiKeyVal( "delete"             , Highlight_Type.CONTROL ),
    new HiKeyVal( "complex"            , Highlight_Type.CONTROL ),
    new HiKeyVal( "real"               , Highlight_Type.CONTROL ),
    new HiKeyVal( "imag"               , Highlight_Type.CONTROL ),
    new HiKeyVal( "panic"              , Highlight_Type.CONTROL ),
    new HiKeyVal( "recover"            , Highlight_Type.CONTROL ),

    // Types
    new HiKeyVal( "const"              , Highlight_Type.VARTYPE ),
    new HiKeyVal( "interface"          , Highlight_Type.VARTYPE ),
    new HiKeyVal( "map"                , Highlight_Type.VARTYPE ),
    new HiKeyVal( "package"            , Highlight_Type.VARTYPE ),
    new HiKeyVal( "struct"             , Highlight_Type.VARTYPE ),
    new HiKeyVal( "type"               , Highlight_Type.VARTYPE ),
    new HiKeyVal( "var"                , Highlight_Type.VARTYPE ),
    new HiKeyVal( "int"                , Highlight_Type.VARTYPE ),
    new HiKeyVal( "int8"               , Highlight_Type.VARTYPE ),
    new HiKeyVal( "int16"              , Highlight_Type.VARTYPE ),
    new HiKeyVal( "int32"              , Highlight_Type.VARTYPE ),
    new HiKeyVal( "int64"              , Highlight_Type.VARTYPE ),
    new HiKeyVal( "uintptr"            , Highlight_Type.VARTYPE ),
    new HiKeyVal( "float32"            , Highlight_Type.VARTYPE ),
    new HiKeyVal( "float64"            , Highlight_Type.VARTYPE ),
    new HiKeyVal( "complex128"         , Highlight_Type.VARTYPE ),
    new HiKeyVal( "complex64"          , Highlight_Type.VARTYPE ),
    new HiKeyVal( "bool"               , Highlight_Type.VARTYPE ),
    new HiKeyVal( "byte"               , Highlight_Type.VARTYPE ),
    new HiKeyVal( "rune"               , Highlight_Type.VARTYPE ),
    new HiKeyVal( "string"             , Highlight_Type.VARTYPE ),
    new HiKeyVal( "error"              , Highlight_Type.VARTYPE ),

    // Constants
    new HiKeyVal( "true"               , Highlight_Type.CONST   ),
    new HiKeyVal( "false"              , Highlight_Type.CONST   ),
    new HiKeyVal( "iota"               , Highlight_Type.CONST   ),
    new HiKeyVal( "nil"                , Highlight_Type.CONST   ),

    new HiKeyVal( "import"             , Highlight_Type.DEFINE  ),
  };
}

