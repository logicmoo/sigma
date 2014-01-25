/* This code is copyright Articulate Software (c) 2003.  Some
portions copyright Teknowledge (c) 2003 and reused under the terms of
the GNU license.  This software is released under the GNU Public
License <http://www.gnu.org/copyleft/gpl.html>.  Users of this code
also consent, by use of this code, to credit Articulate Software and
Teknowledge in any writings, briefings, publications, presentations,
or other representations of any software which incorporates, builds
on, or uses this code.  Please cite the following article in any
publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, in
Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed
Systems, August 9, Acapulco, Mexico. See also http://sigmakee.sourceforge.net
*/

package com.articulate.sigma;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.articulate.sigma.KB;

/** ************************************************************
 * Handle operations on an individual formula.  This includes
 * formatting for presentation as well as pre-processing for sending
 * to the inference engine.
 */
public class Formula implements Comparable {

    protected static final String AND    = "and";
    protected static final String OR     = "or";
    protected static final String NOT    = "not";
    protected static final String IF     = "=>";
    protected static final String IFF    = "<=>";
    protected static final String UQUANT = "forall";
    protected static final String EQUANT = "exists";
    protected static final String EQUAL  = "equal";
    protected static final String GT     = "greaterThan";
    protected static final String GTET   = "greaterThanOrEqualTo";
    protected static final String LT     = "lessThan";
    protected static final String LTET   = "lessThanOrEqualTo";

    protected static final String KAPPAFN  = "KappaFn";
    protected static final String PLUSFN   = "AdditionFn";
    protected static final String MINUSFN  = "SubtractionFn";
    protected static final String TIMESFN  = "MultiplicationFn";
    protected static final String DIVIDEFN = "DivisionFn";
    protected static final String SKFN     = "SkFn";
    protected static final String SK_PREF = "Sk";
    protected static final String FN_SUFF = "Fn";
    protected static final String V_PREF  = "?";
    protected static final String R_PREF  = "@";
    protected static final String VX      = "?X";
    protected static final String VVAR    = "?VAR";
    protected static final String RVAR    = "@ROW";

    protected static final String LP = "(";
    protected static final String RP = ")";
    protected static final String SPACE = " ";

    protected static final String LOG_TRUE  = "True";
    protected static final String LOG_FALSE = "False";

    /** The SUO-KIF logical operators. */
    public static final List<String> LOGICAL_OPERATORS = Arrays.asList(UQUANT,
                                                                        EQUANT,
                                                                        AND,
                                                                        OR,
                                                                        NOT,
                                                                        IF,
                                                                        IFF);

    /** SUO-KIF mathematical comparison predicates. */
    private static final List<String> COMPARISON_OPERATORS = Arrays.asList(EQUAL,
                                                                           GT,
                                                                           GTET,
                                                                           LT,
                                                                           LTET);

    /** The SUO-KIF mathematical functions are implemented in Vampire. */
    private static final List<String> MATH_FUNCTIONS = Arrays.asList(PLUSFN,
                                                                     MINUSFN,
                                                                     TIMESFN,
                                                                     DIVIDEFN);

    public static final List<String> DOC_PREDICATES = Arrays.asList("documentation",
                                                                    "comment",
                                                                    "format" //,
                                                                    // "termFormat"
                                                                    );
    /** The source file in which the formula appears. */
    protected String sourceFile;

    /** The line in the file on which the formula starts. */
    public int startLine;

    /** The line in the file on which the formula ends. */
    public int endLine;

    /** The length of the file in bytes at the position immediately
     *  after the end of the formula.  This value is used only for
     *  formulas entered via KB.tell().  In general, you should not
     *  count on it being set to a value other than -1L.
     */
    public long endFilePosition = -1L;
	public ArrayList<String> errors = new ArrayList<String>();

    /** The formula. */
    public String theFormula;
	
    public static final String termMentionSuffix  = "__m";
    public static final String classSymbolSuffix  = "__t";  // for the case when a class is used as an instance
    public static final String termSymbolPrefix   = "s__";
    public static final String termVariablePrefix = "V__";

    /** ***************************************************************
     *  Returns the platform-specific line separator String
     */
    public String getLineSeparator() {
        return System.getProperty("line.separator");
    }

    public String getSourceFile() {
        return this.sourceFile;
    }

    public void setSourceFile(String filename) {
        this.sourceFile = filename;
        return;
    }

	public ArrayList<String> getErrors() {
		return this.errors;
	}
	
    /** ***************************************************************
     * Should be false if this Formula occurs in and was loaded from
     * sourceFile.  Should be true if this Formula does not actually
     * occur in sourceFile but was computed (derived) from at least
     * some Formulae in sourceFile, possibly in combination with other
     * Formulae not is sourceFile.
     */
    private boolean isComputed = false;

    /** ***************************************************************
     * Should return false if this Formula occurs in and was loaded
     * from sourceFile.  Should return true if this Formula does not
     * actually occur in sourceFile but was computed (derived) from at
     * least some Formulae in sourceFile, possibly in combination with
     * other Formulae not is sourceFile.
     */
    public boolean getIsComputed() {
        return isComputed;
    }

    /** ***************************************************************
     * Sets the value of isComputed to val.
     */
    public void setIsComputed(boolean val) {
        isComputed = val;
        return;
    }

    /** ***************************************************************
     * A list of TPTP formulas (Strings) that together constitute the
     * translation of theFormula.  This member is a List, because
     * predicate variable instantiation and row variable expansion
     * might cause theFormula to expand to several TPTP formulas.
     */
    public ArrayList<String> theTptpFormulas = null;

    /** ***************************************************************
     * Returns an ArrayList of the TPTP formulas (Strings) that
     * together constitute the TPTP translation of theFormula.
     *
     * @return An ArrayList of Strings, or an empty ArrayList if no
     * translations have been created or entered.
     */
    public ArrayList<String> getTheTptpFormulas() {

        if (theTptpFormulas == null)
            theTptpFormulas = new ArrayList<String>();
        return theTptpFormulas;
    }

    /** ***************************************************************
     * Clears theTptpFormulas if the ArrayList exists, else does
     * nothing.
     */
    public void clearTheTptpFormulas() {

        if (theTptpFormulas != null)
            theTptpFormulas.clear();
        return;
    }

    /** *****************************************************************
     * A list of clausal (resolution) forms generated from this
     * Formula.
     */
    private ArrayList theClausalForm = null;

    /** *****************************************************************
     */
	public Formula(Formula f) {
		this.endLine = f.endLine;
		this.startLine = f.startLine;
		this.sourceFile = f.sourceFile.intern();
		this.theFormula = f.theFormula.intern();
	}
	
    /** *****************************************************************
     */
	public Formula() {
	}	
	
    /** ***************************************************************
     * Returns a List of the clauses that together constitute the
     * resolution form of this Formula.  The list could be empty if
     * the clausal form has not yet been computed.
     *
     * @return ArrayList
     */
    public ArrayList getTheClausalForm() {

        if (theClausalForm == null) {
            if (!StringUtil.emptyString(theFormula))
                theClausalForm = Clausifier.toNegAndPosLitsWithRenameInfo(this);
        }
        return theClausalForm;
    }

    /** ***************************************************************
     * This method clears the list of clauses that together constitute
     * the resolution form of this Formula, and can be used in
     * preparation for recomputing the clauses.
     */
    public void clearTheClausalForm() {

        if (theClausalForm != null)
            theClausalForm.clear();
        theClausalForm = null;
    }

    /** ***************************************************************
     * Returns a List of List objects.  Each such object contains, in
     * turn, a pair of List objects.  Each List object in a pair
     * contains Formula objects.  The Formula objects contained in the
     * first List object (0) of a pair represent negative literals
     * (antecedent conjuncts).  The Formula objects contained in the
     * second List object (1) of a pair represent positive literals
     * (consequent conjuncts).  Taken together, all of the clauses
     * constitute the resolution form of this Formula.
     *
     * @return A List of Lists.
     */
    public ArrayList getClauses() {

        ArrayList clausesWithVarMap = getTheClausalForm();
        if ((clausesWithVarMap == null) || clausesWithVarMap.isEmpty())
            return null;
        return (ArrayList) clausesWithVarMap.get(0);
    }

    /** ***************************************************************
     * Returns a map of the variable renames that occurred during the
     * translation of this Formula into the clausal (resolution) form
     * accessible via this.getClauses().
     *
     * @return A Map of String (SUO-KIF variable) key-value pairs.
     */
    public HashMap getVarMap() {

        ArrayList clausesWithVarMap = getTheClausalForm();
        if ((clausesWithVarMap == null) || (clausesWithVarMap.size() < 3))
            return null;
        return (HashMap) clausesWithVarMap.get(2);
    }

    /** ***************************************************************
     * Returns the variable in this Formula that corresponds to the
     * clausal form variable passed as input.
     *
     * @return A SUO-KIF variable (String), which may be just the
     * input variable.
     
    public String getOriginalVar(String var) {

        Map varmap = getVarMap();
        if (varmap == null)
            return var;
        return Clausifier.getOriginalVar(var, varmap);
    }
*/
    /** ***************************************************************
     * This constant indicates the maximum predicate arity supported
     * by the current implementation of Sigma.
     */
    protected static final int MAX_PREDICATE_ARITY = 7;

    /** ***************************************************************
     * Read a String into the variable 'theFormula'.
     */
    public void read(String s) {
        theFormula = s;
    }

    /** ***************************************************************
     */
    public static String integerToPaddedString(int i, int digits) {

        String result = Integer.toString(i);
        while (result.length() < digits) {
            result = "0" + result;
        }
        return result;
    }

    /** ***************************************************************
    *  @return a unique ID by appending the hashCode() of the
    *  formula String to the file name in which it appears
     */
    public String createID() {

        String fname = sourceFile;
        if (!StringUtil.emptyString(fname) && fname.lastIndexOf(File.separator) > -1)
            fname = fname.substring(fname.lastIndexOf(File.separator)+1);
        int hc = theFormula.hashCode();
        String result = null;
        if (hc < 0)
            result = "N" + (new Integer(hc)).toString().substring(1) + fname;
        else
            result = (new Integer(hc)).toString() + fname;
        return result;
    }

    /** ***************************************************************
     * Copy the Formula.  This is in effect a deep copy.
     */
    private Formula copy() {

        Formula result = new Formula();
        if (sourceFile != null)
            result.sourceFile = sourceFile.intern();
        result.startLine = startLine;
        result.endLine = endLine;
        if (theFormula != null)
            result.theFormula = theFormula.intern();
        return result;
    }

    /** ***************************************************************
     */
    private Formula deepCopy() {
        return copy();
    }

    /** ***************************************************************
     * Implement the Comparable interface by defining the compareTo
     * method.  Formulas are equal if their formula strings are equal.
     */
    public int compareTo(Object f) throws ClassCastException {
    	
        if (!f.getClass().getName().equalsIgnoreCase("com.articulate.sigma.Formula"))
            throw new ClassCastException("Error in Formula.compareTo(): "
                                         + "Class cast exception for argument of class: "
                                         + f.getClass().getName());
        return theFormula.compareTo(((Formula) f).theFormula);
    }

    /** ***************************************************************
     * Returns true if the Formula contains no unbalanced parentheses
     * or unbalanced quote characters, otherwise returns false.
     *
     * @return boolean
     */
    public boolean isBalancedList() {
    	
        boolean ans = false;
        if (this.listP()) {
            if (this.empty())
                ans = true;
            else {
                String input = this.theFormula.trim();
                List quoteChars = Arrays.asList('"', '\'');
                int i = 0;
                int len = input.length();
                int end = len - 1;
                int pLevel = 0;
                int qLevel = 0;
                char prev = '0';
                char ch = prev;
                boolean insideQuote = false;
                char quoteCharInForce = '0';
                while (i < len) {
                    ch = input.charAt(i);
                    if (!insideQuote) {
                        if (ch == '(')
                            pLevel++;
                        else if (ch == ')')
                            pLevel--;
                        else if (quoteChars.contains(ch) && (prev != '\\')) {
                            insideQuote = true;
                            quoteCharInForce = ch;
                            qLevel++;
                        }
                    }
                    else if (quoteChars.contains(ch)
                             && (ch == quoteCharInForce)
                             && (prev != '\\')) {
                        insideQuote = false;
                        quoteCharInForce = '0';
                        qLevel--;
                    }
                    prev = ch;
                    i++;
                }
                ans = ((pLevel == 0) && (qLevel == 0));
            }
        }
        return ans;
    }

    /** ***************************************************************
     * @return the LISP 'car' of the formula as a String - the first
     * element of the list. Note that this operation has no side
     * effect on the Formula.
     *
     * Currently (10/24/2007) this method returns the empty string
     * ("") when invoked on an empty list.  Technically, this is
     * wrong.  In most LISPS, the car of the empty list is the empty
     * list (or nil).  But some parts of the Sigma code apparently
     * expect this method to return the empty string when invoked on
     * an empty list.
     */
    public String car() {

        String ans = null;
        if (this.listP()) {
            if (this.empty()) 
                // NS: Clean this up someday.
                ans = "";  // this.theFormula;            
            else {
                String input = this.theFormula.trim();
                StringBuilder sb = new StringBuilder();
                List quoteChars = Arrays.asList('"', '\'');
                int i = 1;
                int len = input.length();
                int end = len - 1;
                int level = 0;
                char prev = '0';
                char ch = prev;
                boolean insideQuote = false;
                char quoteCharInForce = '0';
                while (i < end) {
                    ch = input.charAt(i);
                    if (!insideQuote) {
                        if (ch == '(') {
                            sb.append(ch);
                            level++;
                        }
                        else if (ch == ')') {
                            sb.append(ch);
                            level--;
                            if (level <= 0) 
                                break;                            
                        }
                        else if (Character.isWhitespace(ch) && (level <= 0)) {
                            if (sb.length() > 0) 
                                break;                            
                        }
                        else if (quoteChars.contains(ch) && (prev != '\\')) {
                            sb.append(ch);
                            insideQuote = true;
                            quoteCharInForce = ch;
                        }
                        else 
                            sb.append(ch);                        
                    }
                    else if (quoteChars.contains(ch)
                             && (ch == quoteCharInForce)
                             && (prev != '\\')) {
                        sb.append(ch);
                        insideQuote = false;
                        quoteCharInForce = '0';
                        if (level <= 0) 
                            break;                        
                    }
                    else 
                        sb.append(ch);                    
                    prev = ch;
                    i++;
                }
                ans = sb.toString();
            }
        }
        return ans;
    }

    /** ***************************************************************
     * Return the LISP 'cdr' of the formula - the rest of a list minus its
     * first element.
     * Note that this operation has no side effect on the Formula.
     */
    public String cdr() {

        String ans = null;
        if (this.listP()) {
            if (this.empty()) 
                ans = this.theFormula;            
            else {
                String input = theFormula.trim();
                List quoteChars = Arrays.asList('"', '\'');
                int i = 1;
                int len = input.length();
                int end = len - 1;
                int level = 0;
                char prev = '0';
                char ch = prev;
                boolean insideQuote = false;
                char quoteCharInForce = '0';
                int carCount = 0;
                while (i < end) {
                    ch = input.charAt(i);
                    if (!insideQuote) {
                        if (ch == '(') {
                            carCount++;
                            level++;
                        }
                        else if (ch == ')') {
                            carCount++;
                            level--;
                            if (level <= 0) 
                                break;                            
                        }
                        else if (Character.isWhitespace(ch) && (level <= 0)) {
                            if (carCount > 0) 
                                break;                            
                        }
                        else if (quoteChars.contains(ch) && (prev != '\\')) {
                            carCount++;
                            insideQuote = true;
                            quoteCharInForce = ch;
                        }
                        else 
                            carCount++;                        
                    }
                    else if (quoteChars.contains(ch)
                             && (ch == quoteCharInForce)
                             && (prev != '\\')) {
                        carCount++;
                        insideQuote = false;
                        quoteCharInForce = '0';
                        if (level <= 0) 
                            break;                        
                    }
                    else 
                        carCount++;                    
                    prev = ch;
                    i++;
                }
                if (carCount > 0) {
                    int j = i + 1;
                    if (j < end) 
                        ans = "(" + input.substring(j, end).trim() + ")";                    
                    else 
                        ans = "()";                    
                }
            }
        }
        return ans;
    }

    /** ***************************************************************
     * Returns a new Formula which is the result of 'consing' a String
     * into this Formula, similar to the LISP procedure of the same
     * name.  This procedure is a little bit of a kluge, since this
     * Formula is treated simply as a LISP object (presumably, a LISP
     * list), and could be degenerate or malformed as a Formula.
     *
     * Note that this operation has no side effect on the original Formula.
     *
     * @param obj The String object that will become the 'car' (or
     * head) of the resulting Formula (list).
     *
     * @return a new Formula, or the original Formula if the cons fails.
     */
    public Formula cons(String obj) {

        Formula ans = this;
        String fStr = this.theFormula;
        if (!StringUtil.emptyString(obj) && !StringUtil.emptyString(fStr)) {
            String theNewFormula = null;
            if (this.listP()) {
                if (this.empty())
                    theNewFormula = ("(" + obj + ")");
                else
                    theNewFormula = ("(" + obj + " " + fStr.substring(1, (fStr.length() - 1)) + ")");
            }
            else
                // This should never happen during clausification, but
                // we include it to make this procedure behave
                // (almost) like its LISP namesake.
                theNewFormula = ("(" + obj + " . " + fStr + ")");
            if (theNewFormula != null) {
                ans = new Formula();
                ans.read(theNewFormula);
            }
        }
        return ans;
    }

    /** ***************************************************************
     * @return a new Formula, or the original Formula if the cons fails.
     */
    public Formula cons(Formula f) {

        return cons(f.theFormula);
    }

    /** ***************************************************************
     * Returns the LISP 'cdr' of the formula as a new Formula, if
     * possible, else returns null.
     *
     * Note that this operation has no side effect on the Formula.
     * @return a Formula, or null.
     */
    public Formula cdrAsFormula() {
    	
        String thisCdr = this.cdr();
        if (listP(thisCdr)) {
            Formula f = new Formula();
            f.read(thisCdr);
            return f;
        }
        return null;
    }

    /** ***************************************************************
     * Returns the LISP 'car' of the formula as a new Formula, if
     * possible, else returns null.
     *
     * Note that this operation has no side effect on the Formula.
     * @return a Formula, or null.
     */
    public Formula carAsFormula() {
    	
        String thisCar = this.car();
        if (listP(thisCar)) {
            Formula f = new Formula();
            f.read(thisCar);
            return f;
        }
        return null;
    }
    /** ***************************************************************
     * Returns the LISP 'cadr' (the second list element) of the
     * formula.
     *
     * Note that this operation has no side effect on the Formula.
     * @return a String, or the empty string if the is no cadr.
     */
    public String cadr() {
    	
        return this.getArgument(1);
    }

    /** ***************************************************************
     * Returns the LISP 'cddr' of the formula - the rest of the rest,
     * or the list minus its first two elements.
     *
     * Note that this operation has no side effect on the Formula.
     * @return a String, or null.
     */
    public String cddr() {
    	
        Formula fCdr = this.cdrAsFormula();
        if (fCdr != null) 
            return fCdr.cdr();        
        return null;
    }

    /** ***************************************************************
     * Returns the LISP 'cddr' of the formula as a new Formula, if
     * possible, else returns null.
     *
     * Note that this operation has no side effect on the Formula.
     *
     * @return a Formula, or null.
     */
    public Formula cddrAsFormula() {
    	
        String thisCddr = this.cddr();
        if (listP(thisCddr)) {
            Formula f = new Formula();
            f.read(thisCddr);
            return f;
        }
        return null;
    }

    /** ***************************************************************
     * Returns the LISP 'caddr' of the formula, which is the third
     * list element of the formula.
     *
     * Note that this operation has no side effect on the Formula.
     *
     * @return a String, or the empty string if there is no caddr.
     *
     */
    public String caddr() {
        return this.getArgument(2);
    }

    /** ***************************************************************
     * Returns the LISP 'append' of the formulas
     * Note that this operation has no side effect on the Formula.
     * @return a Formula
     */
    public Formula append(Formula f) {

        Formula newFormula = new Formula();
        newFormula.read(theFormula);
        if (newFormula.equals("") || newFormula.atom()) {
            System.out.println("Error in KB.append(): attempt to append to non-list: " + theFormula);
            return this;
        }
        if (f == null || f.theFormula == null || f.theFormula == "" || f.theFormula.equals("()"))
            return newFormula;
        f.theFormula = f.theFormula.trim();
        if (!f.atom())
            f.theFormula = f.theFormula.substring(1,f.theFormula.length()-1);
        int lastParen = theFormula.lastIndexOf(")");
        String sep = "";
        if (lastParen > 1)
            sep = " ";
        newFormula.theFormula = newFormula.theFormula.substring(0,lastParen) + sep + f.theFormula + ")";
        return newFormula;
    }

    /** ***************************************************************
     * Test whether the String is a LISP atom.
     */
    public static boolean atom(String s) {

        boolean ans = false;
        if (!StringUtil.emptyString(s)) {
            String str = s.trim();
            ans = (StringUtil.isQuotedString(s) ||
                  (!str.contains(")") && !str.matches(".*\\s.*")) );
        }
        return ans;
    }

    /** ***************************************************************
     * Test whether the Formula is a LISP atom.
     */
    public boolean atom() {

        return Formula.atom(theFormula);
    }

    /** ***************************************************************
     * Test whether the Formula is an empty list.
     */
    public boolean empty() {

        return Formula.empty(theFormula);
    }

    /** ***************************************************************
     * Test whether the String is an empty formula.  Not to be
     * confused with a null string or empty string.  There must be
     * parentheses with nothing or whitespace in the middle.
     */
    public static boolean empty(String s) {
        return (listP(s) && s.matches("\\(\\s*\\)"));
    }

    /** ***************************************************************
     * Test whether the Formula is a list.
     */
    public boolean listP() {

        return Formula.listP(theFormula);
    }

    /** ***************************************************************
     * Test whether the String is a list.
     */
    public static boolean listP(String s) {

        boolean ans = false;
        if (!StringUtil.emptyString(s)) {
            String str = s.trim();
            ans = (str.startsWith("(") && str.endsWith(")"));
        }
        return ans;
    }

    /** ***************************************************************
     * @see #validArgs() validArgs below for documentation
     */
    private String validArgsRecurse(Formula f, String filename, Integer lineNo) {

		if (f.theFormula == "" || !f.listP() || f.atom() || f.empty())
			return "";
        String pred = f.car();
        String rest = f.cdr();
        Formula restF = new Formula();
        restF.read(rest);
        int argCount = 0;
        while (!restF.empty()) {
            argCount++;
            String arg = restF.car();
            Formula argF = new Formula();
            argF.read(arg);
            String result = validArgsRecurse(argF, filename, lineNo);
            if (result != "")
                return result;
            restF.theFormula = restF.cdr();
        }
        String location = "";
        if ((filename != null) && (lineNo != null)) 
            location = "near line " + lineNo + " in " + filename;
        if (pred.equals(AND) || pred.equals(OR)) {
            if (argCount < 2) {            	
                String errString = "Too few arguments for 'and' or 'or'at " + location + ": " + f.toString();
                errors.add(errString);
                return errString;
            }
        }
        else if (pred.equals(UQUANT) || pred.equals(EQUANT)) {
            if (argCount != 2) {
                String errString = "Wrong number of arguments for quantifer at " + location + ":" + f.toString();
                errors.add(errString);
                return errString;
            }
            else {
                Formula quantF = new Formula();
                quantF.read(rest);
                if (!listP(quantF.car())) {
                    String errString = "No var list for quantifier at " + location + ":" + f.toString();
                    errors.add(errString);
                    return errString;
                }
            }
        }
        else if (pred.equals(IFF) || pred.equals(IF)) {
            if (argCount != 2) {
                String errString = "Wrong number of arguments for '<=>' or '=>' at " + location + ":" + f.toString();
                errors.add(errString);
                return errString;
            }
        }
        else if (pred.equals(EQUAL)) {
            if (argCount != 2) {
                String errString = "Wrong number of arguments for 'equals' at " + location + ":" + f.toString();
                errors.add(errString);
                return errString;
            }
        }
        else if (// !(isVariable(pred))
                 // &&
                 (KBmanager.getMgr().getPref("holdsPrefix").equalsIgnoreCase("yes")
                  && (argCount > (MAX_PREDICATE_ARITY + 1)))
                 ||
                 (!KBmanager.getMgr().getPref("holdsPrefix").equalsIgnoreCase("yes")
                  && (argCount > MAX_PREDICATE_ARITY))) {           
			String errString = "Maybe too many arguments at " + location + ": " + f.toString();
            errors.add(errString);
            return errString;        
        }
        return "";
    }

    /** ***************************************************************
     * Test whether the Formula uses logical operators and predicates
     * with the correct number of arguments.  "equals", "<=>", and
     * "=>" are strictly binary.  "or", and "and" are binary or
     * greater. "not" is unary.  "forall" and "exists" are unary with
     * an argument list.  Warn if we encounter a formula that has more
     * arguments than MAX_PREDICATE_ARITY.
     *
     * @param filename If not null, denotes the name of the file being
     * parsed.
     *
     * @param lineNo If not null, indicates the location of the
     * expression (formula) being parsed in the file being read.
     *
     * @return an empty String if there are no problems or an error message
     * if there are.
     */
    public String validArgs(String filename, Integer lineNo) {

        if (theFormula == null || theFormula == "")
            return "";
        Formula f = new Formula();
        f.read(theFormula);
        String result = validArgsRecurse(f, filename, lineNo);
        return result;
    }

    /** ***************************************************************
     * Test whether the Formula uses logical operators and predicates
     * with the correct number of arguments.  "equals", "<=>", and
     * "=>" are strictly binary.  "or", and "and" are binary or
     * greater. "not" is unary.  "forall" and "exists" are unary with
     * an argument list.  Warn if we encounter a formula that has more
     * arguments than MAX_PREDICATE_ARITY.
     *
     * @return an empty String if there are no problems or an error message
     * if there are.
     */
    public String validArgs() {
        return this.validArgs(null, null);
    }

    /** ***************************************************************
     * Not yet implemented!  Test whether the Formula has variables that are not properly
     * quantified.  The case tested for is whether a quantified variable
     * in the antecedent appears in the consequent or vice versa.
     *
     *  @return an empty String if there are no problems or an error message
     *  if there are.
     */
    public String badQuantification() {
        return "";
    }

    /** ***************************************************************
     * Parse a String into an ArrayList of Formulas. The String must be
     * a LISP-style list.
     */
    private ArrayList<Formula> parseList(String s) {

        ArrayList<Formula> result = new ArrayList<Formula>();
        Formula f = new Formula();
        f.read("(" + s + ")");
        if (f.empty())
            return result;
        while (!f.empty()) {
            String car = f.car();
            f.read(f.cdr());
            Formula newForm = new Formula();
            newForm.read(car);
            result.add(newForm);
        }
        return result;
    }

    /** ***************************************************************
     * Compare two lists of formulas, testing whether they are equal,
     * without regard to order.  (B A C) will be equal to (C B A). The
     * method iterates through one list, trying to find a match in the other
     * and removing it if a match is found.  If the lists are equal, the
     * second list should be empty once the iteration is complete.
     * Note that the formulas being compared must be lists, not atoms, and
     * not a set of formulas unenclosed by parentheses.  So, "(A B C)"
     * and "(A)" are valid, but "A" is not, nor is "A B C".
     */
    private boolean compareFormulaSets(String s) {
    	
        ArrayList<Formula> thisList = parseList(this.theFormula.substring(1,this.theFormula.length()-1));
        ArrayList<Formula> sList = parseList(s.substring(1,s.length()-1));
        if (thisList.size() != sList.size())
            return false;
        for (int i = 0; i < thisList.size(); i++) {
            for (int j = 0; j < sList.size(); j++) {
                if ((thisList.get(i)).logicallyEquals((sList.get(j)).theFormula)) {
                    sList.remove(j);
                    j = sList.size();
                }
            }
        }
        return sList.size() == 0;
    }

    /** ***************************************************************
     * Test if the contents of the formula are equal to the argument
     * at a deeper level than a simple string equals.  The only logical
     * manipulation is to treat conjunctions and disjunctions as unordered
     * bags of clauses. So (and A B C) will be logicallyEqual(s) for example,
     * to (and B A C).  Note that this is a fairly time-consuming operation
     * and should not generally be used for comparing large sets of formulas.
     */
    public boolean logicallyEquals(String s) {

        if (this.equals(s))
            return true;
        if (Formula.atom(s) && s.compareTo(theFormula) != 0)
            return false;

        Formula form = new Formula();
        form.read(this.theFormula);
        Formula sform = new Formula();
        sform.read(s);

        if (form.car().intern() == "and" || form.car().intern() == "or") {
            if (sform.car().intern() != sform.car().intern())
                return false;
            form.read(form.cdr());
            sform.read(sform.cdr());
            return form.compareFormulaSets(sform.theFormula);
        }
        else {
            Formula newForm = new Formula();
            newForm.read(form.car());
            Formula newSform = new Formula();
            newSform.read(sform.cdr());
            return newForm.logicallyEquals(sform.car()) &&
                newSform.logicallyEquals(form.cdr());
        }
    }

    /** ***************************************************************
     * If equals is overridedden, hashCode must use the same
     * "significant" fields.
     */
    public int hashCode() {

        String thisString = Clausifier.normalizeVariables(this.theFormula).trim();
        return (thisString.hashCode());
    }

    /** ***************************************************************
     * Test if the contents of the formula are equal to the
     * argument. Normalize all variables.
     */
	public boolean equals(Formula f) {

        String thisString = Clausifier.normalizeVariables(this.theFormula).trim();
        String argString = Clausifier.normalizeVariables(f.theFormula).trim();
        return (thisString.equals(argString));
    }

    /** ***************************************************************
     * Test if the contents of the formula are equal to the String argument.
     * Normalize all variables.
     */
    public boolean equals(String s) {

        String f = theFormula;
        Formula form = new Formula();
        Formula sform = new Formula();

        form.theFormula = f;
        s = Clausifier.normalizeVariables(s).intern();
        sform.read(s);
        s = sform.toString().trim().intern();

        form.theFormula = Clausifier.normalizeVariables(theFormula);
        f = form.toString().trim().intern();
        return (f == s);
    }

    /** ***************************************************************
     * Test if the contents of the formula are equal to the argument.
     */
    public boolean deepEquals(Formula f) {

        return (f.theFormula.intern() == theFormula.intern()) &&
            (f.sourceFile.intern() == sourceFile.intern());
    }

    /** ***************************************************************
     * Return the numbered argument of the given formula.  The first
     * element of a formula (i.e. the predicate position) is number 0.
     * Returns the empty string if there is no such argument position.
     */
    public String getArgument(int argnum) {

        String ans = "";
        Formula form = new Formula();
        form.read(theFormula);
        for (int i = 0 ; form.listP() ; i++) {
            ans = form.car();
            if (i == argnum) break;
            form.read(form.cdr());
        }
        if (ans == null) ans = ""; 
        return ans;
    }

    /** ***************************************************************
     * Returns a non-negative int value indicating the top-level list
     * length of this Formula if it is a proper listP(), else returns
     * -1.  One caveat: This method assumes that neither null nor the
     * empty string are legitimate list members in a wff.  The return
     * value is likely to be wrong if this assumption is mistaken.
     *
     * @return A non-negative int, or -1.
     */
    public int listLength() {
    	
        int ans = -1;
        if (this.listP()) {
            ans = 0;
            while (!StringUtil.emptyString(this.getArgument(ans)))
                ++ans;
        }
        return ans;
    }

    /** ***************************************************************
     * Return all the arguments in a simple formula as a list, starting
     * at the given argument.  If formula is complex (i.e. an argument
     * is a function or sentence), then return null.  If the starting
     * argument is greater than the number of arguments, also return
     * null.
     */
    public ArrayList<String> argumentsToArrayList(int start) {

        if (theFormula.indexOf('(',1) != -1)
            return null;
        int index = start;
        ArrayList<String> result = new ArrayList<String>();
        String arg = getArgument(index);
        while (arg != null && arg != "" && arg.length() > 0) {
            result.add(arg.intern());
            index++;
            arg = getArgument(index);
        }
        if (index == start)
            return null;
        return result;
    }

    /** ***************************************************************
     * Translate SUMO inequalities to the typical inequality symbols that
     * some theorem provers require.
     */
    private static String translateInequalities(String s) {

        if (s.equalsIgnoreCase("greaterThan")) return ">";
        if (s.equalsIgnoreCase("greaterThanOrEqualTo")) return ">=";
        if (s.equalsIgnoreCase("lessThan")) return "<";
        if (s.equalsIgnoreCase("lessThanOrEqualTo")) return "<=";
        return "";
    }

    /** ***************************************************************
     * Collects all variables in this Formula.  Returns an ArrayList
     * containing a pair of ArrayLists.  The first contains all
     * explicitly quantified variables in the Formula.  The second
     * contains all variables in Formula that are not within the scope
     * of some explicit quantifier.
     *
     * @return An ArrayList containing two ArrayLists, each of which
     * could be empty
     */
    public ArrayList<ArrayList<String>> collectVariables() {

        ArrayList<ArrayList<String>> ans = new ArrayList<ArrayList<String>>();
        ans.add(new ArrayList());
        ans.add(new ArrayList());
    	HashSet<String> quantified = new HashSet<String>();
    	HashSet<String> unquantified = new HashSet<String>();
        unquantified.addAll(collectAllVariables());
        quantified.addAll(collectQuantifiedVariables());
        unquantified.removeAll(quantified);
        ans.get(0).addAll(quantified);
        ans.get(1).addAll(unquantified);
        return ans;
    }

    /** ***************************************************************
     * Collects all variables in this Formula.  Returns an ArrayList
     * of String variable names (with initial '?').  Note that 
     * duplicates are not removed.
     *
     * @return An ArrayList of String variable names
     */
    private ArrayList<String> collectAllVariables() {
    	    
    	ArrayList<String> result = new ArrayList<String>();
    	HashSet<String> resultSet = new HashSet<String>();
    	if (listLength() < 1)
    		return result;
    	Formula fcar = new Formula();
    	fcar.read(this.car());
    	if (fcar.isVariable()) 
    		resultSet.add(fcar.theFormula);
    	else {
    		if (fcar.listP())
    			resultSet.addAll(fcar.collectAllVariables());
    	}
    	Formula fcdr = new Formula();
    	fcdr.read(this.cdr());
    	if (fcdr.isVariable()) 
    		resultSet.add(fcdr.theFormula);
    	else {
    		if (fcdr.listP())
    			resultSet.addAll(fcdr.collectAllVariables());
    	}
    	result.addAll(resultSet);
    	return result;
    }
    
    /** ***************************************************************
     * Collects all quantified variables in this Formula.  Returns an ArrayList
     * of String variable names (with initial '?').  Note that 
     * duplicates are not removed.
     *
     * @return An ArrayList of String variable names
     */
    private ArrayList<String> collectQuantifiedVariables() {
    	    
    	ArrayList<String> result = new ArrayList<String>();
    	HashSet<String> resultSet = new HashSet<String>();
    	if (listLength() < 1)
    		return result;
    	Formula fcar = new Formula();
    	fcar.read(this.car());
    	if (fcar.theFormula.equals(UQUANT) || fcar.theFormula.equals(EQUANT)) { 
        	Formula remainder = new Formula();
        	remainder.read(this.cdr());
        	if (!remainder.listP()) {
        		System.out.println("Error in Formula.collectQuantifiedVariables(): incorrect quantification: " + this.toString());
        		return result;
        	}
        	Formula varlist = new Formula();
        	varlist.read(remainder.car());
        	resultSet.addAll(varlist.collectAllVariables());
    		resultSet.addAll(remainder.cdrAsFormula().collectQuantifiedVariables());
    	}
    	else {
    		if (fcar.listP())
    			resultSet.addAll(fcar.collectQuantifiedVariables());
    		resultSet.addAll(this.cdrAsFormula().collectQuantifiedVariables());
    	}
    	result.addAll(resultSet);
    	return result;
    }

    /** ***************************************************************
     * Collect all the unquantified variables in a formula
     */
    private ArrayList collectUnquantifiedVariables() {
        return collectVariables().get(1);
    }

    /** ***************************************************************
     * Collect all the terms in a formula
     */
    public ArrayList<String> collectTerms() {

        HashSet<String> resultSet = new HashSet<String>();
        if (this.theFormula == null || this.theFormula == "") {
			System.out.println("Error in Formula.collectTerms(): " +
					"No formula to collect terms from: " + this);
            return null;
        }
        if (this.empty())
            return new ArrayList<String>();
        if (this.atom())
            resultSet.add(theFormula);
        else {
            Formula f = new Formula();
            f.read(theFormula);
            while (!f.empty() && f.theFormula != null && f.theFormula != "") {
                Formula f2 = new Formula();
                f2.read(f.car());
                resultSet.addAll(f2.collectTerms());
                f.read(f.cdr());
            }
        }
        ArrayList<String> result = new ArrayList(resultSet);
        return result;
    }

    /** ***************************************************************
     *  Replace variables with a value as given by the map argument
     */
    public Formula substituteVariables(Map<String,String> m) {

        Formula newFormula = new Formula();
        newFormula.read("()");
        if (atom()) {
            if (m.keySet().contains(theFormula)) {
                theFormula = (String) m.get(theFormula);
                if (this.listP())
                    theFormula = "(" + theFormula + ")";
            }
            return this;
        }
        if (!empty()) {
            Formula f1 = new Formula();
            f1.read(this.car());
            if (f1.listP()) 
                newFormula = newFormula.cons(f1.substituteVariables(m));            
            else
                newFormula = newFormula.append(f1.substituteVariables(m));
            Formula f2 = new Formula();
            f2.read(this.cdr());
            newFormula = newFormula.append(f2.substituteVariables(m));
        }
        return newFormula;
    }

    /** ***************************************************************
     * Makes implicit quantification explicit.
     *
     * @param query controls whether to add universal or existential
     * quantification.  If true, add existential.
     *
     * @result the formula as a String, with explicit quantification
     */
    public String makeQuantifiersExplicit(boolean query) {
    	
        String result = this.theFormula;
        String arg0 = this.car();
        ArrayList<ArrayList<String>> vpair = collectVariables();
        ArrayList<String> quantVariables = vpair.get(0);
        ArrayList<String> unquantVariables = vpair.get(1);

        if (!unquantVariables.isEmpty()) {   // Quantify all the unquantified variables
            StringBuilder sb = new StringBuilder();
            sb.append((query ? "(exists (" : "(forall ("));
            boolean afterTheFirst = false;
            Iterator<String> itu = unquantVariables.iterator();
            while (itu.hasNext()) {
                if (afterTheFirst) sb.append(" ");
                sb.append(itu.next());
                afterTheFirst = true;
            }
            sb.append(") ");
            sb.append(this.theFormula);
            sb.append(")");
            result = sb.toString();
        }
        return result;
    }

    /** ***************************************************************
     * @param kb - The KB used to compute variable arity relations.
     *
     * @return Returns true if this Formula contains any variable
     * arity relations, else returns false.
     */
    protected boolean containsVariableArityRelation(KB kb) {

        boolean ans = false;
        Set relns = kb.kbCache.getCachedRelationValues("instance", "VariableArityRelation", 2, 1);
        if (relns == null)
            relns = new HashSet();
        relns.addAll(KB.VA_RELNS);
        String r = null;
        Iterator it = relns.iterator();
        while (it.hasNext()) {
            r = (String) it.next();
            ans = (this.theFormula.indexOf(r) != -1);
            if (ans) { break; }
        }
        return ans;
    }

    /** ***************************************************************
     * @param kb - The KB used to compute variable arity relations.
     * @param relationMap is a Map of String keys and values where
     *                    the key is the renamed relation and the
     *                    value is the original name.  This is set
     *                    as a side effect of this method.
     * @return A new version of the Formula in which every
     * VariableArityRelation has been renamed to include a numeric
     * suffix corresponding to the actual number of arguments in the
     * Formula.
     */
    protected Formula renameVariableArityRelations(KB kb, TreeMap<String,String> relationMap) {

        Formula result = this;
        if (this.listP()) {
            StringBuilder sb = new StringBuilder();
            Formula f = new Formula();
            f.read(this.theFormula);
            int flen = f.listLength();
            String suffix = ("_" + (flen - 1));
            String arg = null;
            sb.append("(");
            for (int i = 0 ; i < flen ; i++) {
                arg = f.getArgument(i);
                if (i > 0)
                    sb.append(" ");
                if ((i == 0) && kb.isVariableArityRelation(arg) && !arg.endsWith(suffix)) {
                    relationMap.put(arg + suffix, arg);
                    arg += suffix;
                }
                else if (listP(arg)) {
                    Formula argF = new Formula();
                    argF.read(arg);
                    arg = argF.renameVariableArityRelations(kb,relationMap).theFormula;
                }
                sb.append(arg);
            }
            sb.append(")");
            f = new Formula();
            f.read(sb.toString());
            result = f;
        }
        return result;
    }

    /** ***************************************************************
     * Gathers the row variable names in this.theFormula and returns
     * them in a TreeSet.
     *
     * @return a TreeSet, possibly empty, containing row variable
     * names, each of which will start with the row variable
     * designator '@'.
     */
    private TreeSet<String> findRowVars() {

        TreeSet<String> result = new TreeSet<String>();
            if (!StringUtil.emptyString(this.theFormula)
                && this.theFormula.contains(R_PREF)) {
                Formula f = new Formula();
                f.read(this.theFormula);
                while (f.listP() && !f.empty()) {
                    String arg = f.getArgument(0);
                    if (arg.startsWith(R_PREF))
                        result.add(arg);
                    else {
                        Formula argF = new Formula();
                        argF.read(arg);
                        if (argF.listP())
                            result.addAll(argF.findRowVars());
                    }
                    f.read(f.cdr());
                }
            }
        return result;
    }

    /** ***************************************************************
     * Expand row variables, keeping the information about the original
     * source formula.  Each variable is treated like a macro that
     * expands to up to seven regular variables.  For example
     *
     * (=>
     *    (and
     *       (subrelation ?REL1 ?REL2)
     *       (holds__ ?REL1 @ROW))
     *    (holds__ ?REL2 @ROW))
     *
     * would become
     *
     * (=>
     *    (and
     *       (subrelation ?REL1 ?REL2)
     *       (holds__ ?REL1 ?ARG1))
     *    (holds__ ?REL2 ?ARG1))
     *
     * (=>
     *    (and
     *       (subrelation ?REL1 ?REL2)
     *       (holds__ ?REL1 ?ARG1 ?ARG2))
     *    (holds__ ?REL2 ?ARG1 ?ARG2))
     * etc.
     *
     * @return an ArrayList of Formulas, or an empty ArrayList.
     */
    public ArrayList expandRowVars(KB kb) {       
    	
		ArrayList resultList = new ArrayList();
        TreeSet rowVars = (this.theFormula.contains(R_PREF)
                           ? this.findRowVars()
                           : null);
        // If this Formula contains no row vars to expand, we just
        // add it to resultList and quit.
        if ((rowVars == null) || rowVars.isEmpty()) 
            resultList.add(this);        
        else {
            Formula f = new Formula();
            f.read(this.theFormula);
            Set accumulator = new LinkedHashSet();
            accumulator.add(f);
            List working = new ArrayList();
            long t1 = 0L;

            // Iterate through the row variables
            String rowvar = null;
            Iterator<String> irv = rowVars.iterator();
            while (irv.hasNext()) {
                rowvar = irv.next();
                working.clear();
                working.addAll(accumulator);
                accumulator.clear();

                String fstr = null;
                Iterator<Formula> itw = working.iterator();
                while (itw.hasNext()) {
                    f = itw.next();
                    fstr = f.theFormula;
                    if (!fstr.contains(R_PREF)
                        || (fstr.indexOf("\"") > -1)) {
                        f.sourceFile = this.sourceFile;
                        resultList.add(f);
                    }
                    else {
                        t1 = System.currentTimeMillis();
                        int[] range = f.getRowVarExpansionRange(kb, rowvar);
                        boolean hasVariableArityRelation = (range[0] == 0);
                        t1 = System.currentTimeMillis();
                        range[1] = adjustExpansionCount(hasVariableArityRelation, range[1], rowvar);
                        Formula newF = null;
                        StringBuilder varRepl = new StringBuilder();

                        for (int j = 1 ; j < range[1] ; j++) {
                            if (varRepl.length() > 0)
                                varRepl.append(" ");
                            varRepl.append("?");
                            varRepl.append(rowvar.substring(1));
                            // varRepl.append("_");
                            varRepl.append(Integer.toString(j));
                            if (hasVariableArityRelation) {
                                newF = new Formula();
                                newF.read(fstr.replaceAll(rowvar, varRepl.toString()));
                                // Copy the source file information for each expanded formula.
                                newF.sourceFile = this.sourceFile;
                                if (newF.theFormula.contains(R_PREF)
                                    && (newF.theFormula.indexOf("\"") == -1)) {
                                    accumulator.add(newF);
                                }
                                else
                                    resultList.add(newF);
                            }
                        }
                        if (!hasVariableArityRelation) {
                            newF = new Formula();
                            newF.read(fstr.replaceAll(rowvar, varRepl.toString()));
                            // Copy the source file information for each expanded formula.
                            newF.sourceFile = this.sourceFile;
                            if (newF.theFormula.contains(R_PREF)
                                && (newF.theFormula.indexOf('"') == -1)) {
                                accumulator.add(newF);
                            }
                            else
                                resultList.add(newF);
                        }
                    }
                }
            }
        }
        return resultList;
    }

    /** ***************************************************************
     * This method attempts to revise the number of row var expansions
     * to be done, based on the occurrence of forms such as (<pred>
     * @ROW1 ?ITEM).  Note that variables such as ?ITEM throw off the
     * default expected expansion count, and so must be dealt with to
     * prevent unnecessary expansions.
     *
     * @param variableArity Indicates whether the overall expansion
     * count for the Formula is governed by a variable arity relation,
     * or not.
     *
     * @param count The default expected expansion count, possibly to
     * be revised.
     *
     * @param var The row variable to be expanded.
     *
     * @return An int value, the revised expansion count.  In most
     * cases, the count will not change.    
     */
    private int adjustExpansionCount(boolean variableArity, int count, String var) {

        int revisedCount = count;
        if (!StringUtil.emptyString(var)) {
            String rowVar = var;
            if (!var.startsWith("@"))
                rowVar = ("@" + var);
            List accumulator = new ArrayList();
            List working = new ArrayList();
            if (this.listP() && !this.empty())
                accumulator.add(this);
            while (!accumulator.isEmpty()) {
                working.clear();
                working.addAll(accumulator);
                accumulator.clear();
                for (int i = 0 ; i < working.size() ; i++) {
                    Formula f = (Formula) working.get(i);
                    List literal = f.literalToArrayList();

                    int len = literal.size();
                    if (literal.contains(rowVar) && !isVariable(f.car())) {
                        if (!variableArity && (len > 2))
                            revisedCount = (count - (len - 2));
                        else if (variableArity)
                            revisedCount = (10 - len);
                    }
                    if (revisedCount < 2) 
                        revisedCount = 2;                        
                    while (!f.empty()) {
                        String arg = f.car();
                        Formula argF = new Formula();
                        argF.read(arg);
                        if (argF.listP() && !argF.empty())
                            accumulator.add(argF);
                        f = f.cdrAsFormula();
                    }
                }
            }
        }
        return revisedCount;
    }

    /** ***************************************************************
     * Returns a two-place int[] indicating the low and high points of
     * the expansion range (number of row var instances) for the input
     * row var.
     *
     * @param kb A KB required for processing.
     *
     * @param rowVar The row var (String) to be expanded.
     *
     * @return A two-place int[] object.  The int[] indicates a
     * numeric range.  int[0] holds the start (lowest number) in the
     * range, and int[1] holds the highest number.  The default is
     * [1,8].  If the Formula does not contain     
     */
    private int[] getRowVarExpansionRange(KB kb, String rowVar) {

        int[] ans = new int[2];
        ans[0] = 1;
        ans[1] = 8;
        if (!StringUtil.emptyString(rowVar)) {
            String var = rowVar;
            if (!var.startsWith("@"))
                var = "@" + var;
            Map minMaxMap = this.getRowVarsMinMax(kb);
            int[] newArr = (int[]) minMaxMap.get(var);
            if (newArr != null)
                ans = newArr;
        }
        return ans;
    }

    /** ***************************************************************
     * Applied to a SUO-KIF Formula with row variables, this method
     * returns a Map containing an int[] of length 2 for each row var
     * that indicates the minimum and maximum number of row var
     * expansions to perform.
     *
     * @param kb A KB required for processing.
     *
     * @return A Map in which the keys are distinct row variables and
     * the values are two-place int[] objects.  The int[] indicates a
     * numeric range.  int[0] is the start (lowest number) in the
     * range, and int[1] is the end.  If the Formula contains no row
     * vars, the Map is empty.
     */
    private Map getRowVarsMinMax(KB kb) {

		Map ans = new HashMap();
        long t1 = System.currentTimeMillis();
        ArrayList clauseData = this.getTheClausalForm();
        // if (trace) System.out.println("  getTheClausalForm() == " + clauseData);
        if (!((clauseData instanceof ArrayList) && (clauseData.size() > 2)))
            return ans;
        // System.out.println("\nclauseData == " + clauseData + "\n");
        ArrayList clauses = (ArrayList) clauseData.get(0);
        // System.out.println("\nclauses == " + clauses + "clauses.size() == " + clauses.size() + "\n");
        if (clauses == null || clauses.isEmpty())
            return ans;
        Map varMap = (Map) clauseData.get(2);
        Map rowVarRelns = new HashMap();
        for (int i = 0 ; i < clauses.size() ; i++) {
            ArrayList clause = (ArrayList) clauses.get(i);
            // if (trace) System.out.println("  clause == " + clause);
            if ((clause != null) && !clause.isEmpty()) {
                // First we get the neg lits.  It may be that
                // we should use *only* the neg lits for this
                // task, but we will start by combining the neg
                // lits and pos lits into one list of literals
                // and see how that works.
                ArrayList literals = (ArrayList) clause.get(0);
                ArrayList posLits = (ArrayList) clause.get(1);
                literals.addAll(posLits);
                // if (trace) System.out.println("  literals == " + literals);
                for (Iterator itl = literals.iterator(); itl.hasNext();) {
                    Formula litF = (Formula) itl.next();;
                    litF.computeRowVarsWithRelations(rowVarRelns, varMap);
                }
            }
			// logger.finest("rowVarRelns == " + rowVarRelns);
            if (!rowVarRelns.isEmpty()) {
                for (Iterator kit = rowVarRelns.keySet().iterator(); kit.hasNext();) {
                    String rowVar = (String) kit.next();
                    String origRowVar = Clausifier.getOriginalVar(rowVar, varMap);
                    int[] minMax = (int[]) ans.get(origRowVar);
                    if (minMax == null) {
                        minMax = new int[2];
                        minMax[0] = 0;
                        minMax[1] = 8;
                        ans.put(origRowVar, minMax);
                    }
                    TreeSet val = (TreeSet) rowVarRelns.get(rowVar);
                    for (Iterator vit = val.iterator(); vit.hasNext();) {
                        String reln = (String) vit.next();
                        int arity = kb.getValence(reln);
                        if (arity < 1) {
                            // It's a VariableArityRelation or we
                            // can't find an arity, so do nothing.
                            ;
                        }
                        else {
                            minMax[0] = 1;
                            int arityPlusOne = (arity + 1);
                            if (arityPlusOne < minMax[1])
                                minMax[1] = arityPlusOne;
                        }
                    }
                }
            }
        }
        return ans;
    }

    /** ***************************************************************
     * Finds all the relations in this Formula that are applied to row
     * variables, and for which a specific arity might be computed.
     * Note that results are accumulated in varsToRelns, and the
     * variable correspondences (if any) in varsToVars are used to
     * compute the results.
     *
     * @param varsToRelns A Map for accumulating row var data for one
     * Formula literal.  The keys are row variables (Strings) and the
     * values are TreeSets containing relations (Strings) that might
     * help to constrain the row var during row var expansion.
     *
     * @param varsToVars A Map of variable correspondences, the leaves
     * of which might include row variables
     *
     * @return void
     * */
    protected void computeRowVarsWithRelations(Map varsToRelns, Map varsToVars) {

        Formula f = this;
        if (f.listP() && !f.empty()) {
            String relation = f.car();
            if (!isVariable(relation) && !relation.equals(SKFN)) {
                Formula newF = f.cdrAsFormula();
                while (newF.listP() && !newF.empty()) {
                    String term = newF.car();
                    String rowVar = term;
                    if (isVariable(rowVar)) {
                        if (rowVar.startsWith(V_PREF) && (varsToVars != null))
                            rowVar = Clausifier.getOriginalVar(term, varsToVars);
                    }
                    if (rowVar.startsWith(R_PREF)) {
                        TreeSet relns = (TreeSet) varsToRelns.get(term);
                        if (relns == null) {
                            relns = new TreeSet();
                            varsToRelns.put(term, relns);
                            varsToRelns.put(rowVar, relns);
                        }
                        relns.add(relation);
                    }
                    else {
                        Formula termF = new Formula();
                        termF.read(term);
                        termF.computeRowVarsWithRelations(varsToRelns, varsToVars);
                    }
                    newF = newF.cdrAsFormula();
                }
            }
        }
        return;
    }

    /** ***************************************************************
     * Returns a HashMap in which the keys are the Relation constants
     * gathered from this Formula, and the values are ArrayLists in
     * which the ordinal positions 0 - n are occupied by the names of
     * the corresponding argument types.  n should never be greater
     * than the value of Formula.MAX_PREDICATE_ARITY.  For each
     * Predicate key, the length of its ArrayList should be equal to
     * the predicate's valence + 1.  For each Function, the length of
     * its ArrayList should be equal to its valence.  Only Functions
     * will have argument types in the 0th position of the ArrayList,
     * since this position contains a function's range type.  This
     * means that all Predicate ArrayLists will contain at least one
     * null value.  A null value will also be added to the nth
     * position of an ArrayList when no value can be obtained for that
     * position.
     *
     * @return A HashMap that maps every Relation occurring in this
     * Formula to an ArrayList indicating the Relation's argument
     * types.  Some HashMap keys may map to null values or empty
     * ArrayLists, and most ArrayLists will contain some null values.
     */
    public HashMap<String, ArrayList> gatherRelationsWithArgTypes(KB kb) {

        HashMap<String, ArrayList> argtypemap = new HashMap<String, ArrayList>();
        Set<String> relations = gatherRelationConstants();
        for (String r : relations) {
            int atlen = (Formula.MAX_PREDICATE_ARITY + 1);
            ArrayList argtypes = new ArrayList();
            for (int i = 0; i < atlen; i++) 
                argtypes.add(kb.getArgType(r, i));                
            argtypemap.put(r, argtypes);
        }
        return argtypemap;
    }

    /** ***************************************************************
     * Returns a HashSet of all atomic KIF Relation constants that
     * occur as Predicates or Functions (argument 0 terms) in this
     * Formula.
     *
     * @return a HashSet containing the String constants that denote
     * KIF Relations in this Formula, or an empty HashSet.
     */
    public HashSet<String> gatherRelationConstants() {

        HashSet<String> relations = new HashSet<String>();
        Set<String> accumulator = new HashSet<String>();
        if (this.listP() && !this.empty())
            accumulator.add(this.theFormula);
        List<String> kifLists = new ArrayList<String>();
        Formula f = null;
        while (!accumulator.isEmpty()) {
            kifLists.clear();
            kifLists.addAll(accumulator);
            accumulator.clear();
            String klist = null;
            for (Iterator it = kifLists.iterator(); it.hasNext();) {
                klist = (String) it.next();
                if (listP(klist)) {
                    f = new Formula();
                    f.read(klist);
                    for (int i = 0; !f.empty(); i++) {
                        String arg = f.car();
                        if (listP(arg)) {
                            if (!empty(arg)) accumulator.add(arg);
                        }
                        else if (isQuantifier(arg)) {
                            accumulator.add(f.getArgument(2));
                            break;
                        }
                        else if ((i == 0)
                                 && !isVariable(arg)
                                 && !isLogicalOperator(arg)
                                 && !arg.equals(SKFN)
                                 && !StringUtil.isQuotedString(arg)
                                 && !arg.matches(".*\\s.*")) {
                            relations.add(arg);
                        }
                        f = f.cdrAsFormula();
                    }
                }
            }
        }
        return relations;
    }

    /** ***************************************************************
     * Convert an ArrayList of Formulas to an ArrayList of Strings.
     */
    private ArrayList formulasToStrings(ArrayList list) {

        ArrayList result = new ArrayList();
        for (int i = 0; i < list.size(); i++) 
            result.add(((Formula) list.get(i)).theFormula);        
        return result;
    }

    /** ***************************************************************
     * Test whether a Formula is a functional term.  Note this assumes
     * the textual convention of all functions ending with "Fn".
     */
    public boolean isFunctionalTerm() {

        boolean ans = false;
        if (this.listP()) {
            String pred = this.car();
            ans = ((pred.length() > 2) && pred.endsWith(FN_SUFF));
        }
        return ans;
    }

    /** ***************************************************************
     * Test whether a Formula is a functional term
     */
    public static boolean isFunctionalTerm(String s) {

        Formula f = new Formula();
        f.read(s);
        return f.isFunctionalTerm();
    }

    /** ***************************************************************
     * Test whether a Formula contains a Formula as an argument to
     * other than a logical operator.
     */
    public boolean isHigherOrder() {

        if (this.listP()) {
            String pred = this.car();
            boolean logop = isLogicalOperator(pred);
            ArrayList al = literalToArrayList();
            for (int i = 1; i < al.size(); i++) {
                String arg = (String) al.get(i);
                Formula f = new Formula();
                f.read(arg);
                if (!atom(arg) && !f.isFunctionalTerm()) {
                    if (logop) {
                        if (f.isHigherOrder())
                            return true;
                    }
                    else
                        return true;
                }
            }
        }
        return false;
    }

    /** ***************************************************************
     * Test whether an Object is a variable
     */
    public static boolean isVariable(Object term) {

        return (!StringUtil.emptyString(term)
                && (((String)term).startsWith(V_PREF)
                    || ((String)term).startsWith(R_PREF)));
    }

    /** ***************************************************************
     * Test whether the formula is a variable
     */
    public  boolean isVariable() {
        return isVariable(theFormula);
    }

    /** ***************************************************************
     * Returns true only if this Formula, explicitly quantified or
     * not, starts with "=>" or "<=>", else returns false.  It would
     * be better to test for the occurrence of at least one positive
     * literal with one or more negative literals, but this test would
     * require converting the Formula to clausal form.
     */
    public boolean isRule() {
    	
        boolean ans = false;
        if (this.listP()) {
            String arg0 = this.car();
            if (isQuantifier(arg0)) {
                String arg2 = this.getArgument(2);
                if (Formula.listP(arg2)) {
                    Formula newF = new Formula();
                    newF.read(arg2);
                    ans = newF.isRule();
                }
            }
            else {
                ans = Arrays.asList(IF, IFF).contains(arg0);
            }
        }
        return ans;
    }

    /** ***************************************************************
     * Test whether a list with a predicate is a quantifier list
     */
    public static boolean isQuantifierList(String listPred, String previousPred) {

        return ((previousPred.equals(EQUANT) || previousPred.equals(UQUANT)) &&
                (listPred.startsWith(R_PREF) || listPred.startsWith(V_PREF)));
    }

    /** ***************************************************************
     * Test whether a Formula is a simple list of terms (including
     * functional terms).
     */
    public boolean isSimpleClause() {

        Formula f = new Formula();
        f.read(theFormula);
        while (!f.empty()) {
            if (listP(f.car())) {
                Formula f2 = new Formula();
                f2.read(f.car());
                if (!Formula.isFunction(f2.car()))
                    return false;
                else if (!f2.isSimpleClause())
                    return false;
            }
            f.read(f.cdr());
        }
        return true;
    }

    /** ***************************************************************
     * Test whether a Formula is a simple clause wrapped in a
     * negation.
     */
    public boolean isSimpleNegatedClause() {

        Formula f = new Formula();
        f.read(theFormula);
        if (f == null || f.empty() || f.atom())
            return false;
        if (f.car().equals("not")) {
            f.read(f.cdr());
            if (empty(f.cdr())) {
                f.read(f.car());
                return f.isSimpleClause();
            }
            else
                return false;
        }
        else
            return false;
    }

    /** ***************************************************************
     * Test whether a predicate is a logical quantifier
     */
    public static boolean isQuantifier(String pred) {

        return (!StringUtil.emptyString(pred)
                && (pred.equals(EQUANT)
                    || pred.equals(UQUANT)));
    }

    /** ***************************************************************
     * A static utility method.
     * @param obj Any object, but should be a String.
     * @return true if obj is a SUO-KIF commutative logical operator,
     * else false.
     */
    public static boolean isCommutative(String obj) {

        return (!StringUtil.emptyString(obj)
                && (obj.equals(AND)
                    || obj.equals(OR)));
    }

    /** ***************************************************************
     * Returns the dual logical operator of op, or null if op is not
     * an operator or has no dual.
     *
     * @param term A String, assumed to be a SUO-KIF logical operator
     *
     * @return A String, the dual operator of op, or null.
     */
    protected static String getDualOperator(String op) {
    	
        String ans = null;
        if (op instanceof String) {
            String[][] duals = { { UQUANT, EQUANT },
                                 { EQUANT, UQUANT },
                                 { AND,    OR     },
                                 { OR,     AND    },
                                 { NOT,    ""     },
                                 { "",     NOT    },
                                 { LOG_TRUE,  LOG_FALSE  },
                                 { LOG_FALSE, LOG_TRUE   }
            };
            for (int i = 0; i < duals.length; i++) 
                if (op.equals(duals[i][0])) ans = duals[i][1];            
        }
        return ans;
    }

    /** ***************************************************************
     * Returns true if term is a standard FOL logical operator, else
     * returns false.
     *
     * @param term A String, assumed to be an atomic SUO-KIF term.
     */
    public static boolean isLogicalOperator(String term) {

        return (!StringUtil.emptyString(term) && LOGICAL_OPERATORS.contains(term));
    }

    /** ***************************************************************
     * Returns true if term is a valid SUO-KIF term, else
     * returns false.
     *
     * @param term A String, assumed to be an atomic SUO-KIF term.
     */
    public static boolean isTerm(String term) {

        if (!StringUtil.emptyString(term) && !listP(term) &&
                Character.isJavaIdentifierStart(term.charAt(0))) {
            for (int i = 0; i < term.length(); i++) {
                if (!Character.isJavaIdentifierPart(term.charAt(i)))
                    return false;
            }
            return true;
        }
        else
            return false;
    }

    /** ***************************************************************
     * Returns true if term is a SUO-KIF predicate for comparing two
     * (typically numeric) terms, else returns false.
     *
     * @param term A String.
     */
    public static boolean isComparisonOperator(String term) {

        return (!StringUtil.emptyString(term) && COMPARISON_OPERATORS.contains(term));
    }

    /** ***************************************************************
     * Returns true if term is a SUO-KIF mathematical function, else
     * returns false.
     *
     * @param term A String.
     */
    public static boolean isMathFunction(String term) {

        return (!StringUtil.emptyString(term) && MATH_FUNCTIONS.contains(term));
    }

    /** ***************************************************************
     * Returns true if formula is a valid formula with no variables,
     * else returns false.
     */
    public static boolean isGround(String form) {

        if (StringUtil.emptyString(form))
            return false;
        if (form.indexOf("\"") < 0)
            return (form.indexOf("?") < 0 && form.indexOf("@") < 0);
        boolean inQuote = false;
        for (int i = 0; i < form.length(); i++) {
            if (form.charAt(i) == '"')
                inQuote = !inQuote;
            if ((form.charAt(i) == '?' || form.charAt(i) == '@') && !inQuote)
                return false;
        }
        return true;
    }

    /** ***************************************************************
     * Returns true if formula has variable, else returns false.
     */
    public boolean isGround() {
        return isGround(theFormula);
    }

    /** ***************************************************************
     * Returns true if term is a SUO-KIF function, else returns false.
     * Note that this test is purely syntactic, and could fail for
     * functions that do not adhere to the convention of ending all
     * functions with "Fn".
     *
     * @param term A String.
     */
    public static boolean isFunction(String term) {
        return (!StringUtil.emptyString(term) && term.endsWith(FN_SUFF));
    }

    /** ***************************************************************
     * Returns true if term is a SUO-KIF Skolem term, else returns false.
     *
     * @param term A String.
     *
     * @return true or false
     */
    public static boolean isSkolemTerm(String term) {
        return (!StringUtil.emptyString(term)
                && term.trim().matches("^.?" + SK_PREF + "\\S*\\s*\\d+"));
    }

    /** ***************************************************************
     * @return An ArrayList (ordered tuple) representation of the
     * Formula, in which each top-level element of the Formula is
     * either an atom (String) or another list.
     */
    public ArrayList<String> literalToArrayList() {
    	
        ArrayList<String> tuple = new ArrayList<String>();
        Formula f = this;
        if (f.listP()) {
            while (!f.empty()) {
                tuple.add(f.car());
                f = f.cdrAsFormula();
            }
        }        
        return tuple;
    }

    /** ***************************************************************
     *  Replace v with term
     */
    public Formula replaceVar(String v, String term) {

        Formula newFormula = new Formula();
        newFormula.read("()");
        if (this.isVariable()) {
            if (theFormula.equals(v))
                theFormula = term;
            return this;
        }
        if (!this.empty()) {
            Formula f1 = new Formula();
            f1.read(this.car());
			// logger.finest("car: " + f1.theFormula);
            if (f1.listP())
                newFormula = newFormula.cons(f1.replaceVar(v,term));
            else
                newFormula = newFormula.append(f1.replaceVar(v,term));
            Formula f2 = new Formula();
            f2.read(this.cdr());
			// logger.finest("cdr: " + f2);
            newFormula = newFormula.append(f2.replaceVar(v,term));
        }
        return newFormula;
    }

    /** ***************************************************************
     * Compare the given formula to the query and return whether
     * they are the same.
     */
    public static boolean isQuery(String query, String formula) {

        boolean result = false;

        Formula f = new Formula();
        f.read(formula);
        result = f.equals(query);
        return result;
    }

    /** ***************************************************************
     * Compare the given formula to the negated query and return whether
     * they are the same (minus the negation).
     */
    public static boolean isNegatedQuery(String query, String formulaString) {

        boolean result = false;
        String fstr = formulaString.trim();
        if (fstr.startsWith("(not")) {
            Formula f = new Formula();
            f.read(fstr);
            result = query.equals(f.getArgument(1));
        }
        return result;
    }

    /** ***************************************************************
     * Remove the 'holds' prefix wherever it appears.
     */
    public static String postProcess(String s) {

        s = s.replaceAll("holds_\\d+__ ","");
        s = s.replaceAll("apply_\\d+__ ","");
        return s;
    }

    /** ***************************************************************
     * Format a formula for either text or HTML presentation by inserting
     * the proper hyperlink code, characters for indentation and end of line.
     * A standard LISP-style pretty printing is employed where an open
     * parenthesis triggers a new line and added indentation.
     *
     * @param hyperlink - the URL to be referenced to a hyperlinked term.
     * @param indentChars - the proper characters for indenting text.
     * @param eolChars - the proper character for end of line.
     */
    public String format(String hyperlink, String indentChars, String eolChars) {

        if (this.theFormula == null)
            return "";
        String result = this.theFormula;
        if (!StringUtil.emptyString(this.theFormula))
            this.theFormula = this.theFormula.trim();
        String legalTermChars = "-:";
        String varStartChars = "?@";
        String quantifiers = "forall|exists";
        StringBuilder token = new StringBuilder();
        StringBuilder formatted = new StringBuilder();
        int indentLevel = 0;
        boolean inQuantifier = false;
        boolean inToken = false;
        boolean inVariable = false;
        boolean inVarlist = false;
        boolean inComment = false;

        int flen = this.theFormula.length();
        char pch = '0';  // char at (i-1)
        char ch = '0';   // char at i
        for (int i = 0; i < flen; i++) {
			// logger.finest("formatted string = " + formatted.toString());
            ch = this.theFormula.charAt(i);
            if (inComment) {     // In a comment
                formatted.append(ch);
                if ((i > 70) && (ch == '/')) // add spaces to long URL strings
                    formatted.append(" ");
                if (ch == '"')
                    inComment = false;
            }
            else {
                if ((ch == '(')
                    && !inQuantifier
                    && ((indentLevel != 0) || (i > 1))) {
                    if ((i > 0) && Character.isWhitespace(pch)) {
                        formatted = formatted.deleteCharAt(formatted.length()-1);
                    }
                    formatted.append(eolChars);
                    for (int j = 0; j < indentLevel; j++)
                        formatted.append(indentChars);
                }
                if ((i == 0) && (indentLevel == 0) && (ch == '('))
                    formatted.append(ch);
                if (!inToken && !inVariable && Character.isJavaIdentifierStart(ch)) {
                    token = new StringBuilder(ch);
                    inToken = true;
                }
                if (inToken && (Character.isJavaIdentifierPart(ch)
                                || (legalTermChars.indexOf(ch) > -1)))
                    token.append(ch);
                if (ch == '(') {
                    if (inQuantifier) {
                        inQuantifier = false;
                        inVarlist = true;
                        token = new StringBuilder();
                    }
                    else
                        indentLevel++;
                }
                if (ch == '"')
                    inComment = true;
                if (ch == ')') {
                    if (!inVarlist)
                        indentLevel--;
                    else
                        inVarlist = false;
                }
                if ((token.indexOf("forall") > -1) || (token.indexOf("exists") > -1))
                    inQuantifier = true;
                if (inVariable
                    && !Character.isJavaIdentifierPart(ch)
                    && (legalTermChars.indexOf(ch) == -1))
                    inVariable = false;
                if (varStartChars.indexOf(ch) > -1)
                    inVariable = true;
                if (inToken
                    && !Character.isJavaIdentifierPart(ch)
                    && (legalTermChars.indexOf(ch) == -1)) {
                    inToken = false;
                    if (StringUtil.isNonEmptyString(hyperlink)) {
                        formatted.append("<a href=\"");
                        formatted.append(hyperlink);
                        formatted.append("&term=");
                        formatted.append(token);
                        formatted.append("\">");
                        formatted.append(token);
                        formatted.append("</a>");
                    }
                    else
                        formatted.append(token);
                    token = new StringBuilder();
                }
                if ((i > 0) && !inToken && !(Character.isWhitespace(ch) && (pch == '('))) {
                    if (Character.isWhitespace(ch)) {
                        if (!Character.isWhitespace(pch))
                            formatted.append(" ");
                    }
                    else
                        formatted.append(ch);
                }
            }
            pch = ch;
        }
        if (inToken) {    // A term which is outside of parenthesis, typically, a binding.
            if (StringUtil.isNonEmptyString(hyperlink)) {
                formatted.append("<a href=\"");
                formatted.append(hyperlink);
                formatted.append("&term=");
                formatted.append(token);
                formatted.append("\">");
                formatted.append(token);
                formatted.append("</a>");
            }
            else
                formatted.append(token);
        }
        result = formatted.toString();
        return result;
    }

    /** ***************************************************************
     * Format a formula for text presentation.
     * @deprecated
     */
    public String textFormat() {

        return format("","  ",new Character((char) 10).toString());
    }

    /** ***************************************************************
     * Format a formula for text presentation.
     */
    public String toString() {

        return format("","  ",new Character((char) 10).toString());
    }

    /** ***************************************************************
     * Format a formula for HTML presentation.
     */
    public String htmlFormat(String html) {

        return format(html,"&nbsp;&nbsp;&nbsp;&nbsp;","<br>\n");
    }

    /** ***************************************************************
     * Format a formula for HTML presentation.
     */
    public String htmlFormat(KB kb) {

        String fKbHref = "";
        KBmanager mgr = KBmanager.getMgr();
        String hostname = mgr.getPref("hostname");
        if (StringUtil.emptyString(hostname))
            hostname = "localhost";
        String port = mgr.getPref("port");
        if (StringUtil.emptyString(port))
            port = "8080";
        String kbHref = ("http://" + hostname + ":" + port + "/sigma/Browse.jsp?kb=" + kb.name);
        fKbHref = format(kbHref,"&nbsp;&nbsp;&nbsp;&nbsp;","<br>\n");
        return fKbHref;
    }

    /** ***************************************************************
     * Format a formula as a prolog statement.  Note that only tuples
     * are converted properly at this time.  Statements with any embedded
     * formulas or functions will be rejected with a null return.
     */
    public String toProlog() {

        if (!listP()) {
			System.out.println("Not a formula: " + theFormula);
            return "";
        }
        if (empty()) {
        	System.out.println("Empty formula: " + theFormula);
            return "";
        }
        StringBuilder result = new StringBuilder();
        String relation = car();
        Formula f = new Formula();
        f.theFormula = cdr();
        if (!Formula.atom(relation)) {
        	System.out.println("Relation not an atom: " + relation);
            return "";
        }
        result.append(relation + "('");
        while (!f.empty()) {
            String arg = f.car();
            f.theFormula = f.cdr();
            if (!Formula.atom(arg)) {
            	System.out.println("Argument not an atom: " + arg);
                return "";
            }
            result.append(arg + "'");
            if (!f.empty())
                result.append(",'");
            else
                result.append(").");
        }
        return result.toString();
    }

    /** ***************************************************************
     *  Replace term2 with term1
     */
    public Formula rename(String term2, String term1) {

        Formula newFormula = new Formula();
        newFormula.read("()");
        if (this.atom()) {
            if (theFormula.equals(term2))
                theFormula = term1;
            return this;
        }
        if (!this.empty()) {
            Formula f1 = new Formula();
            f1.read(this.car());
            if (f1.listP())
                newFormula = newFormula.cons(f1.rename(term2,term1));
            else
                newFormula = newFormula.append(f1.rename(term2,term1));
            Formula f2 = new Formula();
            f2.read(this.cdr());
            newFormula = newFormula.append(f2.rename(term2,term1));
        }
        return newFormula;
    }

    /** ***************************************************************
     * A test method.  It expects two command line arguments for the 
     * input file and output file.
     */
    public static void testClausifier(String[] args) {

        BufferedWriter bw = null;
        try {
            long t1 = System.currentTimeMillis();
            int count = 0;
            String inpath = args[0];
            String outpath = args[1];
            if (!StringUtil.emptyString(inpath) && !StringUtil.emptyString(outpath)) {
                File infile = new File(inpath);
                if (infile.exists()) {
                    KIF kif = new KIF();
                    kif.setParseMode(KIF.RELAXED_PARSE_MODE);
                    kif.readFile(infile.getCanonicalPath());
                    if (! kif.formulas.isEmpty()) {
                        File outfile = new File(outpath);
                        if (outfile.exists()) { outfile.delete(); }
                        bw = new BufferedWriter(new FileWriter(outfile, true));
                        Iterator it = kif.formulas.values().iterator();
                        Iterator it2 = null;
                        Formula f = null;
                        Formula clausalForm = null;
                        while (it.hasNext()) {
                            it2 = ((List) it.next()).iterator();
                            while (it2.hasNext()) {
                                f = (Formula) it2.next();
                                clausalForm = Clausifier.clausify(f);
                                if (clausalForm != null) {
                                    bw.write(clausalForm.theFormula);
                                    bw.newLine();
                                    count++;
                                }
                            }
                        }
                        try {
                            bw.flush();
                            bw.close();
                            bw = null;
                        }
                        catch (Exception bwe) {
                            bwe.printStackTrace();
                        }
                    }
                }
            }
            long dur = (System.currentTimeMillis() - t1);
        }
        catch (Exception ex) {
			System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        finally {
            if (bw != null) {
                try {
                    bw.close();
                }
                catch (Exception e2) {
                }
            }
        }
        return;
    }
    
    /** ***************************************************************
     * A test method.
     */
    public static void testCollectVariables() {
    	
    	Formula f = new Formula();
    	f.read("(=> " +
                "  (and " +
                "    (attribute ?H Muslim) " +
                "    (equal " +
                "      (WealthFn ?H) ?W)) " +
                "(modalAttribute " +
                "  (exists (?Z ?T) " +
                "    (and " +
                "      (instance ?Z Zakat) " +
                "      (instance ?Y Year) " +
                "      (during ?Y " +
                "        (WhenFn ?H)) " +
                "      (holdsDuring ?Y " +
                "        (attribute ?H FullyFormed)) " +
                "      (agent ?Z ?H) " +
                "      (patient ?Z ?T) " +
                "      (monetaryValue ?T ?C) " +
                "      (greaterThan ?C " +
                "        (MultiplicationFn ?W 0.025)))) Obligation)) ");
    	System.out.println("Quantified variables: " + f.collectQuantifiedVariables());
    	System.out.println("All variables: " + f.collectAllVariables());
    	System.out.println("Unquantified variables: " + f.collectUnquantifiedVariables());
    	System.out.println("Terms: " + f.collectTerms());
    }
    
    /** ***************************************************************
     * A test method.
     */
    public static void main(String[] args) {

    	testCollectVariables();
    	//testTptpParse();
        //Formula.resolveTest3();
        //Formula.unifyTest1();
        //Formula.unifyTest2();
        //Formula.unifyTest3();
        //Formula.unifyTest4();
        //Formula.unifyTest5();
        //Formula f1 = new Formula();
        //Formula f2 = new Formula();
        //Formula f3 = new Formula();
        /**
        f1.read("(=> (attribute ?Agent Investor) (exists (?Investing) (agent ?Investing ?Agent)))");
        System.out.println(f1);
        System.out.println(f1.clausify());
        f2.read("(attribute Bob Investor)");
        TreeMap tm = f1.unify(f2);
        if (tm != null)
            System.out.println(f1.substitute(tm));
        f3.read("(attribute ?X Investor)");
        tm = f3.unify(f2);
        System.out.println(tm);
        if (tm != null)
            System.out.println(f3.substitute(tm));
        f1.read("(=> (and (instance ?CITY AmericanCity) (part ?CITY California) " +
                "(not (equal ?CITY LosAngelesCalifornia))) (greaterThan (CardinalityFn " +
                "(ResidentFn LosAngelesCalifornia)) (CardinalityFn (ResidentFn ?CITY))))");
        System.out.println(f1);
        System.out.println(f1.clausify());
        f1.read("(not (instance ?X Human))");
        System.out.println(f1);
        System.out.println(f1.clausify());
        f1.read("(not (and (instance ?X Human) (attribute ?X Male)))");
        System.out.println(f1);
        System.out.println(f1.clausify());
        f1.read("(not (instance ?X2 Human))");
        System.out.println(f1.isSimpleNegatedClause());

        System.out.println(f1.append(f3));
 * */

        //f1.read("(not (and (exists (?MEMBER) (member ?MEMBER Org1-1)) (instance Org1-1 Foo))");
        //f1.read("(not (attribute ?VAR1 Criminal))");
        //f2.read("(or (not (attribute ?X5 American)) (not (instance ?X6 Weapon)) (not (instance ?X7 Nation)) " +
        //        "(not (attribute ?X7 Hostile)) (not (instance ?X8 Selling)) (not (agent ?X8 ?X5)) (not (patient ?X8 ?X6)) " +
        //        "(not (recipient ?X8 ?X7)) (attribute ?X5 Criminal))");
        //f1.read("(or (not (attribute ?VAR1 American)) (not (instance ?VAR2 Weapon)) (not (instance ?VAR3 Nation)) (not (attribute ?VAR3 Hostile)) (not (instance ?VAR4 Selling)) (not (agent ?VAR4 ?VAR1)) (not (patient ?VAR4 ?VAR2)) (not (recipient ?VAR4 ?VAR3)))");
        // (or
        //   (not
        //     (attribute ?VAR1 American))
        //   (not
        //     (instance ?VAR2 Weapon))
        //   (not
        //     (instance ?VAR3 Nation))
        //   (not
        //     (attribute ?VAR3 Hostile))
        //   (not
        //     (instance ?VAR4 Selling))
        //   (not
        //     (agent ?VAR4 ?VAR1))
        //   (not
        //     (patient ?VAR4 ?VAR2))
        //   (not
        //     (recipient ?VAR4 ?VAR3)))
        //f2.read("(or (agent ?X15 West) (not (possesses Nono ?X16)) (not (instance ?X16 Missile)))");
        //f1 = f1.clausify();
        //f2.read("(=> (instance ?X290 Collection) (exists (?X12) (and (instance ?X290 Foo) (member ?X12 ?X290))))");
        //System.out.println(f2.toNegAndPosLitsWithRenameInfo());
        //f2 = f2.clausify();
        //System.out.println(f2);
        //f3.read("(member (SkFn 1 ?X290) ?X290))");
        //System.out.println(f3.isSimpleClause());
        //Formula result = new Formula();
        //TreeMap mappings = f1.resolve(f2,result);
        //System.out.println(mappings);
        //System.out.println(result);
        //f1.read("(not (p a))");
        //f2.read("(not (p a))");
        //System.out.println(f1.unify(f2));
        //f1.read("(not (q a))");
        //f2.read("(not (p a))");
        //System.out.println(f1.unify(f2));

        //f1.read("(s O C)");
        //f2.read("(or (not (s ?X7 C)) (not (s O C)))");
        //Formula newResult = new Formula();
        //System.out.println(f1.resolve(f2,newResult));
        //System.out.println(newResult);

        //f1.read("(or (not (possesses Nono ?X16)) (not (instance ?X16 Missile)) (not (attribute West American)) (not (instance ?VAR2 Weapon)) (not (instance ?VAR3 Nation)) (not (attribute ?VAR3 Hostile)) (not (instance ?X15 Selling)) (not (patient ?X15 ?VAR2)) (not (recipient ?X15 ?VAR3))) ");
        //System.out.println(f1.toCanonicalClausalForm());

        /*
        f1.read("(not (enemies Nono America))");
        f2.read("(enemies Nono America)");
        System.out.println(f1.resolve(f2,f3));
        System.out.println(f3);
        f1.read("(or (not (agent ?VAR1 West)) (not (enemies Nono America)) (not (instance ?VAR1 Selling)) " +
                "(not (instance ?VAR2 Weapon)) (not (patient ?VAR1 ?VAR2)) (not (recipient ?VAR1 Nono)))");
        System.out.println(f1.resolve(f2,f3));
        System.out.println(f3); */

/*
        f1.read("()");
        f2.read("()");
        System.out.println(f1.appendClauseInCNF(f2));
        f2.read("(foo A B)");
        System.out.println(f1.appendClauseInCNF(f2));   // yields (foo A B)
        f1.read("(foo A B)");
        f2.read("(bar B C)");
        System.out.println(f1.appendClauseInCNF(f2));   // yields (or (foo A B) (bar B C))
        f1.read("(or (foo A B) (not (bar B C)))");
        f2.read("(not (baz D E))");
        System.out.println(f2.appendClauseInCNF(f1));   // yields (or (not (baz D E)) (foo A B) (not (bar B C)))
        System.out.println(f1.appendClauseInCNF(f2));   // yields (or (foo A B) (not (bar B C)) (not baz D E)))
        f1.read("(or (foo A B) (bar B C))");
        f2.read("(or (baz D E) (bop F G))");
        System.out.println(f1.appendClauseInCNF(f2));   // yields (or (foo A B) (bar B C) (baz D E) (bop F G))
*/
        /**
        f1.read("(member (SkFn 1 ?X3) ?X3)");
        f3.read("(member ?VAR1 Org1-1)");
        tm = f1.unify(f3);
        System.out.println(tm);
        System.out.println(f1.substitute(tm));
        System.out.println(f3.substitute(tm));

        f1.read("(attribute West American)");
        f3.read("(attribute ?VAR1 Criminal)");
        System.out.println(f1);
        System.out.println(f3);
        System.out.println(f1.unify(f3));
         * */

    }

}  // Formula.java
