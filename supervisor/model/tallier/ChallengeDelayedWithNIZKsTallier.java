package supervisor.model.tallier;

import java.io.ByteArrayInputStream;
import java.util.*;

import auditorium.Bugout;

import crypto.adder.*;

import sexpression.ASExpression;
import sexpression.ListExpression;
import sexpression.stream.ASEInputStreamReader;
import crypto.interop.AdderKeyManipulator;

/**
 * Tallier for elections with both NIZKs and the challenge-commit model enabled.
 *
 * @author Montrose
 */
public class ChallengeDelayedWithNIZKsTallier extends EncryptedTallierWithNIZKs {
    private List<String> writeIns;

	private Map<ASExpression, byte[]> _pendingVotes = new HashMap<ASExpression, byte[]>();

    /**
     * Constructor.
     *
     * @param pub  - The PublicKey used to encrypt votes to be tallied.
     * @param priv - The PrivateKey to be used to decrypt the totals.
     */
    public ChallengeDelayedWithNIZKsTallier(PublicKey pub, PrivateKey priv) {
        super(pub, priv);
    }

    /**
     * Analogous to
     * @see supervisor.model.tallier.EncryptedTallierWithNIZKs#recordVotes(byte[], sexpression.ASExpression)
     *
     * Performs the same operations, just not when the vote is first recorded. This will happen
     * when a ballot is dropped into the ballot box.
     */
	public void confirmed(ASExpression nonce) {
		byte[] ballotBytes = _pendingVotes.remove(nonce);
		
		if(_finalPublicKey == null)
			_finalPublicKey = AdderKeyManipulator.generateFinalPublicKey(_publicKey);
		else{
			PublicKey copy = AdderKeyManipulator.generateFinalPublicKey(_publicKey);
			
			if(!_finalPublicKey.equals(copy))
				Bugout.err("Final public key changed!\n"+_finalPublicKey+"\n\n"+copy);
		}

		ASEInputStreamReader in = new ASEInputStreamReader(
				new ByteArrayInputStream(ballotBytes));
		
		try {
			ASExpression sexp = in.read();
			/* Check that the ballot is well-formed */
			ListExpression ballot = (ListExpression)sexp;

            System.out.println("SExpression okay!");

            /* TODO This is writein code that doesn't really work */
            /* Pop the key "vote" off the end of each ballot
             * ASExpression writeInKey = ballot.get(ballot.size() - 1);
             * ballot = new ListExpression(Arrays.copyOfRange(ballot.getArray(), 0, ballot.getArray().length - 2));
             *
             * byte[] key = parseKey(writeInKey);
             */

            for(int i = 0; i < ballot.size(); i++){
				ListExpression raceGroup = (ListExpression)ballot.get(i);

                /* TODO Split off any writeIns, if they're present, and store them, encrypted, in a list to be dealt with later
				ListExpression wholeVote = (ListExpression)raceGroup.get(0);
                String[] voteParts = wholeVote.toString().split("`");
                ListExpression voteE = new ListExpression(voteParts[0]);

                If there is a write in keep track of it
                if(voteParts.length > 1)
                    writeIns.add(voteParts[1]);

                */

                ListExpression voteE = (ListExpression)raceGroup.get(0);
				ListExpression voteIdsE = (ListExpression)raceGroup.get(1);
				ListExpression proofE = (ListExpression)raceGroup.get(2);
				ListExpression publicKeyE = (ListExpression)raceGroup.get(3);

                confirmValid(voteE, voteIdsE, proofE, publicKeyE);
				
				//Vote vote = Vote.fromString(voteE.get(1).toString());
				Vote vote = Vote.fromASE(voteE.get(1));
				List<String> voteIds = new ArrayList<String>();
				for(int j = 0; j < voteIdsE.get(1).size(); j++)
					voteIds.add(((ListExpression)voteIdsE.get(1)).get(j).toString());
				
				//VoteProof voteProof = VoteProof.fromString(proofE.get(1).toString());
				VoteProof voteProof = VoteProof.fromASE(proofE.get(1));
				
				//PublicKey suppliedPublicKey = PublicKey.fromString(publicKeyE.get(1).toString());
				PublicKey suppliedPublicKey = PublicKey.fromASE(publicKeyE.get(1));
				
				if(!(suppliedPublicKey.toString().trim().equals(_finalPublicKey.toString().trim()))){
					Bugout.err("!!!Expected supplied final PublicKey to match generated\nSupplied: "+suppliedPublicKey+"\nGenerated: "+_finalPublicKey+"!!!");
					return;
				}
				
				if(!voteProof.verify(vote, _finalPublicKey, 0, 1)){
					Bugout.err("!!!Ballot failed NIZK test!!!");
					return;
				}
				
				String subElectionId = makeId(voteIds);

				Election election = _results.get(subElectionId);
				
				if(election == null)
					election = new Election(_publicKey.getP());
				
				election.castVote(vote);
				
				_results.put(subElectionId, election);
			}//for
		}catch(Exception e){
            e.printStackTrace();
            Bugout.err("Malformed ballot received <"+e.getMessage()+">");
			Bugout.err("Rejected ballot:\n"+new String(ballotBytes));
		}
    }

    /* TODO More write-in code
    private byte[] parseKey(ASExpression writeInKey) {
        ElgamalCiphertext keyCipher = ElgamalCiphertext.fromASE(writeInKey);

        byte[] key = _privateKey.decrypt(keyCipher).bigintValue().toByteArray();

        return key;

    }
    */

    /**
     * Put new votes in a list until they get confirmed, i.e. cast.
     *
     * @see supervisor.model.tallier.ITallier#recordVotes(byte[], sexpression.ASExpression)
     *
     * @param ballot the ballot to confirm
     * @param nonce the nonce associated with that ballot, to avoid bogus confirms
     */
	public void recordVotes(byte[] ballot, ASExpression nonce) {
		_pendingVotes.put(nonce, ballot);
	}

}