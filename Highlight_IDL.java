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

class Highlight_IDL extends Highlight_Code
{
  Highlight_IDL( FileBuf fb )
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
    new HiKeyVal( "abstract"   , Highlight_Type.VARTYPE ),
    new HiKeyVal( "any"        , Highlight_Type.VARTYPE ),
    new HiKeyVal( "attribute"  , Highlight_Type.VARTYPE ),
    new HiKeyVal( "boolean"    , Highlight_Type.VARTYPE ),
    new HiKeyVal( "case"       , Highlight_Type.CONTROL ),
    new HiKeyVal( "char"       , Highlight_Type.VARTYPE ),
    new HiKeyVal( "component"  , Highlight_Type.CONTROL ),
    new HiKeyVal( "const"      , Highlight_Type.VARTYPE ),
    new HiKeyVal( "consumes"   , Highlight_Type.CONTROL ),
    new HiKeyVal( "context"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "custom"     , Highlight_Type.CONTROL ),
    new HiKeyVal( "default"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "double"     , Highlight_Type.VARTYPE ),
    new HiKeyVal( "emits"      , Highlight_Type.CONTROL ),
    new HiKeyVal( "enum"       , Highlight_Type.VARTYPE ),
    new HiKeyVal( "eventtype"  , Highlight_Type.CONTROL ),
    new HiKeyVal( "exception"  , Highlight_Type.CONTROL ),
    new HiKeyVal( "factory"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "FALSE"      , Highlight_Type.CONST   ),
    new HiKeyVal( "finder"     , Highlight_Type.CONTROL ),
    new HiKeyVal( "fixed"      , Highlight_Type.CONTROL ),
    new HiKeyVal( "float"      , Highlight_Type.VARTYPE ),
    new HiKeyVal( "getraises"  , Highlight_Type.CONTROL ),
    new HiKeyVal( "home"       , Highlight_Type.CONTROL ),
    new HiKeyVal( "import"     , Highlight_Type.DEFINE  ),
    new HiKeyVal( "in"         , Highlight_Type.CONTROL ),
    new HiKeyVal( "inout"      , Highlight_Type.CONTROL ),
    new HiKeyVal( "interface"  , Highlight_Type.VARTYPE ),
    new HiKeyVal( "local"      , Highlight_Type.VARTYPE ),
    new HiKeyVal( "long"       , Highlight_Type.VARTYPE ),
    new HiKeyVal( "module"     , Highlight_Type.VARTYPE ),
    new HiKeyVal( "multiple"   , Highlight_Type.CONTROL ),
    new HiKeyVal( "native"     , Highlight_Type.VARTYPE ),
    new HiKeyVal( "Object"     , Highlight_Type.VARTYPE ),
    new HiKeyVal( "octet"      , Highlight_Type.VARTYPE ),
    new HiKeyVal( "oneway"     , Highlight_Type.CONTROL ),
    new HiKeyVal( "out"        , Highlight_Type.CONTROL ),
    new HiKeyVal( "primarykey" , Highlight_Type.VARTYPE ),
    new HiKeyVal( "private"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "provides"   , Highlight_Type.CONTROL ),
    new HiKeyVal( "public"     , Highlight_Type.CONTROL ),
    new HiKeyVal( "publishes"  , Highlight_Type.CONTROL ),
    new HiKeyVal( "raises"     , Highlight_Type.CONTROL ),
    new HiKeyVal( "readonly"   , Highlight_Type.VARTYPE ),
    new HiKeyVal( "setraises"  , Highlight_Type.CONTROL ),
    new HiKeyVal( "sequence"   , Highlight_Type.CONTROL ),
    new HiKeyVal( "short"      , Highlight_Type.VARTYPE ),
    new HiKeyVal( "string"     , Highlight_Type.VARTYPE ),
    new HiKeyVal( "struct"     , Highlight_Type.VARTYPE ),
    new HiKeyVal( "supports"   , Highlight_Type.CONTROL ),
    new HiKeyVal( "switch"     , Highlight_Type.CONTROL ),
    new HiKeyVal( "TRUE"       , Highlight_Type.CONST   ),
    new HiKeyVal( "truncatable", Highlight_Type.CONTROL ),
    new HiKeyVal( "typedef"    , Highlight_Type.VARTYPE ),
    new HiKeyVal( "typeid"     , Highlight_Type.VARTYPE ),
    new HiKeyVal( "typeprefix" , Highlight_Type.VARTYPE ),
    new HiKeyVal( "unsigned"   , Highlight_Type.VARTYPE ),
    new HiKeyVal( "union"      , Highlight_Type.VARTYPE ),
    new HiKeyVal( "uses"       , Highlight_Type.CONTROL ),
    new HiKeyVal( "ValueBase"  , Highlight_Type.VARTYPE ),
    new HiKeyVal( "valuetype"  , Highlight_Type.VARTYPE ),
    new HiKeyVal( "void"       , Highlight_Type.VARTYPE ),
    new HiKeyVal( "wchar"      , Highlight_Type.VARTYPE ),
    new HiKeyVal( "wstring"    , Highlight_Type.VARTYPE )
  };
}

