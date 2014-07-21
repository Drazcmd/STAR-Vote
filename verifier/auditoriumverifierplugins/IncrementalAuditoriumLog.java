/**
  * This file is part of VoteBox.
  * 
  * VoteBox is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License version 3 as published by
  * the Free Software Foundation.
  * 
  * You should have received a copy of the GNU General Public License
  * along with VoteBox, found in the root of any distribution or
  * repository containing all or part of VoteBox.
  * 
  * THIS SOFTWARE IS PROVIDED BY WILLIAM MARSH RICE UNIVERSITY, HOUSTON,
  * TX AND IS PROVIDED 'AS IS' AND WITHOUT ANY EXPRESS, IMPLIED OR
  * STATUTORY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, WARRANTIES OF
  * ACCURACY, COMPLETENESS, AND NONINFRINGEMENT.  THE SOFTWARE USER SHALL
  * INDEMNIFY, DEFEND AND HOLD HARMLESS RICE UNIVERSITY AND ITS FACULTY,
  * STAFF AND STUDENTS FROM ANY AND ALL CLAIMS, ACTIONS, DAMAGES, LOSSES,
  * LIABILITIES, COSTS AND EXPENSES, INCLUDING ATTORNEYS' FEES AND COURT
  * COSTS, DIRECTLY OR INDIRECTLY ARISING OUR OF OR IN CONNECTION WITH
  * ACCESS OR USE OF THE SOFTWARE.
 */

package verifier.auditoriumverifierplugins;

import auditorium.IncorrectFormatException;
import auditorium.Message;
import sexpression.ASExpression;
import verifier.*;
import verifier.value.DAGValue;
import verifier.value.Expression;
import verifier.value.SetValue;
import verifier.value.Value;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This verifier plugin maintains all-set and all-dag based on incremental log
 * data given via this API (rather than read from a file). It also registers the
 * signature-verify primitive.
 * 
 * @author kyle
 * 
 */
public class IncrementalAuditoriumLog implements IIncrementalPlugin {

	private Verifier _verifier;
	private ArrayList<Expression> _allset;
	private SetValue _allsetValue;
	private DagBuilder _alldag;
	private DAGValue _alldagValue;
    private HashChainVerifier hashChainVerifier;

    public IncrementalAuditoriumLog(){}

    public IncrementalAuditoriumLog(HashChainVerifier hashChainVerifier) {
        this.hashChainVerifier = hashChainVerifier;
    }

	/**
	 * @see verifier.IVerifierPlugin#init(verifier.Verifier)
	 */
	public void init(Verifier verifier) {
		_allset = new ArrayList<>();
		_alldag = new DagBuilder();
		_verifier = verifier;

        hashChainVerifier.init(verifier);

		registerHandlers();
		registerGlobals();
	}

	/**
	 * Add incremental log data.
	 * 
	 * @param entry Message to append to the log.
	 */
	public void addLogData(Message entry) {

        try {
            hashChainVerifier.verifyIncremental(entry);
        } catch (HashChainCompromisedException e) {
            /* TODO We should probably not throw a runtime exception, but some how note the hash chain was compromised. */
            throw new RuntimeException(e);
        }
        _allset.add(new Expression(entry.toASE()));
		_alldag.add(entry);
		registerGlobals();
	}

	/**
	 * Add incremental log data.
	 * 
	 * @param entry S-expression representing a message to append to the log.
	 */
	public void addLogData(ASExpression entry) throws InvalidLogEntryException {
		try {
			_allset.add(new Expression(entry));
			_alldag.add(new Message(entry));
			registerGlobals();
		} catch (IncorrectFormatException e) { throw new InvalidLogEntryException(e); }
	}

	/**
	 * Add incremental log data.
	 * 
	 * @param entry Expression value representing a message to append to the log.
	 */
	public void addLogData(Expression entry) throws InvalidLogEntryException {
		try {
			_allset.add(entry);
			_alldag.add(new Message(entry.getASE()));
			registerGlobals();
		} catch (IncorrectFormatException e) { throw new InvalidLogEntryException(e); }
	}

	/**
	 * Seal the logs -- the verifier will no longer return a reduction now. If
	 * something isn't in the log the verifier will expect that it never will
	 * be.
	 */
	public void closeLog() {
		_allsetValue.seal();
		_alldagValue.seal();
	}

    /**
     * Initialization of global variables.
     */
	private void registerGlobals() {
		HashMap<String, Value> bindings = new HashMap<>();

		_allsetValue = new SetValue(_allset.toArray(new Expression[_allset.size()]));
		bindings.put("all-set", _allsetValue);
		_alldagValue = _alldag.toDAG();

		/* TODO XXX: hack: this is the only way I could figure to pass along an outer tuning parameter to an interior data structure. I think we
		   TODO should work on standardizing this, that is, figure out which class holds the arguments for everyone. Verifier's as good a choice as
		   TODO any. [10/09/2007 11:37 dsandler] */
        /* If the key is present, toggle based on the value */
		if (_verifier.getArgs().containsKey("dagcache")) {
			if (Boolean.parseBoolean(_verifier.getArgs().get("dagcache")))
				_alldagValue.enableCache();
			else
				_alldagValue.disableCache();
		}

		bindings.put("all-dag", _alldagValue);

		ActivationRecord.END.setBindings(bindings);
	}

    /**
     * Registers handlers for this IncrementalAuditoriumLog
     */
	private void registerHandlers() {
		_verifier.getPrimitiveFactories().put("signature-verify", SignatureVerify.FACTORY);
	}
}