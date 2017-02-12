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

class Highlight_Java extends Highlight_Code
{
  Highlight_Java( FileBuf fb )
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
    new HiKeyVal( "abstract"           , Highlight_Type.VARTYPE ),
    new HiKeyVal( "boolean"            , Highlight_Type.VARTYPE ),
    new HiKeyVal( "Boolean"            , Highlight_Type.VARTYPE ),
    new HiKeyVal( "break"              , Highlight_Type.CONTROL ),
    new HiKeyVal( "byte"               , Highlight_Type.VARTYPE ),
    new HiKeyVal( "Byte"               , Highlight_Type.VARTYPE ),
    new HiKeyVal( "case"               , Highlight_Type.CONTROL ),
    new HiKeyVal( "catch"              , Highlight_Type.CONTROL ),
    new HiKeyVal( "char"               , Highlight_Type.VARTYPE ),
    new HiKeyVal( "Character"          , Highlight_Type.VARTYPE ),
    new HiKeyVal( "class"              , Highlight_Type.VARTYPE ),
    new HiKeyVal( "const"              , Highlight_Type.VARTYPE ),
    new HiKeyVal( "continue"           , Highlight_Type.CONTROL ),
    new HiKeyVal( "default"            , Highlight_Type.CONTROL ),
    new HiKeyVal( "do"                 , Highlight_Type.CONTROL ),
    new HiKeyVal( "double"             , Highlight_Type.VARTYPE ),
    new HiKeyVal( "Double"             , Highlight_Type.VARTYPE ),
    new HiKeyVal( "else"               , Highlight_Type.CONTROL ),
    new HiKeyVal( "enum"               , Highlight_Type.VARTYPE ),
    new HiKeyVal( "extends"            , Highlight_Type.VARTYPE ),
    new HiKeyVal( "final"              , Highlight_Type.VARTYPE ),
    new HiKeyVal( "float"              , Highlight_Type.VARTYPE ),
    new HiKeyVal( "Float"              , Highlight_Type.VARTYPE ),
    new HiKeyVal( "finally"            , Highlight_Type.CONTROL ),
    new HiKeyVal( "for"                , Highlight_Type.CONTROL ),
    new HiKeyVal( "goto"               , Highlight_Type.CONTROL ),
    new HiKeyVal( "if"                 , Highlight_Type.CONTROL ),
    new HiKeyVal( "implements"         , Highlight_Type.VARTYPE ),
    new HiKeyVal( "import"             , Highlight_Type.DEFINE  ),
    new HiKeyVal( "instanceof"         , Highlight_Type.VARTYPE ),
    new HiKeyVal( "int"                , Highlight_Type.VARTYPE ),
    new HiKeyVal( "Integer"            , Highlight_Type.VARTYPE ),
    new HiKeyVal( "interface"          , Highlight_Type.VARTYPE ),
    new HiKeyVal( "Iterator"           , Highlight_Type.VARTYPE ),
    new HiKeyVal( "long"               , Highlight_Type.VARTYPE ),
    new HiKeyVal( "Long"               , Highlight_Type.VARTYPE ),
    new HiKeyVal( "main"               , Highlight_Type.DEFINE  ),
    new HiKeyVal( "native"             , Highlight_Type.VARTYPE ),
    new HiKeyVal( "new"                , Highlight_Type.VARTYPE ),
    new HiKeyVal( "package"            , Highlight_Type.DEFINE  ),
    new HiKeyVal( "private"            , Highlight_Type.CONTROL ),
    new HiKeyVal( "protected"          , Highlight_Type.CONTROL ),
    new HiKeyVal( "public"             , Highlight_Type.CONTROL ),
    new HiKeyVal( "return"             , Highlight_Type.CONTROL ),
    new HiKeyVal( "short"              , Highlight_Type.VARTYPE ),
    new HiKeyVal( "Short"              , Highlight_Type.VARTYPE ),
    new HiKeyVal( "static"             , Highlight_Type.VARTYPE ),
    new HiKeyVal( "strictfp"           , Highlight_Type.VARTYPE ),
    new HiKeyVal( "String"             , Highlight_Type.VARTYPE ),
    new HiKeyVal( "System"             , Highlight_Type.DEFINE  ),
    new HiKeyVal( "super"              , Highlight_Type.VARTYPE ),
    new HiKeyVal( "switch"             , Highlight_Type.CONTROL ),
    new HiKeyVal( "synchronized"       , Highlight_Type.CONTROL ),
    new HiKeyVal( "this"               , Highlight_Type.VARTYPE ),
    new HiKeyVal( "throw"              , Highlight_Type.CONTROL ),
    new HiKeyVal( "throws"             , Highlight_Type.CONTROL ),
    new HiKeyVal( "transient"          , Highlight_Type.VARTYPE ),
    new HiKeyVal( "try"                , Highlight_Type.CONTROL ),
    new HiKeyVal( "void"               , Highlight_Type.VARTYPE ),
    new HiKeyVal( "Void"               , Highlight_Type.VARTYPE ),
    new HiKeyVal( "volatile"           , Highlight_Type.VARTYPE ),
    new HiKeyVal( "while"              , Highlight_Type.CONTROL ),
    new HiKeyVal( "virtual"            , Highlight_Type.VARTYPE ),
    new HiKeyVal( "true"               , Highlight_Type.CONST   ),
    new HiKeyVal( "false"              , Highlight_Type.CONST   ),
    new HiKeyVal( "null"               , Highlight_Type.CONST   ),
    new HiKeyVal( "@Deprecated"        , Highlight_Type.DEFINE  ),
    new HiKeyVal( "@Override"          , Highlight_Type.DEFINE  ),
    new HiKeyVal( "@SuppressWarnings"  , Highlight_Type.DEFINE  ),
  };
}

