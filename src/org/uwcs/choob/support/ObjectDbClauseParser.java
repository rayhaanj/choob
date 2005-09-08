/* Generated By:JavaCC: Do not edit this line. ObjectDbClauseParser.java */
        package org.uwcs.choob.support;
        import java.util.Map;
        import java.util.List;
        import java.util.ArrayList;
        import java.util.HashMap;
        import java.util.Iterator;
        import java.io.StringReader;

        public class ObjectDbClauseParser implements ObjectDbClauseParserConstants {
                private Map nameMap = new HashMap();
                private int joins = 0;
                private List joinList = new ArrayList();
                private String sortOrder = "";
                private String limitTo = "";

                public static void main(String args[])
                {
                        try
                        {
                                for(int i=0; i<args.length; i++)
                                        System.out.println(getSQL(args[i], null));
                        }
                        catch (ParseException e)
                        {
                                System.out.println("Parse exception: " + e);
                                e.printStackTrace();
                        }
                }

                public static String getSQL(String clause, String className) throws ParseException {
                        ObjectDbClauseParser parser = new ObjectDbClauseParser (new StringReader(clause));

                        String whereClause = parser.ClauseList();
                        StringBuffer joinText = new StringBuffer();
                        Iterator l = parser.joinList.listIterator();
                        int i = 0;
                        while( l.hasNext() )
                        {
                                String fieldName = (String)l.next();
                                joinText.append("INNER JOIN ObjectStoreData o" + i + " ON ObjectStore.ObjectID = o" + i + ".ObjectID AND o" + i + ".FieldName = \"" + fieldName + "\" ");
                                i++;
                        }
                        String classQuery = (className == null) ? "" : " AND ClassName = \"" + className + "\" ";
                        return joinText.toString() + "WHERE " + whereClause + classQuery + parser.sortOrder + parser.limitTo;
                }

                public String getFieldName(String name)
                {
                        String realName = (String)nameMap.get(name.toLowerCase());
                        if (realName != null)
                                return realName;
                        // Need to add!
                        realName = "o"+joins+".FieldValue";
                        joinList.add(name.toLowerCase());
                        joins++;
                        nameMap.put(name.toLowerCase(), realName);
                        return realName;
                }

/*
	ClauseList() = Clause() ClauseExtra()
*/
  final public String ClauseExtra() throws ParseException {
                String s;
                StringBuffer t = new StringBuffer();
    label_1:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case AND:
      case OR:
        ;
        break;
      default:
        jj_la1[0] = jj_gen;
        break label_1;
      }
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case AND:
        jj_consume_token(AND);
        s = Clause();
                                 t.append(" AND " + s);
        break;
      case OR:
        jj_consume_token(OR);
        s = Clause();
                                t.append(" OR " + s);
        break;
      default:
        jj_la1[1] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
    }
                        {if (true) return t.toString();}
    throw new Error("Missing return statement in function");
  }

  final public String ClauseList() throws ParseException {
                String s, t;
    s = Clause();
    t = ClauseExtra();
                        {if (true) return s + t;}
    throw new Error("Missing return statement in function");
  }

  final public String Clause() throws ParseException {
                java.util.Vector list;
                String s;
                Token t;
                int i;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case NOT:
      jj_consume_token(NOT);
      s = Clause();
                        {if (true) return "NOT " + s;}
      break;
    case _NAME:
      t = jj_consume_token(_NAME);
      s = OperatorAndValue(t.image);
                        {if (true) return s;}
      break;
    case _OPENBRACKET:
      jj_consume_token(_OPENBRACKET);
      s = ClauseList();
      jj_consume_token(_CLOSEBRACKET);
                        {if (true) return "(" + s + ")";}
      break;
    case SORT:
      jj_consume_token(SORT);
      ParseSort();
                  {if (true) return "1";}
      break;
    case LIMIT:
      jj_consume_token(LIMIT);
      ParseLimit();
                  {if (true) return "1";}
      break;
    default:
      jj_la1[2] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    throw new Error("Missing return statement in function");
  }

  final public String OperatorAndValue(String name) throws ParseException {
                String realName = getFieldName(name);
                Token s, t;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case NUMOP:
      s = jj_consume_token(NUMOP);
      t = jj_consume_token(_NUMVALUE);
                        {if (true) return realName + " " + s.image + " " + t.image;}
      break;
    case GENOP:
      s = jj_consume_token(GENOP);
      t = jj_consume_token(_GENVALUE);
                        {if (true) return realName + " " + s.image + " " + t.image;}
      break;
    case RLIKE:
      jj_consume_token(RLIKE);
      s = jj_consume_token(_TEXTVALUE);
                        {if (true) return realName + " REGEXP " + s.image;}
      break;
    case LIKE:
      jj_consume_token(LIKE);
      s = jj_consume_token(_TEXTVALUE);
                        {if (true) return realName + " LIKE " + s.image;}
      break;
    default:
      jj_la1[3] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    throw new Error("Missing return statement in function");
  }

  final public void ParseSort() throws ParseException {
                Token t;
                String order = "ASC";
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case ASC:
    case DESC:
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case ASC:
        jj_consume_token(ASC);

        break;
      case DESC:
        jj_consume_token(DESC);
                                 order = "DESC";
        break;
      default:
        jj_la1[4] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
      break;
    default:
      jj_la1[5] = jj_gen;
      ;
    }
    t = jj_consume_token(SORT_NAME);
                        sortOrder = " ORDER BY " + getFieldName(t.image) + " " + order;
  }

  final public void ParseLimit() throws ParseException {
                Token t1, t2;
    jj_consume_token(LIMIT_OPENBRACKET);
    t1 = jj_consume_token(LIMIT_NUMVALUE);
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case LIMIT_COMMA:
      jj_consume_token(LIMIT_COMMA);
      t2 = jj_consume_token(LIMIT_NUMVALUE);
      jj_consume_token(LIMIT_CLOSEBRACKET);
                                limitTo = " LIMIT " + t1.image + ", " + t2.image;
      break;
    case LIMIT_CLOSEBRACKET:
      jj_consume_token(LIMIT_CLOSEBRACKET);
                                limitTo = " LIMIT " + t1.image;
      break;
    default:
      jj_la1[6] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
  }

  public ObjectDbClauseParserTokenManager token_source;
  SimpleCharStream jj_input_stream;
  public Token token, jj_nt;
  private int jj_ntk;
  private int jj_gen;
  final private int[] jj_la1 = new int[7];
  static private int[] jj_la1_0;
  static {
      jj_la1_0();
   }
   private static void jj_la1_0() {
      jj_la1_0 = new int[] {0x30000000,0x30000000,0x1f00,0x7800000,0x6000,0x6000,0xc0000,};
   }

  public ObjectDbClauseParser(java.io.InputStream stream) {
    jj_input_stream = new SimpleCharStream(stream, 1, 1);
    token_source = new ObjectDbClauseParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 7; i++) jj_la1[i] = -1;
  }

  public void ReInit(java.io.InputStream stream) {
    jj_input_stream.ReInit(stream, 1, 1);
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 7; i++) jj_la1[i] = -1;
  }

  public ObjectDbClauseParser(java.io.Reader stream) {
    jj_input_stream = new SimpleCharStream(stream, 1, 1);
    token_source = new ObjectDbClauseParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 7; i++) jj_la1[i] = -1;
  }

  public void ReInit(java.io.Reader stream) {
    jj_input_stream.ReInit(stream, 1, 1);
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 7; i++) jj_la1[i] = -1;
  }

  public ObjectDbClauseParser(ObjectDbClauseParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 7; i++) jj_la1[i] = -1;
  }

  public void ReInit(ObjectDbClauseParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 7; i++) jj_la1[i] = -1;
  }

  final private Token jj_consume_token(int kind) throws ParseException {
    Token oldToken;
    if ((oldToken = token).next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    if (token.kind == kind) {
      jj_gen++;
      return token;
    }
    token = oldToken;
    jj_kind = kind;
    throw generateParseException();
  }

  final public Token getNextToken() {
    if (token.next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    jj_gen++;
    return token;
  }

  final public Token getToken(int index) {
    Token t = token;
    for (int i = 0; i < index; i++) {
      if (t.next != null) t = t.next;
      else t = t.next = token_source.getNextToken();
    }
    return t;
  }

  final private int jj_ntk() {
    if ((jj_nt=token.next) == null)
      return (jj_ntk = (token.next=token_source.getNextToken()).kind);
    else
      return (jj_ntk = jj_nt.kind);
  }

  private java.util.Vector jj_expentries = new java.util.Vector();
  private int[] jj_expentry;
  private int jj_kind = -1;

  public ParseException generateParseException() {
    jj_expentries.removeAllElements();
    boolean[] la1tokens = new boolean[30];
    for (int i = 0; i < 30; i++) {
      la1tokens[i] = false;
    }
    if (jj_kind >= 0) {
      la1tokens[jj_kind] = true;
      jj_kind = -1;
    }
    for (int i = 0; i < 7; i++) {
      if (jj_la1[i] == jj_gen) {
        for (int j = 0; j < 32; j++) {
          if ((jj_la1_0[i] & (1<<j)) != 0) {
            la1tokens[j] = true;
          }
        }
      }
    }
    for (int i = 0; i < 30; i++) {
      if (la1tokens[i]) {
        jj_expentry = new int[1];
        jj_expentry[0] = i;
        jj_expentries.addElement(jj_expentry);
      }
    }
    int[][] exptokseq = new int[jj_expentries.size()][];
    for (int i = 0; i < jj_expentries.size(); i++) {
      exptokseq[i] = (int[])jj_expentries.elementAt(i);
    }
    return new ParseException(token, exptokseq, tokenImage);
  }

  final public void enable_tracing() {
  }

  final public void disable_tracing() {
  }

        }
