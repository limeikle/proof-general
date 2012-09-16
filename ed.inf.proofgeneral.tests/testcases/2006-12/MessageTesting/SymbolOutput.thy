theory SymbolOutput imports Main
begin

(* This is a test of escaping in symbol output in Isabelle (not us) *)
thm trancl_def

ML {* string_of_thm (thm "trancl_def") *}

(* this should produce a readable output, now one that is
multiply quoted! *)

(* CORRECT: *)

(* <pgmltext>trancl <sym name="equiv">&lt;equiv;&gt;</sym> etc... *)

(* INCORRECT (too much escaping): *)

(* <pgmltext>trancl &lt;sym name = &quot;equiv&quot;&gt;\&amp;lt;equiv&amp;gt;&lt;/sym&gt; *)

(* Goal text should be similar: *)

lemma "P ≡ P"
by auto

lemma "(λx. x*x) y >= (y::nat)"
by simp

(* This should look like this: (CORRECT):
goal (1 subgoal):
 1. <atom kind = "free">y</atom> <sym name = "le">\&lt;le&gt;</sym> <atom kind = "free">y</atom> * <atom kind = "free">y</atom></statedisplay></pgml></proofstate></pgip>
<pgip tag = "Isabelle/Isar" id = "space.davesnet/da/679/1172539075.078" destid = "PG-Eclipse" class = "pg" refid = "PG-Eclipse" refseq = "263" seq = "536"><ready/></pgip>
*)

end
