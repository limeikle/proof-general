(*  Title:      HOL/ex/Merge.thy
    ID:         $Id: MergeSort.thy,v 1.1 2006/12/19 15:22:46 da Exp $
    Author:     Tobias Nipkow
    Copyright   2002 TU Muenchen
*)

header{*Merge Sort*}

theory MergeSort
imports Sorting
begin

consts merge :: "('a::linorder)list * 'a list \<Rightarrow> 'a list"

recdef merge "measure(%(xs,ys). size xs + size ys)"
    "merge(x#xs, y#ys) =
         (if x \<le> y then x # merge(xs, y#ys) else y # merge(x#xs, ys))"

    "merge(xs,[]) = xs"

    "merge([],ys) = ys"

lemma multiset_of_merge[simp]:
     "multiset_of (merge(xs,ys)) = multiset_of xs + multiset_of ys"
apply(induct xs ys rule: merge.induct)
apply (auto simp: union_ac)
done

lemma set_merge[simp]: "set(merge(xs,ys)) = set xs \<union> set ys"
apply(induct xs ys rule: merge.induct)
apply auto
done

lemma sorted_merge[simp]:
     "sorted (op \<le>) (merge(xs,ys)) = (sorted (op \<le>) xs & sorted (op \<le>) ys)"
apply(induct xs ys rule: merge.induct)
apply(simp_all add: ball_Un linorder_not_le order_less_le)
apply(blast intro: order_trans)
done

consts msort :: "('a::linorder) list \<Rightarrow> 'a list"
recdef msort "measure size"
    "msort [] = []"
    "msort [x] = [x]"
    "msort xs = merge(msort(take (size xs div 2) xs),
		      msort(drop (size xs div 2) xs))"

theorem sorted_msort: "sorted (op \<le>) (msort xs)"
by (induct xs rule: msort.induct) simp_all

theorem multiset_of_msort: "multiset_of (msort xs) = multiset_of xs"
apply (induct xs rule: msort.induct)
  apply simp_all
apply (subst union_commute)
apply (simp del:multiset_of_append add:multiset_of_append[symmetric] union_assoc)
apply (simp add: union_ac)
done

end
