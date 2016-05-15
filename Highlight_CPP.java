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

class Highlight_CPP extends Highlight_Code
{
  Highlight_CPP( FileBuf fb )
  {
    super( fb );
  }

  void Find_Styles_Keys()
  {
    Hi_FindKey( m_HiPairs );
  }
  // Find keys starting on st up to but not including fn line
  void Find_Styles_Keys_In_Range( final CrsPos st
                                , final int    fn )
  {
    Hi_FindKey_In_Range( m_HiPairs, st, fn );
  }
  HiKeyVal[] m_HiPairs =
  {
    new HiKeyVal( "if"                 , Highlight_Type.CONTROL ),
    new HiKeyVal( "else"               , Highlight_Type.CONTROL ),
    new HiKeyVal( "for"                , Highlight_Type.CONTROL ),
    new HiKeyVal( "while"              , Highlight_Type.CONTROL ),
    new HiKeyVal( "do"                 , Highlight_Type.CONTROL ),
    new HiKeyVal( "return"             , Highlight_Type.CONTROL ),
    new HiKeyVal( "switch"             , Highlight_Type.CONTROL ),
    new HiKeyVal( "case"               , Highlight_Type.CONTROL ),
    new HiKeyVal( "break"              , Highlight_Type.CONTROL ),
    new HiKeyVal( "default"            , Highlight_Type.CONTROL ),
    new HiKeyVal( "continue"           , Highlight_Type.CONTROL ),
    new HiKeyVal( "template"           , Highlight_Type.CONTROL ),
    new HiKeyVal( "struct"             , Highlight_Type.CONTROL ),
    new HiKeyVal( "public"             , Highlight_Type.CONTROL ),
    new HiKeyVal( "protected"          , Highlight_Type.CONTROL ),
    new HiKeyVal( "private"            , Highlight_Type.CONTROL ),
    new HiKeyVal( "typedef"            , Highlight_Type.CONTROL ),
    new HiKeyVal( "delete"             , Highlight_Type.CONTROL ),
    new HiKeyVal( "operator"           , Highlight_Type.CONTROL ),
    new HiKeyVal( "sizeof"             , Highlight_Type.CONTROL ),
    new HiKeyVal( "using"              , Highlight_Type.CONTROL ),
    new HiKeyVal( "namespace"          , Highlight_Type.CONTROL ),
    new HiKeyVal( "goto"               , Highlight_Type.CONTROL ),
    new HiKeyVal( "friend"             , Highlight_Type.CONTROL ),
    new HiKeyVal( "throw"              , Highlight_Type.CONTROL ),
    new HiKeyVal( "and"                , Highlight_Type.CONTROL ),
    new HiKeyVal( "or"                 , Highlight_Type.CONTROL ),
    new HiKeyVal( "not"                , Highlight_Type.CONTROL ),
    new HiKeyVal( "new"                , Highlight_Type.VARTYPE ),
    new HiKeyVal( "const_cast"         , Highlight_Type.VARTYPE ),
    new HiKeyVal( "static_cast"        , Highlight_Type.VARTYPE ),
    new HiKeyVal( "dynamic_cast"       , Highlight_Type.VARTYPE ),
    new HiKeyVal( "reinterpret_cast"   , Highlight_Type.VARTYPE ),
    new HiKeyVal( "int"                , Highlight_Type.VARTYPE ),
    new HiKeyVal( "long"               , Highlight_Type.VARTYPE ),
    new HiKeyVal( "void"               , Highlight_Type.VARTYPE ),
    new HiKeyVal( "this"               , Highlight_Type.VARTYPE ),
    new HiKeyVal( "bool"               , Highlight_Type.VARTYPE ),
    new HiKeyVal( "char"               , Highlight_Type.VARTYPE ),
    new HiKeyVal( "const"              , Highlight_Type.VARTYPE ),
    new HiKeyVal( "short"              , Highlight_Type.VARTYPE ),
    new HiKeyVal( "float"              , Highlight_Type.VARTYPE ),
    new HiKeyVal( "double"             , Highlight_Type.VARTYPE ),
    new HiKeyVal( "signed"             , Highlight_Type.VARTYPE ),
    new HiKeyVal( "unsigned"           , Highlight_Type.VARTYPE ),
    new HiKeyVal( "extern"             , Highlight_Type.VARTYPE ),
    new HiKeyVal( "static"             , Highlight_Type.VARTYPE ),
    new HiKeyVal( "enum"               , Highlight_Type.VARTYPE ),
    new HiKeyVal( "uint8_t"            , Highlight_Type.VARTYPE ),
    new HiKeyVal( "uint16_t"           , Highlight_Type.VARTYPE ),
    new HiKeyVal( "uint32_t"           , Highlight_Type.VARTYPE ),
    new HiKeyVal( "uint64_t"           , Highlight_Type.VARTYPE ),
    new HiKeyVal( "int8_t"             , Highlight_Type.VARTYPE ),
    new HiKeyVal( "int16_t"            , Highlight_Type.VARTYPE ),
    new HiKeyVal( "int32_t"            , Highlight_Type.VARTYPE ),
    new HiKeyVal( "int64_t"            , Highlight_Type.VARTYPE ),
    new HiKeyVal( "FILE"               , Highlight_Type.VARTYPE ),
    new HiKeyVal( "DIR"                , Highlight_Type.VARTYPE ),
    new HiKeyVal( "class"              , Highlight_Type.VARTYPE ),
    new HiKeyVal( "typename"           , Highlight_Type.VARTYPE ),
    new HiKeyVal( "virtual"            , Highlight_Type.VARTYPE ),
    new HiKeyVal( "inline"             , Highlight_Type.VARTYPE ),
    new HiKeyVal( "true"               , Highlight_Type.CONST   ),
    new HiKeyVal( "false"              , Highlight_Type.CONST   ),
    new HiKeyVal( "NULL"               , Highlight_Type.CONST   ),
    new HiKeyVal( "__FUNCTION__"       , Highlight_Type.DEFINE  ),
    new HiKeyVal( "__PRETTY_FUNCTION__", Highlight_Type.DEFINE  ),
    new HiKeyVal( "__FILE__"           , Highlight_Type.DEFINE  ),
    new HiKeyVal( "__LINE__"           , Highlight_Type.DEFINE  ),
    new HiKeyVal( "__TIMESTAMP__"      , Highlight_Type.DEFINE  ),
  };
}

