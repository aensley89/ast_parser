package lexer;

import java.io.IOException;

/**
 * The Lexer class is responsible for scanning the source file which is a stream
 * of characters and returning a stream of tokens; each token object will
 * contain the string (or access to the string) that describes the token along
 * with an indication of its location in the source program to be used for error
 * reporting; we are tracking line numbers; white spaces are space, tab,
 * newlines
 */
public class Lexer {

    private boolean atEOF = false;
    private char ch;     // next character to process
    private SourceReader source;

    // positions in line of current token
    private int startPosition, endPosition;

    public Lexer(String sourceFile) throws Exception {
        new TokenType();  // init token table
        source = new SourceReader(sourceFile);
        ch = source.read();
    }

    public static void main(String args[]) {
        Token tok;
        try {

            //Read from the command line for the filename
            Lexer lex = new Lexer(args[0]);

            while (true) {
                tok = lex.nextToken();

                //if token returns error token we print the line for error and exit
                if (tok.getKind() == Tokens.Error) {
                    System.out.println("Error in Line " + lex.source.getLineno() + " ErrorValue: " + tok.toString());
                    System.exit(0);
                }
                //Printing the information about the line and the token
                String p = "Left: " + tok.getLeftPosition()
                        + "| Right: " + tok.getRightPosition() + "| Type: "
                        + TokenType.tokens.get(tok.getKind()) + "| Value: ";
                // if ((tok.getKind() == Tokens.Identifier) || (tok.getKind() == Tokens.INTeger))
                p += tok.toString();
                System.out.println(p + "| Line: " + lex.source.getLineno());
            }
        } catch (Exception e) {
        }
    }

    /**
     * newIdTokens are either ids or reserved words; new id's will be inserted
     * in the symbol table with an indication that they are id's
     *
     * @param id is the String just scanned - it's either an id or reserved word
     * @param startPosition is the column in the source file where the token
     * begins
     * @param endPosition is the column in the source file where the token ends
     * @return the Token; either an id or one for the reserved words
     */
    public Token newIdToken(String id, int startPosition, int endPosition) {
        return new Token(startPosition, endPosition, Symbol.symbol(id, Tokens.Identifier));
    }

    /**
     * number tokens are inserted in the symbol table; we don't convert the
     * numeric strings to numbers until we load the bytecodes for interpreting;
     * this ensures that any machine numeric dependencies are deferred until we
     * actually run the program; i.e. the numeric constraints of the hardware
     * used to compile the source program are not used
     *
     * @param number is the int String just scanned
     * @param startPosition is the column in the source file where the int
     * begins
     * @param endPosition is the column in the source file where the int ends
     * @return the int Token
     */
    public Token newNumberToken(String number, int startPosition, int endPosition) {
        return new Token(startPosition, endPosition,
                Symbol.symbol(number, Tokens.INTeger));
    }

    public Token newFloatToken(String number, int startPosition, int endPosition) {
        return new Token(startPosition, endPosition,
                Symbol.symbol(number, Tokens.Float));
    }

    public Token newSciToken(String number, int startPosition2, int endPosition2) {
        return new Token(startPosition, endPosition,
                Symbol.symbol(number, Tokens.ScientificN));
    }

    public Token newErrorToken(String number, int startPosition2, int endPosition2) {
        return new Token(startPosition, endPosition,
                Symbol.symbol(number, Tokens.Error));
    }

    public Token newCharToken(String number, int startPosition2, int endPosition2) {
        return new Token(startPosition, endPosition,
                Symbol.symbol(number, Tokens.Char));
    }

    /**
     * build the token for operators (+ -) or separators (parens, braces) filter
     * out comments which begin with two slashes
     *
     * @param s is the String representing the token
     * @param startPosition is the column in the source file where the token
     * begins
     * @param endPosition is the column in the source file where the token ends
     * @return the Token just found
     */
    public Token makeToken(String s, int startPosition, int endPosition) {
        if (s.equals("//")) {  // filter comment
            try {
                int oldLine = source.getLineno();
                do {
                    ch = source.read();
                } while (oldLine == source.getLineno());
            } catch (Exception e) {
                atEOF = true;
            }
            return nextToken();
        }
        Symbol sym = Symbol.symbol(s, Tokens.BogusToken); // be sure it's a valid token
        if (sym == null) {
            System.out.println("******** illegal character: " + s + " " + "Line: " + source.getLineno());
            atEOF = true;
            return nextToken();
        }
        return new Token(startPosition, endPosition, sym);
    }

    /**
     * @return the next Token found in the source file
     */
    public Token nextToken() { // ch is always the next char to process
        if (atEOF) {
            if (source != null) {
                source.close();
                source = null;
            }
            return null;
        }
        try {
            while (Character.isWhitespace(ch)) {  // scan past whitespace
                ch = source.read();
            }
        } catch (Exception e) {
            atEOF = true;
            return nextToken();
        }
        startPosition = source.getPosition();
        endPosition = startPosition - 1;

        if (Character.isJavaIdentifierStart(ch)) {
            // return tokens for ids and reserved words
            String id = "";
            try {
                do {
                    endPosition++;
                    id += ch;
                    ch = source.read();
                } while (Character.isJavaIdentifierPart(ch));
            } catch (Exception e) {
                atEOF = true;
            }
            return newIdToken(id, startPosition, endPosition);

        }

        //This block is a translation of state diagram that I drew
        //case number starts with digit
        if (Character.isDigit(ch)) {
            try {
                String number = "";

                //Read the second item
                endPosition++;
                number += ch;
                ch = source.read();
	        	//check if the second item is digit, decimal or neither

                //second item is digit
                if (Character.isDigit(ch)) {
                    //move to section 1 then checke for decimal
                    do {
                        endPosition++;
                        number += ch;
                        ch = source.read();
                    } while (Character.isDigit(ch));

                    if (ch == '.') {

                        do {
                            endPosition++;
                            number += ch;
                            ch = source.read();

                        } while (Character.isDigit(ch));

                        return newFloatToken(number, startPosition, endPosition);
                    } else {
                        return newNumberToken(number, startPosition, endPosition);
                    }

                } //second item is decimal
                else if (ch == '.') {
                    //move to section 2 check for scientific notaiton
                    do {
                        endPosition++;
                        number += ch;
                        ch = source.read();
                    } while (Character.isDigit(ch));

                    if (ch == 'E' || ch == 'e') {
                        //move to scientific notation
                        endPosition++;
                        number += ch;
                        ch = source.read();

                        if (ch == '+' || ch == '-') {
                            endPosition++;
                            number += ch;
                            ch = source.read();

                            if (Character.isDigit(ch)) {
                                do {
                                    endPosition++;
                                    number += ch;
                                    ch = source.read();
                                } while (Character.isDigit(ch));

                                return newSciToken(number, startPosition, endPosition);
                            } else {
                                return newErrorToken(number, startPosition, endPosition);
                            }

                        } else if (Character.isDigit(ch)) {
                            do {
                                endPosition++;
                                number += ch;
                                ch = source.read();
                            } while (Character.isDigit(ch));

                            return newSciToken(number, startPosition, endPosition);
                        } else {
                            return newErrorToken(number, startPosition, endPosition);
                        }

                    } else {
                        return newFloatToken(number, startPosition, endPosition);
                    }

                } //second item is none
                else {
                    return newNumberToken(number, startPosition, endPosition);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //case number starts with decimal  
        if (ch == '.') {
            try {
                String number = "";
                endPosition++;
                number += ch;
                ch = source.read();

                //digit followed by decimal
                if (Character.isDigit(ch)) {
                    do {
                        endPosition++;
                        number += ch;
                        ch = source.read();
                    } while (Character.isDigit(ch));

                    return newFloatToken(number, startPosition, endPosition);

                } else {
                    return newErrorToken(number, startPosition, endPosition);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Check for char, if it is followed by '
        if (ch == '\'') {

            try {
                String character = "";
                //Read the second item
                endPosition++;
                character += ch;
                ch = source.read();

                if (ch == '\'') //quote followed by qoute is error
                {
                    return newErrorToken(character, startPosition, endPosition);
                } else {
                    endPosition++;
                    character += ch;
                    ch = source.read();

                    if (ch == '\'') //quote followed by a char followed by a qoute
                    {
                        endPosition++;
                        character += ch;
                        ch = source.read();
                        return newCharToken(character, startPosition, endPosition);
                    } else //quote followed by no closing qoute after 1 letter throws an error token
                    {
                        return newErrorToken(character, startPosition, endPosition);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        // At this point the only tokens to check for are one or two
        // characters; we must also check for comments that begin with
        // 2 slashes
        String charOld = "" + ch;
        String op = charOld;
        Symbol sym;
        try {
            endPosition++;
            ch = source.read();
            op += ch;
            // check if valid 2 char operator; if it's not in the symbol
            // table then don't insert it since we really have a one char
            // token
            sym = Symbol.symbol(op, Tokens.BogusToken);
            if (sym == null) {  // it must be a one char token
                return makeToken(charOld, startPosition, endPosition);//return error token
            }
            endPosition++;
            ch = source.read();
            return makeToken(op, startPosition, endPosition);
        } catch (Exception e) {
        }
        atEOF = true;
        if (startPosition == endPosition) {
            op = charOld;
        }
        return makeToken(op, startPosition, endPosition);
    }

}
