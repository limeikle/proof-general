(*  Title:      HOL/SVC_Oracle.ML
    ID:         $Id: svc_oracle.ML,v 1.1 2006/12/19 15:22:46 da Exp $
    Author:     Lawrence C Paulson
    Copyright   1999  University of Cambridge

Installing the oracle for SVC (Stanford Validity Checker)

The following code merely CALLS the oracle;
  the soundness-critical functions are at HOL/Tools/svc_funcs.ML

Based upon the work of Soren T. Heilmann
*)


(*Generalize an Isabelle formula, replacing by Vars
  all subterms not intelligible to SVC.*)
fun svc_abstract t =
  let
    (*The oracle's result is given to the subgoal using compose_tac because
      its premises are matched against the assumptions rather than used
      to make subgoals.  Therefore , abstraction must copy the parameters
      precisely and make them available to all generated Vars.*)
    val params = Term.strip_all_vars t
    and body   = Term.strip_all_body t
    val Us = map #2 params
    val nPar = length params
    val vname = ref "V_a"
    val pairs = ref ([] : (term*term) list)
    fun insert t =
        let val T = fastype_of t
            val v = Logic.combound (Var ((!vname,0), Us--->T), 0, nPar)
        in  vname := Symbol.bump_string (!vname);
            pairs := (t, v) :: !pairs;
            v
        end;
    fun replace t =
        case t of
            Free _  => t  (*but not existing Vars, lest the names clash*)
          | Bound _ => t
          | _ => (case AList.lookup Pattern.aeconv (!pairs) t of
                      SOME v => v
                    | NONE   => insert t)
    (*abstraction of a numeric literal*)
    fun lit (t as Const("0", _)) = t
      | lit (t as Const("1", _)) = t
      | lit (t as Const("Numeral.number_of", _) $ w) = t
      | lit t = replace t
    (*abstraction of a real/rational expression*)
    fun rat ((c as Const("HOL.plus", _)) $ x $ y) = c $ (rat x) $ (rat y)
      | rat ((c as Const("HOL.minus", _)) $ x $ y) = c $ (rat x) $ (rat y)
      | rat ((c as Const("HOL.divide", _)) $ x $ y) = c $ (rat x) $ (rat y)
      | rat ((c as Const("HOL.times", _)) $ x $ y) = c $ (rat x) $ (rat y)
      | rat ((c as Const("HOL.uminus", _)) $ x) = c $ (rat x)
      | rat t = lit t
    (*abstraction of an integer expression: no div, mod*)
    fun int ((c as Const("HOL.plus", _)) $ x $ y) = c $ (int x) $ (int y)
      | int ((c as Const("HOL.minus", _)) $ x $ y) = c $ (int x) $ (int y)
      | int ((c as Const("HOL.times", _)) $ x $ y) = c $ (int x) $ (int y)
      | int ((c as Const("HOL.uminus", _)) $ x) = c $ (int x)
      | int t = lit t
    (*abstraction of a natural number expression: no minus*)
    fun nat ((c as Const("HOL.plus", _)) $ x $ y) = c $ (nat x) $ (nat y)
      | nat ((c as Const("HOL.times", _)) $ x $ y) = c $ (nat x) $ (nat y)
      | nat ((c as Const("Suc", _)) $ x) = c $ (nat x)
      | nat t = lit t
    (*abstraction of a relation: =, <, <=*)
    fun rel (T, c $ x $ y) =
            if T = HOLogic.realT then c $ (rat x) $ (rat y)
            else if T = HOLogic.intT then c $ (int x) $ (int y)
            else if T = HOLogic.natT then c $ (nat x) $ (nat y)
            else if T = HOLogic.boolT then c $ (fm x) $ (fm y)
            else replace (c $ x $ y)   (*non-numeric comparison*)
    (*abstraction of a formula*)
    and fm ((c as Const("op &", _)) $ p $ q) = c $ (fm p) $ (fm q)
      | fm ((c as Const("op |", _)) $ p $ q) = c $ (fm p) $ (fm q)
      | fm ((c as Const("op -->", _)) $ p $ q) = c $ (fm p) $ (fm q)
      | fm ((c as Const("Not", _)) $ p) = c $ (fm p)
      | fm ((c as Const("True", _))) = c
      | fm ((c as Const("False", _))) = c
      | fm (t as Const("op =",  Type ("fun", [T,_])) $ _ $ _) = rel (T, t)
      | fm (t as Const("Orderings.less",  Type ("fun", [T,_])) $ _ $ _) = rel (T, t)
      | fm (t as Const("Orderings.less_eq", Type ("fun", [T,_])) $ _ $ _) = rel (T, t)
      | fm t = replace t
    (*entry point, and abstraction of a meta-formula*)
    fun mt ((c as Const("Trueprop", _)) $ p) = c $ (fm p)
      | mt ((c as Const("==>", _)) $ p $ q)  = c $ (mt p) $ (mt q)
      | mt t = fm t  (*it might be a formula*)
  in (list_all (params, mt body), !pairs) end;


(*Present the entire subgoal to the oracle, assumptions and all, but possibly
  abstracted.  Use via compose_tac, which performs no lifting but will
  instantiate variables.*)

fun svc_tac i st =
  let
    val (abs_goal, _) = svc_abstract (Logic.get_goal (Thm.prop_of st) i)
    val th = svc_oracle (Thm.theory_of_thm st) abs_goal
   in compose_tac (false, th, 0) i st end
   handle TERM _ => no_tac st;


(*check if user has SVC installed*)
fun svc_enabled () = getenv "SVC_HOME" <> "";
fun if_svc_enabled f x = if svc_enabled () then f x else ();
