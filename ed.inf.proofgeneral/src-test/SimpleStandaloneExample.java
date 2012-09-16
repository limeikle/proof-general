/*
 * SimpleStandaloneExample
 *
 * Created by alex, 03-May-2005.
 */

// FIXME da: this has compile errors since utils was shifted about, and
// also some static variables have been changed into preferences.
// Can probably be revived easily.

//import java.io.FileReader;
//import java.util.ArrayList;
//
//import ed.inf.proofgeneral.ProofGeneralPlugin;
//import ed.inf.proofgeneral.sessionmanager.CommandQueuedWithValues;
//import ed.inf.proofgeneral.sessionmanager.ScriptingException;
//import ed.inf.proofgeneral.standalone.ProverCommandResponse;
//import ed.inf.proofgeneral.standalone.ProverStandalone;
//import ed.inf.utils.eclipse.XmlPreferenceStore;
//import ed.inf.utils.*;

/**
 * SimpleStandaloneExample
 *
 */
//public class SimpleStandaloneExample {
//
//	static {
//		//ProofGeneralPlugin.LOG_PROVER_IO = false;
//	}
//
//	public static void main(String[] args) {
//		//ProofGeneralPlugin.LOG_PROVER_IO = false;
//		XmlPreferenceStore myPrefs = null;
//		try {
//			myPrefs = new XmlPreferenceStore();
//			myPrefs.setFromReader(new FileReader("config/IsabellePrefs.xml"));
//		} catch (Exception e) {
//			System.err.println("error loading preferences from 'config/IsabellePrefs.xml', using defaults (may be out of date); "+e);
//			myPrefs = null;
//		}
//		ProverStandalone prover = ProverStandalone.createIsabelleWithLogic("HOL", myPrefs);
//		// ProofGeneralPlugin.PROVER_KNOWLEDGE_GRAB_ENABLED = false;
//
//		try {
////			ProofGeneralPlugin.LOG_EVENTS_RUNNING = true;
////			ProofGeneralPlugin.LOG_EVENTS_FIRING = true;
////			ProofGeneralPlugin.LOG_PROVER_IO = true;
//
//		  ProverCommandResponse cmd = prover.sendCommand("theory Test = Main:", true);
//		  System.out.println("STARTED THEORY");
//		  System.out.println(cmd.firstIncoming.parseTree.getStringValue());
//		  System.out.println();
//
//			boolean log = true;
//			sendWithDetails("constdefs "+
//			  "IsInterval :: \"(nat=>bool)=>bool\" "+
//			  "\"IsInterval R == (\\<forall>a b c. (((a<b) \\<and> (b<c) \\<and> (R a) \\<and> (R c)) --> (R b)))\"",
//				prover, log);
//			sendWithDetails("lemma IsInterval1: \"[| IsInterval R ; R x ; R y ; x<a ; a<y|] ==> R a\"",
//					prover, log);
//
//			//			(* I am obliged to do this *)
////			apply (simp (no_asm_use) only: IsInterval_def)
////			(* because this loops infinitely
////			apply (simp only: IsInterval_def)
////			*)
////
//
////			sendWithDetails("apply (simp only: IsInterval_def)", prover, log);
////			System.out.println("(should have looped)");
//
//			//TODO this 'interrupt' example should me moved to a separte method as it isn't vital, other things are more interesting (like good proof below)
//			//General.sleep(3000);
//			//ProofGeneralPlugin.LOG_PROVER_IO = true;  //will show lots of responses from infinite simp loop
//			System.out.println("sending a command that will loop infinitely (simp only: IsInterval_def); just to demo 'interrupt' capabilities");
//			cmd = prover.queueCommand("apply (simp only: IsInterval_def)", log);						//an infinite simp loop
//			General.sleep(100);
//			System.out.println("command finished="+cmd.isFinished()+"; prover busy="+prover.sm.proverState.isBusy());
//			if (cmd.errorEvent!=null) {
//				System.out.println("   GOT (as error): "+cmd.errorEvent.parseTree.asXML());
//			}
//			if (cmd.firstIncoming!=null) {
//				System.out.println("   GOT: "+cmd.firstIncoming.parseTree.asXML());
//			}
//			System.out.println(General.makeDateString()+"  interrupt");
//			prover.sm.sendInterrupt();  //but be caseful with this, it might kill isabelle if isabelle isn't busy!
//
//			System.out.println(General.makeDateString()+"  command finished="+cmd.isFinished()+"; prover busy="+prover.sm.proverState.isBusy());
//			System.out.println("now wait for prover i/o to be read completely ("+prover.sm.getFiringQueueInfo()[0]+" events so far)");
//			prover.waitForAvailable(false);   //let pending commands complete; but not necessarily events
//			//ProofGeneralPlugin.LOG_PROVER_IO = true;
//			//ProofGeneralPlugin.LOG_EVENTS_FIRING = true;
//			//ProofGeneralPlugin.LOG_EVENTS_RUNNING = true;
//
//			System.out.println(General.makeDateString()+"  command finished="+cmd.isFinished()+"; prover busy="+prover.sm.proverState.isBusy());
//			System.out.println("command probably won't have finished, but the prover should no longer busy.");
//			System.out.println("however our loop command may have spawned lots of events which still have to be processed");
//			System.out.println("("+prover.sm.getFiringQueueInfo()[1]+" messages generated, of which "+prover.sm.getFiringQueueInfo()[0]+" have been (or are being) handled)");
//			System.out.println("so we wait (up to 30s) on prover");
//			prover.waitForAvailable(true, 30*1000);
//			System.out.println(General.makeDateString()+"  prover caught up (or timed out) [# events = "+prover.sm.getFiringQueueInfo()[1]+"/"+prover.sm.getFiringQueueInfo()[0]+"]");
//
//
//			System.out.println();
//			System.out.println(General.makeDateString()+"  continuing");
//			sendWithDetails("apply (simp (no_asm_use) only: IsInterval_def)", prover, log);
//			System.out.println(General.makeDateString()+"  did it!");
//			sendWithDetails("apply (drule_tac x=x in spec)", prover, log);
//			sendWithDetails("apply (drule_tac x=a in spec)", prover, log);
//			sendWithDetails("apply (drule_tac x=y in spec)", prover, log);
//			sendWithDetails("apply simp", prover, log);
//			sendWithDetails("done", prover, log);
//			System.out.println(General.makeDateString()+"  DONE proof");
//
////			KnowledgeItem ki = prover.sm.proverState.getProverKnowledge().getItem("IsInterval1");
////			if (ki!=null) System.out.println("KNOWLEDGE: "+ki.name+", "+ki.getStatementHtml());
////			System.out.println();
////			General.sleep(100);
////			ki = prover.sm.proverState.getProverKnowledge().getItem("IsInterval1");
////			if (ki!=null) System.out.println("KNOWLEDGE: "+ki.name+", "+ki.getStatementHtml());
////
////			//sendWithDetails("apply assumption", prover, log);
////
//////			simpleProof(prover, true);
////		  System.out.println();
////
////		  //multiSendTest(prover);
////		  //General.sleep(3200);  //just enough time to get prover knowledge loaded
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			try {
//			  prover.waitForAvailable();   //let anything pending complete
//			} catch (InterruptedException e) {}
//			prover.dispose();
//			ThreadPool.get().closeAll();
//			System.out.println(General.makeDateString()+"  disposed of prover.");
//		}
//	}
//
//	/**
//	 * @param prover
//	 * @throws ScriptingException
//	 * @throws InterruptedException
//	 */
//	public static void multiSendTest(ProverStandalone prover) throws ScriptingException, InterruptedException {
//		ProverCommandResponse cmd;
//
//	  long t0 = System.currentTimeMillis();
//	  cmd = prover.sendCommand("ML {* commas (map fst (thms_of (theory \"HOL\"))) *}", false);
//	  System.out.println("GOT in "+General.makeTimeString(System.currentTimeMillis()-t0)+"\n"+
//	  		(cmd.errorEvent!=null ? cmd.errorEvent.parseTree.getStringValue() :
//	  			cmd.firstIncoming.parseTree.getStringValue()));
//	  System.out.println();
//
//	  t0 = System.currentTimeMillis();
//	  cmd = prover.sendCommand("ML {* commas (map fst (thms_of (theory \"HOL\"))) *}", false);
//	  System.out.println("GOT in "+General.makeTimeString(System.currentTimeMillis()-t0)+"\n"+
//	  		(cmd.errorEvent!=null ? cmd.errorEvent.parseTree.getStringValue() :
//	  			cmd.firstIncoming.parseTree.getStringValue()));
//	  System.out.println();
//
//	  t0 = System.currentTimeMillis();
//	  cmd = prover.sendCommand("ML {* \"hello world\" *}", false);
//	  System.out.println("GOT in "+General.makeTimeString(System.currentTimeMillis()-t0)+"\n"+
//	  		(cmd.errorEvent!=null ? cmd.errorEvent.parseTree.getStringValue() :
//	  			cmd.firstIncoming.parseTree.getStringValue()));
//	  System.out.println();
//
//		ProofGeneralPlugin.LOG_PROVER_IO = true;
//		  t0 = System.currentTimeMillis();
//		  cmd = new ProverCommandResponse("ML {* commas "+
//		  		"(map (Pretty.string_of o Display.pretty_thm_no_quote o snd) (thms_of (theory \"HOL\")))"
//		  		//"(map fst (thms_of (theory \"HOL\")))"
//		  		+" *}");
//		  cmd.commandElement = prover.parseCommandWaiting(cmd.commandString);
//		  System.out.println("PARSED 1 in "+General.makeTimeString(System.currentTimeMillis()-t0));
//
//		  long t1 = System.currentTimeMillis();
//		  final ProverCommandResponse cmd2 = new ProverCommandResponse("ML {* \"hello world\" *}");
//		  cmd2.commandElement = prover.parseCommandWaiting(cmd2.commandString);
//		  System.out.println("PARSED 2 in "+General.makeTimeString(System.currentTimeMillis()-t1));
//		  final ProverCommandResponse cmd3 = new ProverCommandResponse("ML {* \"hello other world\" *}");
//		  cmd3.commandElement = prover.parseCommandWaiting(cmd3.commandString);
//		  System.out.println("PARSED 3 in "+General.makeTimeString(System.currentTimeMillis()-t1));
//
//		  final long t2 = System.currentTimeMillis();
//
//			prover.sm.sendCommand(new CommandQueuedWithValues(prover.sm, cmd2.commandElement, cmd2,
//					null, Boolean.FALSE, new MutableObject(prover.myProverListener)) {
//				public void onSend() {
//					//System.out.println(General.makeTimeString(System.currentTimeMillis()-t2)+"  starting "+cmd2.commandString);
//					cmd2.events = new ArrayList();
//				}
//			});
//		  prover.queueCommand(cmd, false);
//		  //odd 500ms delay to here
//			prover.sm.sendCommand(new CommandQueuedWithValues(prover.sm, cmd3.commandElement, cmd3,
//					null, Boolean.FALSE, new MutableObject(prover.myProverListener)) {
//				public void onSend() {
//					//System.out.println(General.makeTimeString(System.currentTimeMillis()-t2)+"  starting "+cmd2.commandString);
//					cmd3.events = new ArrayList();
//				}
//			});
//
//		  //prover.sendCommand(cmd2);
//		  while (System.currentTimeMillis()-t2<4000) {
//		  	System.out.println(
//		  			General.makePaddedString(General.makeTimeString(System.currentTimeMillis()-t2), 8, ' ')+"  "+
//						(!cmd.isParsed() ? "X" : (!cmd.isFinished() ? "P" : "F")) + "  " +
//						(!cmd2.isParsed() ? "X" : (!cmd2.isFinished() ? "P" : "F")) );
//		  	Thread.sleep(100);
//		  }
////		  t0 = System.currentTimeMillis();
////		  cmd = prover.queueCommand("ML {* commas (map fst (thms_of (theory \"HOL\"))) *}");
////		  System.out.println("QUEUED 1 in "+General.makeTimeString(System.currentTimeMillis()-t0));
////		  long t1 = System.currentTimeMillis();
////		  ProverCommandResponse cmd2 = prover.queueCommand("ML {* \"hello world\" *}");
////		  System.out.println("QUEUED 2 in "+General.makeTimeString(System.currentTimeMillis()-t1));
////		  long t2 = System.currentTimeMillis();
////		  while (System.currentTimeMillis()-t2<300) {
////		  	System.out.println(
////		  			General.makePaddedString(General.makeTimeString(System.currentTimeMillis()-t2), 10, ' ')+
////						(!cmd.isParsed() ? "X" : (!cmd.isFinished() ? "P" : "F")) + "  " +
////						(!cmd2.isParsed() ? "X" : (!cmd2.isFinished() ? "P" : "F")) );
////		  }
//
//		  System.out.println("GOT in "+General.makeTimeString(System.currentTimeMillis()-t0)+"\n"+
//		  		(cmd.errorEvent!=null ? cmd.errorEvent.parseTree.getStringValue() :
//		  			cmd.firstIncoming.parseTree.getStringValue()));
//		  System.out.println();
//
//		  //ProverCommandResponse slowCmd = prover.sendCommand("");
//
//	}
//
//	public static void sendWithDetails(String command, ProverStandalone prover, boolean log) {
//		System.out.println("SENDING: "+command);
//		try {
//			ProverCommandResponse cmd = prover.sendCommand(command, log);
//			if (cmd.errorEvent==null) {
//				if (cmd.firstIncoming==null) {
//					System.out.println("    GOT:    (command processed without response)");
//				} else {
//					System.out.println("    GOT:    "+cmd.firstIncoming.parseTree.getStringValue());
//				}
//			} else {
//				System.out.println("  ERROR:    "+cmd.errorEvent.parseTree.getStringValue());
//			}
//		} catch (InterruptedException e) {
//			System.out.println("  ERROR:    (interrupted)");
//		} catch (ScriptingException e) {
//			System.out.println("  ERROR:    "+e);
//		}
//	}
//
//	public static void simpleProof(ProverStandalone prover, boolean log) {
//		sendWithDetails("lemma a_b_sym: \"a = b ==> b = a\"", prover, log);
//		sendWithDetails("apply (rule sym)", prover, log);
//		sendWithDetails("done", prover, log);   //TODO if we send this, it can't parse the next line ???
//		sendWithDetails("apply assumption", prover, log);
//		sendWithDetails("done", prover, log);
//	}
//}
//
//
///*
//
//<pgip class="pa" id="PG-Eclipse" seq="2"><opentheory thyname="Test" parentnames="Main;">theory Test = Main:</opentheory></pgip>
//<pgip class="pa" id="PG-Eclipse" seq="4"><theoryitem name="IsInterval" objtype="term">constdefs IsInterval :: "(nat=&gt;bool)=&gt;bool" "IsInterval R == (\&lt;forall&gt;a b c. (((a&lt;b) \&lt;and&gt; (b&lt;c) \&lt;and&gt; (R a) \&lt;and&gt; (R c)) --&gt; (R b)))"</theoryitem></pgip>
//<pgip class="pa" id="PG-Eclipse" seq="6"><opengoal thmname="IsInterval1">lemma IsInterval1: "[| IsInterval R ; R x ; R y ; x&lt;a ; a&lt;y|] ==&gt; R a"</opengoal></pgip>
//<pgip class="pa" id="PG-Eclipse" seq="8"><proofstep>apply (simp only: IsInterval_def)</proofstep></pgip>
//
//<pgip class="pa" id="PG-Eclipse" seq="9"><interruptprover/></pgip>
//
//*/