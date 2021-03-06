(*  ID:         $Id: InductiveInvariant_examples.thy,v 1.1 2006/12/19 15:22:46 da Exp $
    Author:	Sava Krsti\'{c} and John Matthews
*)

header {* Example use if an inductive invariant to solve termination conditions *}

theory InductiveInvariant_examples imports InductiveInvariant  begin

text "A simple example showing how to use an inductive invariant
      to solve termination conditions generated by recdef on
      nested recursive function definitions."

consts g :: "nat => nat"

recdef (permissive) g "less_than"
  "g 0 = 0"
  "g (Suc n) = g (g n)"

text "We can prove the unsolved termination condition for
      g by showing it is an inductive invariant."

recdef_tc g_tc[simp]: g
apply (rule allI)
apply (rule_tac x=n in tfl_indinv_wfrec [OF g_def])
apply (auto simp add: indinv_def split: nat.split)
apply (frule_tac x=nat in spec)
apply (drule_tac x="f nat" in spec)
by auto


text "This declaration invokes Isabelle's simplifier to
      remove any termination conditions before adding
      g's rules to the simpset."
declare g.simps [simplified, simp]


text "This is an example where the termination condition generated
      by recdef is not itself an inductive invariant."

consts g' :: "nat => nat"
recdef (permissive) g' "less_than"
  "g' 0 = 0"
  "g' (Suc n) = g' n + g' (g' n)"

thm g'.simps


text "The strengthened inductive invariant is as follows
      (this invariant also works for the first example above):"

lemma g'_inv: "g' n = 0"
thm tfl_indinv_wfrec [OF g'_def]
apply (rule_tac x=n in tfl_indinv_wfrec [OF g'_def])
by (auto simp add: indinv_def split: nat.split)

recdef_tc g'_tc[simp]: g'
by (simp add: g'_inv)

text "Now we can remove the termination condition from
      the rules for g' ."
thm g'.simps [simplified]


text {* Sometimes a recursive definition is partial, that is, it
        is only meant to be invoked on "good" inputs. As a contrived
        example, we will define a new version of g that is only
        well defined for even inputs greater than zero. *}

consts g_even :: "nat => nat"
recdef (permissive) g_even "less_than"
  "g_even (Suc (Suc 0)) = 3"
  "g_even n = g_even (g_even (n - 2) - 1)"


text "We can prove a conditional version of the unsolved termination
      condition for @{term g_even} by proving a stronger inductive invariant."

lemma g_even_indinv: "\<exists>k. n = Suc (Suc (2*k)) ==> g_even n = 3"
apply (rule_tac D="{n. \<exists>k. n = Suc (Suc (2*k))}" and x=n in tfl_indinv_on_wfrec [OF g_even_def])
apply (auto simp add: indinv_on_def split: nat.split)
by (case_tac ka, auto)


text "Now we can prove that the second recursion equation for @{term g_even}
      holds, provided that n is an even number greater than two."

theorem g_even_n: "\<exists>k. n = 2*k + 4 ==> g_even n = g_even (g_even (n - 2) - 1)"
apply (subgoal_tac "(\<exists>k. n - 2 = 2*k + 2) & (\<exists>k. n = 2*k + 2)")
by (auto simp add: g_even_indinv, arith)


text "McCarthy's ninety-one function. This function requires a
      non-standard measure to prove termination."

consts ninety_one :: "nat => nat"
recdef (permissive) ninety_one "measure (%n. 101 - n)"
  "ninety_one x = (if 100 < x
                     then x - 10
                     else (ninety_one (ninety_one (x+11))))"

text "To discharge the termination condition, we will prove
      a strengthened inductive invariant:
         S x y == x < y + 11"

lemma ninety_one_inv: "n < ninety_one n + 11"
apply (rule_tac x=n in tfl_indinv_wfrec [OF ninety_one_def])
apply force
apply (auto simp add: indinv_def)
apply (frule_tac x="x+11" in spec)
apply (frule_tac x="f (x + 11)" in spec)
by arith

text "Proving the termination condition using the
      strengthened inductive invariant."

recdef_tc ninety_one_tc[rule_format]: ninety_one
apply clarify
by (cut_tac n="x+11" in ninety_one_inv, arith)

text "Now we can remove the termination condition from
      the simplification rule for @{term ninety_one}."

theorem def_ninety_one:
"ninety_one x = (if 100 < x
                   then x - 10
                   else ninety_one (ninety_one (x+11)))"
by (subst ninety_one.simps,
    simp add: ninety_one_tc)

end
