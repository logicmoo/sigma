/** This code is copyright Articulate Software (c) 2003.
This software is released under the GNU Public License
<http://www.gnu.org/copyleft/gpl.html>.  Users of this code also consent,
by use of this code, to credit Articulate Software and Teknowledge in any
writings, briefings, publications, presentations, or other representations
of any software which incorporates, builds on, or uses this code.  Please
cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, in Working
Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico. see also 
http://sigmakee.sourceforge.net 
*/

/*************************************************************************************************/
package com.articulate.sigma;

import TPTPWorld.AnswerExtractor;
import TPTPWorld.Binding;
import TPTPWorld.TPTPFormula;
import TPTPWorld.TPTPParser;
import tptp_parser.SimpleTptpParserOutput;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;

public class TPTP2SUMO {

  /** ***************************************************************
   * Convenience routine that calls the main convert() method below
   */
  public static String convert (String tptp, boolean instantiated) throws Exception { 

      return convert(new BufferedReader(new StringReader(tptp)), null, instantiated);
  }

  /** ***************************************************************
   * Convenience routine that calls the main convert() method below
   */
  public static String convert (Reader in, boolean instantiated) throws Exception {

      return convert(new BufferedReader(in), null, instantiated);
  }

  /** ***************************************************************
   * Convenience routine that calls the main convert() method below
   */
  public static String convert (String tptp, ArrayList<Binding> answer, 
                                boolean instantiated) throws Exception {

      return convert(new BufferedReader(new StringReader(tptp)), answer, instantiated);
  }

  /** ***************************************************************
   * Convenience routine that calls the main convert() method below
   */
  public static String convert (Reader in, ArrayList<Binding> answer, 
                                boolean instantiated) throws Exception {

      return convert(new BufferedReader(in), answer, instantiated);
  }

  /** ***************************************************************
   * Convert a TPTP proof to a SUMO XML-wrapped proof
   */
  public static String convert(BufferedReader reader, ArrayList<Binding> answer, 
                               boolean instantiated) throws Exception {

      StringBuffer result = new StringBuffer();
      TPTPParser parser = TPTPParser.parse(reader);
      Hashtable<String,TPTPFormula> ftable = parser.ftable;
      Vector<SimpleTptpParserOutput.TopLevelItem> Items = parser.Items;
//      System.out.println("# of formulas: " + ftable.size());
//      System.out.println("# of Items: " + Items.size());
  
      //----Start SUMO output
      result.append("<queryResponse>\n");
      result.append("  <answer result='yes' number='1'>\n");
  
      //----Start proof output
      StringBuffer proof = new StringBuffer();
      proof.append("    <proof>\n");
      for (SimpleTptpParserOutput.TopLevelItem item : Items) {
          String name = TPTPParser.getName(item);
          TPTPFormula formula = ftable.get(name);
          proof.append(convertTPTPFormula(formula, ftable, instantiated));
      }
      //----End proof output
      proof.append("    </proof>\n");
  
      // print out answer (if exists)
      // (CURRENTLY: only metis proof prints variable bindings)
      StringBuffer binding = new StringBuffer();
      ArrayList<Binding> binds;
      if (answer != null && !answer.isEmpty()) 
          binds = answer;
      else 
          binds = AnswerExtractor.extractAnswers(ftable);      
      if (!binds.isEmpty()) {
          binding.append("  <bindingSet type='definite'>\n");
          binding.append("    <binding>\n");
          for (Binding bind : binds) {
              assert bind.binding != null;
              binding.append("      <var name='?" + transformVariable(bind.variable) + 
                             "' value='" + transformTerm(bind.binding) + "'/>\n");
          }
          binding.append("    </binding>\n");
          binding.append("  </bindingSet>\n");
      }
  
      //----Append proof after bindings (xml order matters)
      result.append(binding);
      result.append(proof);
  
      //----End SUMO output
      result.append("  </answer>\n");
      result.append("  <summary proofs='1'/>\n");
      result.append("</queryResponse>\n");
  
      return result.toString();
  }

  /** ***************************************************************
   * Convert a TPTP proof step to a SUMO XML-wrapped proof step
   */
  private static StringBuffer convertTPTPFormula (TPTPFormula formula, 
                                                  Hashtable<String,TPTPFormula> ftable, 
                                                  boolean instantiated) {

      StringBuffer result = new StringBuffer();
      int indent = 12;
      int indented = 0;
      
      result.append("      <proofStep>\n");
      result.append("        <premises>\n");
      SimpleTptpParserOutput.Annotations annotations = null;    
      SimpleTptpParserOutput.Source source = null;
      String sourceInfo = "";
  
      //----Add parents info as "premises"
      for (TPTPFormula parent : formula.parent) {
          result.append("          <premise>\n");
          result.append(convertType(parent, indent+2, indented));
          result.append("          </premise>\n");
      }
      result.append("        </premises>\n");
      result.append("        <conclusion>\n");
      result.append(convertType(formula, indent, indented));
      if (formula.parent.isEmpty()) {
          if (formula.type.equals("conjecture")) {
        	  if (!instantiated) 
                  result.append("          <query type='" + ProofStep.QUERY + "'/>\n");              
        	  else 
                  result.append("          <query type='" + ProofStep.INSTANTIATED_QUERY + "'/>\n");              
          } 
          else if (formula.type.equals("negated_conjecture")) 
              result.append("          <query type='" + ProofStep.NEGATED_QUERY + "'/>\n");          
      }
      result.append("        </conclusion>\n");
      result.append("      </proofStep>\n");
      return result;
  }

  /** ***************************************************************
   * Convert a single annotated TPTP clause to a single XML-wrapped SUMO formula.
   * This is the main entry point for this class.
   */
  public static StringBuffer convertType (TPTPFormula formula, int indent, int indented) {
  
      return convertType(formula,indent,indented,false);
  }
  
  /** ***************************************************************
   * Convert a single annotated TPTP clause to a single SUMO formula, possibly XML-wrapped.
   * This is the main entry point for this class.
   */
  public static StringBuffer convertType (TPTPFormula formula, int indent, int indented, boolean noXML) {

      StringBuffer result = new StringBuffer();
      String type = "";
      int id = formula.id;
      SimpleTptpParserOutput.TopLevelItem item = formula.item;
      if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Formula) {
          SimpleTptpParserOutput.AnnotatedFormula AF = (SimpleTptpParserOutput.AnnotatedFormula) item;
          type = "formula";
          if (!noXML)
              result.append(addIndent(indent-2,indented) + "<" + type + " number='" + id + "'>\n");
          result.append(convertFormula(AF.getFormula(),indent,indented));
          if (!noXML)
        	  result.append("\n" + addIndent(indent-2,indented) + "</" + type + ">\n");
      } 
      else if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Clause) {
          SimpleTptpParserOutput.AnnotatedClause AC = (SimpleTptpParserOutput.AnnotatedClause) item;
          type = "clause";
          if (!noXML)
              result.append(addIndent(indent-2,indented) + "<" + type + " number='" + id + "'>\n");
          result.append(convertClause(AC.getClause(),indent,indented));
          if (!noXML)
        	  result.append("\n" + addIndent(indent-2,indented) + "</" + type + ">\n");
      } 
      else 
          result.append("Error: TPTP Formula syntax unknown for converting");      
      return result;
  }

  /** ***************************************************************
   */
  private static StringBuffer convertConnective (SimpleTptpParserOutput.BinaryConnective connective) {

      StringBuffer result = new StringBuffer();
      switch (connective) {
      case And:
          result.append("and");
          break;
      case Or:
          result.append("or");
          break;
      case Equivalence: 
          result.append("<=>");
          break;
      case Implication:
          result.append("=>");
          break;
      case ReverseImplication:
          result.append("<=");
          break;
      case Disequivalence:
          result.append("not <=>");
          break;
      case NotOr:
          result.append("not or");
          break;
      case NotAnd:
          result.append("not and");
          break;
      default:
          result.append("Not a connective");
          break;
      }
      return result;
  }

  /** ***************************************************************
   */
  private static String convertQuantifier (SimpleTptpParserOutput.Quantifier quantifier) {
  
      switch (quantifier) {
      case ForAll:
          return "forall";
      case Exists:
          return "exists";
      default:
          return "Not a quantifier";
      }
  }

  /** ***************************************************************
   */
  private static boolean kifAssociative (SimpleTptpParserOutput.BinaryConnective connective) {

      switch (connective) {
      case And:
      case Or:
          return true;
      default:
          return false;
      }
  }

  /** ***************************************************************
   */
  private static String addIndent (int indent, int indented) {

      String res = "";
      for (int i = indented+1; i <= indent; i++) 
          res += " ";      
      return res;    
  }

  /** ***************************************************************
   * remove dollar sign, for special tptp terms such as $false and $true
   */
  private static String removeDollarSign (String argument) {

      if (argument.length() > 0) {
          if (argument.charAt(0) == '$') 
              return argument.substring(1,argument.length());
          else
              return argument;          
      }
      return "";
  }
  
  /** ***************************************************************
   * remove termVariablePrefix
   */
  private static String transformVariable (String variable) {

      return variable.replace(Formula.termVariablePrefix, "");
  } 

  /** ***************************************************************
   * remove termSymbolPrefix and termMentionSuffix
   */
  public static String transformTerm (String term) {

      term = term.replaceFirst(Formula.termSymbolPrefix, "");
      term = term.replace(Formula.termMentionSuffix, "");
      return term;
  }

  /** ***************************************************************
   * A term is a variable or function symbol with term arguments.
   * Variables are symbols with a question mark in SUO-KIF or capitalized symbols
   * in TPTP.  Constants are functions with arity 0.
   */
  private static String convertTerm (SimpleTptpParserOutput.Formula.Atomic atom) {

	//  System.out.println("INFO in TPTP2SUMO.convertTerm(): " + atom);
      String res = "";
      LinkedList<SimpleTptpParserOutput.Term> arguments = (LinkedList)atom.getArguments();
      if (arguments != null) 
          res += "(";
      // "esk" comes up in the function SigGetNewSkolemCode in E's source code, with the form esk<count>_<ar>, standing for Skolemization
      if (!atom.getPredicate().startsWith("esk"))
          res += transformTerm(removeDollarSign(atom.getPredicate()));
      if (arguments != null) {
          for (int n = 0; n < arguments.size();  n++) {
              if (((SimpleTptpParserOutput.Term)arguments.get(n)).getTopSymbol().isVariable()) 
                  res += " " + "?" + transformVariable(arguments.get(n).toString());
              else if (arguments.get(n).toString().indexOf("(") != -1)
            	  res += " " + convertTerm(arguments.get(n).toAtom());
              else
                  res += " " + transformTerm(removeDollarSign(arguments.get(n).toString()));              
          }
      }
      if (arguments != null) 
          res += ")";      
      return res;
  }

  /** ***************************************************************
   * A formula is a literal or compound formula.
   * A compound formula consists of  (logical operator formula [formula]*)
   * (but conforming to arity of logical operators).
   */
  private static StringBuffer convertFormula (SimpleTptpParserOutput.Formula formula, int indent, int indented) {

	//  System.out.println("INFO in TPTP2SUMO.convertFormula(): " + formula);
      StringBuffer result = new StringBuffer();
      switch(formula.getKind()) {
      case Atomic:
          result.append(addIndent(indent,indented));
          result.append(convertTerm((SimpleTptpParserOutput.Formula.Atomic)formula));
          break;
      case Negation:
          result.append(addIndent(indent,indented));
          result.append("(" + "not" + " ");
          result.append(convertFormula(((SimpleTptpParserOutput.Formula.Negation)formula).getArgument(),indent+4,indent+4));
          result.append(")");
          break;
      case Binary:
          result.append(addIndent(indent,indented));
          result.append("(");
          result.append(convertConnective(((SimpleTptpParserOutput.Formula.Binary)formula).getConnective()));
          result.append(" ");
          result.append(convertFormula(((SimpleTptpParserOutput.Formula.Binary)formula).getLhs(),indent+4,indent+4));
          result.append("\n");
          while (kifAssociative(((SimpleTptpParserOutput.Formula.Binary)formula).getConnective()) &&
                 ((SimpleTptpParserOutput.Formula.Binary)formula).getRhs().getKind() == SimpleTptpParserOutput.Formula.Kind.Binary &&
                 ((SimpleTptpParserOutput.Formula.Binary)formula).getConnective() == 
                   ((SimpleTptpParserOutput.Formula.Binary)((SimpleTptpParserOutput.Formula.Binary)formula).getRhs()).getConnective()) {
              formula = ((SimpleTptpParserOutput.Formula.Binary)formula).getRhs();
              result.append(convertFormula(((SimpleTptpParserOutput.Formula.Binary)formula).getLhs(),indent+4,0));
              result.append("\n");
          }
          result.append(convertFormula(((SimpleTptpParserOutput.Formula.Binary)formula).getRhs(),indent+4,0));
          result.append(")");
          break;
      case Quantified:
          result.append(addIndent(indent,indented));
          result.append("(");
          result.append(convertQuantifier(((SimpleTptpParserOutput.Formula.Quantified)formula).getQuantifier()));
          result.append(" (");
          result.append("?" + transformVariable(((SimpleTptpParserOutput.Formula.Quantified)formula).getVariable()));
          while (((SimpleTptpParserOutput.Formula.Quantified)formula).getKind() == SimpleTptpParserOutput.Formula.Kind.Quantified &&
                 ((SimpleTptpParserOutput.Formula.Quantified)formula).getMatrix().getKind() == SimpleTptpParserOutput.Formula.Kind.Quantified &&
                 ((SimpleTptpParserOutput.Formula.Quantified)formula).getQuantifier() == 
                   ((SimpleTptpParserOutput.Formula.Quantified)((SimpleTptpParserOutput.Formula.Quantified)formula).getMatrix()).getQuantifier()) {
              formula = ((SimpleTptpParserOutput.Formula.Quantified)formula).getMatrix();
              result.append(" ?" + transformVariable(((SimpleTptpParserOutput.Formula.Quantified)formula).getVariable()));
          }
          result.append(") ");
          if (((SimpleTptpParserOutput.Formula.Quantified)formula).getMatrix().getKind() == SimpleTptpParserOutput.Formula.Kind.Negation ||
              ((SimpleTptpParserOutput.Formula.Quantified)formula).getMatrix().getKind() == SimpleTptpParserOutput.Formula.Kind.Atomic) {
              result.append(convertFormula(((SimpleTptpParserOutput.Formula.Quantified)formula).getMatrix(),indent,indent));
          } 
          else {
              result.append("\n");
              result.append(convertFormula(((SimpleTptpParserOutput.Formula.Quantified)formula).getMatrix(),indent + 4,0));
          }
          result.append(")");
          break;
      default:
          result.append("Error in TPTP2SUMO.convertFormula(): TPTP Formula syntax unkown for converting");
          break;
      }
      return result;
  }

  /** ***************************************************************
   */
  private static StringBuffer convertClause (SimpleTptpParserOutput.Clause clause, int indent, int indented) {

	//  System.out.println("INFO in TPTP2SUMO.convertClause(): " + clause);
      StringBuffer result = new StringBuffer();
      LinkedList<SimpleTptpParserOutput.Literal> literals = (LinkedList) clause.getLiterals();
      result.append(addIndent(indent,indented));
      if (literals == null) {
          result.append("false\n");
          return result;
      }   
      assert !literals.isEmpty();
      if (literals.size() == 1)
          result.append(convertLiteral(literals.get(0),indent,indent));
      else {
          result.append("(");
          result.append(convertConnective(SimpleTptpParserOutput.BinaryConnective.Or));
          result.append(" ");
          result.append(convertLiteral(literals.get(0),indent,indent));
          for (int i = 1; i < literals.size(); i++) {
              result.append("\n");
              result.append(convertLiteral(literals.get(i),indent+4,0));
          }
          result.append(")");
      }
      return result;
  }

  /** ***************************************************************
   * An atom is a predicate symbol with term arguments.
   * A literal is an atom or negated atom.
   */
  private static StringBuffer convertLiteral (SimpleTptpParserOutput.Literal literal, int indent, int indented) {
  
	//  System.out.println("INFO in TPTP2SUMO.convertLiteral(): " + literal);
      StringBuffer result = new StringBuffer();
      result.append(addIndent(indent,indented));
      if (literal.isPositive()) 
          result.append(convertTerm((SimpleTptpParserOutput.Formula.Atomic)literal.getAtom()));
      else {
          result.append("(not ");
          result.append(convertTerm((SimpleTptpParserOutput.Formula.Atomic)literal.getAtom()));
          result.append(")");
      }
      return result;
  }

  /** ***************************************************************
   */
  public static void main (String args[]) {

      //String formula = "fof(1,axiom,(    s_holds_2__(s_p,s_a) ),    file('/tmp/SystemOnTPTP11002/Simple2965.tptp',kb_Simple_1)).fof(2,conjecture,(    s_holds_2__(s_p,s_a) ),    file('/tmp/SystemOnTPTP11002/Simple2965.tptp',prove_from_Simple)).fof(3,negated_conjecture,(    ~ s_holds_2__(s_p,s_a) ),    inference(assume_negation,[status(cth)],[2])).fof(4,negated_conjecture,(    ~ s_holds_2__(s_p,s_a) ),    inference(fof_simplification,[status(thm)],[3,theory(equality)])).cnf(5,plain,    ( s_holds_2__(s_p,s_a) ),    inference(split_conjunct,[status(thm)],[1])).cnf(6,negated_conjecture,    ( ~ s_holds_2__(s_p,s_a) ),    inference(split_conjunct,[status(thm)],[4])).cnf(7,negated_conjecture,    ( $false ),    inference(rw,[status(thm)],[6,5,theory(equality)])).cnf(8,negated_conjecture,    ( $false ),    inference(cn,[status(thm)],[7,theory(equality)])).cnf(9,negated_conjecture,    ( $false ),    8,    [proof]).";
      //String formula = "fof(pel55,conjecture,(killed(X,Z) )). cnf(1,plain,( agatha = butler| hates(agatha,agatha) ),inference(subst,[[X,$fot(X0)]],[pel55])). cnf(6,plain,( a ) , inference(subst,[[X0,$fot(skolemFOFtoCNF_X)],[Z,$fot(a)]],[1]))."; 
      //String formula = "fof(pel55_1,axiom,(? [X] : (lives(X) & killed(X,agatha) ) )).fof(pel55,conjecture,(? [X] : killed(X,agatha) )).cnf(0,plain,(killed(skolemFOFtoCNF_X,agatha)), inference(fof_to_cnf,[],[pel55_1])).cnf(1,plain,(~killed(X,agatha)),inference(fof_to_cnf,[],[pel55])).cnf(2,plain,(~killed(skolemFOFtoCNF_X,agatha)),inference(subst,[[X,$fot(skolemFOFtoCNF_X)]],[1])).cnf(3,theorem,($false),inference(resolve,[$cnf(killed(skolemFOFtoCNF_X,agatha))],[0,2])).";
      //String formula = "fof(ax1,axiom,(! [X0] : (~s__irreflexiveOn(s__relatedInternalConcept__m,X0) | ! [X1] : (~s__instance(X0,s__SetOrClass) | ~s__instance(X1,X0) | ~s__relatedInternalConcept(X1,X1))))).";
      //String clause  = "cnf(ax1,axiom,(~s__irreflexiveOn(s__relatedInternalConcept__m,X0) | ~s__instance(X0,s__SetOrClass) | ~s__instance(X1,X0) | ~s__relatedInternalConcept(X1,X1))).";
      String clause = "cnf(c_0_10,negated_conjecture,($answer(esk1_1(X1))|~s__subclass(X1,s__Object)), c_0_8).";
      String inFile;
      FileReader file;
      String kif = "";
      try {
          if (args.length == 1) {
              inFile = args[0];
	          file = new FileReader(inFile);
	          kif = TPTP2SUMO.convert(file, false);
	          System.out.println("START---");
	          System.out.println(kif);
	          System.out.println("END-----");
          }
          else {
              StringReader reader = new StringReader(clause);
              // kif = TPTP2SUMO.convert(reader, false);
              TPTPParser tptpP = TPTPParser.parse(new BufferedReader(reader));
              System.out.println(tptpP.Items.get(0));
              Iterator<String> it = tptpP.ftable.keySet().iterator();
              while (it.hasNext()) {
            	  String id = it.next();
            	  TPTPFormula tptpF = tptpP.ftable.get(id);
            	  System.out.println(convertType(tptpF,0,0));
              }
          }
      } 
      catch (Exception e) { 
          System.out.println("e: " + e);
      }
  }
}
 
