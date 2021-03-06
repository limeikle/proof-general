(*
    ID:         $Id: reflection.ML,v 1.1 2006/12/19 15:22:46 da Exp $
    Author:     Amine Chaieb, TU Muenchen

A trial for automatical reification.
*)

signature REFLECTION = sig
  val genreify_tac: Proof.context -> thm list -> term option -> int -> tactic
  val reflection_tac: Proof.context -> thm list -> term option -> int -> tactic
  val gen_reflection_tac: Proof.context -> (cterm -> thm)
    -> thm list -> term option -> int -> tactic
end;

structure Reflection : REFLECTION
= struct

val ext2 = thm "ext2";
val nth_Cons_0 = thm "nth_Cons_0";
val nth_Cons_Suc = thm "nth_Cons_Suc";

  (* Make a congruence rule out of a defining equation for the interpretation *)
  (* th is one defining equation of f, i.e.
     th is "f (Cp ?t1 ... ?tn) = P(f ?t1, .., f ?tn)" *)
  (* Cp is a constructor pattern and P is a pattern *)

  (* The result is:
      [|?A1 = f ?t1 ; .. ; ?An= f ?tn |] ==> P (?A1, .., ?An) = f (Cp ?t1 .. ?tn) *)
  (*  + the a list of names of the A1 .. An, Those are fresh in the ctxt*)


fun mk_congeq ctxt fs th = 
  let 
   val (f as Const(fN,fT)) = th |> prop_of |> HOLogic.dest_Trueprop |> HOLogic.dest_eq 
				|> fst |> strip_comb |> fst
   val thy = ProofContext.theory_of ctxt
   val cert = Thm.cterm_of thy
   val (((_,_),[th']), ctxt') = Variable.import true [th] ctxt
   val (lhs, rhs) = HOLogic.dest_eq (HOLogic.dest_Trueprop (Thm.prop_of th'))
   fun add_fterms (t as t1 $ t2) = 
       if exists (fn f => (t |> strip_comb |> fst) aconv f) fs then insert (op aconv) t
       else add_fterms t1 #> add_fterms t2
     | add_fterms (t as Abs(xn,xT,t')) = 
       if (fN mem (term_consts t)) then (fn _ => [t]) else (fn _ => [])
     | add_fterms _ = I
   val fterms = add_fterms rhs []
   val (xs, ctxt'') = Variable.variant_fixes (replicate (length fterms) "x") ctxt'
   val tys = map fastype_of fterms
   val vs = map Free (xs ~~ tys)
   val env = fterms ~~ vs
		    (* FIXME!!!!*)	
   fun replace_fterms (t as t1 $ t2) =
       (case AList.lookup (op aconv) env t of
	    SOME v => v
	  | NONE => replace_fterms t1 $ replace_fterms t2)
     | replace_fterms t = (case AList.lookup (op aconv) env t of
			       SOME v => v
			     | NONE => t)
      
   fun mk_def (Abs(x,xT,t),v) = HOLogic.mk_Trueprop ((HOLogic.all_const xT)$ Abs(x,xT,HOLogic.mk_eq(v$(Bound 0), t)))
     | mk_def (t, v) = HOLogic.mk_Trueprop (HOLogic.mk_eq (v, t))
   fun tryext x = (x RS ext2 handle _ =>  x)
   val cong = (Goal.prove ctxt'' [] (map mk_def env)
			  (HOLogic.mk_Trueprop (HOLogic.mk_eq (lhs, replace_fterms rhs)))
			  (fn x => LocalDefs.unfold_tac (#context x) (map tryext (#prems x)) 
							THEN rtac th' 1)) RS sym
	      
   val (cong' :: vars') = 
       Variable.export ctxt'' ctxt (cong :: map (Drule.mk_term o cert) vs)
   val vs' = map (fst o fst o Term.dest_Var o Thm.term_of o Drule.dest_term) vars'
					      
  in  (vs', cong') end; 
 (* congs is a list of pairs (P,th) where th is a theorem for *)
        (* [| f p1 = A1; ...; f pn = An|] ==> f (C p1 .. pn) = P *)
val FWD = curry (op OF);

 (* da is the decomposition for atoms, ie. it returns ([],g) where g
 returns the right instance f (AtC n) = t , where AtC is the Atoms
 constructor and n is the number of the atom corresponding to t *)

(* Generic decomp for reification : matches the actual term with the
rhs of one cong rule. The result of the matching guides the
proof synthesis: The matches of the introduced Variables A1 .. An are
processed recursively
 The rest is instantiated in the cong rule,i.e. no reification is needed *)

exception REIF of string;

val bds = ref ([]: (typ * ((term list) * (term list))) list);

fun index_of t = 
 let 
  val tt = HOLogic.listT (fastype_of t)
 in 
  (case AList.lookup (op =) (!bds) tt of
    NONE => error "index_of : type not found in environements!"
  | SOME (tbs,tats) =>
    let
     val i = find_index_eq t tats
     val j = find_index_eq t tbs 
    in (if j= ~1 then 
	    if i= ~1 
	    then (bds := AList.update (op =) (tt,(tbs,tats@[t])) (!bds) ; 
		  length tbs + length tats) 
	    else i else j)
    end)
 end;

fun dest_listT (Type ("List.list", [T])) = T;

fun decomp_genreif da cgns (t,ctxt) =
 let 
  val thy = ProofContext.theory_of ctxt 
  val cert = cterm_of thy
  fun tryabsdecomp (s,ctxt) = 
   (case s of 
     Abs(xn,xT,ta) => 
     (let
       val ([xn],ctxt') = Variable.variant_fixes ["x"] ctxt
       val (xn,ta) = variant_abs (xn,xT,ta)
       val x = Free(xn,xT)
       val _ = (case AList.lookup (op =) (!bds) (HOLogic.listT xT)
		 of NONE => error "tryabsdecomp: Type not found in the Environement"
		  | SOME (bsT,atsT) => 
		    (bds := AList.update (op =) (HOLogic.listT xT, ((x::bsT), atsT)) (!bds)))
      in ([(ta, ctxt')] , 
	  fn [th] => ((let val (bsT,asT) = the(AList.lookup (op =) (!bds) (HOLogic.listT xT))
		       in (bds := AList.update (op =) (HOLogic.listT xT,(tl bsT,asT)) (!bds))
		       end) ; 
		      hd (Variable.export ctxt' ctxt [(forall_intr (cert x) th) COMP allI])))
	end)
    | _ => da (s,ctxt))
  in 
  (case cgns of 
    [] => tryabsdecomp (t,ctxt)
  | ((vns,cong)::congs) => ((let
        val cert = cterm_of thy
	val certy = ctyp_of thy
        val (tyenv, tmenv) =
        Pattern.match thy
        ((fst o HOLogic.dest_eq o HOLogic.dest_Trueprop) (concl_of cong), t)
        (Envir.type_env (Envir.empty 0),Term.Vartab.empty)
        val (fnvs,invs) = List.partition (fn ((vn,_),_) => vn mem vns) (Vartab.dest tmenv)
        val (fts,its) = 
	    (map (snd o snd) fnvs,
             map (fn ((vn,vi),(tT,t)) => (cert(Var ((vn,vi),tT)), cert t)) invs)
	val ctyenv = map (fn ((vn,vi),(s,ty)) => (certy (TVar((vn,vi),s)), certy ty)) (Vartab.dest tyenv)
    in (fts ~~ (replicate (length fts) ctxt), FWD (instantiate (ctyenv, its) cong))
    end)
      handle MATCH => decomp_genreif da congs (t,ctxt)))
  end;
          (* looks for the atoms equation and instantiates it with the right number *)

fun mk_decompatom eqs (t,ctxt) =
 let 
  val tT = fastype_of t
  fun isat eq = 
   let 
    val rhs = eq |> prop_of |> HOLogic.dest_Trueprop |> HOLogic.dest_eq |> snd
   in exists_Const 
	  (fn (n,ty) => n="List.nth" 
			andalso 
			AList.defined (op =) (!bds) (domain_type ty)) rhs 
	  andalso fastype_of rhs = tT
   end
  fun get_nth t = 
   case t of
     Const("List.nth",_)$vs$n => (t,vs,n)
   | t1$t2 => (get_nth t1 handle REIF "get_nth" => get_nth t2)
   | Abs(_,_,t') => get_nth t'
   | _ => raise REIF "get_nth"
  val ([xn,vsn],ctxt') = Variable.variant_fixes ["x","vs"] ctxt
  val thy = ProofContext.theory_of ctxt'
  val cert = cterm_of thy
  fun tryeqs [] = raise REIF "Can not find the atoms equation"
    | tryeqs (eq::eqs) = ((
      let 
       val rhs = eq |> prop_of |> HOLogic.dest_Trueprop 
			  |> HOLogic.dest_eq |> snd
       val (nt,vs,n) = get_nth rhs
       val ntT = fastype_of nt
       val ntlT = HOLogic.listT ntT
       val (bsT,asT) = the (AList.lookup (op =) (!bds) ntlT) 
       val x = Var ((xn,0),ntT)
       val rhs_P = subst_free [(nt,x)] rhs
       val (_, tmenv) = Pattern.match 
			    thy (rhs_P, t)
			    (Envir.type_env (Envir.empty 0),Term.Vartab.empty)
       val tml = Vartab.dest tmenv
       val SOME (_,t') = AList.lookup (op =) tml (xn,0)
       val cvs = 
	   cert (fold_rev (fn x => fn xs => Const("List.list.Cons", ntT --> ntlT --> ntlT)$x$xs)
		       bsT (Free (vsn, ntlT)))
       val cts = map (fn ((vn,vi),(tT,t)) => (cert(Var ((vn,vi),tT)), cert t)) 
		     (AList.delete (op =) (xn,0) tml)
       val th = (instantiate 
		     ([],
		      [(cert vs, cvs),(cert n, t' |> index_of |> IntInf.fromInt |> HOLogic.mk_nat |> cert)]
		      @cts) eq) RS sym
      in hd (Variable.export ctxt' ctxt [th])
      end)
	  handle MATCH => tryeqs eqs)
 in ([], fn _ => tryeqs (filter isat eqs))
 end;

(* 
fun mk_decompatom eqs (t,ctxt) =
 let 
  val tT = fastype_of t
  val tlT = HOLogic.listT tT
  val (bsT,asT) = (the (AList.lookup (op =) (!bds) tlT) 
		   handle Option => error "mk_decompatom: Type not found in the env.")
  fun isateq (_$_$(Const("List.nth",_)$vs$_)) = (fastype_of vs = tlT)
    | isateq _ = false
 in case List.find (isateq o HOLogic.dest_Trueprop o prop_of) eqs of
     NONE => raise REIF "Can not find the atoms equation"
   | SOME th =>
     ([],
      fn ths =>
        let 
         val ([x], ctxt') = Variable.variant_fixes ["vs"] ctxt
	 val cert = cterm_of (ProofContext.theory_of ctxt')
         val (Const("List.nth",_)$(vs as Var((vsn,vsi),_))$n) = 
             (snd o HOLogic.dest_eq o HOLogic.dest_Trueprop o prop_of) th
         val cvs = 
	     cert (foldr (fn (x,xs) => Const("List.list.Cons", tT --> tlT --> tlT)$x$xs) 
			 (Free(x,tlT)) bsT)
	 val th' = (instantiate ([],
				 [(cert vs, cvs), 
				  (cert n, cert  (HOLogic.mk_nat(index_of t)))]) th)
		       RS sym
        in hd (Variable.export ctxt' ctxt [th']) end)
 end;
*)
  (* Generic reification procedure: *)
  (* creates all needed cong rules and then just uses the theorem synthesis *)

fun mk_congs ctxt raw_eqs = 
 let 
  val fs = fold_rev (fn eq =>
		     insert (op =) (eq |> prop_of |> HOLogic.dest_Trueprop 
			 |> HOLogic.dest_eq |> fst |> strip_comb 
			 |> fst)) raw_eqs []
  val tys = fold_rev (fn f => fn ts => (f |> fastype_of |> binder_types |> split_last |> fst) 
				    union ts) fs []
  val _ = bds := AList.make (fn _ => ([],[])) tys
  val (vs, ctxt') = Variable.variant_fixes (replicate (length tys) "vs") ctxt
  val thy = ProofContext.theory_of ctxt'
  val cert = cterm_of thy
  val vstys = map (fn (t,v) => (t,SOME (cert (Free(v,t))))) 
		  (tys ~~ vs)
  fun insteq eq ts = 
   let val itms = map (fn t => t|> (AList.lookup (op =) vstys) |> the) ts
   in instantiate' [] itms eq
   end
  val eqs = map (fn eq => eq |> prop_of |> HOLogic.dest_Trueprop 
			     |> HOLogic.dest_eq |> fst |> strip_comb |> fst |> fastype_of 
			     |> binder_types |> split_last |> fst 
			     |> (insteq eq)) raw_eqs
  val (ps,congs) = split_list (map (mk_congeq ctxt' fs) eqs)
 in  ps ~~ (Variable.export ctxt' ctxt congs)
 end;

fun genreif ctxt raw_eqs t =
 let 
  val _ = bds := []
  val congs = mk_congs ctxt raw_eqs
  val th = divide_and_conquer (decomp_genreif (mk_decompatom raw_eqs) congs) (t,ctxt)
  val tys = th |> prop_of |> HOLogic.dest_Trueprop |> HOLogic.dest_eq |> snd
	       |> strip_comb |> fst |> fastype_of |> strip_type |> fst
	       |> split_last |> fst
  val cert = cterm_of (ProofContext.theory_of ctxt)
  val cvs = map (fn t => t |> (AList.lookup (op =) (!bds)) |> the |> snd 
			   |> HOLogic.mk_list (dest_listT t) |> cert |> SOME)
		tys
  val th' = (instantiate' [] cvs (th RS sym)) RS sym
  val t' = (fst o HOLogic.dest_eq o HOLogic.dest_Trueprop o prop_of) th'
  val th'' = Goal.prove ctxt [] [] (HOLogic.mk_Trueprop (HOLogic.mk_eq (t, t')))
			(fn _ => Simp_tac 1)
  val _ = bds := []
 in FWD trans [th'',th']
 end;

fun genreflect ctxt conv corr_thm raw_eqs t =
    let val th = FWD trans [genreif ctxt raw_eqs t, corr_thm RS sym]
        val ft = (snd o Thm.dest_comb o snd o Thm.dest_comb o snd o Thm.dest_comb o cprop_of) th
        val rth = conv ft
    in simplify (HOL_basic_ss addsimps raw_eqs addsimps [nth_Cons_0, nth_Cons_Suc])
                (simplify (HOL_basic_ss addsimps [rth]) th)
    end

fun genreify_tac ctxt eqs to i = (fn st =>
  let
    val P = HOLogic.dest_Trueprop (List.nth (prems_of st, i - 1))
    val t = (case to of NONE => P | SOME x => x)
    val th = (genreif ctxt eqs t) RS ssubst
  in rtac th i st
  end);

    (* Reflection calls reification and uses the correctness *)
        (* theorem assumed to be the dead of the list *)
fun gen_reflection_tac ctxt conv (corr_thm :: raw_eqs) to i = (fn st =>
  let
    val P = HOLogic.dest_Trueprop (nth (prems_of st) (i - 1));
    val t = the_default P to;
    val th = genreflect ctxt conv corr_thm raw_eqs t
      RS ssubst;
  in rtac th i st end);

fun reflection_tac ctxt = gen_reflection_tac ctxt NBE.normalization_conv;

end
